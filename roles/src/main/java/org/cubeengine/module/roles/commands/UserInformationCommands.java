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
package org.cubeengine.module.roles.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.module.roles.Roles;
import org.cubeengine.module.roles.RolesUtil;
import org.cubeengine.module.roles.RolesUtil.FoundOption;
import org.cubeengine.module.roles.RolesUtil.FoundPermission;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;

import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.service.permission.SubjectData.GLOBAL_CONTEXT;
import static org.spongepowered.api.text.action.TextActions.showText;
import static org.spongepowered.api.text.format.TextColors.*;

@Command(name = "user", desc = "Manage users")
public class UserInformationCommands extends ContainerCommand
{
    private I18n i18n;
    private RolesPermissionService service;

    public UserInformationCommands(CommandManager base, I18n i18n, RolesPermissionService service)
    {
        super(base, Roles.class);
        this.i18n = i18n;
        this.service = service;
    }

    @Alias(value = "listurole")
    @Command(desc = "Lists roles of a user")
    public void list(CommandSource ctx, @Default User player)
    {
        List<SubjectReference> parents = player.getSubjectData().getParents(GLOBAL_CONTEXT);
        List<SubjectReference> transientParents = player.getTransientSubjectData().getParents(GLOBAL_CONTEXT);

        Text translation = i18n.translate(ctx, NEUTRAL, "Roles of {user}:", player);
        if (ctx.hasPermission("cubeengine.roles.command.roles.user.assign"))
        {
            translation = translation.toBuilder().append(Text.of(" ")).append(
                i18n.translate(ctx, POSITIVE, "(+)").toBuilder().onClick(TextActions.executeCallback(
                    sender -> {
                        i18n.send(sender, POSITIVE, "Click on the role you want to add to {user}.", player);

                        for (Subject subject : service.getGroupSubjects().getLoadedSubjects())
                        {
                            // TODO perm check for each role
                            if (!parents.contains(subject.asSubjectReference()) && subject instanceof FileSubject)
                            {
                                sender.sendMessage(Text.of(YELLOW, " - ", subject.getIdentifier()).toBuilder().onClick(
                                    TextActions.runCommand("/roles user assign " + player.getName() + " " + subject.getIdentifier())).build());
                            }
                        }
                    })).onHover(showText(i18n.translate(ctx, POSITIVE, "Click to add role"))).build()).build();
        }
        ctx.sendMessage(translation);

        if (ctx.hasPermission("cubeengine.roles.command.roles.user.remove"))
        {
            Text removeText1 = i18n.translate(ctx, NEGATIVE, "Click to remove role.");
            parents.stream().map(p -> {
                try{
                    return p.resolve().get();
                }
                catch (ExecutionException | InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }).forEach(parent -> {
                    // TODO perm check for each role
                    Text removeText = Text.of(RED, "(-)").toBuilder().onClick(TextActions.executeCallback(sender -> {
                        i18n.send(sender, NEGATIVE, "Do you really want to remove {role} from {user}?", parent, player);
                        ctx.sendMessage(i18n.translate(sender, TextFormat.NONE, "Confirm").toBuilder().color(DARK_GREEN).onClick(
                            TextActions.runCommand("/roles user remove " + player.getName() + " " + parent.getIdentifier())).build());
                    })).onHover(showText(removeText1)).build();
                    ctx.sendMessage(Text.of("- ", GOLD, parent.getIdentifier(), " ", removeText));
                });
        }
        else
        {
            parents.forEach(parent -> ctx.sendMessage(Text.of("- ", GOLD, parent.getSubjectIdentifier())));
        }
        String transientText = i18n.getTranslation(ctx, "transient");
        transientParents.forEach(parent -> ctx.sendMessage(Text.of("- ", GOLD, parent.getSubjectIdentifier(), GRAY, " (", YELLOW, transientText, GRAY, ")")));


    }


    @Alias(value = "checkuperm")
    @Command(alias = "checkperm", desc = "Checks for permissions of a user [in context]")
    public void checkPermission(CommandSource ctx, @Default User player, @Complete(PermissionCompleter.class) String permission, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);

        Text permText = RolesUtil.permText(ctx, permission, service, i18n);
        FoundPermission found = RolesUtil.findPermission(service, player, permission, contexts);
        FoundPermission foundNow = RolesUtil.findPermission(service, player, permission, player.getActiveContexts());

        i18n.send(ctx, NEUTRAL, "Player {user} permission check {txt#permission}", player, permText);
        if (found != null)
        {
            Text from = getFromText(ctx, player, found);
            if (found.value)
            {
                i18n.send(ctx, POSITIVE, "Set to {text:true:color=DARK_GREEN} in {context} {txt#info}", context, from);
            }
            else
            {
                i18n.send(ctx, NEGATIVE, "Set to {text:false:color=DARK_RED} in {context} {txt#info}", context, from);
            }
        }
        else
        {
            i18n.send(ctx, NEGATIVE, "Not set in {context}", context);
        }
        if (foundNow != null)
        {
            Text from = getFromText(ctx, player, foundNow);
            if (foundNow.value)
            {
                i18n.send(ctx, POSITIVE, "Set to {text:true:color=DARK_GREEN} in their active contexts {txt#info}", from);
            }
            else
            {
                i18n.send(ctx, NEGATIVE, "Set to {text:false:color=DARK_RED} in their active contexts {txt#info}", from);
            }
        }
        else
        {
            i18n.send(ctx, NEGATIVE, "Not set in {context}", context);
        }
    }

