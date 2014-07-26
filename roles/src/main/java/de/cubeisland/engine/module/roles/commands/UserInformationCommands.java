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
package de.cubeisland.engine.module.roles.commands;

import java.util.Map;

import org.bukkit.World;

import de.cubeisland.engine.core.command.context.CubeContext;
import de.cubeisland.engine.core.command.reflected.context.Flag;
import de.cubeisland.engine.core.command.reflected.context.Flags;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.NParams;
import de.cubeisland.engine.core.command.reflected.context.Named;
import de.cubeisland.engine.core.command.reflected.Alias;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.permission.PermDefault;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RolesAttachment;
import de.cubeisland.engine.module.roles.role.TempDataStore;
import de.cubeisland.engine.module.roles.role.UserDatabaseStore;
import de.cubeisland.engine.module.roles.role.resolved.ResolvedMetadata;
import de.cubeisland.engine.module.roles.role.resolved.ResolvedPermission;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

public class UserInformationCommands extends UserCommandHelper
{
    public UserInformationCommands(Roles module)
    {
        super(module);
    }

    @Alias(names = "listuroles")
    @Command(desc = "Lists roles of a user [in world]")
    @IParams(@Grouped(req = false, value = @Indexed(label = "player", type = User.class)))
    @NParams(@Named(names = "in", label = "world", type = World.class))
    public void list(CubeContext context)
    {
        User user = this.getUser(context, 0);
        if (user == null) return;
        World world = this.getWorld(context);
        if (world == null) return;
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(user);
        // List all assigned roles
        context.sendTranslated(NEUTRAL, "Roles of {user} in {world}:", user, world);
        for (Role pRole : rolesAttachment.getDataHolder(world).getRoles())
            {
            if (pRole.isGlobal())
            {
                context.sendMessage(String.format(this.LISTELEM_VALUE,"global",pRole.getName()));
                continue;
            }
            context.sendMessage(String.format(this.LISTELEM_VALUE,world.getName(),pRole.getName()));
        }
    }

