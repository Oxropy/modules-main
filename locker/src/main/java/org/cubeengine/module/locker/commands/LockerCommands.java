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
package org.cubeengine.module.locker.commands;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parameter.argument.Completer;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.storage.KeyBook;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.module.locker.storage.ProtectionFlag;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

@Command(name = "locker", desc = "Locker commands", alias = "l")
public class LockerCommands extends ContainerCommand
{
    private final Locker module;
    final LockManager manager;
    private StringMatcher sm;
    private I18n i18n;

    @Inject
    public LockerCommands(CommandManager base, Locker module, I18n i18n, StringMatcher sm)
    {
        super(base, Locker.class);
        this.module = module;
        this.manager = module.getManager();
        this.sm = sm;
        this.i18n = i18n;
    }

    @Alias(value = "cinfo")
    @Command(desc = "Shows information about a protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void info(Player context, @Flag boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }
        KeyBook keyBook = KeyBook.getKeyBook((context).getItemInHand(HandTypes.MAIN_HAND), context, this.module, i18n);
        if (keyBook != null)
        {
            Lock lock = this.manager.getLockById(keyBook.lockID);
            if (lock != null && keyBook.isValidFor(lock))
            {
                i18n.send(context, POSITIVE, "The strong magic surrounding this KeyBook allows you to access the designated protection");
                if (lock.isBlockLock())
                {
                    Location loc = lock.getFirstLocation();
                    i18n.send(context, POSITIVE, "The protection corresponding to this book is located at {vector} in {world}", loc.getBlockPosition(), loc.getExtent());
                }
                else
                {
                    for (Entity entity : context.getWorld().getEntities())
                    {
                        if (entity.getUniqueId().equals(lock.getEntityUID()))
                        {
                            Location loc = entity.getLocation();
                            i18n.send(context, POSITIVE, "The entity protection corresponding to this book is located at {vector} in {world}", loc.getBlockPosition(), loc.getExtent());
                            return;
                        }
                    }
                    i18n.send(context, POSITIVE, "Your magic is not strong enough to locate the corresponding entity protection!");
                }
            }
            else
            {
                i18n.send(context, NEUTRAL, "As you inspect the KeyBook closer you realize that its magic power has disappeared!");
                keyBook.invalidate();
            }
            return;
        }
        manager.commandListener.submitLockAction(context, (lock, loc, entity) -> {
            lock.showInfo(context);
        });
        i18n.send(context, POSITIVE, "Right click to show protection-info");
    }

    @Alias(value = "cpersist")
    @Command(desc = "persists your last locker command")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void persist(Player context)
    {
        if (this.manager.commandListener.persist(context))
        {
            i18n.send(context, POSITIVE, "Your commands will now persist!");
            return;
        }
        i18n.send(context, POSITIVE, "Your commands will now no longer persist!");
    }

    @Alias(value = "cremove")
    @Command(desc = "Shows information about a protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void remove(Player context, @Flag boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }
        this.manager.commandListener.submitLockAction(context, (lock, loc, entity) -> manager.removeLock(lock, context, false));
        i18n.send(context, POSITIVE, "Right click a protection to remove it!");
    }

