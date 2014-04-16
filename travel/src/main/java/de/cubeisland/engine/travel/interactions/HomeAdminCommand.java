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
package de.cubeisland.engine.travel.interactions;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandResult;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.exception.IncorrectUsageException;
import de.cubeisland.engine.core.command.parameterized.CommandParameterIndexed;
import de.cubeisland.engine.core.command.parameterized.Flag;
import de.cubeisland.engine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.engine.core.command.reflected.Alias;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.Grouped;
import de.cubeisland.engine.core.command.reflected.Indexed;
import de.cubeisland.engine.core.command.result.confirm.ConfirmResult;
import de.cubeisland.engine.core.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.travel.Travel;
import de.cubeisland.engine.travel.storage.Home;
import de.cubeisland.engine.travel.storage.HomeManager;
import de.cubeisland.engine.travel.storage.TeleportPointModel;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

public class HomeAdminCommand extends ContainerCommand
{
    private final HomeManager manager;
    private final Travel module;

    public HomeAdminCommand(Travel module)
    {
        super(module, "admin", "Teleport to another users home");
        this.addIndexed(new CommandParameterIndexed(new String[]{"[owner:]home"}, String.class, false, true, 1));
        this.module = module;
        this.manager = module.getHomeManager();
    }

    @Override
    public CommandResult run(CommandContext context)
    {
        // TODO
        if (context.isSender(User.class))
        {
            User sender = (User)context.getSender();
            User user = context.getUser(0);
            Home home;
            if (user == null)
            {
                sender.sendTranslated(NEGATIVE, "Player {user} not found!", context.getString(0));
                return null;
            }

            if (context.getArgCount() == 2)
            {
                home = manager.find(user, context.getString(1));
                if (home == null)
                {
                    sender.sendTranslated(NEGATIVE, "{user} does not have a home named {name#home}!", user, context.getString(1));
                    return null;
                }
            }
            else
            {
                home = manager.find(user, "home");
                if (home == null)
                {
                    sender.sendTranslated(NEGATIVE, "{user} does not have a home!", user);
                    return null;
                }
            }
            Location location = home.getLocation();
            if (location == null)
            {
                context.sendTranslated(NEGATIVE, "This home is in a world that no longer exists!");
                return null;
            }
            sender.teleport(location, TeleportCause.COMMAND);
            if (home.getWelcomeMsg() != null)
            {
                sender.sendMessage(home.getWelcomeMsg());
            }
            else
            {
                sender.sendTranslated(POSITIVE, "You have been teleported to {user}'s home!", user);
            }
            return null;
        }
        else
        {
            return super.run(context);
        }
    }

