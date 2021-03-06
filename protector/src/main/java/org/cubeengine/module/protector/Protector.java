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
package org.cubeengine.module.protector;

import org.cubeengine.butler.parameter.enumeration.SimpleEnumButler;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.parser.DefaultedCatalogTypeParser;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.protector.command.RegionCommands;
import org.cubeengine.module.protector.command.SettingsCommands;
import org.cubeengine.module.protector.command.parser.TristateParser;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.region.RegionFormatter;
import org.cubeengine.module.zoned.Zoned;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;

// TODO fill/empty bucket (in hand)
// TNT can be ignited ( but no world change )
// TNT/Creeper does no damage to player?

@Singleton
@Module
public class Protector extends CubeEngineModule
{
    private Path modulePath;
    @Inject private Reflector reflector;
    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    @InjectService private PermissionService ps;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private ModuleManager mm;
    @Inject private TaskManager tm;
    private Log logger;
    private Zoned zoned;

    private RegionManager manager;

    @Listener
    public void onEnable(GamePostInitializationEvent event)
    {
        cm.getProviders().register(this, new TristateParser(), Tristate.class);
        cm.getProviders().register(this, new SimpleEnumButler(), SettingsListener.MoveType.class, SettingsListener.UseType.class, SettingsListener.SpawnType.class);
        cm.getProviders().register(this, new DefaultedCatalogTypeParser<>(EntityType.class, null), EntityType.class);
        this.logger = mm.getLoggerFor(Protector.class);
        this.modulePath = mm.getPathFor(Protector.class);
        this.zoned = (Zoned) mm.getModule(Zoned.class);
        manager = new RegionManager(modulePath, reflector, logger, zoned);
        ps.registerContextCalculator(new RegionContextCalculator(manager));
        RegionCommands regionCmd = new RegionCommands(cm, this, manager, i18n, tm, em);
        i18n.getCompositor().registerFormatter(new RegionFormatter());
        cm.addCommand(regionCmd);
        SettingsCommands settingsCmd = new SettingsCommands(zoned.getManager(), manager, i18n, ps, pm, em, cm);
        regionCmd.addCommand(settingsCmd);
    }

    @Listener
    public void onServerStarted(GameStartedServerEvent event)
    {
        manager.reload();
        mm.getLoggerFor(Protector.class).info("{} Regions loaded", manager.getRegionCount());
    }
}
