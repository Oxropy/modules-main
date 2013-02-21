package de.cubeisland.cubeengine.signmarket;

import de.cubeisland.cubeengine.conomy.account.Account;
import de.cubeisland.cubeengine.conomy.currency.Currency;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.ChatFormat;
import de.cubeisland.cubeengine.core.util.InventoryGuardFactory;
import de.cubeisland.cubeengine.core.util.InventoryUtil;
import de.cubeisland.cubeengine.core.util.RomanNumbers;
import de.cubeisland.cubeengine.core.util.matcher.Match;
import de.cubeisland.cubeengine.signmarket.storage.SignMarketBlockModel;
import de.cubeisland.cubeengine.signmarket.storage.SignMarketItemModel;
import gnu.trove.map.hash.TLongLongHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static de.cubeisland.cubeengine.core.util.InventoryUtil.*;

public class MarketSign
{
    private final Signmarket module;
    private SignMarketItemModel itemInfo;
    private SignMarketBlockModel blockInfo;

    private TLongLongHashMap breakingSign = new TLongLongHashMap();

    private Currency currency;
    private boolean editMode;

    public MarketSign(Signmarket module, Location location)
    {
        this(module, location, null);
    }

    public MarketSign(Signmarket module, Location location, User owner)
    {
        this.itemInfo = new SignMarketItemModel();
        this.blockInfo = new SignMarketBlockModel(location);
        this.blockInfo.setOwner(owner);
        this.module = module;
    }

    /**
     * Saves all MarketSignData into the database
     */
    public void saveToDatabase()
    {
        if (this.isValidSign(null))
        {
            this.module.getMarketSignFactory().syncAndSaveSign(this);
        }
        this.updateSign();
    }

    public void breakSign()
    {
        this.dropContents();
        this.module.getMarketSignFactory().delete(this);
    }

    public void dropContents()
    {
        if (this.isAdminSign() || !this.itemInfo.hasStock() || this.editMode || this.itemInfo.stock <= 0 || this.itemInfo.sharesStock()) // no stock / edit mode / shared stock
        {
            return;
        }
        ItemStack item = this.itemInfo.getItem().clone();
        item.setAmount(this.itemInfo.stock);
        this.itemInfo.stock = 0; // just to be sure no items are duped
        if (this.module.getConfig().allowOverStackedOutOfSign)
        {
            this.getLocation().getWorld().dropItemNaturally( this.getLocation(), item);
            return;
        }
        for (ItemStack itemStack : splitIntoMaxItems(item, item.getMaxStackSize()))
        {
            this.getLocation().getWorld().dropItemNaturally(this.getLocation(), itemStack);
        }
    }

    /**
     * Sets the itemstack to buy/sell
     *
     * @param itemStack
     */
    public void setItemStack(ItemStack itemStack, boolean setAmount)
    {
        this.itemInfo.setItem(itemStack);
        this.blockInfo.amount = itemStack.getAmount();
    }

    /**
     * Changes this market-sign to be a BUY-sign
     */
    public void setBuy()
    {
        this.blockInfo.signType = true;
        this.blockInfo.demand = null;
    }

    /**
     * Changes this market-sign to be a SELL-sign
     */
    public void setSell()
    {
        this.blockInfo.signType = false;
    }

    /**
     * Sets the owner of this market-sign to given user.
     * <p>Sets stock to 0 if null before
     *
     * @param user
     */
    public void setOwner(User user)
    {
        if (user == null)
        {
            throw new IllegalArgumentException("Use setAdminSign() instead!");
        }
        this.blockInfo.owner = user.key;
        if (this.getStock() == null)
        {
            this.setStock(0);
        }
    }

    /**
     * Sets this market-sign to be an admin sign
     * <p>owner = null
     * <p>demand = null
     */
    public void setAdminSign()
    {
        this.blockInfo.owner = null;
        this.blockInfo.demand = null;
    }

    /**
     * Sets the amount to buy/sell with each click
     *
     * @param amount
     */
    public void setAmount(int amount)
    {
        if (amount < 0)
            throw new IllegalArgumentException("The amount has to be greater than 0!");
        this.blockInfo.amount = amount;
    }

    /**
     * Sets the price to buy/sell the specified amount of items with each click
     *
     * @param price
     */
    public void setPrice(long price)
    {
        this.blockInfo.price = price;
    }

