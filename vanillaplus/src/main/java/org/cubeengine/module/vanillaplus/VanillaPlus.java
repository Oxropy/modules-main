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
package org.cubeengine.module.vanillaplus;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.addition.FoodCommands;
import org.cubeengine.module.vanillaplus.addition.GodCommand;
import org.cubeengine.module.vanillaplus.addition.HealCommand;
import org.cubeengine.module.vanillaplus.addition.InformationCommands;
import org.cubeengine.module.vanillaplus.addition.InvseeCommand;
import org.cubeengine.module.vanillaplus.addition.ItemDBCommand;
import org.cubeengine.module.vanillaplus.addition.MovementCommands;
import org.cubeengine.module.vanillaplus.addition.PlayerInfoCommands;
import org.cubeengine.module.vanillaplus.addition.PluginCommands;
import org.cubeengine.module.vanillaplus.addition.StashCommand;
import org.cubeengine.module.vanillaplus.addition.SudoCommand;
import org.cubeengine.module.vanillaplus.addition.UnlimitedFood;
import org.cubeengine.module.vanillaplus.addition.UnlimitedItems;
import org.cubeengine.module.vanillaplus.fix.ColoredSigns;
import org.cubeengine.module.vanillaplus.fix.FlymodeFixListener;
import org.cubeengine.module.vanillaplus.fix.ImmutableSafeLoginData;
import org.cubeengine.module.vanillaplus.fix.OverstackedListener;
import org.cubeengine.module.vanillaplus.fix.PaintingListener;
import org.cubeengine.module.vanillaplus.fix.SafeLoginData;
import org.cubeengine.module.vanillaplus.fix.SafeLoginDataBuilder;
import org.cubeengine.module.vanillaplus.fix.SpawnFixListener;
import org.cubeengine.module.vanillaplus.fix.TamedListener;
import org.cubeengine.module.vanillaplus.improvement.BorderCommands;
import org.cubeengine.module.vanillaplus.improvement.ClearInventoryCommand;
import org.cubeengine.module.vanillaplus.improvement.DifficultyCommand;
import org.cubeengine.module.vanillaplus.improvement.GameModeCommand;
import org.cubeengine.module.vanillaplus.improvement.ItemCommands;
import org.cubeengine.module.vanillaplus.improvement.ItemModifyCommands;
import org.cubeengine.module.vanillaplus.improvement.KillCommands;
import org.cubeengine.module.vanillaplus.improvement.OpCommands;
import org.cubeengine.module.vanillaplus.improvement.PlayerListCommand;
import org.cubeengine.module.vanillaplus.improvement.SaveCommands;
import org.cubeengine.module.vanillaplus.improvement.StopCommand;
import org.cubeengine.module.vanillaplus.improvement.TimeCommands;
import org.cubeengine.module.vanillaplus.improvement.WeatherCommands;
import org.cubeengine.module.vanillaplus.improvement.WhitelistCommand;
import org.cubeengine.module.vanillaplus.improvement.removal.ButcherCommand;
import org.cubeengine.module.vanillaplus.improvement.removal.RemoveCommands;
import org.cubeengine.module.vanillaplus.improvement.summon.SpawnMobCommand;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.service.matcher.EnchantMatcher;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.matcher.TimeMatcher;
import org.cubeengine.libcube.service.matcher.WorldMatcher;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.processor.Module;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;

