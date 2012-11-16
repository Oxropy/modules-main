package de.cubeisland.cubeengine.shout.interactions;

import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.annotation.Command;
import de.cubeisland.cubeengine.core.command.annotation.Param;
import de.cubeisland.cubeengine.core.i18n.I18n;
import de.cubeisland.cubeengine.shout.Shout;
import de.cubeisland.cubeengine.shout.announce.Announcement;
import java.io.IOException;
import java.util.Iterator;

public class ShoutSubCommands
{
    private Shout module;

    public ShoutSubCommands(Shout module)
    {
        this.module = module;
    }

    @Command(names = {
        "list", "announcements"
    }, desc = "List all announcements")
    public void list(CommandContext context)
    {
        Iterator<Announcement> iter = this.module.getAnnouncementManager().getAnnouncemets().iterator();
        if (iter.hasNext())
        {
            context.sendMessage("shout", "Here is the list of announcements:");
            while (iter.hasNext())
            {
                context.sendMessage(" - " + iter.next().getName());
            }
        }
        else
        {
            context.sendMessage("shout", "There are no announcements loaded!");
        }
    }

    @Command(desc = "Create the structure for a new announcement", min = 1, params = {
        @Param(names =
        {
            "delay", "d"
        }),
        @Param(names =
        {
            "world", "w"
        }),
        //@Param(names = {"permission", "p"}, type = Permission.class), TODO
        @Param(names =
        {
            "group", "g"
        }),
        @Param(names =
        {
            "message", "m"
        }),
        @Param(names =
        {
            "locale", "l"
        })
    })
    public void create(CommandContext context)
    {
        if (!context.hasNamed("message"))
        {
            context.sendMessage("shout", "You have to include a message!");
            return;
        }

        String message = context.getString("message");
        String locale = this.module.getCore().getConfiguration().defaultLanguage;
        if (context.hasNamed("locale"))
        {
            locale = I18n.normalizeLanguage(context.getString("locale"));
        }
        else if (context.getSenderAsUser() != null)
        {
            locale = context.getSenderAsUser().getLanguage();
        }

        try
        {
            this.module.getAnnouncementManager().createAnnouncement(
                context.getString(0),
                message,
                context.getString("delay", "10 minutes"),
                context.getString("world", "*"),
                context.getString("group", "*"),
                context.getString("permission", "*"),
                locale);
        }
        catch (IllegalArgumentException ex)
        {
            context.sendMessage("shout", "Some of your arguments are not valid.");
            context.sendMessage("shout", "The error message was: %s", ex.getLocalizedMessage());
        }
        catch (IOException ex)
        {
            context.sendMessage("shout", "There was an error creating some of the files.");
            context.sendMessage("shout", "The error message was: %s", ex.getLocalizedMessage());
        }

        module.getAnnouncementManager().reload();

        context.sendMessage("shout", "Your announcement have been created and loaded into the plugin");
    }

    @Command(desc = "clean all loaded announcements form memory and load from disk")
    public void reload(CommandContext context)
    {
        module.getAnnouncementManager().reload();
        context.sendMessage("shout", "All players and announcements have now been reloaded");
    }
}