    @Alias(names = "checkuperm")
    @Command(alias = "checkperm", desc = "Checks for permissions of a user [in world]")
    @IParams({@Grouped(@Indexed(label = "player", type = User.class)),
              @Grouped(@Indexed(label = "permission"))})
    @NParams(@Named(names = "in", label = "world", type = World.class))
    public void checkpermission(CubeContext context)
    {
        User user = context.getArg(0);
        World world = this.getWorld(context);
        if (world == null) return;
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(user);
        // Search for permission
        String permission = context.getArg(1);
        ResolvedPermission resolvedPermission = rolesAttachment.getDataHolder(world).getPermissions().get(permission);
        if (user.isOp())
        {
            context.sendTranslated(POSITIVE, "{user} is op!", user);
        }
        if (user.isOnline()) // Can have superperm
        {
            boolean superPerm = user.hasPermission(permission);
            context.sendTranslated(NEUTRAL, "SuperPerm Node: {bool}", superPerm);
        }
        if (resolvedPermission == null)
        {

            PermDefault defaultFor = this.module.getCore().getPermissionManager().getDefaultFor(permission);
            if (defaultFor == null)
            {
                context.sendTranslated(NEGATIVE, "Permission {input} neither set nor registered!", permission);
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Permission {input} not set but default is: {name#default}!", permission, defaultFor.name());
            }
            return;
        }
        if (resolvedPermission.isSet())
        {
            context.sendTranslated(POSITIVE, "The player {user} does have access to {input#permission} in {world}", user, permission, world);
        }
        else
        {
            context.sendTranslated(NEGATIVE, "The player {user} does not have access to {input#permission} in {world}", user, permission, world);
        }
        // Display origin
        TempDataStore store = resolvedPermission.getOrigin();
        if (resolvedPermission.getOriginPermission() != null) // indirect permission
        {
            permission = resolvedPermission.getOriginPermission();
        }
        context.sendTranslated(NEUTRAL, "Permission inherited from:");
        if (user.getName().equals(store.getName()))
        {
            context.sendTranslated(NEUTRAL, "{input#permission} directly assigned to the user!", permission);
            return;
        }
        context.sendTranslated(NEUTRAL, "{input#permission} in the role {name}!", permission, store.getName());
    }

    @Alias(names = "listuperm")
    @Command(alias = "listperm", desc = "List permission assigned to a user in a world")
    @IParams(@Grouped(req = false, value = @Indexed(label = "player", type = User.class)))
    @NParams(@Named(names = "in", label = "world", type = World.class))
    @Flags(@Flag(longName = "all", name = "a"))
    public void listpermission(CubeContext context)
    {
        User user = this.getUser(context, 0);
        if (user == null) return;
        World world = this.getWorld(context);
        if (world == null) return;
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(user);
        UserDatabaseStore rawData = rolesAttachment.getDataHolder(world);
        Map<String,Boolean> perms = context.hasFlag("a") ? rawData.getAllRawPermissions() : rawData.getRawPermissions();
        if (perms.isEmpty())
        {
            if (context.hasFlag("a"))
            {
                context.sendTranslated(NEUTRAL, "{user} has no permissions set in {world}.", user, world);
                return;
            }
            context.sendTranslated(NEUTRAL, "{user} has no permissions set directly in {world}.", user, world);
            return;
        }
        context.sendTranslated(NEUTRAL, "Permissions of {user} in {world}.", user, world);
        for (Map.Entry<String, Boolean> entry : perms.entrySet())
        {
            context.sendMessage(String.format(this.LISTELEM_VALUE,entry.getKey(), entry.getValue()));
        }
    }

    @Alias(names = "checkumeta")
    @Command(alias = {"checkdata", "checkmeta"}, desc = "Checks for metadata of a user [in world]")
    @IParams({@Grouped(@Indexed(label = "player", type = User.class)),
              @Grouped(@Indexed(label = "metadatakey"))})
    @NParams(@Named(names = "in", label = "world", type = World.class))
    public void checkmetadata(CubeContext context)
    {
        User user = context.getArg(0);
        World world = this.getWorld(context);
        if (world == null) return;
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(user);
        // Check metadata
        String metaKey = context.getArg(1);
        UserDatabaseStore dataHolder = rolesAttachment.getDataHolder(world);
        Map<String,ResolvedMetadata> metadata = dataHolder.getMetadata();
        if (!metadata.containsKey(metaKey))
        {
            context.sendTranslated(NEUTRAL, "{input#key} is not set for {user} in {world}.", metaKey, user, world);
            return;
        }
        context.sendTranslated(NEUTRAL, "{input#key}: {input#value} is set for {user} in {world}.", metaKey, metadata.get(metaKey).getValue(), user, world);
        if (metadata.get(metaKey).getOrigin() != dataHolder)
        {
            context.sendTranslated(NEUTRAL, "Origin: {name#role}", metadata.get(metaKey).getOrigin().getName());
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Origin: {text:directly assigned}");
        }
    }

    @Alias(names = "listumeta")
    @Command(alias = {"listdata", "listmeta"}, desc = "Lists assigned metadata from a user [in world]")
    @IParams(@Grouped(req = false, value = @Indexed(label = "player", type = User.class)))
    @NParams(@Named(names = "in", label = "world", type = World.class))
    @Flags(@Flag(longName = "all", name = "a"))
    public void listmetadata(CubeContext context)
    {
        User user = this.getUser(context, 0);
        if (user == null) return;
        World world = this.getWorld(context);
        if (world == null) return;
        RolesAttachment rolesAttachment = this.manager.getRolesAttachment(user);
        UserDatabaseStore rawData = rolesAttachment.getDataHolder(world);
        Map<String, String> metadata = context.hasFlag("a") ? rawData.getAllRawMetadata() : rawData.getRawMetadata();
        // List all metadata
        context.sendTranslated(NEUTRAL, "Metadata of {user} in {world}:", user, world);
        for (Map.Entry<String, String> entry : metadata.entrySet())
        {
            context.sendMessage(String.format(this.LISTELEM_VALUE,entry.getKey(), entry.getValue()));
        }
    }
}
