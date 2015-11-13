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
package org.cubeengine.module.locker.storage;

import java.util.Arrays;
import java.util.Optional;

import com.flowpowered.math.vector.Vector3d;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.service.i18n.I18n;
import org.jooq.types.UInteger;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.VelocityData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;

import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.data.key.Keys.DISPLAY_NAME;
import static org.spongepowered.api.data.key.Keys.ITEM_LORE;
import static org.spongepowered.api.effect.sound.SoundTypes.*;
import static org.spongepowered.api.item.ItemTypes.PAPER;

public class KeyBook
{
    public static final String TITLE = ChatFormat.RESET.toString() + ChatFormat.GOLD + "KeyBook " + ChatFormat.DARK_GREY + "#";
    public ItemStack item;
    public final Player holder;
    private final Locker module;
    private I18n i18n;
    public final UInteger lockID;
    private final String keyBookName;

    private KeyBook(ItemStack item, Player holder, Locker module, I18n i18n)
    {
        this.item = item;
        this.holder = holder;
        this.module = module;
        this.i18n = i18n;
        keyBookName = item.get(DISPLAY_NAME).map(Texts::toPlain).orElse("");
        lockID = UInteger.valueOf(Long.valueOf(keyBookName.substring(keyBookName.indexOf('#') + 1, keyBookName.length())));
    }

    public static KeyBook getKeyBook(Optional<ItemStack> item, Player currentHolder, Locker module, I18n i18n)
    {
        if (!item.isPresent())
        {
            return null;
        }
        if (item.get().getItem() == ItemTypes.ENCHANTED_BOOK
            && item.get().get(DISPLAY_NAME).map(Texts::toPlain).map(s -> s.contains(TITLE)).orElse(false))
        {
            try
            {
                return new KeyBook(item.get(), currentHolder, module, i18n);
            }
            catch (NumberFormatException|IndexOutOfBoundsException ignore)
            {}
        }
        return null;
    }

    public boolean check(Lock lock, Location effectLocation)
    {
        if (lock.getId().equals(lockID)) // Id matches ?
        {
            // Validate book
            if (this.isValidFor(lock))
            {
                if (effectLocation != null)
                {
                    i18n.sendTranslated(holder, POSITIVE, "As you approach with your KeyBook the magic lock disappears!");
                    holder.playSound(PISTON_EXTEND, effectLocation.getPosition(), 1, 2);
                    holder.playSound(PISTON_EXTEND, effectLocation.getPosition(), 1, (float)1.5);
                    lock.notifyKeyUsage(holder);
                }
                return true;
            }
            else
            {
                i18n.sendTranslated(holder, NEGATIVE, "You try to open the container with your KeyBook");
                i18n.sendTranslated(holder, NEGATIVE, "but you get forcefully pushed away!");
                this.invalidate();
                holder.playSound(GHAST_SCREAM, effectLocation.getPosition(), 1, 1);

                final Vector3d userDirection = holder.getRotation();

                // TODO damaging player working? /w effects see Lock for effects playing manually
                holder.offer(Keys.HEALTH, holder.getHealthData().health().get() - 1);
                VelocityData velocity = holder.getOrCreate(VelocityData.class).get();
                velocity.velocity().set(userDirection.mul(-3));
                holder.offer(velocity);
                return false;
            }
        }
        else
        {
            i18n.sendTranslated(holder, NEUTRAL, "You try to open the container with your KeyBook but nothing happens!");
            holder.playSound(BLAZE_HIT, effectLocation.getPosition(), 1, 1);
            holder.playSound(BLAZE_HIT, effectLocation.getPosition(), 1, (float)0.8);
            return false;
        }
    }

    public void invalidate()
    {
        item.offer(DISPLAY_NAME, Texts.of(TextColors.DARK_RED, "Broken KeyBook"));
        item.offer(ITEM_LORE, Arrays.asList(i18n.getTranslation(holder, NEUTRAL, "This KeyBook"),
                                            i18n.getTranslation(holder, NEUTRAL, "looks old and"),
                                            i18n.getTranslation(holder, NEUTRAL, "used up. It"),
                                            i18n.getTranslation(holder, NEUTRAL, "won't let you"),
                                            i18n.getTranslation(holder, NEUTRAL, "open any containers!")));
        item = module.getGame().getRegistry().createBuilder(ItemStackBuilder.class).fromItemStack(item).itemType(PAPER).build();
        holder.setItemInHand(item);
    }

    public boolean isValidFor(Lock lock)
    {
        boolean b = keyBookName.startsWith(lock.getColorPass());
        if (!b)
        {
            this.module.getProvided(Log.class).debug("Invalid KeyBook detected! {}|{}", lock.getColorPass(), keyBookName);
        }
        return b;
    }
}
