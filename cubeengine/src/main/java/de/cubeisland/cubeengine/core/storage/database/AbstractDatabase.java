package de.cubeisland.cubeengine.core.storage.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.storage.Storage;
import de.cubeisland.cubeengine.core.util.worker.AsyncTaskQueue;

import org.apache.commons.lang.Validate;

import static de.cubeisland.cubeengine.core.logger.LogLevel.ERROR;

/**
 * Abstract Database implementing most of the database methods.
 * Extend this class and complement it to use the database.
 */
public abstract class AbstractDatabase implements Database
{
    private final ConcurrentMap<String, String> statements = new ConcurrentHashMap<String, String>();
    private final ConcurrentMap<String, PreparedStatement> preparedStatements = new ConcurrentHashMap<String, PreparedStatement>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // TODO thread factory!
    private final AsyncTaskQueue taskQueue = new AsyncTaskQueue(this.executorService); // TODO shutdown!

    @Override
    public Object getLastInsertedId(Class owner, String name, Object... params) throws SQLException
    {
        PreparedStatement statement = this.bindValues(this.getStoredStatement(owner, name), params);
        statement.execute();
        final ResultSet result = statement.getGeneratedKeys();
        if (result.next())
        {
            return result.getObject("GENERATED_KEY");
        }
        throw new SQLException("Failed to retrieve the last inserted ID!");
    }

    @Override
    public ResultSet query(String query, Object... params) throws SQLException
    {
        this.getConnection();
        return this.createAndBindValues(query, params).executeQuery();
    }

    @Override
    public ResultSet preparedQuery(Class owner, String name, Object... params) throws SQLException
    {
        this.getConnection();
        return this.bindValues(getStoredStatement(owner, name), params).executeQuery();
    }

    @Override
    public int update(String query, Object... params) throws SQLException
    {
        return this.createAndBindValues(query, params).executeUpdate();
    }

    @Override
    public void asyncUpdate(final String query, final Object... params)
    {
        this.taskQueue.addTask(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    update(query, params);
                }
                catch (SQLException e)
                {
                    CubeEngine.getLog().log(ERROR, "An asynchronous query failed!", e);
                }
            }
        });
    }

    @Override
    public int preparedUpdate(Class owner, String name, Object... params) throws SQLException
    {
        this.getConnection();
        return this.bindValues(getStoredStatement(owner, name), params).executeUpdate();
    }

    @Override
    public void asnycPreparedUpdate(final Class owner, final String name, final Object... params)
    {
        this.taskQueue.addTask(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    preparedUpdate(owner, name, params);
                }
                catch (SQLException e)
                {
                    CubeEngine.getLog().log(ERROR, "An asynchronous query failed!", e);
                }
            }
        });
    }

    @Override
    public boolean execute(String query, Object... params) throws SQLException
    {
        try
        {
            return this.createAndBindValues(query, params).execute();
        }
        catch (SQLException e)
        {
            throw new SQLException("SQL-Error while executing: " + query, e);
        }
    }

    @Override
    public void asyncExecute(final String query, final Object... params)
    {
        this.taskQueue.addTask(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    execute(query, params);
                }
                catch (SQLException e)
                {
                    CubeEngine.getLog().log(ERROR, "An asynchronous query failed!", e);
                }
            }
        });
    }

    @Override
    public boolean preparedExecute(Class owner, String name, Object... params) throws SQLException
    {
        this.getConnection();
        return this.bindValues(getStoredStatement(owner, name), params).execute();
    }

    @Override
    public void asyncPreparedExecute(final Class owner, final String name, final Object... params)
    {

        this.taskQueue.addTask(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    preparedExecute(owner, name, params);
                }
                catch (SQLException e)
                {
                    CubeEngine.getLog().log(ERROR, "An asynchronous query failed!", e);
                }
            }
        });
    }

    protected PreparedStatement createAndBindValues(String query, Object... params) throws SQLException
    {
        return this.bindValues(this.prepareStatement(query), params);
    }

    protected PreparedStatement bindValues(PreparedStatement statement, Object... params) throws SQLException
    {
        for (int i = 0; i < params.length; ++i)
        {
            statement.setObject(i + 1, params[i]);
        }
        return statement;
    }

    @Override
    public void storeStatement(Class owner, String name, String statement) throws SQLException
    {
        Validate.notNull(owner, "The owner must not be null!");
        Validate.notNull(name, "The name must not be null!");
        Validate.notNull(statement, "The statement must not be null!");

        this.statements.put(owner.getName() + "_" + name.toLowerCase(Locale.ENGLISH), statement);
    }

    @Override
    public PreparedStatement prepareStatement(String statement) throws SQLException
    {
        if (statement == null)
        {
            return null;
        }
        return this.getConnection().prepareStatement(statement, PreparedStatement.RETURN_GENERATED_KEYS);
    }

    @Override
    public PreparedStatement getStoredStatement(Class owner, String name)
    {
        Validate.notNull(owner, "The owner must not be null!");
        Validate.notNull(name, "The name must not be null!");

        name = owner.getName() + "_" + name.toLowerCase(Locale.ENGLISH);
        PreparedStatement statement = this.preparedStatements.get(name);
        if (statement == null)
        {
            String raw = this.statements.get(name);
            if (raw != null)
            {
                try
                {
                    statement = this.prepareStatement(raw);
                    if (statement != null)
                    {
                        this.preparedStatements.put(name, statement);
                    }
                }
                catch (SQLException e)
                {
                    CubeEngine.getLog().log(ERROR, "A statement could not be prepared!", e);
                }
            }
        }
        if (statement == null)
        {
            throw new IllegalArgumentException("Statement not found!");
        }
        return statement;
    }

    @Override
    public void startTransaction() throws SQLException
    {
        this.getConnection().setAutoCommit(false);
    }

    //TODO end Transaction

    @Override
    public void commit() throws SQLException
    {
        this.getConnection().commit();
    }

    @Override
    public void rollback() throws SQLException
    {
        this.getConnection().rollback();
    }

    @Override
    public void update(Storage manager) // TODO rename!
    {
        manager.updateStructure();
    }

    @Override
    public void clearStatementCache()
    {
        this.preparedStatements.clear();
    }

    @Override
    public void queueOperation(Runnable operation)
    {
        this.taskQueue.addTask(operation);
    }

    @Override
    public void shutdown()
    {
        this.taskQueue.shutdown();
    }
}