    private Text getFromText(CommandSource ctx, @Default User player, FoundPermission found)
    {
        Text from;
        if (found.subject == player)
        {
            from = i18n.translate(ctx, NEUTRAL, "Permission is directly assigned to the user!");
        }
        else
        {
            from = Text.of(i18n.translate(ctx, NEUTRAL, "Permission inherited from:"), Text.NEW_LINE,
                    TextColors.GOLD, RolesUtil.permText(ctx, found.permission, service, i18n), Text.NEW_LINE,
                           i18n.translate(ctx, NEUTRAL, "in the role {name}!", found.subject.getIdentifier()));
        }
        from = Text.of("(?)").toBuilder().onHover(TextActions.showText(from)).build();
        return from;
    }

    @Alias(value = "listuperm")
    @Command(alias = "listperm", desc = "List permission assigned to a user [in context]")
    public void listPermission(CommandSource ctx, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = toSet(context);
        Map<String, Boolean> permissions = new HashMap<>();
        if (all)
        {
            RolesUtil.fillPermissions(player, contexts, permissions, service);
        }
        else
        {
            permissions.putAll(player.getSubjectData().getPermissions(contexts));
        }
        if (permissions.isEmpty())
        {
            if (all)
            {
                i18n.send(ctx, NEUTRAL, "{user} has no permissions set in {context}.", player, context);
                return;
            }
            i18n.send(ctx, NEUTRAL, "{user} has no permissions set directly in {context}.", player, context);
            return;
        }
        i18n.send(ctx, NEUTRAL, "Permissions of {user} in {context}:", player, context);
        for (Map.Entry<String, Boolean> entry : permissions.entrySet())
        {
            ctx.sendMessage(Text.of("- ", YELLOW, RolesUtil.permText(ctx, entry.getKey(), service, i18n), TextColors.WHITE, ": ", GOLD, entry.getValue()));
        }
    }

    @Alias(value = {"checkUOption", "checkUData"})
    @Command(alias = {"checkData"}, desc = "Checks for options of a user [in context]")
    public void checkOption(CommandSource ctx, @Default User player, String key, @Named("in") @Default Context context)
    {
        Set<Context> contexts = toSet(context);

        Optional<FoundOption> option = RolesUtil.getOption(service, player, key, contexts, true);
        if (!option.isPresent())
        {
            i18n.send(ctx, NEUTRAL, "{input#key} is not set for {user} in {context}.", key, player, context);
            return;
        }

        i18n.send(ctx, NEUTRAL, "{input#key}: {input#value} is set for {user} in {context}.", key, option.get().value, player, context);
        if (option.get().subject.getIdentifier().equals(player.getIdentifier()))
        {
            i18n.send(ctx, NEUTRAL, "Options is directly assigned to the user!");
        }
        else
        {
            i18n.send(ctx, NEUTRAL, "Options inherited from the role {name}!", option.get().subject.getIdentifier());
        }
    }

    @Alias(value = {"listUOption", "listUData"})
    @Command(alias = "listData", desc = "Lists assigned options from a user [in context]")
    public void listOption(CommandSource ctx, @Default User player, @Named("in") @Default Context context, @Flag boolean all)
    {
        Set<Context> contexts = toSet(context);
        if (!all)
        {
            Map<String, String> options = (player.getSubjectData()).getOptions(contexts);
            i18n.send(ctx, NEUTRAL, "Options of {user} directly set in {context}:", player, context);
            for (Map.Entry<String, String> entry : options.entrySet())
            {
                ctx.sendMessage(Text.of("- ", YELLOW, entry.getKey(), TextColors.WHITE, ": ", GOLD, entry.getValue()));
            }
            return;
        }
        try
        {
            Subject subject = service.getUserSubjects().loadSubject(player.getIdentifier()).get();
            Map<String, FoundOption> options = new HashMap<>();
            RolesUtil.fillOptions(subject, contexts, options, service);
            i18n.send(ctx, NEUTRAL, "Options of {user} in {context}:", player, context);
            for (Map.Entry<String, FoundOption> entry : options.entrySet())
            {
                Subject owner = entry.getValue().subject;
                Text key = Text.of(YELLOW, entry.getKey()).toBuilder().onHover(TextActions.showText(Text.of(YELLOW, owner.getContainingCollection().getIdentifier(), GRAY, ":", YELLOW, owner.getFriendlyIdentifier()))).build();
                ctx.sendMessage(Text.of("- ", key, TextColors.WHITE, ": ", GOLD, entry.getValue().value));
            }
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
