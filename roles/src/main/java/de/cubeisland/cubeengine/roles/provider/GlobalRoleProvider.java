package de.cubeisland.cubeengine.roles.provider;

import de.cubeisland.cubeengine.roles.Roles;
import de.cubeisland.cubeengine.roles.role.Role;

import java.io.File;
import java.util.Locale;

import static de.cubeisland.cubeengine.core.logger.LogLevel.DEBUG;

public class GlobalRoleProvider extends RoleProvider
{
    public GlobalRoleProvider(Roles module)
    {
        super(module, true);
        this.createBasePerm();
    }

    @Override
    public void createBasePerm()
    {
        this.basePerm = this.module.getBasePermission().createAbstractChild("global");
    }

    @Override
    public void loadInConfigurations(File rolesFolder)
    {
        this.module.getLog().log(DEBUG, "Loading global roles...");
        if (this.init) // provider is already initialized!
        {
            return;
        }
        if (this.folder == null)
        {
            // Sets the folder for this provider
            this.folder = rolesFolder;
        }
        super.loadInConfigurations(rolesFolder);
    }

    @Override
    public Role getRole(String roleName)
    {
        if (roleName == null)
        {
            return null;
        }
        if (roleName.startsWith("g:"))
        {
            roleName = roleName.substring(2);
        }
        return this.roles.get(roleName.toLowerCase(Locale.ENGLISH));
    }
}
