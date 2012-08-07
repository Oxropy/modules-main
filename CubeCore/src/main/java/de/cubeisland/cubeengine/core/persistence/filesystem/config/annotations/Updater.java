package de.cubeisland.cubeengine.core.persistence.filesystem.config.annotations;

import de.cubeisland.cubeengine.core.persistence.filesystem.config.ConfigurationUpdater;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Anselm Brehme
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Updater
{
    Class<? extends ConfigurationUpdater> value();
}