package de.cubeisland.cubeengine.core.user;

import de.cubeisland.cubeengine.core.Core;
import de.cubeisland.cubeengine.core.storage.SingleKeyStorage;
import de.cubeisland.cubeengine.core.storage.StorageException;
import de.cubeisland.cubeengine.core.storage.database.querybuilder.ComponentBuilder;
import de.cubeisland.cubeengine.core.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static de.cubeisland.cubeengine.core.storage.database.querybuilder.ComponentBuilder.EQUAL;
import static de.cubeisland.cubeengine.core.storage.database.querybuilder.ComponentBuilder.LESS;

class UserStorage extends SingleKeyStorage<Long, User>
{
    private static final int REVISION = 1;
    private final Core core;

    UserStorage(Core core)
    {
        super(core.getDB(), User.class, REVISION);
        this.core = core;
        this.initialize();
    }


    @Override
    public void initialize()
    {
        super.initialize();
        try
        {
            this.database.storeStatement(User.class, "get_by_name", this.database.getQueryBuilder().select().wildcard().from(this.tableName).where().field("player").is(ComponentBuilder.EQUAL).value().end().end());

            this.database.storeStatement(User.class, "cleanup", database.getQueryBuilder().select(dbKey).from(tableName).where().field("lastseen").is(LESS).value().and().field("nogc").is(EQUAL).value(false).end().end());

            this.database.storeStatement(User.class, "clearpw", database.getQueryBuilder().update(tableName).set("passwd").end().end());
        }
        catch (SQLException e)
        {
            throw new StorageException("Failed to initialize the user-manager!", e);
        }
    }

    public void resetAllPasswords()
    {
        try
        {
            this.database.preparedUpdate(modelClass, "clearpw", (Object)null);
        }
        catch (SQLException ex)
        {
            throw new StorageException("An SQL-Error occurred while clearing passwords", ex);
        }
    }

    /**
     * Custom Getter for getting User from DB by Name
     *
     * @param playerName the name
     * @return the User OR null if not found
     */
    protected User loadUser(String playerName)
    {
        User loadedModel = null;
        try
        {
            ResultSet resultSet = this.database.preparedQuery(modelClass, "get_by_name", playerName);
            ArrayList<Object> values = new ArrayList<Object>();
            if (resultSet.next())
            {
                for (String name : this.allFields)
                {
                    values.add(resultSet.getObject(name));
                }
                loadedModel = this.modelConstructor.newInstance(values);
            }
        }
        catch (SQLException e)
        {
            throw new StorageException("An SQL-Error occurred while creating a new Model from database", e,this.database.getStoredStatement(modelClass,"get_by_name"));
        }
        catch (Exception e)
        {
            throw new StorageException("An unknown error occurred while creating a new Model from database", e);
        }
        return loadedModel;
    }

    /**
     * Searches for too old UserData and remove it.
     */
    public void cleanup()
    {
        this.database.queueOperation(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Timestamp time = new Timestamp(System.currentTimeMillis() - StringUtils.convertTimeToMillis(core.getConfiguration().userManagerCleanupDatabase));
                    ResultSet result = database.preparedQuery(User.class, "cleanup", time);

                    while (result.next())
                    {
                        deleteByKey(result.getLong("key"));
                    }
                }
                catch (SQLException e)
                {
                    // TODO this exception will be uncaught
                    throw new StorageException("An SQL-Error occurred while cleaning the user-table", e, database.getStoredStatement(modelClass, "cleanup"));
                }
                catch (Exception e)
                {
                    // TODO this exception will be uncaught
                    throw new StorageException("An unknown Error occurred while cleaning the user-table", e);
                }
            }
        });
    }
}