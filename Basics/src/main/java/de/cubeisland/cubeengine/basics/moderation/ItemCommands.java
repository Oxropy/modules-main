package de.cubeisland.cubeengine.basics.moderation;

import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.basics.BasicsPerm;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.annotation.Command;
import de.cubeisland.cubeengine.core.command.annotation.Flag;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.matcher.EnchantMatcher;
import de.cubeisland.cubeengine.core.util.matcher.MaterialMatcher;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.cubeisland.cubeengine.core.command.exception.IllegalParameterValue.illegalParameter;
import static de.cubeisland.cubeengine.core.command.exception.InvalidUsageException.*;
import static de.cubeisland.cubeengine.core.command.exception.PermissionDeniedException.denyAccess;
import de.cubeisland.cubeengine.core.util.ChatFormat;

/**
 * item-related commands /itemdb /rename /headchange /unlimited /enchant /give
 * /item /more /repair /stack
 */
public class ItemCommands
{
    private Basics basics;

    public ItemCommands(Basics basics)
    {
        this.basics = basics;
    }

    @Command(desc = "Looks up an item for you!", max = 1, usage = "[item]")
    public void itemDB(CommandContext context)
    {
        if (context.hasIndexed(0))
        {
            ItemStack item = MaterialMatcher.get().matchItemStack(context.getString(0));
            if (item != null)
            {
                context.sendMessage("basics", "&aMatched &e%s &f(&e%d&f:&e%d&f) &afor &f%s",
                        MaterialMatcher.get().getNameFor(item), item.getType().getId(), item.getDurability(), context.getString(0));
            }
            else
            {
                context.sendMessage("basics", "&cCould not find any item named &e%s&c!", context.getString(0));
            }
        }
        else
        {
            User sender = context.getSenderAsUser("basics", "&cYou need 1 parameter!");
            if (sender.getItemInHand().getType().equals(Material.AIR))
            {
                invalidUsage(context, "basics", "&eYou hold nothing in your hands!");
            }
            else
            {
                String found = MaterialMatcher.get().getNameFor(sender.getItemInHand());
                if (found == null)
                {
                    context.sendMessage("basics", "&cItemname unknown! Itemdata: &e%d&f:&e%d&f",
                            sender.getItemInHand().getType().getId(),
                            sender.getItemInHand().getDurability());
                    return;
                }
                context.sendMessage("basics", "&aThe Item in your hand is: &e%s &f(&e%d&f:&e%d&f)",
                        found,
                        sender.getItemInHand().getType().getId(),
                        sender.getItemInHand().getDurability());
            }
        }
    }

    @Command(desc = "Changes the display name of the item in your hand.", usage = "<name> [-lore]", min = 1, flags =
    @Flag(longName = "lore", name = "l"))
    public void rename(CommandContext context)
    {//TODO better lore
        String name = context.getStrings(0);
        ItemStack item = context.getSenderAsUser("basics", "&cTrying to give your &etoys &ca name?").getItemInHand();
        ItemMeta meta = item.getItemMeta();
        name = ChatFormat.parseFormats(name);
        if (context.hasFlag("l"))
        {
            meta.setLore(new ArrayList<String>(Arrays.asList(name)));
        }
        else
        {
            meta.setDisplayName(name);
        }
        item.setItemMeta(meta);
        context.sendMessage("basics", "&aYou now hold &6%s &ain your hands!", name);
    }

    @Command(names =
    {
        "headchange", "skullchange"
    }, desc = "Changes a skull to a players skin.", usage = "<name>", min = 1)
    @SuppressWarnings("deprecation")
    public void headchange(CommandContext context)
    {
        String name = context.getString(0);
        User sender = context.getSenderAsUser("basics", "&cTrying to give your &etoys &ca name?");

        if (sender.getItemInHand().getType().equals(Material.SKULL_ITEM))
        {
            sender.getItemInHand().setDurability((short) 3);
            SkullMeta meta = ((SkullMeta) sender.getItemInHand().getItemMeta());
            meta.setOwner(name);
            sender.getItemInHand().setItemMeta(meta);
            context.sendMessage("basics", "&aYou now hold &6%s's &ahead in your hands!", name);
        }
        else
        {
            context.sendMessage("basics", "&cYou are not holding a head.");
        }
    }