    @Alias(value = "cunlock")
    @Command(desc = "Unlocks a password protected chest")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void unlock(Player context, String password, @Flag boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }
        manager.commandListener.submitLockAction(context, (lock, loc, entity) -> lock.unlock(context, lock.getFirstLocation(), password));
        i18n.send(context, POSITIVE, "Right click to unlock a password protected chest!");
    }

    @Alias(value = "cmodify")
    @Command(desc = "adds or removes player from the accesslist")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void modify(Player context, List<PlayerAccess> players, @Flag boolean global, @Flag boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }

        if (global)
        {
            this.manager.setGlobalAccess(context, players);
        }
        else
        {
            this.manager.commandListener.submitLockAction(context, (lock, loc, entity) -> lock.modifyLock(context, players));
            i18n.send(context, POSITIVE, "Right click a protection to modify it!");
        }
    }

    @Alias(value = "cgive")
    @Command(desc = "gives a protection to someone else")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void give(Player context, User player, @Flag boolean persist)
    {
        if (persist)
        {
            this.persist(context);
        }
        this.manager.commandListener.submitLockAction(context, (lock, loc, entity) -> {
            if (lock.isOwner(context) || player.hasPermission(module.perms().CMD_GIVE_OTHER.getId()))
            {
                lock.setOwner(player);
                i18n.send(context, NEUTRAL, "{user} is now the owner of this protection.", player);
                return;
            }
            i18n.send(context, NEGATIVE, "This is not your protection!");
        });
        i18n.send(context, POSITIVE, "Right click a protection to give it to {user}!", player);
    }

    @Alias(value = "ckey")
    @Command(desc = "creates a KeyBook or invalidates previous KeyBooks")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void key(Player context, @Flag boolean invalidate, @Flag boolean persist)
    {
        if (!this.module.getConfig().allowKeyBooks)
        {
            i18n.send(context, NEGATIVE, "KeyBooks are deactivated!");
            return;
        }
        if (persist)
        {
            this.persist(context);
        }
        if (invalidate)
        {
            this.manager.commandListener.submitLockAction(context, (lock, loc, entity) -> {
                if (!lock.isOwner(context))
                {
                    i18n.send(context, NEGATIVE, "This is not your protection!");
                    return;
                }
                if (lock.hasPass())
                {
                    i18n.send(context, NEUTRAL, "You cannot invalidate KeyBooks for password protected locks.");
                    i18n.send(context, POSITIVE, "Change the password to invalidate them!");
                    return;
                }
                lock.invalidateKeyBooks();
                Optional<TileEntity> te = loc.getTileEntity();
                if (te.isPresent() && te.get() instanceof Carrier)
                {
                    // TODO check if this is working
                    Sponge.getCauseStackManager().pushCause(context);
                    ((Carrier) te.get()).getInventory().<Container>query(QueryOperationTypes.INVENTORY_TYPE.of(Container.class)).getViewers().forEach(Player::closeInventory);
                }
            });
            i18n.send(context, POSITIVE, "Right click a protection to invalidate old KeyBooks for it!");
            return;
        }
        this.manager.commandListener.submitLockAction(context, (lock, loc, entity) -> {
            if (!lock.isOwner(context) && !context.hasPermission(module.perms().CMD_KEY_OTHER.getId()))
            {
                i18n.send(context, NEGATIVE, "This is not your protection!");
                return;
            }
            if (lock.isPublic())
            {
                i18n.send(context, NEUTRAL, "This protection is public!");
                return;
            }
            lock.attemptCreatingKeyBook(context, true);
        });
        i18n.send(context, POSITIVE, "Right click a protection to with a book to create a new KeyBook!");
    }

    @Alias(value = "cflag")
    @Command(desc = "Sets or unsets flags")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void flag(Player context,
                     @Named("set") @Complete(FlagCompleter.class) @Label("flags...") String setFlags,
                     @Named("unset") @Complete(FlagCompleter.class) @Label("flags...") String unsetFlags,
                     @Flag boolean persist)
    {
        if (setFlags == null && unsetFlags == null)
        {
            i18n.send(context, NEUTRAL, "You need to define which flags to {text:set} or {text:unset}!");
            i18n.send(context, NEUTRAL, "The following flags are available:");
            Text format = Text.of("  ", TextColors.GRAY, "-", TextColors.GOLD);
            for (String flag : ProtectionFlag.getNames())
            {
                context.sendMessage(Text.of(format, flag));
            }
            i18n.send(context, NEUTRAL, "You can also unset {text:all}");
            return;
        }
        if (persist)
        {
            this.persist(context);
        }
        if (setFlags != null && unsetFlags != null)
        {
            i18n.send(context, NEGATIVE, "You have cannot set and unset flags at the same time!");
            return;
        }
        if (setFlags != null)
        {
            this.manager.commandListener.submitLockAction(context, (lock, loc, entity) -> {
                if (!lock.isOwner(context) && !lock.hasAdmin(context) && !context.hasPermission(module.perms().CMD_MODIFY_OTHER.getId()))
                {
                    i18n.send(context, NEGATIVE, "You are not allowed to modify the flags for this protection!");
                    return;
                }
                short flags = 0;
                for (ProtectionFlag protectionFlag : ProtectionFlag.matchFlags(sm, setFlags))
                {
                    flags |= protectionFlag.flagValue;
                }
                lock.setFlags((short)(flags | lock.getFlags()));
                i18n.send(context, NEUTRAL, "Flags set!");
            });
        }
        else
        {
            this.manager.commandListener.submitLockAction(context, (lock, loc, entity) -> {
                if (!lock.isOwner(context) && !lock.hasAdmin(context) && !context.hasPermission(module.perms().CMD_MODIFY_OTHER.getId()))
                {
                    i18n.send(context, NEGATIVE, "You are not allowed to modify the flags for this protection!");
                    return;
                }
                if ("all".equalsIgnoreCase(unsetFlags))
                {
                    lock.setFlags(ProtectionFlag.NONE);
                    i18n.send(context, POSITIVE, "All flags are now unset!");
                    return;
                }
                short flags = 0;
                for (ProtectionFlag protectionFlag : ProtectionFlag.matchFlags(sm, unsetFlags))
                {
                    flags |= protectionFlag.flagValue;
                }
                lock.setFlags((short) (lock.getFlags() & ~flags));
                i18n.send(context, NEUTRAL, "Flags unset!");
            });
        }
        i18n.send(context, POSITIVE, "Right click a protection to change its flags!");
    }

    public static class FlagCompleter implements Completer
    {
        @Override
        public List<String> suggest(Class type, CommandInvocation invocation)
        {
            String subToken = invocation.currentToken();
            if (subToken.contains(","))
            {
                subToken = subToken.substring(subToken.lastIndexOf(",") + 1);
            }
            return ProtectionFlag.getTabCompleteList(invocation.currentToken(), subToken);
        }
    }

}