/**
 * A module to improve vanilla commands:
 *
 * /clear 	Clears items from player inventory. {@link ClearInventoryCommand#clearinventory}
 * ??? /deop 	Revoke operator status from a player.
 * /difficulty 	Sets the difficulty level. {@link DifficultyCommand#difficulty}
 * ??? /effect 	Add or remove status effects.
 * /enchant 	Enchants a player item. {@link ItemModifyCommands#enchant}
 * /execute (sudo)	Executes another command. {@link SudoCommand#sudo}
 * /gamemode 	Sets a player's game mode. {@link GameModeCommand#gamemode}
 * /give 	Gives an item to a player. {@link ItemCommands#give},{@link ItemCommands#item}
 * ??? /help 	Provides help for commands.
 * /kill (butcher,remove,removeALl)   Kills entities (players, mobs, items, etc.). {@link ButcherCommand#butcher},{@link RemoveCommands#remove},{@link RemoveCommands#removeAll}
 * /list 	Lists players on the server. {@link PlayerListCommand#list}
 * ??? /op 	Grants operator status to a player.
 * ??? /replaceitem 	Replaces items in inventories.
 * /save-all 	Saves the server to disk. {@link SaveCommands#saveall}
 * ??? /save-off 	Disables automatic server saves.
 * ??? /save-on 	Enables automatic server saves.
 * configure say color??? /say 	Displays a message to multiple players.
 * /seed 	Displays the world seed.
 * ??? /setidletimeout 	Sets the time before idle players are kicked.
 * ??? /spreadplayers 	Teleports entities to random locations.
 * /stop 	Stops a server.
 * /summon (spawnmob) Summons an entity. {@link SpawnMobCommand#spawnMob}
 * /time 	Changes or queries the world's game time. {@link TimeCommands#time}
 * ??? /toggledownfall 	Toggles the weather.
 * /weather 	Sets the weather. {@link WeatherCommands#weather}
 * /whitelist 	Manages server whitelist. {@link WhitelistCommand}
 * ??? /xp 	Adds or removes player experience.
 *
 * Extra commands:
 *
 * /plugins {@link PluginCommands#plugins}
 * /version {@link PluginCommands#version}
 * /pweather {@link WeatherCommands#pweather}
 * /ptime {@link TimeCommands#ptime}
 * /more {@link ItemCommands#more}
 * /stack {@link ItemCommands#stack}
 * /rename {@link ItemModifyCommands#rename}
 * /headchange {@link ItemModifyCommands#headchange}
 * /repair {@link ItemModifyCommands#repair}
 * /kill (for players) {@link KillCommands#kill}
 * /suicide (kill self) {@link KillCommands#suicide}
 */

@Singleton
@Module
public class VanillaPlus extends CubeEngineModule
{
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    @Inject private MaterialMatcher mm;
    @Inject private EnchantMatcher em;
    @Inject private EntityMatcher enm;
    @Inject private TimeMatcher tm;
    @Inject private ModuleManager momu;
    @Inject private WorldMatcher wm;
    @Inject private TaskManager tam;
    @Inject private PermissionManager pm;
    @Inject private Broadcaster bc;
    @Inject private InventoryGuardFactory invGuard;
    @ModuleConfig private VanillaPlusConfig config;
    @Inject private EventManager evm;
    @Inject private StringMatcher sm;

    private PluginContainer plugin;

