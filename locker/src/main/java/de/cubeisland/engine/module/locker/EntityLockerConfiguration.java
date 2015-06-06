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
package de.cubeisland.engine.module.locker;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.module.core.util.matcher.EntityMatcher;
import de.cubeisland.engine.module.locker.storage.ProtectedType;
import org.spongepowered.api.entity.EntityType;

public class EntityLockerConfiguration extends LockerSubConfig<EntityLockerConfiguration, EntityType>
{
    public EntityLockerConfiguration(EntityType entityType)
    {
        super(ProtectedType.getProtectedType(entityType));
        this.type = entityType;
    }

    public String getTitle()
    {
        return type.getName();
    }

    public static class EntityLockerConfigConverter extends LockerSubConfigConverter<EntityLockerConfiguration>
    {
        private EntityMatcher em;

        public EntityLockerConfigConverter(Log logger, EntityMatcher em)
        {
            super(logger);
            this.em = em;
        }

        protected EntityLockerConfiguration fromString(String s) throws ConversionException
        {
            EntityType entityType = em.any(s);
            if (entityType == null)
            {
                throw ConversionException.of(this, s, "Invalid EntityType!");
            }
            return new EntityLockerConfiguration(entityType);
        }
    }
}
