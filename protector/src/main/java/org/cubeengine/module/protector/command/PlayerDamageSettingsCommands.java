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

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

@Command(name = "playerDamage", alias = "player", desc = "Manages the region player-damage settings")
public class PlayerDamageSettingsCommands extends AbstractSettingsCommand
{
    public PlayerDamageSettingsCommands(CommandManager base, I18n i18n, SettingsListener psl, PermissionService ps)
    {
        super(base, Protector.class, i18n, psl, ps);
    }

    @Command(desc = "Controls player damage")
    public void all(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            String perm = psl.playerDamgeAll.getId();
            setPermission(context, set, region, role, perm);
            return;
        }
        region.getSettings().entityDamage.all = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: All PlayerDamage Settings updated", region);
    }

    @Command(desc = "Controls player damage by living entities")
    public void living(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            String perm = psl.playerDamgeLiving.getId();
            setPermission(context, set, region, role, perm);
            return;
        }
        region.getSettings().entityDamage.byLiving = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: PlayerDamage by Living Settings updated", region);
    }

    @Command(desc = "Controls pvp damage")
    public void pvp(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            setPermission(context, set, region, role, psl.playerDamgePVP.getId());
            return;
        }
        region.getSettings().playerDamage.pvp = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: PVP Settings updated", region);
    }

    @Command(desc = "Controls mobs targeting players")
    public void targeting(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            setPermission(context, set, region, role, psl.playerTargeting.getId());
            return;
        }
        region.getSettings().playerDamage.aiTargeting = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: PlayerTargeting by AI Settings updated", region);
    }


}