    /**
     * Tries to execute the appropriate action
     * <p>on right-click: use the sign (buy/sell) / if owner take out of stock
     * <p>on left-click: BUY-sign: if correct item in hand & owner of sign -> refill stock
     * <p>on shift left-click: open sign-inventory OR if correct item in hand & owner put all in stock
     * <p>on shift right-click: inspect the sign, shows all information saved
     *
     * @param user
     * @return true if the event shall be canceled
     */
    public boolean executeAction(User user, Action type)
    {
        boolean sneaking = user.isSneaking();
        ItemStack itemInHand = user.getItemInHand();
        if (itemInHand == null)
        {
            itemInHand = new ItemStack(Material.AIR);
        }
        switch (type)
        {
            case LEFT_CLICK_BLOCK:
                if (sneaking)
                {
                    if (this.editMode)
                    {
                        user.sendMessage("signmarket", "&cThis sign is being edited right now!");
                        return true;
                    }
                    if (!this.isAdminSign() && (this.isOwner(user) || MarketSignPerm.SIGN_INVENTORY_ACCESS_OTHER.isAuthorized(user)))
                    {
                        if (this.isBuySign() && this.itemInfo.matchesItem(itemInHand))
                        {
                            int amount = this.putItems(user, true);
                            if (amount != 0)
                                user.sendMessage("signmarket", "&aAdded all (&6%d&a) &6%s &ato the stock!", amount, Match.material().getNameFor(this.itemInfo.getItem()));
                            return true;
                        }
                    }
                    if (MarketSignPerm.SIGN_INVENTORY_SHOW.isAuthorized(user))
                    {
                        final Inventory inventory = this.getInventory();
                        Runnable onClose = new Runnable() {
                            @Override
                            public void run()
                            {
                                if (!MarketSign.this.isAdminSign())
                                {
                                    MarketSign.this.setStock(InventoryUtil.getAmountOf(inventory, MarketSign.this.itemInfo.getItem()));
                                }
                                MarketSign.this.saveToDatabase();
                            }
                        };
                        Runnable onChange = new Runnable() {
                            @Override
                            public void run()
                            {
                                MarketSign.this.setStock(InventoryUtil.getAmountOf(inventory, MarketSign.this.itemInfo.getItem()));
                                MarketSign.this.updateSign();
                            }
                        };
                        InventoryGuardFactory guard = InventoryGuardFactory.prepareInventory(inventory, user)
                                .blockPutInAll().blockTakeOutAll()
                                .onClose(onClose).onChange(onChange);
                        if (this.isAdminSign())
                        {
                            guard.submitInventory(this.module, true);
                        }
                        else if (this.isOwner(user) || MarketSignPerm.SIGN_INVENTORY_ACCESS_OTHER.isAuthorized(user))
                        {
                            ItemStack itemInSign = this.itemInfo.getItem();
                            if (this.isBuySign())
                            {
                                guard.notBlockPutIn(itemInSign).notBlockTakeOut(itemInSign);
                            }
                            else
                            {
                                guard.notBlockTakeOut(itemInSign);
                            }
                            guard.submitInventory(this.module, true);
                        }
                        else
                        {
                            guard.submitInventory(this.module, true);
                        }
                    }
                    else
                    {
                        user.sendMessage("signmarket", "&cYou are not allowed to see the market-signs inventories");
                    }
                }
                else
                // no sneak -> empty & break signs
                {
                    if (this.editMode)
                    {
                        user.sendMessage("signmarket", "&cThis sign is being edited right now!");
                        return true;
                    }
                    if (this.isOwner(user) || MarketSignPerm.SIGN_INVENTORY_ACCESS_OTHER.isAuthorized(user))
                    {
                        if (!this.editMode && this.blockInfo.isBuyOrSell() && this.isBuySign() && this.itemInfo.matchesItem(itemInHand))
                        {
                            int amount = this.putItems(user, false);
                            if (amount != 0)
                                user.sendMessage("signmarket", "&aAdded &6%d&ax &6%s &ato the stock!", amount, Match.material().getNameFor(this.itemInfo.getItem()));
                            return true;
                        }
                        else if (itemInHand != null && itemInHand.getTypeId() != 0)
                        {
                            user.sendMessage("signmarket", "&cUse bare hands to break the sign!");
                            return true;
                        }
                    }
                    if (user.getGameMode().equals(GameMode.CREATIVE)) // instabreak items
                    {
                        if (this.isOwner(user))
                        {
                            if (MarketSignPerm.SIGN_DESTROY_OWN.isAuthorized(user))
                            {
                                this.tryBreak(user);
                            }
                            else
                            {
                                user.sendMessage("signmarket", "&cYou are not allowed to break your own market-signs!");
                            }
                        }
                        else if (this.isAdminSign())
                        {
                            if (MarketSignPerm.SIGN_DESTROY_ADMIN.isAuthorized(user))
                            {
                                this.tryBreak(user);
                            }
                            else
                            {
                                user.sendMessage("signmarket", "&cYou are not allowed to break admin-market-signs!");
                            }
                        }
                        else
                        {
                            if (MarketSignPerm.SIGN_DESTROY_OTHER.isAuthorized(user))
                            {
                                this.tryBreak(user);
                            }
                            else
                            {
                                user.sendMessage("signmarket", "&cYou are not allowed to break others market-signs!");
                            }
                        }
                    }
                    else
                    // first empty items then break
                    {
                        if (this.isAdminSign())
                        {
                            if (MarketSignPerm.SIGN_DESTROY_ADMIN.isAuthorized(user))
                            {
                                this.tryBreak(user);
                            }
                            else
                            {
                                user.sendMessage("signmarket", "&cYou are not allowed to break admin-signs!");
                            }
                        }
                        else if (this.isOwner(user))
                        {
                            if (MarketSignPerm.SIGN_DESTROY_OWN.isAuthorized(user))
                            {
                                this.tryBreak(user);
                            }
                            else
                            {
                                user.sendMessage("signmarket", "&cYou are not allowed to break your own market-signs!");
                            }
                        }
                        else
                        // not owner / not admin
                        {
                            if (MarketSignPerm.SIGN_DESTROY_OTHER.isAuthorized(user))
                            {
                                this.tryBreak(user);
                            }
                            else
                            {
                                user.sendMessage("signmarket", "&cYou are not allowed to destroy others market-signs!");
                            }
                        }
                    }
                }
                return true;
            case RIGHT_CLICK_BLOCK:
                if (sneaking)
                {
                    this.showInfo(user);
                    return true;
                }
                else
                {
                    if (this.isOwner(user))
                    {
                        this.takeItems(user);
                        return true;
                    }
                    if (!this.editMode)
                        return this.useSign(user);
                    user.sendMessage("signmarket", "&cThis sign is beeing edited right now!");
                    return true;
                }
        }
        return false;
    }

