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
package org.cubeengine.module.protector.command;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.protector.region.RegionConfig.setOrUnset;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

@Command(name = "blockdamage", alias = "block", desc = "Manages the region block-damage settings")
public class BlockDamageSettingsCommands extends AbstractSettingsCommand
{
    private I18n i18n;
    private SettingsListener psl;
    private PermissionService ps;

    public BlockDamageSettingsCommands(CommandManager base, I18n i18n, SettingsListener psl, PermissionService ps)
    {
        super(base, Protector.class, i18n, psl, ps);
        this.i18n = i18n;
        this.psl = psl;
        this.ps = ps;
    }

    @Command(desc = "Controls entities breaking blocks")
    public void monster(CommandSource context, Tristate set, @Default @Named("in") Region region)
    {
        region.getSettings().blockDamage.monster = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: BlockDamage by Entity Settings updated", region);
    }

    @Command(desc = "Controls blocks breaking blocks")
    public void block(CommandSource context, BlockType by, Tristate set, @Default @Named("in") Region region)
    {
        setOrUnset(region.getSettings().blockDamage.block, by, set);
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: BlockDamage by Block Settings updated", region);
    }

    @Command(desc = "Controls explosions breaking blocks")
    public void explosion(CommandSource context, Tristate set, @Default @Named("in") Region region)
    {
        region.getSettings().blockDamage.allExplosion = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: BlockDamage by Explosion Settings updated", region);
    }

    @Command(desc = "Controls explosions caused by players breaking blocks")
    public void playerExplosion(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            this.setPermission(context, set, region, role,  psl.explodePlayer.getId());
            return;
        }
        region.getSettings().blockDamage.playerExplosion = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: BlockDamage by Player-Explosion Settings updated", region);
    }

    @Command(desc = "Controls fire breaking blocks")
    public void fire(CommandSource context, Tristate set, @Default @Named("in") Region region)
    {
        this.block(context, BlockTypes.FIRE, set, region);
    }


    @Command(desc = "Controls lightning fire")
    public void lightningFire(CommandSource context, Tristate set, @Default @Named("in") Region region)
    {
        region.getSettings().blockDamage.lightning = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: Lightning Fire Settings updated", region);
    }
}