    @Command(desc = "The user can use unlimited items", max = 1, usage = "[on|off]")
    @SuppressWarnings("deprecation")
    public void unlimited(CommandContext context)
    {
        User sender = context.getSenderAsUser("core", "&cThis command can only be used by a player!");
        boolean unlimited = false;
        if (context.hasIndexed(0))
        {
            if (context.getString(0).equalsIgnoreCase("on"))
            {
                unlimited = true;
            }
            else if (context.getString(0).equalsIgnoreCase("off"))
            {
                unlimited = false;
            }
            else
            {
                invalidUsage(context, "basics", "&eInvalid parameter! Use &aon &eor %coff&e!");
            }
        }
        else
        {
            Object bln = sender.getAttribute(basics, "unlimitedItems");
            unlimited = bln == null;
        }
        if (unlimited)
        {
            sender.setAttribute(basics, "unlimitedItems", true);
            context.sendMessage("basics", "&aYou now have unlimited items to build!");
        }
        else
        {
            sender.removeAttribute(basics, "unlimitedItems");
            context.sendMessage("basics", "&eYou now no longer have unlimited items to build!");
        }
    }

    @Command(desc = "Adds an Enchantment to the item in your hand", max = 2, flags =
    {
        @Flag(longName = "unsafe", name = "u")
    }, usage = "<enchantment> [level] [-unsafe]")
    public void enchant(CommandContext context)
    {
        if (!context.hasIndexed(0))
        {
            context.sendMessage("&aFollowing Enchantments are availiable:\n%s", this.getPossibleEnchantments(null));
            return;
        }
        User sender = context.getSenderAsUser("core", "&eWant to be Harry Potter?");
        ItemStack item = sender.getItemInHand();
        if (item.getType().equals(Material.AIR))
        {
            blockCommand(context, "basics", "&6ProTip: &eYou cannot enchant your fists!");
        }
        Enchantment ench = context.getIndexed(0, Enchantment.class, null);
        if (ench == null)
        {
            String possibleEnchs = this.getPossibleEnchantments(item);
            if (possibleEnchs != null)
            {
                blockCommand(context, "basics", "&cEnchantment &6%s &cnot found! Try one of those instead:\n%s", context.
                        getString(0), possibleEnchs);
            }
            else
            {
                blockCommand(context, "basics", "&cYou can not enchant this item!");
            }
        }
        int level = ench.getMaxLevel();
        if (context.hasIndexed(1))
        {
            level = context.getIndexed(1, Integer.class, 0);
            if (level <= 0)
            {
                illegalParameter(context, "basics", "&cThe enchantment-level has to be a number greater than 0!");
            }
        }
        if (context.hasFlag("u"))
        {
            if (BasicsPerm.COMMAND_ENCHANT_UNSAFE.isAuthorized(sender))
            {
                item.addUnsafeEnchantment(ench, level);
                context.sendMessage("basics", "&aAdded unsafe enchantment: &6%s %d &ato your item!",
                        EnchantMatcher.get().getNameFor(ench), level);
                return;
            }
            denyAccess(sender, "basics", "&cYou are not allowed to add unsafe enchantments!");
        }
        else
        {
            if (ench.canEnchantItem(item))
            {
                if ((level >= ench.getStartLevel()) && (level <= ench.
                        getMaxLevel()))
                {
                    item.addUnsafeEnchantment(ench, level);
                    context.sendMessage("bascics", "&aAdded enchantment: &6%s %d &ato your item!", EnchantMatcher.get().getNameFor(ench), level);
                    return;
                }
                blockCommand(context, "basics", "&cThis enchantment-level is not allowed!");
            }
            String possibleEnchs = this.getPossibleEnchantments(item);
            if (possibleEnchs != null)
            {
                blockCommand(context, "basics", "&cThis enchantment is not allowed for this item!\n&eTry one of those instead:\n%s", possibleEnchs);
            }
            else
            {
                blockCommand(context, "basics", "&cYou can not enchant this item!");
            }
        }
    }