    public void showInfo(User user)
    {
        if (this.editMode)
        {
            user.sendMessage("signmarket", "\n-- &5Sign Market - Edit Mode &f--");
        }
        else
        {
            user.sendMessage("signmarket", "\n--------- &6Sign Market &f---------");
        }
        if (!this.blockInfo.isBuyOrSell())
        {
            user.sendMessage("&5new Sign");
            return;
        }
        if (this.isBuySign())
        {
            if (this.isAdminSign())
            {
                user.sendMessage("signmarket", "&3Buy: &6%d &ffor &6%s &ffrom &6%s", this.getAmount(), this.parsePrice(), "Server");
            }
            else
            {
                user.sendMessage("signmarket", "&3Buy: &6%d &ffor &6%s &ffrom &2%s", this.getAmount(), this.parsePrice(),
                        this.blockInfo.getOwner().getName());
            }
        }
        else
        {
            if (this.isAdminSign())
            {
                user.sendMessage("signmarket", "&3Sell: &6%d &ffor &6%s &fto &6%s", this.getAmount(), this.parsePrice(), "Server");
            }
            else
            {
                user.sendMessage("signmarket", "&3Sell: &6%d &ffor &6%s &fto &2%s", this.getAmount(), this.parsePrice(),
                        this.blockInfo.getOwner().getName());
            }
        }
        if (this.getItem() == null)
        {
            if (this.isInEditMode())
            {
                user.sendMessage("signmarket", "&5No Item");
            }
            else
            {
                user.sendMessage("signmarket", "&4No Item");
            }
        }
        else if (this.itemInfo.getItem().getItemMeta().hasDisplayName() || this.getItem().getItemMeta().hasLore() || !this.getItem().getEnchantments().isEmpty())
        {
            if (this.getItem().getItemMeta().hasDisplayName())
            {
                user.sendMessage("&e" + Match.material().getNameFor(this.getItem()) + " &f(&6" + this.getItem().getItemMeta().getDisplayName() + "&f)");
                if (this.getItem().getItemMeta().hasLore())
                {
                    for (String loreLine : this.getItem().getItemMeta().getLore())
                    {
                        user.sendMessage(" &e-&f " + loreLine);
                    }
                }
                if (!this.getItem().getEnchantments().isEmpty())
                {
                    user.sendMessage("signmarket", "&6Enchantments:");
                }
                for (Map.Entry<Enchantment, Integer> entry : this.getItem().getEnchantments().entrySet())
                {
                    user.sendMessage(" &e-&6 " + Match.enchant().nameFor(entry.getKey()) + " &e" + RomanNumbers.intToRoman(entry.getValue()));
                }
            }
            else
            {
                user.sendMessage("&e" + Match.material().getNameFor(this.getItem()));
            }
        }
        else
        {
            user.sendMessage("&6" + Match.material().getNameFor(this.getItem()));
        }
        if (this.hasStock())
        {
            if (this.isBuySign() == null)
            {
                user.sendMessage("signmarket", "&5New Sign");
            }
            if (this.isBuySign())
            {
                if (this.getItem() == null || this.getAmount() == 0)
                {
                    user.sendMessage("signmarket", "&3In stock: &6%d&f/&cUnkown", this.itemInfo.stock);
                }
                else
                {
                    Integer maxAmount;
                    if (this.isAdminSign())
                    {
                        user.sendMessage("signmarket", "&3In stock: &6%d&f/&6Infinite", this.itemInfo.stock); //TODO config infinite stock for admin?
                        return;
                    }
                    int maxStack = this.getItem().getMaxStackSize();
                    if (maxStack == 64)
                    {
                        maxAmount = 3456; // DoubleChest of 64
                    }
                    else if (this.module.getConfig().allowOverStackedInSign || maxStack > this.getAmount())
                    {
                        if (this.getAmount() > 64)
                        {
                            maxAmount = 3456;
                        }
                        else
                        {
                            maxAmount = 6*9*this.getAmount();
                        }
                    }
                    else
                    {
                        maxAmount = 6*9*maxStack;
                    }
                    user.sendMessage("signmarket", "&3In stock: &6%d&f/&6%d", this.itemInfo.stock, maxAmount);
                }
            }
            else
            {
                if (this.hasDemand())
                {
                    user.sendMessage("signmarket", "&3In stock: &6%d&f/&6%d", this.itemInfo.stock, this.blockInfo.demand);
                }
                else
                {
                    if (this.getItem() == null || this.getAmount() == 0)
                    {
                        user.sendMessage("signmarket", "&3In stock: &6%d&f/&cUnkown", this.itemInfo.stock);
                    }
                    else
                    {
                        Integer maxAmount;
                        if (this.isAdminSign())
                        {
                            //TODO max stock of admin signs?
                            user.sendMessage("signmarket", "&3In stock: &6%d&f/&6Infinite", this.itemInfo.stock);
                        }
                        else
                        {
                            int maxStack = this.getItem().getMaxStackSize();
                            if (maxStack == 64)
                            {
                                maxAmount = 3456; // DoubleChest of 64
                            }
                            else if (this.module.getConfig().allowOverStackedInSign || maxStack > this.getAmount())
                            {
                                if (this.getAmount() > 64)
                                {
                                    maxAmount = 3456;
                                }
                                else
                                {
                                    maxAmount = 6*9*this.getAmount();
                                }
                            }
                            else
                            {
                                maxAmount = 6*9*maxStack;
                            }
                            user.sendMessage("signmarket", "&3In stock: &6%d&f/&6%d", this.itemInfo.stock, maxAmount);
                        }

                    }
                }
            }
        }
    }

