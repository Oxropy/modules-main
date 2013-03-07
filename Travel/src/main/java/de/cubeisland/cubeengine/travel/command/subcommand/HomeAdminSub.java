package de.cubeisland.cubeengine.travel.command.subcommand;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.parameterized.Flag;
import de.cubeisland.cubeengine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.cubeengine.core.command.reflected.Alias;
import de.cubeisland.cubeengine.core.command.reflected.Command;
import de.cubeisland.cubeengine.core.logger.LogLevel;
import de.cubeisland.cubeengine.core.storage.StorageException;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.Pair;
import de.cubeisland.cubeengine.travel.Travel;
import de.cubeisland.cubeengine.travel.storage.Home;
import de.cubeisland.cubeengine.travel.storage.TelePointManager;
import de.cubeisland.cubeengine.travel.storage.TeleportPoint;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class HomeAdminSub
{
    private static final Long ACCEPT_TIMEOUT = 20000l;

    private final Map<CommandSender, Pair<Long, ParameterizedContext>> acceptEntries;
    private final TelePointManager tpManager;
    private final Travel module;

    public HomeAdminSub(Travel module, TelePointManager tpManager)
    {
        this.tpManager = tpManager;
        this.acceptEntries = new HashMap<CommandSender, Pair<Long, ParameterizedContext>>();
        this.module = module;
    }

    @Alias(names = {
        "clearhomes"
    })
    @Command(desc = "Clear all homes (of an user)", flags = {
        @Flag(name = "p", longName = "public")
    }, min = 1, max = 1, usage = " <user> <-public>")
    public void clear(ParameterizedContext context)
    {
        if (context.getArgCount() > 0)
        {
            if (CubeEngine.getUserManager().getUser(context.getString(0), false) == null)
            {
                context.sendMessage("travel", "&3%s &4Isn't an user on this server", context.getString(0));
                return;
            }
            else
            {
                if (context.hasFlag("p"))
                {
                    context.sendMessage("travel", "&5Are you sure you want to delete all public homes ever created by &3%s", context.getString(0));
                    context.sendMessage("travel", "&5To delete all the public homes, do: &9\"/home admin accept\" &5before 20 secunds");
                }
                else
                {
                    context.sendMessage("travel", "&5Are you sure you want to delete all homes ever created by &3%s", context.getString(0));
                    context.sendMessage("travel", "&5To delete all the homes, do: &9\"/home admin accept\" &5before 20 secunds");
                }
            }
        }
        else
        {
            if (context.hasFlag("p"))
            {
                context.sendMessage("travel", "&5Are you sure you want to delete all public homes ever created on this server!?");
                context.sendMessage("travel", "&5To delete all the public homes of every user, do: &9\"/home admin accept\" &5before 20 secunds");
            }
            else
            {
                context.sendMessage("travel", "&5Are you sure you want to delete all homes ever created on this server!?");
                context.sendMessage("travel", "&5To delete all the homes of every user, do: &9\"/home admin accept\" &5before 20 secunds");
            }
        }
        acceptEntries.put(context.getSender(), new Pair<Long, ParameterizedContext>(System.currentTimeMillis(), context));

    }

    @Command(desc = "accept your previous command", min = 0, max = 0)
    public void accept(ParameterizedContext context)
    {
        if (this.acceptEntries.containsKey(context.getSender()))
        {
            if (this.acceptEntries.get(context.getSender()).getLeft() + ACCEPT_TIMEOUT > System.currentTimeMillis())
            {
                ParameterizedContext usedContext = this.acceptEntries.get(context.getSender()).getRight();
                if (usedContext.getCommand().getName().equals("clear"))
                {
                   //  TODO delete homes
                }
                return;
            }
        }
        context.sendMessage("travel", "&4You have nothing to accept");
    }

    @Command(desc = "List all (public) homes", flags = {
        @Flag(name = "p", longName = "public")
    }, min = 0, max = 1, usage = " <user> <-public>")
    public void list(ParameterizedContext context)
    {
        //TODO
    }

    @Command(names = {
        "private"
    }, desc = "Make a home private", min = 1, max = 1, usage = " owner:home")
    public void makePrivate(CommandContext context)
    {
        User user = CubeEngine.getUserManager().getUser(context.getString(0), false);
        Home home = null;
        home = tpManager.getHome(context.getString(0));
        if (home == null)
        {
            context.sendMessage("travel", "&4Couldn't find &9%s", context.getString(0));
            return;
        }
        if (!home.isPublic())
        {
            context.sendMessage("travel", "&9%s &6is already private!", context.getString(0));
            return;
        }
        home.setVisibility(TeleportPoint.Visibility.PRIVATE);
        context.sendMessage("travel", "&9%s &6is now private", context.getString(0));
    }

    @Command(names = {
        "public"
    }, desc = "Make a home public", min = 1, max = 1, usage = " owner:home")
    public void makePublic(CommandContext context)
    {
        User user = CubeEngine.getUserManager().getUser(context.getString(0), false);
        Home home = null;
        home = tpManager.getHome(context.getString(0));
        if (home == null)
        {
            context.sendMessage("travel", "&4Couldn't find &9%s", context.getString(0));
            return;
        }
        if (home.isPublic())
        {
            context.sendMessage("travel", "&9%s &6is already public!", context.getString(0));
            return;
        }
        home.setVisibility(TeleportPoint.Visibility.PUBLIC);
        context.sendMessage("travel", "&9%s &6is now public", context.getString(0));
    }

    private class AcceptEntry
    {
        private CommandContext context;

        public AcceptEntry(CommandContext context)
        {
            this.context = context;
        }
    }
}
