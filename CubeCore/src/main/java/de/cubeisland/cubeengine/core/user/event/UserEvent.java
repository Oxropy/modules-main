package de.cubeisland.cubeengine.core.user.event;

import de.cubeisland.cubeengine.core.CubeCore;
import de.cubeisland.cubeengine.core.persistence.event.ModelEvent;
import de.cubeisland.cubeengine.core.user.User;

/**
 *
 * @author Faithcaio
 */
public abstract class UserEvent extends ModelEvent
{
    private final User user;
    
    public UserEvent(CubeCore core, User user) 
    {
        super(core, user);
        this.user = user;
    }
    
    public User getUser()
    {
        return this.user;
    }

}
