package de.cubeisland.cubeengine.basics.moderation;

import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.basics.BasicsConfiguration;
import de.cubeisland.cubeengine.basics.BasicsPerm;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.annotation.Command;
import de.cubeisland.cubeengine.core.command.annotation.Flag;
import de.cubeisland.cubeengine.core.command.annotation.Param;
import static de.cubeisland.cubeengine.core.command.exception.IllegalParameterValue.illegalParameter;
import static de.cubeisland.cubeengine.core.command.exception.InvalidUsageException.*;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.StringUtils;
import de.cubeisland.cubeengine.core.util.matcher.EntityMatcher;
import de.cubeisland.cubeengine.core.util.matcher.EntityType;
import de.cubeisland.cubeengine.core.util.matcher.MaterialMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.Wither;

/**
 * Commands controlling / affecting worlds. /weather /remove /butcher
 */
public class WorldControlCommands
{

    private BasicsConfiguration config;

    public WorldControlCommands(Basics basics)
    {
        this.config = basics.getConfiguration();
    }

    @Command(desc = "Changes the weather", min = 1, max = 3,
    usage = "<sun|rain|storm> [duration] [in <world>]", params =
    @Param(names = "in", type = World.class))
    public void weather(CommandContext context)
    {
        User sender = context.getSenderAsUser();
        boolean sunny = true;
        boolean noThunder = true;
        int duration = 10000000;
        String weather = StringUtils.matchString(context.getString(0), "sun", "rain", "storm");
        if (weather == null)
        {
            paramNotFound(context, "basics", "&cInvalid weather!\n&eUse &6sun&e, &6rain &eor &6storm&e!");
        }
        if (weather.equalsIgnoreCase("sun"))
        {
            sunny = true;
            noThunder = true;
        }
        else if (weather.equalsIgnoreCase("rain"))
        {
            sunny = false;
            noThunder = true;
        }
        else if (weather.equalsIgnoreCase("storm"))
        {
            sunny = false;
            noThunder = false;
        }
        if (context.hasIndexed(1))
        {
            duration = context.getIndexed(1, Integer.class, 0);
            if (duration == 0)
            {
                illegalParameter(context, "basics", "&cThe given duration is invalid!");
            }
            duration *= 20;
        }
        World world = null;
        if (context.hasNamed("in"))
        {
            world = context.getNamed("in", World.class, null);
            if (world == null)
            {
                illegalParameter(context, "basics", "&cWorld &6%s &cnot found!", context.getString(1));
            }
        }
        else
        {
            if (sender == null)
            {
                invalidUsage(context, "basics", "&cIf not used ingame you have to specify a world!");
            }
            else
            {
                world = sender.getWorld();
            }
        }
        world.setStorm(!sunny);
        world.setThundering(!noThunder);
        world.setWeatherDuration(duration);
        if (world.isThundering() != noThunder && world.hasStorm() != sunny) // weather is not changing
        {
            context.sendMessage("basics", "&aWeather in &6%s &awas already set to &e%s&a!", world.getName(), weather);
        }
        else
        {
            context.sendMessage("basics", "&aChanged weather in &6%s &ato &e%s&a!", world.getName(), weather);
        }
    }