    @Alias(names = {"clearhomes"})
    @Command(desc = "Clear all homes (of an user)",
             flags = {@Flag(name = "pub", longName = "public"),
                      @Flag(name = "priv", longName = "Private")},
             indexed = @Grouped(req = false, value = @Indexed("player")))
    public CommandResult clear(final ParameterizedContext context)
    {
        if (this.module.getConfig().clearOnlyFromConsole && !(context.getSender() instanceof ConsoleCommandSender))
        {
            context.sendMessage("This command has been disabled for ingame use via the configuration");
            return null;
        }
        if (context.getArgCount() > 0)
        {
            if (context.getUser(0) == null)
            {
                context.sendTranslated(NEGATIVE, "Player {user} not found!", context.getString(0));
                return null;
            }
            else
            {
                if (context.hasFlag("pub"))
                {
                    context.sendTranslated(NEUTRAL, "Are you sure you want to delete all public homes ever created by {user}?", context.getString(0));
                    context.sendTranslated(NEUTRAL, "To delete all the public homes, do: {text:/confirm} before 30 seconds has passed");
                }
                else if (context.hasFlag("priv"))
                {
                    context.sendTranslated(NEUTRAL, "Are you sure you want to delete all private homes ever created by {user}?", context.getString(0));
                    context.sendTranslated(NEUTRAL, "To delete all the private homes, do: {text:/confirm} before 30 seconds has passed");
                }
                else
                {
                    context.sendTranslated(NEUTRAL, "Are you sure you want to delete all homes ever created by {user}?", context.getString(0));
                    context.sendTranslated(NEUTRAL, "To delete all the homes, do: &{text:/confirm} before 30 seconds has passed");
                }
            }
        }
        else
        {
            if (context.hasFlag("pub"))
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all public homes ever created on this server!?");
                context.sendTranslated(NEUTRAL, "To delete all the public homes of every user, do: {text:/confirm} before 30 seconds has passed");
            }
            else if (context.hasFlag("priv"))
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all private homes ever created on this server?");
                context.sendTranslated(NEUTRAL, "To delete all the private homes of every user, do: {text:/confirm} before 30 seconds has passed");
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Are you sure you want to delete all homes ever created on this server!?");
                context.sendTranslated(NEUTRAL, "To delete all the homes of every user, do: {text:/confirm} before 30 seconds has passed");
            }
        }
        return new ConfirmResult(new Runnable()
        {
            @Override
            public void run()
            {
                if (context.getArgCount() == 0)
                { // No user
                    manager.massDelete(context.hasFlag("priv"), context.hasFlag("pub"));
                    context.sendTranslated(POSITIVE, "The homes are now deleted");
                }
                else
                {
                    User user = context.getUser(0);
                    manager.massDelete(user, context.hasFlag("priv"), context.hasFlag("pub"));
                    context.sendTranslated(POSITIVE, "Deleted homes.");
                }
            }
        }, context);
    }

    @Command(desc = "List all (public) homes",
             flags = {@Flag(name = "pub", longName = "public"),
                      @Flag(name = "priv", longName = "private"),
                      @Flag(name = "o", longName = "owned"),
                      @Flag(name = "i", longName = "invited")},
             indexed = @Grouped(req = false, value = @Indexed("player")))
    public void list(ParameterizedContext context)
    {
        Set<Home> homes;
        if (context.hasArg(0))
        {
            User user = context.getUser(0);
            if (user == null)
            {
                context.sendTranslated(NEGATIVE, "Player {user} not found!", context.getString(0));
                return;
            }
            homes = manager.list(user, context.hasFlag("o"), context.hasFlag("pub"), context.hasFlag("i"));
        }
        else
        {
            homes = manager.list(context.hasFlag("priv"), context.hasFlag("pub"));
        }
        if (homes.isEmpty())
        {
            context.sendTranslated(NEGATIVE, "The query returned no homes!");
            return;
        }
        for (Home home : homes)
        {
            if (home.isPublic())
            {
                context.sendTranslated(NEUTRAL, "  {user}:{name#home} ({text:public})", home.getOwnerName(), home.getName());
            }
            else
            {
                context.sendTranslated(NEUTRAL, "  {user}:{name#home} ({text:private})", home.getOwnerName(), home.getName());
            }
        }
    }

    @Command(names = {"private", "makeprivate"},
             desc = "Make a players home private",
             indexed = {@Grouped(@Indexed("home")),
                        @Grouped(req = false, value = @Indexed("owner"))})
    public void makePrivate(CommandContext context)
    {
        User user;
        if (context.hasArg(1))
        {
            user = context.getUser(1);
        }
        else if (context.getSender() instanceof User)
        {
            user = (User)context.getSender();
        }
        else
        {
            throw new IncorrectUsageException("Player not provided");
        }
        Home home = manager.find(user, context.getString(0));
        if (home == null)
        {
            context.sendTranslated(NEGATIVE, "Home {input} not found!", context.getString(0));
            return;
        }
        if (!home.isPublic())
        {
            context.sendTranslated(NEGATIVE, "{name#home} is already private!", context.getString(0));
            return;
        }
        home.setVisibility(TeleportPointModel.VISIBILITY_PRIVATE);
        context.sendTranslated(POSITIVE, "{input#home} is now private", context.getString(0));
    }

    @Command(names = {"public", "makepublic"},
             desc = "Make a users home public",
             indexed = {@Grouped(@Indexed("home")),
                        @Grouped(req = false, value = @Indexed("owner"))})
    public void makePublic(CommandContext context)
    {
        User user;
        if (context.hasArg(1))
        {
            user = context.getUser(1);
        }
        else if (context.getSender() instanceof User)
        {
            user = (User)context.getSender();
        }
        else
        {
            throw new IncorrectUsageException("Player not provided");
        }
        Home home = manager.find(user, context.getString(0));
        if (home == null)
        {
            context.sendTranslated(NEGATIVE, "Home {input#home} not found!", context.getString(0));
            return;
        }
        if (home.isPublic())
        {
            context.sendTranslated(NEGATIVE, "{input#home} is already public!", context.getString(0));
            return;
        }
        home.setVisibility(TeleportPointModel.VISIBILITY_PUBLIC);
        context.sendTranslated(POSITIVE, "{input#home} is now public", context.getString(0));
    }
}
