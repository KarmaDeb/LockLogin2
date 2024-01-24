package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin register configuration
 */
public interface RegisterConfiguration extends Serializable {

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
     * Get the registration max time
     *
     * @return the registration max time
     */
    int timeout();

    /**
     * Get the maximum amount of accounts
     * that are allowed to be registered from
     * a same IP
     *
     * @return the max amount of accounts per IP
     */
    int maxAccounts();
}
