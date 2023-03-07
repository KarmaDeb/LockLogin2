package es.karmadev.locklogin.api;

import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;

import java.nio.file.Path;

/**
 * LockLogin plugin
 */
public interface LockLogin {

    /**
     * Get the LockLogin plugin instance
     *
     * @return the locklogin plugin
     * @throws SecurityException if tried to access from an unauthorized source
     */
    Object plugin() throws SecurityException;

    /**
     * Get the plugin working directory
     *
     * @return the plugin working directory
     */
    Path workingDirectory();

    /**
     * Get the plugin runtime
     *
     * @return the plugin runtime
     * @throws SecurityException if tried to access from an unauthorized source
     */
    LockLoginRuntime runtime() throws SecurityException;

    /**
     * Get the plugin network
     *
     * @return the plugin network
     */
    PluginNetwork network();

    /**
     * Get the plugin hasher
     *
     * @return the plugin hasher
     */
    LockLoginHasher hasher();

    /**
     * Get the plugin configuration
     *
     * @return the plugin configuration
     */
    Configuration configuration();

    /**
     * Get the plugin messages
     *
     * @return the plugin messages
     */
    Messages messages();

    /**
     * Get the plugin account factory
     *
     * @param original retrieve the plugin default
     *                 account factory
     * @return the plugin account factory
     */
    AccountFactory<? extends UserAccount> getAccountFactory(final boolean original);

    /**
     * Get the plugin session factory
     *
     * @param original retrieve the plugin default
     *                 session factory
     * @return the plugin session factory
     */
    SessionFactory<? extends UserSession> getSessionFactory(final boolean original);

    /**
     * Define the plugin account factory
     *
     * @param factory the account factory
     */
    void setAccountFactory(final AccountFactory<? extends UserAccount> factory);

    /**
     * Define the plugin session factory
     *
     * @param factory the account session factory
     */
    void setSessionFactory(final SessionFactory<? extends UserSession> factory);

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    void info(final String message, final Object... replaces);

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    void warn(final String message, final Object... replaces);

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    void err(final String message, final Object... replaces);
}
