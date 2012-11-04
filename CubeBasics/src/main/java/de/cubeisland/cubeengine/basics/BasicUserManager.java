package de.cubeisland.cubeengine.basics;

import de.cubeisland.cubeengine.core.storage.BasicStorage;
import de.cubeisland.cubeengine.core.storage.database.Database;
import de.cubeisland.cubeengine.core.user.User;

public class BasicUserManager extends BasicStorage<BasicUser>
{
    private static final int REVISION = 1;
    
    public BasicUserManager(Database database)
    {
        super(database, BasicUser.class, REVISION);
        this.initialize();
    }

    public BasicUser getBasicUser(User user)
    {
        BasicUser model = user.getAttachment(BasicUser.class);
        if (model == null)
        {
            model = this.get(user.getKey());
            if (model == null)
            {
                model = new BasicUser(user);
            }
            user.attach(model);
        }
        return model;
    }
}