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
package de.cubeisland.cubeengine.log.action.logaction.block.player;

import java.util.EnumSet;

import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.hanging.HangingPlaceEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.PLAYER;
import static org.bukkit.Material.*;

/**
 * Placing Item-Frames or Painting
 * <p>Events: {@link HangingPlaceEvent}</p>
 */
public class HangingPlace extends BlockActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(BLOCK, PLAYER);
    }

    @Override
    public String getName()
    {
        return "hanging-place";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event)
    {
        if (this.isActive(event.getEntity().getWorld()))
        {
            if (event.getEntity() instanceof ItemFrame)
            {
                this.logBlockChange(event.getEntity().getLocation(),event.getPlayer(),AIR,ITEM_FRAME,null);
            }
            else if (event.getEntity() instanceof Painting)
            {
                BlockData blockData = BlockData.of(PAINTING,(byte)((Painting)event.getEntity()).getArt().getId());
                this.logBlockChange(event.getEntity().getLocation(),event.getPlayer(),AIR,blockData,null);
            }
        }
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&6%s &agot hung up by &2%s%s&a!",
                           time, logEntry.getNewBlock(),
                            logEntry.getCauserUser().getDisplayName(),loc);
    }

    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).HANGING_PLACE_enable;
    }
}