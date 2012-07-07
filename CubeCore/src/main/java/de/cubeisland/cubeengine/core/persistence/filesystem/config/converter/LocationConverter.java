package de.cubeisland.cubeengine.core.persistence.filesystem.config.converter;

import de.cubeisland.cubeengine.CubeEngine;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

/**
 *
 * @author Faithcaio
 */
public class LocationConverter implements Converter<Location>
{
    private Server server;
    
    public LocationConverter()
    {
        this.server = CubeEngine.getCore().getServer();
    }
    
    public Object from(Location location)
    {
        Map<String,Object> loc = new LinkedHashMap<String, Object>();
        loc.put("world", location.getWorld().getName());
        loc.put("x", location.getX());
        loc.put("y", location.getY());
        loc.put("z", location.getZ());
        loc.put("yaw", location.getYaw());
        loc.put("pitch", location.getPitch());
        return loc;
    }

    public Location to(Object object)
    {
        Map<String, Object> input = (Map<String, Object>)object;
        World world = server.getWorld((String) input.get("world"));
        double x = Double.valueOf(input.get("x").toString());
        double y = Double.valueOf(input.get("y").toString());
        double z = Double.valueOf(input.get("z").toString());
        double yaw = Double.valueOf(input.get("yaw").toString());
        double pitch = Double.valueOf(input.get("pitch").toString());

        return new Location(world, x, y, z, (float) yaw, (float) pitch);
    }
}