    private String getPossibleEnchantments(ItemStack item)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Enchantment enchantment : Enchantment.values())
        {
            if (item == null || enchantment.canEnchantItem(item))
            {
                if (first)
                {
                    sb.append("&e").append(EnchantMatcher.get().getNameFor(enchantment));
                    first = false;
                }
                else
                {
                    sb.append("&f, &e").append(EnchantMatcher.get().getNameFor(enchantment));
                }
            }
        }
        if (sb.length() == 0)
        {
            return null;
        }
        return sb.toString();
    }

    @Command(desc = "Gives the specified Item to a player", flags =
    {
        @Flag(name = "b", longName = "blacklist")
    }, min = 2, max = 3, usage = "<player> <material[:data]> [amount] [-blacklist]")
    @SuppressWarnings("deprecation")
    public void give(CommandContext context)
    {
        User user = context.getUser(0);
        if (user == null)
        {
            paramNotFound(context, "core", "&cUser &2%s &cnot found!", context.getString(0));
        }
        ItemStack item = context.getIndexed(1, ItemStack.class, null);
        if (item == null)
        {
            paramNotFound(context, "core", "&cUnknown Item: &6%s&c!", context.getString(1));
        }
        if (!context.hasFlag("b") && BasicsPerm.COMMAND_GIVE_BLACKLIST.isAuthorized(context.getSender()))
        {
            if (this.basics.getConfiguration().blacklist.contains(item))
            {
                denyAccess(context, "basics", "&cThis item is blacklisted!");
            }
        }
        int amount = item.getMaxStackSize();
        if (context.hasIndexed(2))
        {
            amount = context.getIndexed(2, Integer.class, 0);
            if (amount == 0)
            {
                illegalParameter(context, "basics", "&cThe amount has to be a number greater than 0!");
            }
        }
        item.setAmount(amount);
        user.getInventory().addItem(item);
        user.updateInventory();
        String matname = MaterialMatcher.get().getNameFor(item);
        context.sendMessage("basics", "&aYou gave &2%s &e%d %s&a!", user.getName(), amount, matname);
        user.sendMessage("basics", "&2%s &ajust gave you &e%d %s&a!", context.getSender().getName(), amount, matname);
    }

    @Command(names =
    {
        "item", "i"
    }, desc = "Gives the specified Item to you", max = 2, min = 1, flags =
    {
        @Flag(longName = "blacklist", name = "b")
    }, usage = "<material[:data]> [amount] [-blacklist]")
    @SuppressWarnings("deprecation")
    public void item(CommandContext context)
    {
        User sender = context.getSenderAsUser("core", "&eDid you try to use &6/give &eon your new I-Tem?");
        ItemStack item = context.getIndexed(0, ItemStack.class, null);
        if (item == null)
        {
            paramNotFound(context, "core", "&cUnknown Item: &6%s&c!", context.getString(0));
        }
        if (!context.hasFlag("b") && BasicsPerm.COMMAND_ITEM_BLACKLIST.isAuthorized(sender))
        {
            if (this.basics.getConfiguration().blacklist.contains(item))
            {
                denyAccess(context, "basics", "&cThis item is blacklisted!");
            }
        }
        int amount = item.getMaxStackSize();
        if (context.hasIndexed(1))
        {
            amount = context.getIndexed(1, Integer.class, 0);
            if (amount == 0)
            {
                illegalParameter(context, "basics", "&cThe amount has to be a Number greater than 0!");
            }
        }
        item.setAmount(amount);
        sender.getInventory().addItem(item);
        sender.updateInventory();
        sender.sendMessage("basics", "&eReceived: %d %s ", amount, MaterialMatcher.get().getNameFor(item));
    }

    @Command(desc = "Refills the stack in hand", usage = "[amount] [-a]", flags =
    {
        @Flag(longName = "all", name = "a")
    }, max = 1)
    public void more(CommandContext context)
    {
        User sender = context.getSenderAsUser("core", "&cYou can't get enough of it. Don't you?");
        if (sender.getItemInHand() == null || sender.getItemInHand().getType() == Material.AIR)
        {
            invalidUsage(context, "basics", "&eMore nothing is still nothing!");
        }
        if (context.hasFlag("a"))
        {
            for (ItemStack item : sender.getInventory().getContents())
            {
                if (item.getType() != Material.AIR)
                {
                    item.setAmount(64);
                }
            }
            sender.sendMessage("basics", "&aRefilled all stacks!");
        }
        else
        {
            sender.getItemInHand().setAmount(64);
            if (context.hasIndexed(0))
            {
                Integer amount = context.getIndexed(0, Integer.class);
                if (amount == null || amount <=1)
                {
                    context.sendMessage("basics", "&cInvalid amount! (%s)", context.getString(0));
                    return;
                }
                for (int i = 1 ; i < amount ; ++i)
                {
                    sender.getInventory().addItem(sender.getItemInHand());
                }
                sender.sendMessage("basics", "&aRefilled &6%s &astacks in hand!", context.getString(0));
            }
            else
            {
                sender.sendMessage("basics", "&aRefilled stack in hand!");
            }

        }
    }

    @Command(desc = "Repairs your items", flags =
    {
        @Flag(longName = "all", name = "a")
    }, usage = "[-all]")
    // without item in hand
    public void repair(CommandContext context)
    {
        User sender = context.getSenderAsUser("core", "&eIf you do this you'll &cloose &eyour warranty!");
        if (context.hasFlag("a"))
        {
            List<ItemStack> list = new ArrayList<ItemStack>();
            list.addAll(Arrays.asList(sender.getInventory().getArmorContents()));
            list.addAll(Arrays.asList(sender.getInventory().getContents()));
            int repaired = 0;
            for (ItemStack item : list)
            {
                if (MaterialMatcher.get().isRepairable(item))
                {
                    item.setDurability((short) 0);
                    repaired++;
                }
            }
            if (repaired == 0)
            {
                sender.sendMessage("basics", "&eNo items to repair!");
            }
            else
            {
                sender.sendMessage("basics", "&aRepaired %d items!", repaired);
            }
        }
        else
        {
            ItemStack item = sender.getItemInHand();
            if (MaterialMatcher.get().isRepairable(item))
            {
                if (item.getDurability() == 0)
                {
                    sender.sendMessage("basics", "&eNo need to repair this!");
                    return;
                }
                item.setDurability((short) 0);
                sender.sendMessage("basics", "&aItem repaired!");
            }
            else
            {
                sender.sendMessage("basics", "&eItem cannot be repaired!");
            }
        }
    }

    @Command(desc = "Stacks your items up to 64")
    public void stack(CommandContext context)
    {
        User user = context.getSenderAsUser("basics", "&eNo stacking for you.");
        boolean allow64 = BasicsPerm.COMMAND_STACK_FULLSTACK.isAuthorized(user);
        ItemStack[] items = user.getInventory().getContents();
        int size = items.length;
        boolean changed = false;
        for (int i = 0; i < size; i++)
        {
            ItemStack item = items[i];
            // no null / infinite or unstackable items (if not allowed)
            if (item == null || item.getAmount() <= 0 || (!allow64 && item.getMaxStackSize() == 1))
            {
                continue;
            }
            int max = allow64 ? 64 : item.getMaxStackSize();
            if (item.getAmount() < max)
            {
                int needed = max - item.getAmount();
                for (int j = i + 1; j < size; j++) // search for same item
                {
                    ItemStack item2 = items[j];
                    // no null / infinite or unstackable items (if not allowed)
                    if (item2 == null || item2.getAmount() <= 0 || (!allow64 && item.getMaxStackSize() == 1))
                    {
                        continue;
                    }
                    // compare
                    if (item.isSimilar(item2))
                    {
                        if (item2.getAmount() > needed) // not enough place -> fill up stack
                        {
                            item.setAmount(max);
                            item2.setAmount(item2.getAmount() - needed);
                            break;
                        }
                        else
                        // enough place -> add to stack
                        {
                            items[j] = null;
                            item.setAmount(item.getAmount() + item2.getAmount());
                            needed = max - item.getAmount();
                        }
                        changed = true;
                    }
                }
            }
        }
        if (changed)
        {
            user.getInventory().setContents(items);
            user.sendMessage("&aItems stacked together!");
        }
        else
        {
            user.sendMessage("&eNothing to stack!");
        }
    }
}
