/**
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
package org.cubeengine.module.vanillaplus.fix;

import org.cubeengine.module.basics.Basics;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.data.manipulator.entity.FlyingData;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.entity.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;
import org.spongepowered.api.event.inventory.InventoryClickEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.spongepowered.api.event.Order.EARLY;
import static org.spongepowered.api.event.Order.FIRST;

/**
 * Persists Player flymode on logout so they don't fall out of the sky
 * <p></p>
 * Prevents placing overstacked items into anvil or brewingstands
 */
public class FixListener
{
    private final VanillaPlus module;

    public FixListener(VanillaPlus module)
    {
        this.module = module;
    }

    public final PermissionDescription OVERSTACKED_ANVIL_AND_BREWING = getBasePerm().child("allow-overstacked-anvil-and-brewing");

    @Listener(order = EARLY)
    public void join(final ClientConnectionEvent.Join event)
    {
        // TODO set persisted flymode
        final Player player = event.getUser();
        if (player.getGameModeData().getGameMode() != GameModes.CREATIVE && module.perms().COMMAND_FLY_KEEP.isAuthorized(player))
        {
            player.offer(player.getOrCreate(FlyingData.class).get());
        }
    }

    @Subscribe(order = FIRST)
    public void quit(final PlayerQuitEvent event)
    {
        final Player player = event.getUser();
        if (player.getGameModeData().getGameMode() != GameModes.CREATIVE && player.getData(FlyingData.class).isPresent() && module.perms().COMMAND_FLY_KEEP.isAuthorized(player))
        {
            // TODO set persisted flymode
        }
    }

    @Listener
    public void onPlayerInventoryClick(ClickInventoryEvent event)
    {
        // TODO is this still needed?
        if (this.module.getConfig().preventOverstackedItems && !module.perms().OVERSTACKED_ANVIL_AND_BREWING.isAuthorized(event.getViewer()))
        {

            if (event.getView().getTopInventory() instanceof AnvilInventory
                || event.getView().getTopInventory() instanceof BrewerInventory)
            {
                boolean topClick = event.getRawSlot() < event.getView().getTopInventory().getSize();
                switch (event.getAction())
                {
                    case PLACE_ALL:
                    case PLACE_SOME:
                        if (!topClick) return;
                        if (event.getCursor().getAmount() > event.getCursor().getMaxStackSize())
                        {
                            event.setCancelled(true);
                        }
                        break;
                    case MOVE_TO_OTHER_INVENTORY:
                        if (topClick) return;
                        if (event.getCurrentItem().getAmount() > event.getCurrentItem().getMaxStackSize())
                        {
                            event.setCancelled(true);
                        }
                }
            }
        }
    }
}
