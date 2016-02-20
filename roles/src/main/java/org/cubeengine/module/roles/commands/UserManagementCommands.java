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
package org.cubeengine.module.roles.commands;

import java.util.Set;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.module.roles.commands.RoleCommands.toSet;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

@Alias("manuser")
@Command(name = "user", desc = "Manage users")
public class UserManagementCommands extends ContainerCommand
{
    private RolesPermissionService service;
    private I18n i18n;

    public UserManagementCommands(Roles module, RolesPermissionService service, I18n i18n)
    {
        super(module);
        this.service = service;
        this.i18n = i18n;
    }

    @Alias({"manuadd", "assignurole", "addurole", "giveurole"})
    @Command(alias = {"add", "give"}, desc = "Assign a role to the player [-temp]")
    public void assign(CommandSource ctx,
                       @Default User player,
                       RoleSubject role,
                       @Flag boolean temp,
                       @Named("in") @Default Context context)
    {
        // TODO RoleCompleter & Reader
        if (!role.canAssignAndRemove(ctx, context))
        {
            i18n.sendTranslated(ctx, NEGATIVE, "You are not allowed to assign the role {role} in {input#context}!", role, context);
            return;
        }
        Set<Context> contexts = toSet(context);
        if (temp)
        {
            if (!player.getPlayer().isPresent())
            {
                i18n.sendTranslated(ctx, NEGATIVE, "You cannot assign a temporary role to a offline player!");
                return;
            }
            if (player.getTransientSubjectData().addParent(contexts, role))
            {
                i18n.sendTranslated(ctx, POSITIVE, "Added the role {role} temporarily to {user} in {input#context}.", role, player, context);
                return;
            }
            i18n.sendTranslated(ctx, NEUTRAL, "{user} already had the role {role} in {input#context}.", player, role, context);
            return;
        }
        if (player.getSubjectData().addParent(contexts, role))
        {
            i18n.sendTranslated(ctx, POSITIVE, "Added the role {role} to {user} in {input#context}.", role, player, context);
            return;
        }
        i18n.sendTranslated(ctx, NEUTRAL, "{user} already has the role {role} in {input#context}.", player, role, context);
    }

    @Alias(value = {"remurole", "manudel"})
    @Command(desc = "Removes a role from the player")
    public void remove(CommandSource ctx, @Default Player player, RoleSubject role, @Named("in") @Default Context context)
    {
        if (!role.canAssignAndRemove(ctx, context))
        {
            i18n.sendTranslated(ctx, NEGATIVE, "You are not allowedR to remove the role {role} in {input#context}!", role, context);
            return;
        }
        Set<Context> contexts = toSet(context);
        if (player.getSubjectData().removeParent(contexts, role))
        {
            i18n.sendTranslated(ctx, POSITIVE, "Removed the role {role} from {user} in {input#context}.", role, player, context);
            return;
        }
        i18n.sendTranslated(ctx, NEUTRAL, "{user} did not have the role {role} in {input#context}.", player, role, context);
    }

    @Alias(value = {"clearurole", "manuclear"})
    @Command(desc = "Clears all roles from the player and sets the defaultroles [in context]")
    public void clear(CommandSource ctx, @Default Player player, @Named("in") @Default Context context) // TODO reader for context
    {
        Set<Context> contexts = toSet(context);
        player.getSubjectData().clearParents(contexts);
        i18n.sendTranslated(ctx, NEUTRAL, "Cleared the roles of {user} in {ctx}.", player, context);
        SubjectData defaultData = service.getDefaultData();
        if (!defaultData.getParents(contexts).isEmpty())
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Default roles assigned:");
            for (Subject subject : defaultData.getParents(contexts))
            {
                player.getTransientSubjectData().addParent(contexts, subject);
                ctx.sendMessage(Text.of("- ", TextColors.YELLOW, subject instanceof RoleSubject ? ((RoleSubject) subject).getName() : subject.getIdentifier()));
            }
        }
    }

    @Alias(value = "setuperm")
    @Command(alias = "setperm", desc = "Sets a permission for this user [in context]")
    public void setpermission(CommandSource ctx, @Default Player player, @Complete(PermissionCompleter.class) String permission, @Default Tristate value, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (value == Tristate.UNDEFINED)
        {
            resetpermission(ctx, player, permission, context);
        }
        if (player.getSubjectData().setPermission(contexts, permission, value))
        {
            switch (value)
            {
                case TRUE:
                    i18n.sendTranslated(ctx, POSITIVE, "Permission {input} of {user} set to true!", permission, player);
                    return;
                case FALSE:
                    i18n.sendTranslated(ctx, NEGATIVE, "Permission {input} of {user} set to false!", permission, player);
            }
            return;
        }
        i18n.sendTranslated(ctx, NEGATIVE, "Permission {input} of {user} was already set to {bool}!", permission, player,
                                value.asBoolean());
    }

    @Alias(value = "resetuperm")
    @Command(alias = "resetperm", desc = "Resets a permission for this user [in context]")
    public void resetpermission(CommandSource ctx, @Default Player player, String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (player.getSubjectData().setPermission(contexts, permission, Tristate.UNDEFINED))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Permission {input} of {user} resetted!", permission, player);
            return;
        }
        i18n.sendTranslated(ctx, NEGATIVE, "Permission {input} of {user} was not set!", permission, player);

    }

    @Alias(value = {"setudata","setumeta","setumetadata"})
    @Command(alias = {"setdata", "setmeta"}, desc = "Sets metadata for this user [in context]")
    public void setmetadata(CommandSource ctx, @Default Player player, String metaKey, String metaValue, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (((OptionSubjectData)player.getSubjectData()).setOption(contexts, metaKey, metaValue))
        {
            i18n.sendTranslated(ctx, POSITIVE, "Metadata {input#key} of {user} set to {input#value}!", metaKey,
                                    player, metaValue);
        }
        // TODO msg
    }

    @Alias(value = {"resetudata","resetumeta","resetumetadata"})
    @Command(alias = {"resetdata", "resetmeta", "deletedata", "deletemetadata", "deletemeta"}, desc = "Resets metadata for this user [in context]")
    public void resetmetadata(CommandSource ctx, @Default Player player, String metaKey, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (((OptionSubjectData)player.getSubjectData()).setOption(contexts, metaKey, null))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Metadata {input#key} of {user} removed!", metaKey, player);
        }
        // TODO msg
    }

    @Alias(value = {"clearudata","clearumeta","clearumetadata"})
    @Command(alias = {"cleardata", "clearmeta"}, desc = "Resets metadata for this user [in context]")
    public void clearMetaData(CommandSource ctx, @Default Player player, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);
        if (((OptionSubjectData)player.getSubjectData()).clearOptions(contexts))
        {
            i18n.sendTranslated(ctx, NEUTRAL, "Metadata of {user} cleared!", player);
        }
    }
}
