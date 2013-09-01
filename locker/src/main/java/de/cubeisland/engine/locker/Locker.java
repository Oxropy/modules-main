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
package de.cubeisland.engine.locker;

import de.cubeisland.engine.locker.BlockLockerConfiguration.BlockLockerConfigConverter;
import de.cubeisland.engine.locker.EntityLockerConfiguration.EntityLockerConfigConverter;
import de.cubeisland.engine.locker.commands.LockerAdminCommands;
import de.cubeisland.engine.locker.commands.LockerCommands;
import de.cubeisland.engine.locker.commands.LockerCreateCommands;
import de.cubeisland.engine.locker.storage.LockManager;
import de.cubeisland.engine.locker.storage.TableAccessList;
import de.cubeisland.engine.locker.storage.TableLockLocations;
import de.cubeisland.engine.locker.storage.TableLocks;
import de.cubeisland.engine.core.config.Configuration;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.util.convert.Convert;

public class Locker extends Module
{
    private LockerConfig config;
    private LockManager manager;

    @Override
    public void onEnable()
    {
        Convert.registerConverter(BlockLockerConfiguration.class, new BlockLockerConfigConverter());
        Convert.registerConverter(EntityLockerConfiguration.class, new EntityLockerConfigConverter());
        this.config = Configuration.load(LockerConfig.class, this);
        this.getCore().getDB().registerTable(TableLocks.initTable(this.getCore().getDB()));
        this.getCore().getDB().registerTable(TableLockLocations.initTable(this.getCore().getDB()));
        this.getCore().getDB().registerTable(TableAccessList.initTable(this.getCore().getDB()));
        manager = new LockManager(this);
        LockerCommands mainCmd = new LockerCommands(this, manager);
        this.getCore().getCommandManager().registerCommand(mainCmd);
        this.getCore().getCommandManager().registerCommand(new LockerCreateCommands(this, manager), "locker");
        this.getCore().getCommandManager().registerCommand(new LockerAdminCommands(this, manager), "locker");
        new LockerPerm(this, mainCmd);
        new LockerListener(this, manager);
    }

    @Override
    public void onDisable()
    {
        this.manager.saveAll();
    }

    public LockerConfig getConfig()
    {
        return this.config;
    }
    // TODO handle unloading & garbage collection e.g. when entities got removed by WE

    /*
    Features:
      hopper protection / minecart/wHopper
      lock leashknot / or fence from leashing on it
      global access list
    MagnetContainer: collect Items around (config for min/max/default)
    Drop Transfer: Dropped items are teleported into selected chest
      /droptransfer select
      /droptransfer on
      /droptransfer off
      /droptransfer status
    lock beacon effects?
     */
}