    @Inject
    public VanillaPlus(PluginContainer plugin)
    {
        this.plugin = plugin;
        DataRegistration.<SafeLoginData, ImmutableSafeLoginData>builder()
                .dataClass(SafeLoginData.class).immutableClass(ImmutableSafeLoginData.class)
                .builder(new SafeLoginDataBuilder()).manipulatorId("safe_login")
                .dataName("CubeEngine VanillaPlus Safe Login")
                .buildAndRegister(plugin);
        SafeLoginData.FLYMODE.getQuery();
    }

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        enableImprovements();
        enableFixes();
        enableAdditions();
    }

    private void enableAdditions()
    {
        if (config.add.commandGod)
        {
            cm.addCommands(this, new GodCommand(pm, i18n));
        }
        if (config.add.commandHeal)
        {
            cm.addCommands(this, new HealCommand(pm, i18n, bc));
        }
        if (config.add.commandsInformation)
        {
            cm.addCommands(this, new InformationCommands(pm, this, i18n));
        }
        if (config.add.commandInvsee)
        {
            cm.addCommands(this, new InvseeCommand(pm, invGuard, i18n));
        }
        if (config.add.commandItemDB)
        {
            cm.addCommands(this, new ItemDBCommand(this, mm, em, i18n));
        }
        if (config.add.commandsMovement)
        {
            cm.addCommands(this, new MovementCommands(pm, i18n));
        }
        if (config.add.commandsFood)
        {
            cm.addCommands(this, new FoodCommands(pm, i18n, bc));
        }
        if (config.add.commandsPlayerInformation)
        {
            cm.addCommands(this, new PlayerInfoCommands(i18n));
        }
        if (config.add.commandsPlugins)
        {
            cm.addCommands(this, new PluginCommands(i18n, pm, momu));
        }
        if (config.add.commandStash)
        {
            cm.addCommands(this, new StashCommand(i18n));
        }
        if (config.add.commandSudo)
        {
            cm.addCommands(this, new SudoCommand(i18n, cm));
        }
        if (config.add.commandUnlimited)
        {
            UnlimitedItems cmd = new UnlimitedItems(i18n);
            cm.addCommands(this, cmd);
            evm.registerListener(VanillaPlus.class, cmd);
        }
        if (config.add.unlimitedFood)
        {
            new UnlimitedFood(pm, plugin);
        }
    }

    private void enableFixes()
    {
        if (config.fix.styledSigns)
        {
            evm.registerListener(VanillaPlus.class, new ColoredSigns(pm));
        }
        if (config.fix.preventOverstackedItems)
        {
            evm.registerListener(VanillaPlus.class, new OverstackedListener(pm, this));
        }
        if (config.fix.safeLoginFly)
        {
            evm.registerListener(VanillaPlus.class, new FlymodeFixListener());
        }
        if (config.fix.safeLoginBorder)
        {
            evm.registerListener(VanillaPlus.class, new SpawnFixListener(i18n));
        }
        if (config.fix.paintingSwitcher)
        {
            evm.registerListener(VanillaPlus.class, new PaintingListener(pm, this, i18n));
        }
        if (config.fix.showTamer)
        {
            evm.registerListener(VanillaPlus.class, new TamedListener(i18n));
        }
    }

    private void enableImprovements()
    {
        if (config.improve.commandRemove)
        {
            cm.addCommands(this, new RemoveCommands(this, enm, mm, i18n, cm));
        }
        if (config.improve.commandButcher)
        {
            cm.addCommands(this, new ButcherCommand(pm, this, i18n, cm, sm));
        }
        if (config.improve.commandSummon)
        {
            cm.addCommands(this, new SpawnMobCommand(this, i18n, enm));
        }
        if (config.improve.commandClearinventory)
        {
            cm.addCommands(this, new ClearInventoryCommand(pm, i18n));
        }
        if (config.improve.commandDifficulty)
        {
            cm.addCommands(this, new DifficultyCommand(i18n));
        }
        if (config.improve.commandGamemode)
        {
            cm.addCommands(this, new GameModeCommand(pm, i18n));
        }
        if (config.improve.commandItem)
        {
            cm.addCommands(this, new ItemCommands(pm, mm, em, i18n));
        }
        if (config.improve.commandItemModify)
        {
            cm.addCommands(this, new ItemModifyCommands(pm, i18n));
        }
        if (config.improve.commandKill)
        {
            cm.addCommands(this, new KillCommands(pm, i18n));
        }
        if (config.improve.commandOp)
        {
            cm.addCommands(this, new OpCommands());
        }
        if (config.improve.commandList)
        {
            cm.addCommands(this, new PlayerListCommand(i18n));
        }
        if (config.improve.commandSave)
        {
            cm.addCommands(this, new SaveCommands(i18n));
        }
        if (config.improve.commandStop)
        {
            cm.addCommands(this, new StopCommand(this));
        }
        if (config.improve.commandTime)
        {
            cm.addCommands(this, new TimeCommands(pm, i18n, tm, wm, tam));
        }
        if (config.improve.commandWeather)
        {
            cm.addCommands(this, new WeatherCommands(i18n, cm));
        }
        if (config.improve.commandWhitelist)
        {
            cm.addCommand(new WhitelistCommand(cm, i18n));
        }
        if (config.improve.commandBorderEnable)
        {
            cm.addCommand(new BorderCommands(i18n, cm, momu.getPlugin(VanillaPlus.class).get(), config.improve.commandBorderMax));
        }
    }

    public VanillaPlusConfig getConfig()
    {
        return config;
    }


    /*
    TODO onlinemode cmd

    @Command(desc = "Shows the online mode")
    public void onlinemode(CommandSource context)
    {
        if (Sponge.getServer().getOnlineMode())
        {
            i18n.sendTranslated(context, POSITIVE, "The Server is running in online mode");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The Server is running in offline mode");
        /* Changing online mode is no longer supported on a running server
        BukkitUtils.setOnlineMode(newState);
        if (newState)
        {
            context.sendTranslated(POSITIVE, "The server is now in online-mode.");
        }
        else
        {
            context.sendTranslated(POSITIVE, "The server is not in offline-mode.");
        }
        *//*
}

     @Command(alias = "finduser", desc = "Searches for a user in the database")
    public void searchuser(CommandContext context, @Reader(FindUserReader.class) @Desc("The name to search for") User name)
    {
        if (name.getName().equalsIgnoreCase(context.getString(0)))
        {
            i18n.sendTranslated(context.getSource(), POSITIVE, "Matched exactly! User: {user}", name);
            return;
        }
        i18n.sendTranslated(context.getSource(), POSITIVE, "Matched not exactly! User: {user}", name);
    }
    */
}
