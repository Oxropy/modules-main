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
package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EnchantMatcher;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult.Type;
import org.spongepowered.api.item.inventory.type.InventoryRow;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.item.inventory.ItemStackComparators.ITEM_DATA;
import static org.spongepowered.api.item.inventory.ItemStackComparators.TYPE;

/**
 * item-related commands
 * <p>/give
 * <p>/item
 * <p>/more
 * <p>/stack
 */
public class ItemCommands extends PermissionContainer
{
    private MaterialMatcher materialMatcher;
    private EnchantMatcher enchantMatcher;
    private I18n i18n;

    public final Permission COMMAND_ITEM_ENCHANTMENTS = register("command.item.enchantments.safe", "", null);
    public final Permission COMMAND_ITEM_ENCHANTMENTS_UNSAFE = register("command.item.enchantments.unsafe", "", null);

    public final Permission COMMAND_STACK_FULLSTACK = register("command.stack.fullstack", "", null);

    public ItemCommands(PermissionManager pm, MaterialMatcher materialMatcher, EnchantMatcher enchantMatcher, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.materialMatcher = materialMatcher;
        this.enchantMatcher = enchantMatcher;
        this.i18n = i18n;
    }

    @SuppressWarnings("deprecation")
    @Command(desc = "Gives the specified Item to a player")
    public void give(CommandSource context, User player, @Label("material[:data]") ItemStack item, @Optional Integer amount)
    {
        amount = amount == null ? item.getMaxStackQuantity() : amount;
        if (amount <= 0)
        {
            i18n.send(context, NEGATIVE, "The amount has to be a number greater than 0!");
            return;
        }
        item.setQuantity(amount);
        if (player.getInventory().offer(item.copy()).getType() == Type.SUCCESS)
        {
            String matname = materialMatcher.getNameFor(item);
            i18n.send(context, POSITIVE, "You gave {user} {amount} {input#item}!", player, amount, matname);
            if (player.isOnline())
            {
                i18n.send(player.getPlayer().get(), POSITIVE, "{user} just gave you {amount} {input#item}!", context.getName(), amount, matname);
            }
            return;
        }
        i18n.send(context, NEGATIVE, "{user} had no place for the item.");
    }

    @Command(alias = "i", desc = "Gives the specified Item to you")
    @Restricted(value = Player.class, msg = "Did you try to use {text:/give} on your new I-Tem?")
    @SuppressWarnings("deprecation")
    public void item(Player context, @Label("material[:data]") ItemStack item,
                     @Optional Integer amount,
                     @Named("ench") @Label("enchantment[:level]") String enchantmentString)
    {
        // TODO spawn_egg data
        amount = amount == null ? item.getMaxStackQuantity() : amount;
        if (amount <= 0)
        {
            i18n.send(context, NEGATIVE, "The amount has to be a number greater than 0!");
            return;
        }

        if (enchantmentString != null)
        {
            String[] enchs = StringUtils.explode(",", enchantmentString);
            for (String ench : enchs)
            {
                int enchLvl = 0;
                if (ench.contains(":"))
                {
                    enchLvl = Integer.parseInt(ench.substring(ench.indexOf(":") + 1, ench.length()));
                    ench = ench.substring(0, ench.indexOf(":"));
                }
                if (context.hasPermission(COMMAND_ITEM_ENCHANTMENTS.getId()))
                {
                    if (context.hasPermission(COMMAND_ITEM_ENCHANTMENTS_UNSAFE.getId()))
                    {
                        enchantMatcher.applyMatchedEnchantment(item, ench, enchLvl, true);
                    }
                    else
                    {
                        enchantMatcher.applyMatchedEnchantment(item, ench, enchLvl, false);
                    }
                }
            }
        }
        item.setQuantity(amount);
        Inventory hotbarFirstInventory = getHotbarFirst(context.getInventory());
        if (!hotbarFirstInventory.offer(item.copy()).getRejectedItems().isEmpty())
        {
            i18n.send(context, NEGATIVE, "Not enough space for the item!");
            return;
        }
        i18n.send(context, NEUTRAL, "Received: {amount} {input#item}", amount, materialMatcher.getNameFor(item));
    }

