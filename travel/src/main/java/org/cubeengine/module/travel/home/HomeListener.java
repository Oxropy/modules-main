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
package org.cubeengine.module.travel.home;

import com.google.common.base.Optional;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.mutable.entity.SneakingData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

import static org.spongepowered.api.event.Order.EARLY;

public class HomeListener
{
    private final Travel module;
    private UserManager um;
    private WorldManager wm;
    private final HomeManager homeManager;

    public HomeListener(Travel module, UserManager um, WorldManager wm)
    {
        this.module = module;
        this.um = um;
        this.wm = wm;
        this.homeManager = module.getHomeManager();
    }

    @Listener(order = EARLY)
    public void rightClickBed(InteractBlockEvent.Use event)
    {
        Optional<Player> source = event.getCause().first(Player.class);
        if (source.isPresent())
        {
            if (event.getTargetBlock().getState().getType() != BlockTypes.BED)
            {
                return;
            }
            Player player = source.get();
            User user = um.getExactUser(player.getUniqueId());
            if (player.get(SneakingData.class).isPresent())
            {
                if (homeManager.has(user, "home"))
                {
                    Home home = homeManager.findOne(user, "home");
                    if (player.getLocation().equals(home.getLocation()))
                    {
                        return;
                    }
                    home.setLocation(player.getLocation(), player.getRotation(), wm);
                    home.update();
                    user.sendTranslated(POSITIVE, "Your home has been set!");
                }
                else
                {
                    if (this.homeManager.getCount(user) == this.module.getConfig().homes.max)
                    {
                        user.sendTranslated(CRITICAL, "You have reached your maximum number of homes!");
                        user.sendTranslated(NEGATIVE, "You have to delete a home to make a new one");
                        return;
                    }
                    homeManager.create(user, "home", player.getLocation(), player.getRotation(), false);
                    user.sendTranslated(POSITIVE, "Your home has been created!");
                }
                event.setCancelled(true);
            }
        }
    }
}
