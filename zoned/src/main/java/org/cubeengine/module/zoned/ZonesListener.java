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
package org.cubeengine.module.zoned;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.block.BlockTypes.AIR;

import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

public class ZonesListener
{
    private Map<UUID, ZoneConfig> zonesByPlayer = new HashMap<>();

    private I18n i18n;
    private Permission selectPerm;
    private Reflector reflector;

    @Inject
    public ZonesListener(I18n i18n, PermissionManager pm, Reflector reflector)
    {
        this.i18n = i18n;
        selectPerm = pm.register(Zoned.class, "use-tool", "Allows using the selector tool",null);
        this.reflector = reflector;
    }

    @Listener
    public void onInteract(InteractBlockEvent event, @First Player player)
    {
        if (!(event instanceof InteractBlockEvent.Primary.MainHand) && !(event instanceof InteractBlockEvent.Secondary.MainHand))
        {
            return;
        }

        if (event.getTargetBlock() == BlockSnapshot.NONE)
        {
            return;
        }
        Location<World> block = event.getTargetBlock().getLocation().get();
        if ((int)block.getPosition().length() == 0 || block.getBlockType() == AIR)
        {
            return;
        }
        if (!SelectionTool.inHand(player))
        {
            return;
        }


        if (!player.hasPermission(selectPerm.getId()))
        {
            return;
        }

        ZoneConfig config = getZone(player);
        if (config.world == null)
        {
            config.world = new ConfigWorld(player.getWorld());
        }
        else
        {
            if (config.world.getWorld() != player.getWorld())
            {
                i18n.send(player, NEUTRAL, "Position in new World detected. Clearing all previous Positions.");
                config.clear();
            }
        }

        Text added = config.addPoint(i18n, player, event instanceof InteractBlockEvent.Primary, block.getPosition());
        Text selected = config.getSelected(i18n, player);

        i18n.send(player, POSITIVE, "{txt} ({integer}, {integer}, {integer}). {txt}",
                added, block.getBlockX(), block.getBlockY(), block.getBlockZ(), selected);
        event.setCancelled(true);
    }

    public ZoneConfig getZone(Player player)
    {
        return zonesByPlayer.computeIfAbsent(player.getUniqueId(), k ->  reflector.create(ZoneConfig.class));
    }

    public void setZone(Player player, ZoneConfig zone)
    {
        zonesByPlayer.put(player.getUniqueId(), zone.clone(reflector));
    }
}
