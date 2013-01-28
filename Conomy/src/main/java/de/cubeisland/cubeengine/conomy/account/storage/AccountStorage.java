package de.cubeisland.cubeengine.conomy.account.storage;

import de.cubeisland.cubeengine.conomy.currency.Currency;
import de.cubeisland.cubeengine.core.storage.SingleKeyStorage;
import de.cubeisland.cubeengine.core.storage.StorageException;
import de.cubeisland.cubeengine.core.storage.database.Database;
import de.cubeisland.cubeengine.core.storage.database.querybuilder.QueryBuilder;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class AccountStorage extends SingleKeyStorage<Long, AccountModel>
{
    private static final int REVISION = 1;

    public AccountStorage(Database database)
    {
        super(database, AccountModel.class, REVISION);
        this.initialize();
    }

    @Override
    public void initialize()
    {
        try
        {
            super.initialize();
            QueryBuilder builder = this.database.getQueryBuilder();
            this.database.storeStatement(modelClass, "getByUserID",
                    builder.select().cols(allFields).from(this.tableName).
                        where().field("user_id").isEqual().value().end().end());
            this.database.storeStatement(modelClass, "getByAccountName",
                    builder.select().cols(allFields).from(this.tableName).
                        where().field("name").isEqual().value().end().end());
            this.database.storeStatement(modelClass, "getTopBalance",
                    builder.select().cols(allFields).from(this.tableName).
                        where().field("currencyName").isEqual().value().
                        orderBy("value").desc().limit().offset().end().end());

            this.database.storeStatement(modelClass, "setAllUser",
                    builder.update(this.tableName).set("value").
                        where().field("currencyName").isEqual().value().
                        and().not().field("user_id").isEqual().value(null).end().end());

            this.database.storeStatement(modelClass, "transactAllUser",
                    builder.update(this.tableName).set("value").beginFunction("+").field("value").endFunction().
                        where().field("currencyName").isEqual().value().
                        and().not().field("user_id").isEqual().value(null).end().end());
        }
        catch (SQLException e)
        {
            throw new StorageException("Failed to initialize the account-manager!", e);
        }
    }

    public Collection<AccountModel> loadAccounts(Long userId)
    {
        try
        {
            ResultSet resulsSet = this.database.preparedQuery(modelClass, "getByUserID", userId);
            ArrayList<AccountModel> accs = new ArrayList<AccountModel>();
            while (resulsSet.next())
            {
                AccountModel loadedModel = this.modelClass.newInstance();
                for (Field field : this.fieldNames.keySet())
                {
                    field.set(loadedModel, resulsSet.getObject(this.fieldNames.get(field)));
                }
                accs.add(loadedModel);
            }
            return accs;
        }
        catch (SQLException ex)
        {
            throw new IllegalStateException("Error while reading from Database", ex);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Error while creating fresh Model from Database", ex);
        }
    }

    public Collection<AccountModel> loadAccounts(String name)
    {
        try
        {
            ResultSet resulsSet = this.database.preparedQuery(modelClass, "getByAccountName", name);
            ArrayList<AccountModel> accs = new ArrayList<AccountModel>();
            while (resulsSet.next())
            {
                AccountModel loadedModel = this.modelClass.newInstance();
                for (Field field : this.fieldNames.keySet())
                {
                    field.set(loadedModel, resulsSet.getObject(this.fieldNames.get(field)));
                }
                accs.add(loadedModel);
            }
            return accs;
        }
        catch (SQLException ex)
        {
            throw new IllegalStateException("Error while reading from Database", ex);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Error while creating fresh Model from Database", ex);
        }
    }

    public Collection<AccountModel> getTopAccounts(Currency currency, int fromRank, int toRank)
    {
        try
        {
            ResultSet resulsSet = this.database.preparedQuery(modelClass, "getTopBalance", currency.getName(), toRank - fromRank, fromRank - 1);
            LinkedList<AccountModel> list = new LinkedList<AccountModel>();
            while (resulsSet.next())
            {
                AccountModel loadedModel = this.modelClass.newInstance();
                for (Field field : this.fieldNames.keySet())
                {
                    field.set(loadedModel, resulsSet.getObject(this.fieldNames.get(field)));
                }
                list.add(loadedModel);
            }
            return list;
        }
        catch (SQLException ex)
        {
            throw new IllegalStateException("Error while reading from Database", ex);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Error while creating fresh Model from Database", ex);
        }
    }

    public void transactAll(Currency currency, long amount)
    {
        try
        {
            this.database.preparedUpdate(modelClass, "transactAllUser", amount, currency.getName());
        }
        catch (SQLException ex)
        {
            throw new IllegalStateException("Error while updating database", ex);
        }
    }

    public void setAll(Currency currency, long amount)
    {
        try
        {
            this.database.preparedUpdate(modelClass, "setAllUser", amount, currency.getName());
        }
        catch (SQLException ex)
        {
            throw new IllegalStateException("Error while updating database", ex);
        }
    }
}