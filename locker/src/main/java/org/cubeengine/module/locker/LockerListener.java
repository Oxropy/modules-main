/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.locker;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.entity.EntityTypes.HORSE;

import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.BlockCarrier;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.inject.Inject;

public class LockerListener
{
    private final LockManager manager;
    private I18n i18n;
    private final Locker module;

    @Inject
    public LockerListener(Locker module, I18n i18n)
    {
        this.module = module;
        this.manager = module.getManager();
        this.i18n = i18n;
    }

    @Listener
    public void onPlayerInteract(InteractBlockEvent.Secondary event, @First Player player)
    {
        if (!this.module.getConfig().protectBlockFromRClick || event.getTargetBlock() == BlockSnapshot.NONE) return;

        Location<World> block = event.getTargetBlock().getLocation().get();
        Lock lock = this.manager.getValidLock(block, player);
        if (block.getTileEntity().orElse(null) instanceof Carrier)
        {
            if (!player.hasPermission(module.perms().ALLOW_CONTAINER.getId()))
            {
                i18n.send(player, NEGATIVE, "Strong magic prevents you from accessing any inventory!");
                event.setCancelled(true);
                return;
            }
            if (lock == null) return;

            // Handled by InteractInventoryEvent.Open
            Inventory inv = ((Carrier)block.getTileEntity().get()).getInventory();
            lock.handleInventoryOpen(event, inv, block, player);
        }
        else if (block.supports(Keys.OPEN))
        {
            if (!player.hasPermission(module.perms().ALLOW_DOOR.getId()))
            {
                i18n.send(player, NEGATIVE, "Strong magic prevents you from accessing any door!");
                event.setCancelled(true);
                return;
            }
            if (lock == null) return;
            lock.handleBlockDoorUse(event, player, block);
        }
        else if (lock != null) // other interact e.g. repeater
        {
            lock.handleBlockInteract(event, player);
        }
    }

    @Listener
    public void onPlayerInteractEntity(InteractEntityEvent event, @First Player player)
    {
        if (!this.module.getConfig().protectEntityFromRClick) return;
        Entity entity = event.getTargetEntity();
        if (entity instanceof Living)
        {
            if (!player.hasPermission(module.perms().ALLOW_LIVING_ENTITY.getId()))
            {
                i18n.send(player, NEGATIVE, "Strong magic prevents you from reaching this living entity!");
                event.setCancelled(true);
                return;
            }
        }
        else
        {
            if (!player.hasPermission(module.perms().ALLOW_ENTITY.getId()))
            {
                i18n.send(player, NEGATIVE, "Strong magic prevents you from reaching this entity!");
                event.setCancelled(true);
                return;
            }
        }

        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        if (entity instanceof Carrier || (entity.getType() == HORSE && player.get(Keys.IS_SNEAKING).get()))
        {
            // Handled by InteractInventoryEvent.Open
            lock.handleInventoryOpen(event, ((Carrier) entity).getInventory(), null, player);
        }
        else
        {
            lock.handleEntityInteract(event, player);
        }
    }

    @Listener
    public void onInventoryOpen(InteractInventoryEvent.Open event, @First Player player)
    {
        if (!(event.getTargetInventory() instanceof CarriedInventory)
         || !((CarriedInventory) event.getTargetInventory()).getCarrier().isPresent())
        {
            return;
        }
        Object carrier = ((CarriedInventory) event.getTargetInventory()).getCarrier().get();
        Location<World> loc = null;
        if (carrier instanceof BlockCarrier)
        {
            loc = ((BlockCarrier) carrier).getLocation();
        }
        else if (carrier instanceof Entity)
        {
            loc = ((Entity) carrier).getLocation();
        }
        Lock lock = this.manager.getLockOfInventory(((CarriedInventory) event.getTargetInventory()));
        if (lock == null) return;
        lock.handleInventoryOpen(event, event.getTargetInventory(), loc, player);
    }

    @Listener
    public void onAttackEntity(AttackEntityEvent event, @First EntityDamageSource source)
    {
        Entity target = event.getTargetEntity();
        Player player = null;
        if (source.getSource() instanceof Player)
        {
            player = ((Player) source.getSource());
        }

        if (target instanceof Living)
        {
            if (handleLiving(target, player))
            {
                event.setCancelled(true);
            }
        }
        else
        {
            if (handleHanging(target, player)) // TODO why is this not called?
            {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onEntityAttack(InteractEntityEvent.Primary event)
    {
        Entity target = event.getTargetEntity();
        if (target instanceof Living)
        {
            return;
        }
        Cause causes = event.getCause();
        Optional<Player> playerCause = causes.first(Player.class);

        if (target instanceof Boat || target instanceof Minecart) // Is Vehicle?
        {
            if (handleVehicle(target, playerCause.orElse(null)))
            {
                event.setCancelled(true);
            }
        }
        else if (target instanceof Hanging) // Is Hanging?
        {
            if (handleHanging(target, playerCause.orElse(null)))
            {
                event.setCancelled(true);
            }
        }
        else // TODO other ?
        {

        }
    }

    private boolean handleLiving(Entity target, Player player)
    {
        if (!this.module.getConfig().protectEntityFromDamage) return false;
        Lock lock = this.manager.getLockForEntityUID(target.getUniqueId());
        if (lock == null) return false; // No Lock here
        if (player != null)
        {
            return !lock.handleEntityDamage(player);
        }
        // else other source
        return module.getConfig().protectEntityFromEnvironementalDamage;
    }

    private boolean handleHanging(Entity target, Player playerCause) // Lead / ItemFrame / Image
    {
        if (playerCause != null)
        {
            if (playerCause.hasPermission(module.perms().ALLOW_HANGING.getId()))
            {
                return false;
            }
            Lock lock = this.manager.getLockForEntityUID(target.getUniqueId());
            if (lock == null) return false; // No Lock here
            return !lock.handleEntityDamage(playerCause);
        }
        return false;
    }

    private boolean handleVehicle(Entity target, Player playerCause) // Minecart / Boat
    {
        if (!this.module.getConfig().protectVehicleFromBreak) // Protect vehicles at all?
        {
            return false;
        }
        Lock lock = this.manager.getLockForEntityUID(target.getUniqueId());
        if (lock == null) return false; // No Lock here
        if (playerCause != null)
        {
            return !(lock.isOwner(playerCause) || playerCause.hasPermission(module.perms().BREAK_OTHER.getId())); // Allow when owner or has permission
        }
        return module.getConfig().protectVehicleFromEnvironmental;
    }

    @Listener
    public void onEntityDeath(DestructEntityEvent event, @First EntityDamageSource source) // Cleanup Locks in case Entity dies
    {
        Entity target = event.getTargetEntity();
        Lock lock = this.manager.getLockForEntityUID(target.getUniqueId());
        if (lock == null) return;
        Player player;
        if (source.getSource() instanceof Player)
        {
            player = ((Player)source.getSource());
        }
        else if (source instanceof IndirectEntityDamageSource && ((IndirectEntityDamageSource)source).getIndirectSource() instanceof Player)
        {
            player = ((Player)((IndirectEntityDamageSource)source).getIndirectSource());
        }
        else
        {
            return;
        }
        lock.handleEntityDeletion(player); // Delete Lock and notify user that destroyed Lock
    }
}