    @Command(desc = "Removes entity", usage = "<entityType[:itemMaterial]> [radius]|[-all] [in <world>]", flags =
    @Flag(longName = "all", name = "a"), params =
    @Param(names =
    {
        "in"
    }, type = World.class), min = 1)
    public void remove(CommandContext context)
    {
        User sender = context.getSenderAsUser();
        World world;
        if (context.hasNamed("in"))
        {
            world = context.getNamed("in", World.class);
        }
        else
        {
            if (sender == null)
            {
                invalidUsage(context, "basics", "&cThe butcher will come to YOU tonight!");
            }
            world = sender.getWorld();
        }
        int radius = this.config.removeCmdDefaultRadius;
        if (context.hasFlag("a")) // remove all selected entities in world
        {
            radius = -1;
        }
        else if (sender == null)
        {
            invalidUsage(context, "basics", "&cIf not used ingame you can only remove all!");
        }
        if (context.hasIndexed(1))
        {
            radius = context.getIndexed(1, Integer.class, 0);
            if (radius <= 0)
            {
                illegalParameter(context, "basics", "&cThe radius has to be a number greater than 0!");
            }
        }
        Location loc = null;
        if (sender != null)
        {
            loc = sender.getLocation();
        }
        int entitiesRemoved;
        if (context.getString(0).equalsIgnoreCase("*"))
        {
            List<Entity> list = new ArrayList<Entity>();
            for (Entity entity : world.getEntities())
            {
                if (!(entity instanceof LivingEntity))
                {
                    list.add(entity);
                }
            }
            entitiesRemoved = this.removeEntities(list, loc, radius, false);
        }
        else
        {
            List<Entity> list = world.getEntities(); // All entites remaining in that list will not get deleted!
            String[] s_entityTypes = StringUtils.explode(",", context.getString(0));
            List<org.bukkit.entity.EntityType> types = new ArrayList<org.bukkit.entity.EntityType>();
            for (String s_entityType : s_entityTypes)
            {
                if (s_entityType.contains(":"))
                {
                    EntityType type = EntityMatcher.get().matchEntity(s_entityType.substring(0, s_entityType.indexOf(":")));
                    if (EntityType.DROPPED_ITEM.equals(type))
                    {
                        Material itemtype = MaterialMatcher.get().matchMaterial(s_entityType.substring(s_entityType.indexOf(":") + 1));
                        List<Entity> remList = new ArrayList<Entity>();
                        for (Entity entity : list)
                        {
                            if (entity.getType().equals(EntityType.DROPPED_ITEM.getBukkitType()) && ((Item) entity).getItemStack().getType().equals(itemtype))
                            {
                                remList.add(entity);
                            }
                        }
                        list.removeAll(remList);
                    }
                    else
                    {
                        context.sendMessage("basics", "&cYou can only specify data for removing items!");
                        return;
                    }
                }
                else
                {
                    EntityType type = EntityMatcher.get().matchEntity(s_entityType);
                    if (type == null)
                    {
                        context.sendMessage("basics", "&cInvalid entity-type!\n&eUse &6"
                                + EntityType.DROPPED_ITEM + "&e, &6" + EntityType.ARROW + "&e, &6"
                                + EntityType.BOAT + "&e, &6" + EntityType.MINECART + "&e, &6"
                                + EntityType.PAINTING + "&e, &6" + EntityType.ITEM_FRAME + " &eor &6"
                                + EntityType.EXPERIENCE_ORB);
                        return;
                    }
                    if (type.isAlive())
                    {
                        blockCommand(context, "basics", "&cTo kill living entities use the &e/butcher &ccommand!");
                    }
                    types.add(type.getBukkitType());
                }
            }
            List<Entity> remList = new ArrayList<Entity>();
            for (Entity entity : list)
            {
                if (types.contains(entity.getType()))
                {
                    remList.add(entity);
                }
            }
            list.removeAll(remList);
            remList = world.getEntities();
            remList.removeAll(list);
            entitiesRemoved = this.removeEntities(remList, loc, radius, false);
        }
        if (entitiesRemoved == 0)
        {
            context.sendMessage("basics", "&eNo entities to remove!");
        }
        else if (context.getString(0).equalsIgnoreCase("*"))
        {
            if (radius == -1)
            {
                context.sendMessage("basics", "&aRemoved all entities in &6%s&a! &f(&6%d&f)", world.getName(), entitiesRemoved);
            }
            else
            {
                context.sendMessage("basics", "&aRemoved all entities around you! &f(&6%d&f)", entitiesRemoved);
            }
        }
        else
        {
            if (radius == -1)
            {
                context.sendMessage("basics", "&aRemoved &e%d &aentities in '&6%s&a!", entitiesRemoved, world.getName());
            }
            else
            {
                context.sendMessage("basics", "&aRemoved &e%d &aentities around you!", entitiesRemoved);
            }
        }
    }
    private final Collection<String> BUTCHER_TARGETS = new HashSet<String>()
    {
        
        {
            this.add("pet");
            this.add("golem");
            this.add("animal");
            this.add("npc");
            this.add("other");
            this.add("boss");
            this.add("monster");
        }
    };

