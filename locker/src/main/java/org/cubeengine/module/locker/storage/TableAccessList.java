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
package org.cubeengine.module.locker.storage;

import java.util.UUID;

import org.cubeengine.libcube.util.Version;
import org.cubeengine.module.sql.database.Table;
import org.jooq.TableField;

import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCKS;
import static org.jooq.impl.SQLDataType.BIGINT;
import static org.jooq.impl.SQLDataType.SMALLINT;

public class TableAccessList extends Table<AccessListModel>
{
    public static TableAccessList TABLE_ACCESSLIST;
    public final TableField<AccessListModel, Long> ID = createField("id", BIGINT.nullable(false).identity(true), this);
    public final TableField<AccessListModel, UUID> USER_ID = createField("user_id", UUID_TYPE.nullable(false), this);
    public final TableField<AccessListModel, Long> LOCK_ID = createField("lock_id", BIGINT, this);
    // BitMask granting the user access to a protection (this is NOT restricting) (if ACCESS_PUT is not set on a donation chest it does not matter)
    public final TableField<AccessListModel, Short> LEVEL = createField("level", SMALLINT.nullable(false),this);
    public final TableField<AccessListModel, UUID> OWNER_ID = createField("owner_id", UUID_TYPE.nullable(true), this);

    public TableAccessList()
    {
        super(AccessListModel.class, "locker_accesslist", new Version(1));
        this.setPrimaryKey(ID);
        this.addUniqueKey(USER_ID, LOCK_ID);
        this.addUniqueKey(USER_ID, OWNER_ID);
        this.addForeignKey(TABLE_LOCKS.getPrimaryKey(), LOCK_ID);
        this.addFields(ID, USER_ID, LOCK_ID, LEVEL, OWNER_ID);
        TABLE_ACCESSLIST = this;
    }
}
