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
package de.cubeisland.engine.travel;

import java.util.concurrent.TimeUnit;

import de.cubeisland.engine.core.command.CommandManager;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.storage.database.Database;
import de.cubeisland.engine.core.util.Profiler;
import de.cubeisland.engine.travel.interactions.HomeAdminCommand;
import de.cubeisland.engine.travel.interactions.HomeCommand;
import de.cubeisland.engine.travel.interactions.HomeListener;
import de.cubeisland.engine.travel.interactions.WarpAdminCommand;
import de.cubeisland.engine.travel.interactions.WarpCommand;
import de.cubeisland.engine.travel.storage.HomeManager;
import de.cubeisland.engine.travel.storage.InviteManager;
import de.cubeisland.engine.travel.storage.TableInvite;
import de.cubeisland.engine.travel.storage.TableTeleportPoint;
import de.cubeisland.engine.travel.storage.WarpManager;

public class Travel extends Module
{
    private TravelConfig config;

    private InviteManager inviteManager;
    private HomeManager homeManager;
    private WarpManager warpManager;

    @Override
    public void onEnable()
    {

        this.config = this.loadConfig(TravelConfig.class);
        Database db = this.getCore().getDB();
        db.registerTable(TableTeleportPoint.class);
        db.registerTable(TableInvite.class);

        Profiler.startProfiling("travelEnable");
        this.getLog().trace("Loading TeleportPoints...");
        this.inviteManager = new InviteManager(db, this);
        this.homeManager = new HomeManager(this, this.inviteManager);
        this.warpManager = new WarpManager(this, this.inviteManager);
        this.getLog().trace("Loaded TeleportPoints in {} ms", Profiler.endProfiling("travelEnable",TimeUnit.MILLISECONDS));

        final CommandManager cm = this.getCore().getCommandManager();
        HomeCommand home = new HomeCommand(this);
        cm.registerCommand(home);
        cm.registerCommand(new HomeAdminCommand(this), "home");
        cm.registerCommand(new WarpCommand(this));
        cm.registerCommand(new WarpAdminCommand(this), "warp");
        this.getCore().getEventManager().registerListener(this, new HomeListener(this));
    }

    public TravelConfig getConfig()
    {
        return this.config;
    }

    public HomeManager getHomeManager()
    {
        return homeManager;
    }

    public WarpManager getWarpManager()
    {
        return warpManager;
    }

    public InviteManager getInviteManager()
    {
        return this.inviteManager;
    }
}
