package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin login configuration
 */
public interface LoginConfiguration extends Serializable {

    /**
     * Get if the registration process displays
     * a bossbar
     *
     * @return if registration shows a bossbar
     */
    boolean bossBar();

    /**
     * Get if the unregistered clients
     * should receive blind effect
     *
     * @return if unregistered clients receive blindness
     */
    boolean blindEffect();

    /**
     * Get the login max time
     *
     * @return the login max time
     */
    int timeout();
}
