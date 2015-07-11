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
package de.cubeisland.engine.module.travel;

import de.cubeisland.engine.messagecompositor.parser.component.MessageComponent;
import de.cubeisland.engine.messagecompositor.parser.formatter.AbstractFormatter;
import de.cubeisland.engine.messagecompositor.parser.formatter.Context;
import de.cubeisland.engine.module.travel.home.Home;
import de.cubeisland.engine.service.i18n.I18n;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.NONE;
import static de.cubeisland.engine.service.i18n.formatter.component.ClickComponent.runCommand;
import static de.cubeisland.engine.service.i18n.formatter.component.HoverComponent.hoverText;
import static de.cubeisland.engine.service.i18n.formatter.component.StyledComponent.styled;
import static org.spongepowered.api.text.format.TextStyles.UNDERLINE;

public class TpPointFormatter extends AbstractFormatter<TeleportPoint>
{
    private I18n i18n;

    public TpPointFormatter(I18n i18n)
    {
        super("tppoint");
        this.i18n = i18n;
    }

    @Override
    public MessageComponent format(TeleportPoint object, Context context)
    {
        String cmd = "/" + (object instanceof Home ? "home" : "warp") + " tp " + object.getName() + " " + object.getOwnerName();
        return styled(UNDERLINE, runCommand(cmd, hoverText(i18n.getTranslation(context.getLocale(), NONE, "Click to teleport to {}", object.getName()), object.getName())));
    }
}
