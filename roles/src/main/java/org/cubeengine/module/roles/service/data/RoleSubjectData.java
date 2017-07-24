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
package org.cubeengine.module.roles.service.data;

import static java.util.stream.Collectors.toList;

import org.cubeengine.module.roles.config.PermissionTree;
import org.cubeengine.module.roles.config.RoleConfig;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.collection.RoleCollection;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoleSubjectData extends CachingSubjectData
{
    private final RoleConfig config;

    public RoleSubjectData(RolesPermissionService service, RoleConfig config)
    {
        super(service);
        this.config = config;
    }

    @Override
    public boolean save(boolean changed)
    {
        if (changed)
        {
            cacheOptions();
            cachePermissions();
            cacheParents();
            config.settings = new HashMap<>();
            for (Map.Entry<Context, List<Subject>> entry : parents.entrySet())
            {
                List<String> collect = entry.getValue().stream()
                        .map(r -> r.getContainingCollection().getIdentifier() + ":" + r.getIdentifier() + "#" + RoleSubject.getInternalIdentifier(r))
                        .collect(toList());

                getContextSetting(config, entry.getKey()).parents.addAll(collect);
            }

            for (Map.Entry<Context, Map<String, Boolean>> entry : permissions.entrySet())
            {
                getContextSetting(config, entry.getKey()).permissions = new PermissionTree().setPermissions(entry.getValue());
            }

            for (Map.Entry<Context, Map<String, String>> entry : options.entrySet())
            {
                getContextSetting(config, entry.getKey()).options = entry.getValue();
            }

            try
            {
                Files.createDirectories(config.getFile().toPath().getParent());
            }
            catch (IOException e)
            {
                throw new IllegalStateException(e);
            }
            config.save();// TODO async
        }
        return changed;
    }

    private static RoleConfig.ContextSetting getContextSetting(RoleConfig config, Context context)
    {
        return config.settings.computeIfAbsent(stringify(context), k -> new RoleConfig.ContextSetting());
    }

    public RoleConfig getConfig()
    {
        return config;
    }

    @Override
    protected void cacheParents()
    {
        if (parents.isEmpty()) // not cached
        {
            for (Map.Entry<String, RoleConfig.ContextSetting> entry : config.settings.entrySet())
            {
                boolean parentRemoved = false;
                List<Subject> collect = new ArrayList<>();
                for (String s : entry.getValue().parents)
                {
                    Subject subject = this.getParent(s);
                    if (subject == null)
                    {
                        parentRemoved = true;
                    }
                    else
                    {
                        collect.add(subject);
                    }
                }
                collect.sort(RoleSubject::compare);
                parents.put(asContext(entry.getKey()), collect);
                if (parentRemoved)
                {
                    save(true);
                }
            }
        }
    }

    private Subject getParent(String id)
    {
        // collection:name#uuid
        int nameIndex = id.indexOf(":");
        int idIndex = id.indexOf("#");
        if (nameIndex > 0)
        {
            String type = id.substring(0, nameIndex);
            String name = idIndex > 0 ? id.substring(idIndex + 1) : id.substring(nameIndex + 1);
            SubjectCollection collection = service.getSubjects(type);
            if (collection instanceof RoleCollection)
            {
                return ((RoleCollection) collection).getByInternalIdentifier(name, getConfig().roleName);
            }
            return collection.get(name);
        }
        else
        {
            if (idIndex > 0)
            {
                return roleCollection.getByInternalIdentifier(id.substring(idIndex + 1), getConfig().roleName);
            }
            try
            {
                return roleCollection.getByUUID(UUID.fromString(id)).orElseThrow(() -> new IllegalArgumentException("Could not find a role for " + id));
            }
            catch (IllegalArgumentException e)
            {
                return roleCollection.get(id);
            }
        }
    }

    @Override
    protected void cachePermissions()
    {
        if (permissions.isEmpty()) // not cached
        {
            for (Map.Entry<String, RoleConfig.ContextSetting> entry : config.settings.entrySet())
            {
                permissions.put(asContext(entry.getKey()), entry.getValue().permissions.getPermissions());
            }
        }
    }

    @Override
    protected void cacheOptions()
    {
        if (options.isEmpty()) // not cached
        {
            for (Map.Entry<String, RoleConfig.ContextSetting> entry : config.settings.entrySet())
            {
                options.put(asContext(entry.getKey()), entry.getValue().options);
            }
        }
    }

    @Override
    public void reload()
    {
        config.reload();
        super.reload();
    }

    public void delete()
    {
        try
        {
            Files.delete(config.getFile().toPath());
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
   }
}
