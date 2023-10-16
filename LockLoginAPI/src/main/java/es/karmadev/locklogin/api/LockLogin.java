package es.karmadev.locklogin.api;

import es.karmadev.api.file.yaml.handler.ResourceLoader;
import es.karmadev.locklogin.api.extension.ModuleConverter;
import es.karmadev.locklogin.api.extension.module.ModuleManager;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.api.plugin.ServerHash;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.LanguagePackManager;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.marketplace.MarketPlace;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.auth.ProcessFactory;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * LockLogin plugin
 */
@SuppressWarnings("unused")
public interface LockLogin extends ResourceLoader {

    /**
     * Get the LockLogin plugin instance
     *
     * @return the locklogin plugin
     * @throws SecurityException if tried to access from an unauthorized source
     */
    Object plugin() throws SecurityException;

    /**
     * Get the plugin marketplace
     *
     * @return the plugin marketplace
     */
    MarketPlace getMarketPlace();

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
     * Get the plugin data driver
     *
     * @return the plugin data driver
     */
    SQLDriver driver();

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
    LockLoginRuntime getRuntime() throws SecurityException;

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
    default Messages messages() {
        return languagePackManager().getMessenger();
    }

    /**
     * Get the plugin language pack manager
     *
     * @return the plugin language pack
     * manager
     */
    LanguagePackManager languagePackManager();

    /**
     * Get the plugin auth process factory
     *
     * @return the process factory
     */
    ProcessFactory getProcessFactory();

    /**
     * Get the module converter
     *
     * @return the module converter
     * @param <T> the converter type
     */
    <T> ModuleConverter<T> getConverter();

    /**
     * Get the plugin account factory
     *
     * @param original retrieve the plugin default
     *                 account factory
     * @return the plugin account factory
     */
    <T extends AccountFactory<? extends UserAccount>> T getAccountFactory(final boolean original);

    /**
     * Get the plugin session factory
     *
     * @param original retrieve the plugin default
     *                 session factory
     * @return the plugin session factory
     */
    <T extends SessionFactory<? extends UserSession>> T getSessionFactory(final boolean original);

    /**
     * Get the plugin user factory
     *
     * @param original retrieve the plugin default
     *                 user factory
     * @return the plugin user factory
     */
    <T extends UserFactory<? extends LocalNetworkClient>> T getUserFactory(final boolean original);

    /**
     * Get the plugin server factory
     *
     * @param original retrieve the plugin default
     *                 server factory
     * @return the plugin server factory
     */
    <T extends ServerFactory<? extends NetworkServer>> T getServerFactory(final boolean original);

    /**
     * Get the plugin account manager
     *
     * @return the plugin account manager
     */
    MultiAccountManager accountManager();

    /**
     * Get a service
     *
     * @param name the service name
     * @return the service
     */
    PluginService getService(final String name);

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
     * Get the current server hash
     *
     * @return the server hash
     * @throws SecurityException if tried to be accessed from any
     * external source that's not the self plugin
     */
    ServerHash server() throws SecurityException;

    /**
     * Register a service
     *
     * @param name the service name
     * @param service the plugin service to register
     * @throws UnsupportedOperationException if the service is already registered
     */
    void registerService(final String name, final PluginService service) throws UnsupportedOperationException;

    /**
     * Unregister a service
     *
     * @param name the service name
     * @throws UnsupportedOperationException if the service is plugin internal
     */
    void unregisterService(final String name) throws UnsupportedOperationException;

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
