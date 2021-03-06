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
package org.cubeengine.module.locker.config;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.locker.storage.ProtectedType;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.spongepowered.api.entity.EntityType;

public class EntityLockConfig extends LockConfig<EntityLockConfig, EntityType>
{
    public EntityLockConfig(EntityType entityType)
    {
        super(ProtectedType.getProtectedType(entityType));
        this.type = entityType;
    }

    public String getTitle()
    {
        return type.getId();
    }

    public static class EntityLockerConfigConverter extends LockConfigConverter<EntityLockConfig>
    {
        private EntityMatcher em;

        public EntityLockerConfigConverter(Log logger, EntityMatcher em)
        {
            super(logger);
            this.em = em;
        }

        protected EntityLockConfig fromString(String s) throws ConversionException
        {
            EntityType entityType = em.any(s, null);
            if (entityType == null)
            {
                throw ConversionException.of(this, s, "Invalid EntityType!");
            }
            return new EntityLockConfig(entityType);
        }
    }
}
