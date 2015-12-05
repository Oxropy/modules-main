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
package org.cubeengine.module.multiverse.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import de.cubeisland.engine.module.core.CubeEngine;
import de.cubeisland.engine.reflect.codec.nbt.ReflectedNBT;
import org.bukkit.Bukkit;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.player.Player;
import org.bukkit.inventory.Inventory;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.potion.PotionEffect;

public class PlayerDataConfig extends ReflectedNBT
{
    public String lastName;
    public int heldItemSlot = 0;
    public double health = 20;
    public double maxHealth = 20;
    public int foodLevel = 20;
    public float saturation = 20;
    public float exhaustion = 0;
    public float exp = 0;
    public int lvl = 0;
    public int fireTicks = 0;

    public Collection<PotionEffect> activePotionEffects = new ArrayList<>();
    public Inventory inventory;
    public Inventory enderChest;

    private transient String[] head = null;

    public void applyToPlayer(Player player)
    {
        Inventory inv = player.getInventory();
        if (!player.getName().equals(lastName))
        {
            CubeEngine.getLog().debug("[Worlds] Detected NameChange {} -> {}", lastName, player.getName());
            this.lastName = player.getName();
            this.save();
        }
        player.getInventory().setHeldItemSlot(heldItemSlot);
        player.setMaxHealth(maxHealth);
        player.setHealth(health);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
        player.setLevel(lvl);
        player.setExp(exp);
        player.setFireTicks(fireTicks);

        for (PotionEffect potionEffect : player.getActivePotionEffects())
        {
            player.removePotionEffect(potionEffect.getType());
        }
        player.addPotionEffects(activePotionEffects);

        ItemStack[] contents;
        if (inventory == null)
        {
            contents = new ItemStack[36+4];
        }
        else
        {
            contents = inventory.getContents();
        }

        for (int i = 0; i < contents.length; i++)
        {
            if (i >= inv.getSize() + 4)
            {
                break;
            }
            inv.setItem(i, contents[i]);
        }

        inv = player.getEnderChest();
        if (inventory == null)
        {
            contents = new ItemStack[27];
        }
        else
        {
            contents = enderChest.getContents();
        }
        for (int i = 0; i < contents.length; i++)
        {
            if (i >= inv.getSize())
            {
                break;
            }
            inv.setItem(i, contents[i]);
        }
    }

    public void applyFromPlayer(Player player)
    {
        PlayerInventory playerInventory = player.getInventory();
        this.lastName = player.getName();
        this.heldItemSlot = playerInventory.getHeldItemSlot();
        this.maxHealth = player.getMaxHealth();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.exhaustion = player.getExhaustion();
        this.lvl = player.getLevel();
        this.exp = player.getExp();
        this.fireTicks = player.getFireTicks();
        this.activePotionEffects = new ArrayList<>(player.getActivePotionEffects());

        ItemStack[] contents = playerInventory.getContents();
        ItemStack[] armorContents = playerInventory.getArmorContents();
        ItemStack[] allContents = Arrays.copyOf(contents, contents.length + 9);
        System.arraycopy(armorContents, 0, allContents, contents.length, armorContents.length);
        this.inventory = Bukkit.createInventory(player, allContents.length);
        this.inventory.setContents(allContents);

        ItemStack[] enderContents = player.getEnderChest().getContents();

        this.enderChest = Bukkit.createInventory(player, enderContents.length);
        this.enderChest.setContents(enderContents);
    }

    public void setHead(String... head)
    {
        this.head = head;
    }

    @Override
    public String[] head()
    {
        return this.head;
    }
}