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
package org.cubeengine.module.teleport;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * /setworldspawn 	Sets the world spawn.
 * /spawnpoint 	Sets the spawn point for a player.
 * /tp 	Teleports entities.
 */
@Singleton
@Module
public class Teleport extends CubeEngineModule
{
    // TODO make override of vanilla-commands optional
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private TaskManager tm;
    @Inject private PermissionManager pm;
    @Inject private Broadcaster bc;
    @Inject private I18n i18n;
    @Inject private ModuleManager mm;

    @Inject private TeleportPerm perms;
    @ModuleConfig private TeleportConfiguration config;
    private TpWorldPermissions tpWorld;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        TeleportListener tl = new TeleportListener(this, i18n);
        em.registerListener(Teleport.class, tl);

        cm.addCommands(cm, this, new MovementCommands(this, tl, i18n, mm.getPlugin(Teleport.class).get()));
        cm.addCommands(cm, this, new SpawnCommands(this, em, bc, tl, i18n));
        cm.addCommands(cm, this, new TeleportCommands(this, bc, tl, i18n));
        cm.addCommands(cm, this, new TeleportRequestCommands(this, tm, tl, i18n));
    }

    public TeleportPerm perms()
    {
        return this.perms;
    }

    public TpWorldPermissions permsTpWorld()
    {
        if (tpWorld == null)
        {
            tpWorld = new TpWorldPermissions(perms, pm);
        }
        return tpWorld;
    }

    public TeleportConfiguration getConfig()
    {
        return config;
    }
}