    @Command(desc = "Gets rid of living mobs nearby you", flags =
    {
        @Flag(longName = "lightning", name = "l"), // die with style
        @Flag(longName = "all", name = "a") // infinite radius
    }, params =
    @Param(names = "in", type = World.class), usage = "[types...] [radius] [in world] [-l] [-all]")
    public void butcher(CommandContext context)
    {
        User sender = context.getSenderAsUser();
        Location loc;
        int radius = this.config.butcherCmdDefaultRadius;
        int removed;
        if (sender == null)
        {
            radius = -1;
            loc = this.config.mainWorld.getSpawnLocation();
        }
        else
        {
            loc = sender.getLocation();
        }
        if (context.hasIndexed(1))
        {
            radius = context.getIndexed(1, Integer.class, 0);
            if (radius < 0)
            {
                if (!(radius == -1 && BasicsPerm.COMMAND_BUTCHER_FLAG_ALL.isAuthorized(context.getSender())))
                {
                    illegalParameter(context, "basics", "&cThe radius has to be a number greater than 0!");
                }
            }
        }
        if (context.hasFlag("a") && BasicsPerm.COMMAND_BUTCHER_FLAG_ALL.isAuthorized(context.getSender()))
        {
            radius = -1;
        }
        boolean lightning = false;
        if (context.hasFlag("l") && BasicsPerm.COMMAND_BUTCHER_FLAG_LIGHTNING.isAuthorized(context.getSender()))
        {
            lightning = true;
        }
        List<Entity> list = loc.getWorld().getEntities(); // entities remaining in that list will NOT get removed
        String[] s_types =
        {
            "monster"
        };
        boolean allTypes = false;
        if (context.hasIndexed(0))
        {
            if (context.getString(0).equals("*"))
            {
                allTypes = true;
            }
            else
            {
                s_types = StringUtils.explode(",", context.getString(0));
            }
        }
        List<Entity> remList;
        if (!allTypes)
        {
            for (String s_type : s_types)
            {
                String match = StringUtils.matchString(s_type, this.BUTCHER_TARGETS);
                boolean specialmatch = false;
                if (match == null)
                {
                    match = StringUtils.matchString(s_type, EntityType.livingEntities());
                    if (match == null)
                    {
                        return; //TODO msg
                    }
                    specialmatch = true;
                }
                remList = new ArrayList<Entity>();
                for (Entity entity : list)
                {
                    if ( //TODO change perms
                            (match.equals("pet") && entity instanceof Tameable && ((Tameable) entity).isTamed() && BasicsPerm.COMMAND_BUTCHER_FLAG_PET.isAuthorized(context.getSender()))
                            || (match.equals("golem") && entity instanceof Golem && BasicsPerm.COMMAND_BUTCHER_FLAG_GOLEM.isAuthorized(context.getSender()))
                            || (match.equals("animal") && entity instanceof Animals && !(entity instanceof Tameable) && BasicsPerm.COMMAND_BUTCHER_FLAG_ANIMAL.isAuthorized(context.getSender()))
                            || (match.equals("npc") && entity instanceof NPC && BasicsPerm.COMMAND_BUTCHER_FLAG_NPC.isAuthorized(context.getSender()))
                            || (match.equals("other") && (entity instanceof Ambient || entity instanceof WaterMob) && BasicsPerm.COMMAND_BUTCHER_FLAG_OTHER.isAuthorized(context.getSender()))
                            || (match.equals("boss") && (entity instanceof EnderDragon || entity instanceof Wither)) //TODO perm
                            || (match.equals("monster") && entity instanceof Monster || entity instanceof Slime || entity instanceof Ghast)//TODO perm
                            || (specialmatch
                            && (entity.getType().equals(EntityMatcher.get().matchEntity(match).getBukkitType()))) //TODO perms
                            )
                    {
                        remList.add(entity);
                    }
                }
                list.removeAll(remList);
            }
        }
        remList = loc.getWorld().getEntities();
        if (!allTypes)
        {
            remList.removeAll(list);
        }
        list.clear();
        for (Entity entity : remList)
        {
            if (entity.getType().isAlive())
            {
                list.add(entity);
            }
        }
        remList.retainAll(list);
        removed = this.removeEntities(remList, loc, radius, lightning);
        context.sendMessage("basics", removed == 0 ? "&eNothing to butcher!" : "&aYou just slaughtered &e%d &aliving entities!", removed);
    }

    private int removeEntities(List<Entity> remList, Location loc, int radius, boolean lightning)
    {
        int removed = 0;

        if (radius != -1 && loc == null)
        {
            throw new IllegalStateException("Unknown Location with radius");
        }
        boolean all = radius == -1;
        int radiusSquared = radius * radius;
        final Location entityLocation = new Location(null, 0, 0, 0);
        for (Entity entity : remList)
        {
            if (entity instanceof Player)
            {
                continue;
            }
            entity.getLocation(entityLocation);
            if (!all)
            {

                int distance = (int) (entityLocation.subtract(loc)).lengthSquared();
                if (radiusSquared < distance)
                {
                    continue;
                }
            }
            if (lightning)
            {
                entityLocation.getWorld().strikeLightningEffect(entityLocation);
            }
            entity.remove();
            removed++;
        }
        return removed;
    }
}