    public boolean hasStock() {
        return this.itemInfo.hasStock();
    }

    public boolean hasDemand()
    {
        return this.getDemand() != null;
    }

    private String parsePrice()
    {
        Currency currency = this.getCurrency();
        if (currency == null || this.blockInfo.price == 0)
        {
            if (this.isInEditMode())
            {
                return "&5No Price";
            }
            else
            {
                return "&4No Price";
            }
        }
        return this.getCurrency().formatShort(this.blockInfo.price);
    }

    @SuppressWarnings("deprecation")
    private int putItems(User user, boolean all)
    {
        int amount;
        if (all)
        {
            amount = getAmountOf(user.getInventory(), user.getItemInHand());
        }
        else
        {
            amount = user.getItemInHand().getAmount();
        }
        this.itemInfo.stock = this.itemInfo.stock + amount;
        ItemStack item = this.getItem().clone();
        item.setAmount(amount);
        user.getInventory().removeItem(item);
        Map<Integer, ItemStack> additional = this.addToInventory(this.getInventory(),item);
        int amountGivenBack = 0;
        for (ItemStack itemStack : additional.values())
        {
            amountGivenBack += itemStack.getAmount();
            user.getInventory().addItem(itemStack);
        }
        if (amountGivenBack != 0)
        {
            user.sendMessage("&cThe market-sign inventory is full!");
        }
        user.updateInventory();
        this.saveToDatabase();
        return amount - amountGivenBack;
    }

