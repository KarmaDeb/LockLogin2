package es.karmadev.locklogin.spigot.command.helper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * LockLogin plugin command
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginCommand {

    /**
     * The plugin command name
     *
     * @return the command
     */
    String command();

    /**
     * If the plugin command works
     * in BungeeCord mode
     *
     * @return the command
     */
    boolean useInBungeecord() default false;
}