    public Inventory getHotbarFirst(Inventory inventory)
    {
        return inventory.query(QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class))
        .union(inventory.query(QueryOperationTypes.INVENTORY_TYPE.of(MainPlayerInventory.class)));
    }

    @Command(desc = "Refills the stack in hand")
    @Restricted(value = Player.class, msg = "You can't get enough of it, can you?")
    public void more(Player context, @Optional Integer amount, @Flag boolean all)
    {
        if (all)
        {
            for (Inventory slot : context.getInventory().slots())
            {
                if (slot.peek().isPresent())
                {
                    ItemStack item = slot.peek().get();
                    item.setQuantity(64);
                }
            }
            i18n.send(context, POSITIVE, "Refilled all stacks!");
            return;
        }
        amount = amount == null ? 1 : amount;
        if (amount < 1)
        {
            i18n.send(context, NEGATIVE, "Invalid amount {input#amount}", amount);
            return;
        }

        java.util.Optional<ItemStack> item = context.getItemInHand(HandTypes.MAIN_HAND);
        if (!item.isPresent())
        {
            i18n.send(context, NEUTRAL, "More nothing is still nothing!");
            return;
        }
        item.get().setQuantity(item.get().getMaxStackQuantity());
        context.setItemInHand(HandTypes.MAIN_HAND, item.get());
        if (amount == 1)
        {
            i18n.send(context, POSITIVE, "Refilled stack in hand!");
            return;
        }
        for (int i = 1; i < amount; ++i)
        {
            context.getInventory().offer(item.get().copy());
        }
        i18n.send(context, POSITIVE, "Refilled {amount} stacks in hand!", amount);
    }



    @Command(desc = "Stacks your items up to 64")
    @Restricted(value = Player.class, msg = "No stacking for you.")
    public void stack(Player context)
    {
        boolean allow64 = context.hasPermission(COMMAND_STACK_FULLSTACK.getId());
        allow64 = false; // TODO this is currently not working /w Sponge
        Inventory rows = context.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(InventoryRow.class));
        ItemStack[] items = new ItemStack[rows.capacity()];
        int slotIndex = 0;
        for (Inventory row : rows)
        {
            for (Inventory slot : row.slots())
            {
                items[slotIndex++] = slot.peek().orElse(null);
            }
        }

        int size = items.length;
        boolean changed = false;
        for (int i = 0; i < size; i++)
        {
            ItemStack item = items[i];
            // no null / infinite or unstackable items (if not allowed)
            if (item == null || item.getQuantity() <= 0 || (!allow64 && item.getMaxStackQuantity() == 1))
            {
                continue;
            }
            int max = allow64 ? 64 : item.getMaxStackQuantity();
            if (item.getQuantity() < max)
            {
                int needed = max - item.getQuantity();
                for (int j = i + 1; j < size; j++) // search for same item
                {
                    ItemStack item2 = items[j];
                    // no null / infinite or unstackable items (if not allowed)
                    if (item2 == null || item2.getQuantity() <= 0 || (!allow64 && item.getMaxStackQuantity() == 1))
                    {
                        continue;
                    }
                    // compare
                    if (TYPE.compare(item, item2) == 0 && ITEM_DATA.compare(item, item2) == 0)
                    {
                        if (item2.getQuantity() > needed) // not enough place -> fill up stack
                        {
                            item.setQuantity(max);
                            item2.setQuantity(item2.getQuantity() - needed);
                            break;
                        }
                        // enough place -> add to stack
                        {
                            items[j] = null;
                            item.setQuantity(item.getQuantity() + item2.getQuantity());
                            needed = max - item.getQuantity();
                        }
                        changed = true;
                    }
                }
            }
        }
        if (changed)
        {
            int i = 0;
            for (Inventory row : rows)
            {
                for (Inventory slot : row.slots())
                {
                    ItemStack item = items[i++];
                    if (item == null)
                    {
                        slot.clear();
                    }
                    else
                    {
                        slot.set(item);
                    }
                }
            }
            i18n.send(context, POSITIVE, "Items stacked together!");
            return;
        }
        i18n.send(context, NEUTRAL, "Nothing to stack!");
    }
}