    private Map<Integer, ItemStack> addToInventory(Inventory inventory, ItemStack item)
    {
        if (this.module.getConfig().allowOverStackedInSign)
        {
            if (this.getAmount() > 64)
            {
                return inventory.addItem(splitIntoMaxItems(item, 64));
            }
            return inventory.addItem(splitIntoMaxItems(item, this.getAmount()));
        }
        else
        {
            return inventory.addItem(splitIntoMaxItems(item, item.getMaxStackSize()));
        }
    }

    private Map<Integer, ItemStack> addToUserInventory(User user, ItemStack item)
    {
        if (this.module.getConfig().allowOverStackedOutOfSign)
        {
            return user.getInventory().addItem(splitIntoMaxItems(item, 64));
        }
        else
        {
            return user.getInventory().addItem(splitIntoMaxItems(item, item.getMaxStackSize()));
        }
    }

    @SuppressWarnings("deprecation")
    private void takeItems(User user)
    {
        int amountToTake = this.getAmount();
        if (this.getStock() < amountToTake)
        {
            amountToTake = this.getStock();
        }
        if (amountToTake <= 0)
        {
            user.sendMessage("marketsign", "&cThere are no more items stored in the sign!");
            return;
        }
        ItemStack item = this.getItem().clone();
        item.setAmount(amountToTake);

        this.getInventory().removeItem(item);
        Map<Integer, ItemStack> additional = this.addToUserInventory(user, item);
        int amountGivenBack = 0;
        for (ItemStack itemStack : additional.values())
        {
            amountGivenBack += itemStack.getAmount();
            this.addToInventory(this.getInventory(),itemStack);
        }
        if (amountGivenBack != 0 && (amountGivenBack == this.getAmount() || amountGivenBack == this.getStock()))
        {
            user.sendMessage("&cYour inventory is full!");
        }
        user.updateInventory();
        MarketSign.this.setStock(InventoryUtil.getAmountOf(this.getInventory(), MarketSign.this.itemInfo.getItem()));
        this.saveToDatabase();
    }

