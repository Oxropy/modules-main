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

import org.cubeengine.module.sql.database.AsyncRecord;
import org.spongepowered.api.entity.living.player.User;

import static org.cubeengine.module.locker.storage.TableAccessList.TABLE_ACCESSLIST;
import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCKS;

public class AccessListModel extends AsyncRecord<AccessListModel>
{
    public AccessListModel()
    {
        super(TABLE_ACCESSLIST);
    }

    public AccessListModel newAccess(LockModel model, User modifyUser)
    {
        this.setValue(TABLE_ACCESSLIST.LOCK_ID, model.getValue(TABLE_LOCKS.ID));
        this.setValue(TABLE_ACCESSLIST.USER_ID, modifyUser.getUniqueId());
        this.setValue(TABLE_ACCESSLIST.LEVEL, ACCESS_FULL);
        return this;
    }

    public AccessListModel newGlobalAccess(User sender, User modifyUser, short accessType)
    {
        this.setValue(TABLE_ACCESSLIST.LOCK_ID, null);
        this.setValue(TABLE_ACCESSLIST.USER_ID, modifyUser.getUniqueId());
        this.setValue(TABLE_ACCESSLIST.LEVEL, accessType);
        this.setValue(TABLE_ACCESSLIST.OWNER_ID, sender.getUniqueId());
        return this;
    }

    public boolean canIn()
    {
        return (this.getValue(TABLE_ACCESSLIST.LEVEL) & ACCESS_PUT) == ACCESS_PUT;
    }

    public boolean canOut()
    {
        return (this.getValue(TABLE_ACCESSLIST.LEVEL) & ACCESS_TAKE) == ACCESS_TAKE;
    }

    public static final short ACCESS_TAKE = 1; // put items in chest
    public static final short ACCESS_PUT = 1 << 1; // take items out of chest
    public static final short ACCESS_ADMIN = 1 << 2; // manage accesslist

    public static final short ACCESS_FULL = ACCESS_TAKE | ACCESS_PUT;
    public static final short ACCESS_ALL = ACCESS_FULL | ACCESS_ADMIN;
}
