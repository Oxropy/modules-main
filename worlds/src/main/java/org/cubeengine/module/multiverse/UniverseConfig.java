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
package org.cubeengine.module.multiverse;

import org.cubeengine.service.world.ConfigWorld;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;

import java.util.List;

@SuppressWarnings("all")
public class UniverseConfig extends ReflectedYaml
{
    @Comment("The main world in this universe")
    public ConfigWorld mainWorld;

    @Comment("The worlds in this universe")
    public List<ConfigWorld> worlds;

    @Comment({"Players will keep their gamemode when changing worlds in this universe",
             "You should set this to false if players cannot change their gamemode in this universe"})
    public boolean keepGameMode = false; // if false can use perm

    @Comment({"The gamemode to enforce in every world of this universe",
              "Players with the keep gamemode permission ignore this",
              "leave empty to configure the gamemode for each world individually"})
    public GameMode enforceGameMode = null;

    @Comment("Players will keep their flymode when changing worlds in this universe")
    public boolean keepFlyMode = false; // if false can use perm

    @Comment("If true players do not need permissions to enter this universe")
    public boolean freeAccess = true; // if false generate permission

    public EntityTp entityTp;

    public class EntityTp implements Section
    {
        @Comment("If 2 universes have this option set to true entities can travel from one universe to the other")
        public boolean enable = false;
        @Comment("If 2 universes have this option set to true entities with inventories can travel from one universe to the other too")
        public boolean inventory = false;
    }
}