    public boolean tryBreak(User user)
    {
        if (this.breakingSign.containsKey(user.key) && System.currentTimeMillis() - this.breakingSign.get(user.key) <= 200)//0.2 sec
        {
            Location location = this.getLocation();
            if (this.getStock() != null && this.getStock() == 1337) //pssst i am not here
            {
                location.getWorld().strikeLightningEffect(location);
            }
            this.breakSign();
            location.getWorld().getBlockAt(location).breakNaturally();
            this.breakingSign.remove(user.key);
            return true;
        }
        this.breakingSign.put(user.key, System.currentTimeMillis());
        user.sendMessage("signmarket", "&eDoubleclick to break the sign!");
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean useSign(User user)
    {
        if (this.isValidSign(user))
        {
            if (this.isBuySign())
            {
                if (!this.hasStock() || this.getStock() >= this.getAmount())
                {
                    if (this.canAfford(user))
                    {
                        Account userAccount = this.module.getConomy().getAccountsManager().getAccount(user, this.getCurrency());
                        Account ownerAccount = this.module.getConomy().getAccountsManager().getAccount(this.getOwner(), this.getCurrency());
                        ItemStack item = this.getItem().clone();
                        item.setAmount(this.getAmount());
                        if (checkForPlace(user.getInventory(), item.clone()))
                        {
                            this.module.getConomy().getAccountsManager().transaction(userAccount, ownerAccount, this.getPrice());
                            if (!this.isAdminSign() || this.hasStock())
                            {
                                if (this.isAdminSign())
                                {
                                    this.setStock(this.getStock() - this.getAmount());
                                }
                                else
                                {
                                    this.getInventory().removeItem(item);
                                }
                                this.saveToDatabase();
                            } // else admin sign -> no change
                            user.getInventory().addItem(item);
                            user.updateInventory();
                            user.sendMessage("signmarket","&aYou bought &6%dx %s &afor &6%s&a.",this.getAmount(),Match.material().getNameFor(this.getItem()),this.parsePrice());
                        }
                        else
                        {
                            user.sendMessage("signmarket", "&cYou do not have enough space for these items!");
                        }
                    }
                    else
                    {
                        user.sendMessage("signmarket", "&cYou cannot afford the price of these items!");
                    }
                }
                else
                {
                    user.sendMessage("signmarket", "&cThis market-sign is &4&lSold Out&c!");
                }
            }
            else
            {
                if (this.isAdminSign() || this.getDemand() == null || this.getDemand() - this.getStock() > 0)
                {
                    if (this.isAdminSign() || this.canAfford(this.getOwner()))
                    {
                        if (getAmountOf(user.getInventory(), this.getItem()) >= this.getAmount())
                        {
                            ItemStack item = this.getItem().clone();
                            item.setAmount(this.getAmount());
                            if (this.isAdminSign()
                                || (this.module.getConfig().allowOverStackedInSign
                                    && checkForPlace(this.getInventory(), splitIntoMaxItems(item, 64)))
                                || (!this.module.getConfig().allowOverStackedInSign
                                    && checkForPlace(this.getInventory(), splitIntoMaxItems(item, item.getMaxStackSize()))))
                            {
                                Account userAccount = this.module.getConomy().getAccountsManager().getAccount(user, this.getCurrency());
                                Account ownerAccount = this.module.getConomy().getAccountsManager().getAccount(this.getOwner(), this.getCurrency());
                                this.module.getConomy().getAccountsManager().transaction(ownerAccount, userAccount, this.getPrice());
                                user.getInventory().removeItem(item);
                                if (this.hasStock())
                                {
                                    if (this.isAdminSign())
                                    {
                                        this.setStock(this.getStock()+this.getAmount());
                                    }
                                    else
                                    {
                                        this.addToInventory(this.getInventory(),item);
                                        this.setStock(InventoryUtil.getAmountOf(this.getInventory(),this.getItem()));
                                    }
                                    this.saveToDatabase();
                                } // else admin sign -> no change
                                user.updateInventory();
                                user.sendMessage("signmarket","&aYou sold &6%dx %s &afor &6%s&a.",this.getAmount(),Match.material().getNameFor(this.getItem()),this.parsePrice());
                            }
                            else
                            {
                                user.sendMessage("signmarket", "&cThis market-sign is full and cannot accept more items!");
                            }
                        }
                        else
                        {
                            user.sendMessage("signmarket", "&cYou do not have enough items to sell!");
                        }
                    }
                    else
                    {
                        user.sendMessage("signmarket", "&cThe owner cannot afford the money to aquire your items!");
                    }
                }
                else
                {
                    user.sendMessage("signmarket", "&cThis market-sign is &4&lsatisfied&c! You can no longer sell items to it.");
                }
            }
            return true;
        }
        return false;
    }

    public User getOwner() {
        return this.blockInfo.getOwner();
    }

    public boolean isValidSign(User user)
    {
        boolean result = true;
        if (!this.blockInfo.isBuyOrSell())
        {
            if (user != null)
                user.sendMessage("signmarket", "&cNo sign-type given!");
            result = false;
        }
        if (this.blockInfo.amount <= 0)
        {
            if (user != null)
                user.sendMessage("signmarket", "&cInvalid amount!");
            result = false;
        }
        if (this.blockInfo.price <= 0)
        {
            if (user != null)
                user.sendMessage("signmarket", "&cInvalid price!");
            result = false;
        }
        if (this.itemInfo.getItem() == null)
        {
            if (user != null)
                user.sendMessage("signmarket", "&cNo item given!");
            result = false;
        }
        if (this.blockInfo.currency == null)
        {
            if (user != null)
                user.sendMessage("signmarket", "&cNo currency given!");
            result = false;
        }
        return result;
    }

    public boolean isOwner(User user)
    {
        return this.blockInfo.isOwner(user);
    }

    public void updateSign()
    {
        Block block = this.getLocation().getWorld().getBlockAt(this.getLocation());
        if (block.getState() instanceof Sign)
        {
            Sign blockState = (Sign)block.getState();
            String[] lines = new String[4];
            boolean isValid = this.isValidSign(null);
            if (this.isInEditMode())
            {
                if (this.isAdminSign())
                {
                    lines[0] = "&5&lAdmin-";
                }
                else
                {
                    lines[0] = "&5&l";
                }
            }
            else if (!isValid)
            {
                lines[0] = "&4&l";
            }
            else if (this.isAdminSign())
            {
                lines[0] = "&9&lAdmin-";
            }
            else
            {
                lines[0] = "&1&l";
            }
            if (this.isBuySign() == null)
            {
                if (this.isInEditMode())
                {
                    lines[0] += "Edit";
                }
                else
                {
                    lines[0] += "Invalid";
                }
            }
            else if (this.isBuySign())
            {
                lines[0] += "Buy";
                if (this.isSoldOut())
                {
                    lines[0] = "&4&lSold Out";
                }
            }
            else
            {
                lines[0] += "Sell";
            }
            ItemStack item = this.getItem();
            if (item == null)
            {
                if (this.isInEditMode())
                {
                    lines[1] = "&5No Item";
                }
                else
                {
                    lines[1] = "&4No Item";
                }
            }
            else if (item.getItemMeta().hasDisplayName() || item.getItemMeta().hasLore() || !item.getEnchantments().isEmpty())
            {
                if (item.getItemMeta().hasDisplayName())
                {
                    lines[1] = "&e" + item.getItemMeta().getDisplayName();
                }
                else
                {
                    lines[1] = "&e" + Match.material().getNameFor(this.getItem());
                }
            }
            else
            {
                lines[1] = Match.material().getNameFor(this.getItem());
            }
            if (this.getAmount() == 0)
            {
                if (this.isInEditMode())
                {
                    lines[2] = "&5No amount";
                }
                else
                {
                    lines[2] = "&4No amount";
                }
            }
            else
            {
                lines[2] = String.valueOf(this.getAmount());
                if (this.isBuySign() == null)
                {
                    lines[2] = "&4" + lines[2];
                }
                else if (this.isBuySign())
                {
                    if (this.isSoldOut())
                    {
                        lines[2] += " &4x" + this.getStock();
                    }
                    else if (this.hasStock())
                    {
                        lines[2] += " &1x" + this.getStock();
                    }
                }
                else
                {
                    if (this.hasStock())
                    {
                        User owner = this.blockInfo.getOwner();
                        boolean canAfford = this.isAdminSign() || this.canAfford(owner);
                        boolean demanding = !this.hasDemand() ? true : this.getRemainingDemand() > 0;
                        //TODO config to limit space of admin-signs
                        boolean space = this.module.getConfig().allowOverStackedInSign
                                ? this.getInventory().firstEmpty() != -1
                                : checkForPlace(this.getInventory(),splitIntoMaxItems(this.getItem(),this.getItem().getMaxStackSize()));
                        if (canAfford && demanding && space)
                        {
                            if (this.hasDemand())
                            {
                                lines[2] += " &bx" + (this.getDemand() - this.getStock());
                            }
                            else
                            {
                                lines[2] += " &bx?";
                            }
                        }
                        else
                        {
                            if (this.hasDemand())
                            {
                                lines[2] += " &4x" + (this.getDemand() - this.getStock());
                            }
                            else
                            {
                                lines[2] += " &4x?";
                            }
                        }
                    }
                }
            }
            lines[3] = this.parsePrice();

            lines[0] = ChatFormat.parseFormats(lines[0]);
            lines[1] = ChatFormat.parseFormats(lines[1]);
            lines[2] = ChatFormat.parseFormats(lines[2]);
            lines[3] = ChatFormat.parseFormats(lines[3]);
            for (int i = 0; i < 4; ++i)
            {
                blockState.setLine(i, lines[i]);
            }
            blockState.update();
        }
        else
        {
            this.module.getLogger().warning("Market-Sign is not a sign-block! " + this.getLocation());
        }
    }

    private boolean isSoldOut()
    {
        if (this.blockInfo.isBuyOrSell() && this.isBuySign())
        {
            //TODO infinite buy admin-signs with stock
            if (this.hasStock() && (this.getStock() < this.getAmount() || this.getStock() == 0))
            {
                return true;
            }
        }
        return false;
    }

    private boolean canAfford(User user)
    {
        if (user == null || this.getCurrency() == null || this.getPrice() == 0)
        {
            return true;
        }
        return this.module.getConomy().getAccountsManager().getAccount(user, this.getCurrency()).canAfford(this.getPrice());
    }

    public Inventory getInventory()
    {
        Inventory inventory = this.itemInfo.getInventory();
        if (inventory == null)
        {
            if (this.isAdminSign())
            {
                inventory = Bukkit.getServer().createInventory(this.itemInfo, 9, "Market-Sign"); // Dispenser would be nice BUT cannot rename
            }
            else
            {
                inventory = Bukkit.getServer().createInventory(this.itemInfo, 54, "Market-Sign"); // DOUBLE-CHEST
                ItemStack item = this.getItem().clone();
                item.setAmount(this.itemInfo.stock);
                if (this.itemInfo.stock > 0)
                    this.addToInventory(inventory,item);
            }
            this.itemInfo.initInventory(inventory);
        }
        if (this.isAdminSign())
        {
            inventory.setItem(4, this.getItem());
        }
        return inventory;
    }

    public int getAmount()
    {
        return this.blockInfo.amount;
    }

    /**
     * Returns the replaced infoModel or null if not yet set
     *
     * @param itemInfo
     * @return
     */
    public SignMarketItemModel setItemInfo(SignMarketItemModel itemInfo)
    {
        SignMarketItemModel old = this.itemInfo;
        old.removeSign(this);
        this.itemInfo = itemInfo;
        itemInfo.addSign(this);
        this.blockInfo.itemKey = itemInfo.key;
        return old;
    }

    public void setBlockInfo(SignMarketBlockModel blockModel)
    {
        this.blockInfo = blockModel;
    }

    public Integer getStock()
    {
        return this.itemInfo.stock;
    }

    public void setStock(Integer stock)
    {
        this.itemInfo.stock = stock;
    }

    public Boolean isBuySign()
    {
        return this.blockInfo.signType;
    }

    public Integer getDemand()
    {
        return this.blockInfo.demand;
    }

    public Currency getCurrency()
    {
        if (this.currency == null)
        {
            this.currency = this.module.getConomy().getCurrencyManager().getCurrencyByName(this.blockInfo.currency);
        }
        return this.currency;
    }

    public long getPrice()
    {
        return this.blockInfo.price;
    }

    public boolean isAdminSign() {
        return this.blockInfo.owner == null;
    }

    public void enterEditMode()
    {
        this.editMode = true;
        this.updateSign();
    }

    public void exitEditMode(User user)
    {
        this.editMode = false;
        this.updateSign();
        if (this.isValidSign(user))
        {
            this.saveToDatabase();
        }
    }

    public void setDemand(Integer demand)
    {
        this.blockInfo.demand = demand;
    }

    public void setCurrency(Currency currency)
    {
        this.currency = currency;
        this.blockInfo.currency = currency.getName();
    }

    public boolean isInEditMode()
    {
        return this.editMode;
    }

    public Location getLocation()
    {
        return this.blockInfo.getLocation();
    }

    public Integer getRemainingDemand()
    {
        if (this.getDemand() != null)
        {
            return this.getDemand() - this.getStock();
        }
        return null;
    }

    public void setDefaultFor(User user)
    {
        //TODO set default values.
        if (MarketSignPerm.SIGN_CREATE_ADMIN.isAuthorized(user))
        {
            this.setAdminSign();
        }
        else if (MarketSignPerm.SIGN_CREATE_USER.isAuthorized(user))
        {
            this.setOwner(user);
        }

        if (this.isAdminSign())
        {
            //TODO set stock if forced
        }
    }

    public SignMarketBlockModel getBlockInfo()
    {
        return this.blockInfo;
    }

    public SignMarketItemModel getItemInfo()
    {
        return itemInfo;
    }

    public ItemStack getItem()
    {
        return this.itemInfo.getItem();
    }

    public void applyValues(MarketSign prevMarketSign)
    {
        this.blockInfo.applyValues(prevMarketSign.blockInfo);
        this.itemInfo.applyValues(prevMarketSign.itemInfo);
    }
}
