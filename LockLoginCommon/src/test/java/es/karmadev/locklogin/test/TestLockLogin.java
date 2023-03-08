package es.karmadev.locklogin.test;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.CPluginNetwork;
import es.karmadev.locklogin.common.protection.CPluginHasher;
import es.karmadev.locklogin.common.runtime.CRuntime;
import es.karmadev.locklogin.common.user.CUserFactory;
import es.karmadev.locklogin.common.user.SQLiteDriver;
import es.karmadev.locklogin.common.user.storage.account.CAccountFactory;
import es.karmadev.locklogin.common.user.storage.session.CSessionFactory;
import es.karmadev.locklogin.test.config.TemporalConfiguration;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;

public class TestLockLogin implements LockLogin, KarmaSource {

    public final LockLoginRuntime runtime = new CRuntime();
    public final CPluginNetwork network = new CPluginNetwork();
    public final LockLoginHasher hasher;

    public final SQLiteDriver sqlite = new SQLiteDriver();
    public final CAccountFactory default_account_factory = new CAccountFactory(sqlite);
    public final CSessionFactory default_session_factory = new CSessionFactory(sqlite);
    public final CUserFactory default_user_factory = new CUserFactory(sqlite);
    public AccountFactory<UserAccount> account_factory = null;
    public SessionFactory<UserSession> session_factory = null;
    public UserFactory<LocalNetworkClient> user_factory = null;

    public TestLockLogin() {
        APISource.addProvider(this);

        Class<CurrentPlugin> instance = CurrentPlugin.class;
        try {
            Method initialize = instance.getDeclaredMethod("initialize", LockLogin.class);
            initialize.setAccessible(true);

            initialize.invoke(instance, this);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        hasher = new CPluginHasher();
    }

    /**
     * Get the LockLogin plugin instance
     *
     * @return the locklogin plugin
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public Object plugin() throws SecurityException {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        return this;
    }

    /**
     * Load an internal plugin file
     *
     * @param name the internal file name
     * @return the file
     * @throws SecurityException if the accessor is not the
     *                           plugin himself
     */
    @Override
    public InputStream load(final String name) throws SecurityException {
        return null;
    }

    /**
     * Get the plugin working directory
     *
     * @return the plugin working directory
     */
    @Override
    public Path workingDirectory() {
        return getDataPath();
    }

    /**
     * Get the plugin runtime
     *
     * @return the plugin runtime
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public LockLoginRuntime runtime() throws SecurityException {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        return runtime;
    }

    /**
     * Get the plugin network
     *
     * @return the plugin network
     */
    @Override
    public PluginNetwork network() {
        return network;
    }

    /**
     * Get the plugin hasher
     *
     * @return the plugin hasher
     */
    @Override
    public LockLoginHasher hasher() {
        return hasher;
    }

    /**
     * Get the plugin configuration
     *
     * @return the plugin configuration
     */
    @Override
    public Configuration configuration() {
        return new TemporalConfiguration();
    }

    /**
     * Get the plugin messages
     *
     * @return the plugin messages
     */
    @Override
    public Messages messages() {
        return null;
    }

    /**
     * Get the plugin account factory
     *
     * @param original retrieve the plugin default
     *                 account factory
     * @return the plugin account factory
     */
    @Override
    public AccountFactory<UserAccount> getAccountFactory(final boolean original) {
        return (AccountFactory<UserAccount>) (original ? default_account_factory : (account_factory != null ? account_factory : default_account_factory));
    }

    /**
     * Get the plugin session factory
     *
     * @param original retrieve the plugin default
     *                 session factory
     * @return the plugin session factory
     */
    @Override
    public SessionFactory<UserSession> getSessionFactory(final boolean original) {
        return (SessionFactory<UserSession>) (original ? default_session_factory : (session_factory != null ? session_factory : default_session_factory));
    }

    /**
     * Get the plugin user factory
     *
     * @param original retrieve the plugin default
     *                 user factory
     * @return the plugin user factory
     */
    @Override
    public UserFactory<LocalNetworkClient> getUserFactory(final boolean original) {
        return (UserFactory<LocalNetworkClient>) (original ? default_user_factory : (user_factory != null ? user_factory : default_user_factory));
    }

    /**
     * Define the plugin account factory
     *
     * @param factory the account factory
     */
    @Override
    public void setAccountFactory(final AccountFactory<UserAccount> factory) {
        account_factory = factory;
    }

    /**
     * Define the plugin session factory
     *
     * @param factory the account session factory
     */
    @Override
    public void setSessionFactory(final SessionFactory<UserSession> factory) {
        session_factory = factory;
    }

    /**
     * Define the plugin user factory
     *
     * @param factory the user factory
     */
    @Override
    public void setUserFactory(final UserFactory<LocalNetworkClient> factory) {
        user_factory = factory;
    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void info(final String message, final Object... replaces) {
        console().send(message, Level.INFO, replaces);
    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void warn(final String message, final Object... replaces) {
        console().send(message, Level.WARNING, replaces);
    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void err(final String message, final Object... replaces) {
        console().send(message, Level.GRAVE, replaces);
    }

    /**
     * Log something that is just informative
     *
     * @param message the log message
     */
    @Override
    public void logInfo(final String message) {
        logger().scheduleLog(Level.INFO, message);
    }

    /**
     * Log something that is important
     *
     * @param message the log message
     */
    @Override
    public void logWarn(final String message) {
        logger().scheduleLog(Level.WARNING, message);
    }

    /**
     * Log something that went wrong
     *
     * @param message the log message
     */
    @Override
    public void logErr(final String message) {
        logger().scheduleLog(Level.GRAVE, message);
    }

    /**
     * Log an error
     *
     * @param error   the error
     * @param message the message
     */
    @Override
    public void log(final Throwable error, final String message) {
        logger().scheduleLog(Level.GRAVE, error);
        logger().scheduleLog(Level.INFO, message);
    }


    /**
     * Karma source name
     *
     * @return the source name
     */
    @Override
    public String name() {
        return "LockLogin";
    }

    /**
     * Karma source version
     *
     * @return the source version
     */
    @Override
    public String version() {
        return "1.0.0";
    }

    /**
     * Karma source description
     *
     * @return the source description
     */
    @Override
    public String description() {
        return "";
    }

    /**
     * Karma source authors
     *
     * @return the source authors
     */
    @Override
    public String[] authors() {
        return new String[]{"KarmaDev"};
    }

    /**
     * Karma source update URL
     *
     * @return the source update URL
     */
    @Override
    public String updateURL() {
        return null;
    }
}
