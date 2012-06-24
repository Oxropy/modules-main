package de.cubeisland.cubeengine.core.util.log;

/**
 *
 * @author Robin Bechtel-Ostmann
 */

public enum LogType
{
    MESSAGE("[MESSAGE]"),
    NOTIFICATION("[NOTICE]"),
    INFORMATION("[INFO]"),
    WARNING("[WARNING]"),
    ERROR("[ERROR]"),
    DEBUG("[DEBUG]");
    
    private String name;
    
    private LogType(String name)
    {
        this.name = name;
    }
    
    @Override
    public String toString()
    {
        return this.name;
    }
    
    
}
