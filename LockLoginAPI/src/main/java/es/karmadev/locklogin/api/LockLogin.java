package es.karmadev.locklogin.api;

import es.karmadev.locklogin.api.extension.manager.ModuleManager;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.license.License;
import es.karmadev.locklogin.api.plugin.license.LicenseProvider;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.backup.BackupService;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;

import java.io.InputStream;
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
     * Get if the plugin is running in
     * BungeeCord mode
     *
     * @return if the plugin is in bungee mode
     */
    boolean bungeeMode();

    /**
     * Get if the plugin is running in
     * online mode
     *
     * @return if the server is online mode
     */
    boolean onlineMode();

    /**
     * Get the plugin build type
     *
     * @return the plugin build
     */
    BuildType build();

    /**
     * Load an internal plugin file
     *
     * @param name the internal file name
     * @return the file
     * @throws SecurityException if the accessor is not the
     * plugin himself
     */
    InputStream load(final String name) throws SecurityException;

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
    AccountFactory<UserAccount> getAccountFactory(final boolean original);

    /**
     * Get the plugin session factory
     *
     * @param original retrieve the plugin default
     *                 session factory
     * @return the plugin session factory
     */
    SessionFactory<UserSession> getSessionFactory(final boolean original);

    /**
     * Get the plugin user factory
     *
     * @param original retrieve the plugin default
     *                 user factory
     * @return the plugin user factory
     */
    UserFactory<LocalNetworkClient> getUserFactory(final boolean original);

    /**
     * Get the plugin server factory
     *
     * @param original retrieve the plugin default
     *                 server factory
     * @return the plugin server factory
     */
    ServerFactory<NetworkServer> getServerFactory(final boolean original);

    /**
     * Get the plugin account manager
     *
     * @return the plugin account manager
     */
    MultiAccountManager accountManager();

    /**
     * Get a backup service
     *
     * @param name the service name
     * @return the backup service
     */
    BackupService getBackupService(final String name);

    /**
     * Get the license provider
     *
     * @return the license provider
     */
    LicenseProvider licenseProvider();

    /**
     * Get the current license
     *
     * @return the license
     */
    License license();

    /**
     * Get the plugin module manager
     *
     * @return the plugin module manager
     */
    ModuleManager moduleManager();

    /**
     * Get the plugin premium data store
     *
     * @return the plugin premium store
     */
    PremiumDataStore premiumStore();

    /**
     * Updates the plugin license
     *
     * @param new_license the new plugin license
     * @throws SecurityException if the action was not performed by the plugin
     */
    void updateLicense(final License new_license) throws SecurityException;

    /**
     * Define the plugin account factory
     *
     * @param factory the account factory
     */
    void setAccountFactory(final AccountFactory<UserAccount> factory);

    /**
     * Define the plugin session factory
     *
     * @param factory the account session factory
     */
    void setSessionFactory(final SessionFactory<UserSession> factory);

    /**
     * Define the plugin user factory
     *
     * @param factory the user factory
     */
    void setUserFactory(final UserFactory<LocalNetworkClient> factory);

    /**
     * Define the plugin server factory
     *
     * @param factory the server factory
     */
    void setServerFactory(final ServerFactory<NetworkServer> factory);

    /**
     * Register a backup service
     *
     * @param name the service name
     * @param service the backup service
     */
    void registerBackupService(final String name, final BackupService service);

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

    /**
     * Log something that is just informative
     *
     * @param message the log message
     * @param replaces the message replaces
     */
    void logInfo(final String message, final Object... replaces);

    /**
     * Log something that is important
     *
     * @param message the log message
     * @param replaces the message replaces
     */
    void logWarn(final String message, final Object... replaces);

    /**
     * Log something that went wrong
     *
     * @param message the log message
     * @param replaces the message replaces
     */
    void logErr(final String message, final Object... replaces);

    /**
     * Log an error
     *
     * @param error the error
     * @param message the message
     * @param replaces the message replaces
     */
    void log(final Throwable error, final String message, final Object... replaces);
}
