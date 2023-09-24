package es.karmadev.locklogin.test;

import es.karmadev.api.core.CoreModule;
import es.karmadev.api.core.DefaultRuntime;
import es.karmadev.api.core.source.APISource;
import es.karmadev.api.core.source.SourceManager;
import es.karmadev.api.core.source.runtime.SourceRuntime;
import es.karmadev.api.file.util.NamedStream;
import es.karmadev.api.logger.LogManager;
import es.karmadev.api.logger.SourceLogger;
import es.karmadev.api.logger.log.BoundedLogger;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.schedule.task.TaskScheduler;
import es.karmadev.api.strings.StringFilter;
import es.karmadev.api.strings.placeholder.PlaceholderEngine;
import es.karmadev.api.strings.placeholder.engine.SimpleEngine;
import es.karmadev.api.version.Version;
import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.ModuleConverter;
import es.karmadev.locklogin.api.extension.module.ModuleManager;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.api.plugin.ServerHash;
import es.karmadev.locklogin.api.plugin.database.driver.Driver;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.schema.RowType;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
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
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.database.sql.CSQLDriver;
import es.karmadev.locklogin.common.api.extension.CModuleManager;
import es.karmadev.locklogin.common.api.plugin.file.CPluginConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.lang.InternalPack;
import es.karmadev.locklogin.common.api.protection.CPluginHasher;
import es.karmadev.locklogin.common.api.server.CServerFactory;
import es.karmadev.locklogin.common.api.user.CUserFactory;
import es.karmadev.locklogin.common.api.user.storage.account.CAccountFactory;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("all")
public class DummyLockLogin implements LockLogin, APISource {

    private final InternalPack pack = new InternalPack();

    private final ModuleManager manager = new CModuleManager();
    public final SQLDriver sqlite = new CSQLDriver(Driver.SQLite);
    public final LockLoginRuntime runtime = new AllowAllRuntime();
    public final CPluginNetwork network = new CPluginNetwork(sqlite);
    public final CAccountFactory default_account_factory = new CAccountFactory(sqlite);
    public final CSessionFactory default_session_factory = new CSessionFactory(sqlite);
    public final CUserFactory default_user_factory = new CUserFactory(sqlite);
    public final CServerFactory default_server_factory = new CServerFactory(sqlite);
    public AccountFactory<UserAccount> account_factory = null;
    public SessionFactory<UserSession> session_factory = null;
    public UserFactory<LocalNetworkClient> user_factory = null;
    public ServerFactory<NetworkServer> server_factory = null;

    public LockLoginHasher hasher;
    private Configuration configuration;

    public DummyLockLogin() throws Throwable {
        SourceManager.register(this);

        Class<CurrentPlugin> instance = CurrentPlugin.class;
        try {
            Method initialize = instance.getDeclaredMethod("initialize", LockLogin.class);
            initialize.setAccessible(true);

            initialize.invoke(instance, this);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        configuration = new CPluginConfiguration();
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
     * Get if the plugin is running in
     * BungeeCord mode
     *
     * @return if the plugin is in bungee mode
     */
    @Override
    public boolean bungeeMode() {
        return false;
    }

    /**
     * Get if the plugin is running in
     * online mode
     *
     * @return if the server is online mode
     */
    @Override
    public boolean onlineMode() {
        return false;
    }

    /**
     * Get the plugin data driver
     *
     * @return the plugin data driver
     */
    @Override
    public SQLDriver driver() {
        return sqlite;
    }

    /**
     * Get the plugin build type
     *
     * @return the plugin build
     */
    @Override
    public BuildType build() {
        return BuildType.BETA;
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
        return DummyLockLogin.class.getResourceAsStream("/" + name);
    }

    @Override
    public @NotNull String identifier() {
        return "TEST";
    }

    @Override
    public @NotNull String sourceName() {
        return "LockLogin";
    }

    @Override
    public @NotNull Version sourceVersion() {
        return Version.of(1, 0, 0, "t");
    }

    @Override
    public @NotNull String sourceDescription() {
        return "Test Locklogin";
    }

    @Override
    public @NotNull String[] sourceAuthors() {
        return new String[]{"KarmaDev"};
    }

    @Override
    public @Nullable URI sourceUpdateURI() {
        return null;
    }

    @Override
    public @NotNull SourceRuntime runtime() {
        return new DefaultRuntime(this);
    }

    @Override
    public @NotNull PlaceholderEngine placeholderEngine(String s) {
        return new SimpleEngine();
    }

    @Override
    public @NotNull TaskScheduler scheduler(String s) {
        return SourceManager.getUnsafePrincipal().scheduler(s);
    }

    /**
     * Get the plugin working directory
     *
     * @return the plugin working directory
     */
    @Override
    public Path workingDirectory() {
        return Paths.get("./junit/test");
    }

    @Override
    public @NotNull Path navigate(String s, String... strings) {
        Path initial = workingDirectory();
        for (String str : strings) {
            initial = initial.resolve(str);
        }

        return initial.resolve(s);
    }

    @Override
    public @Nullable NamedStream findResource(String s) {
        return null;
    }

    @Override
    public @NotNull NamedStream[] findResources(String s, @Nullable StringFilter stringFilter) {
        return new NamedStream[0];
    }

    @Override
    public boolean export(String s, Path path) {
        return false;
    }

    @Override
    public SourceLogger logger() {
        return LogManager.getLogger(this);
    }

    @Override
    public @Nullable CoreModule getModule(String s) {
        return null;
    }

    @Override
    public boolean registerModule(CoreModule coreModule) {
        return false;
    }

    @Override
    public void loadIdentifier(String s) {

    }

    @Override
    public void saveIdentifier(String s) {

    }

    /**
     * Get the plugin runtime
     *
     * @return the plugin runtime
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public LockLoginRuntime getRuntime() throws SecurityException {
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
        return configuration;
    }

    /**
     * Get the plugin messages
     *
     * @return the plugin messages
     */
    @Override
    public Messages messages() {
        return pack.getMessenger();
    }

    /**
     * Get the plugin auth process factory
     *
     * @return the process factory
     */
    @Override
    public ProcessFactory getProcessFactory() {
        return null;
    }

    /**
     * Get the module converter
     *
     * @return the module converter
     */
    @Override
    public <T> ModuleConverter<T> getConverter() {
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
     * Get the plugin server factory
     *
     * @param original retrieve the plugin default
     *                 server factory
     * @return the plugin server factory
     */
    @Override
    public ServerFactory<NetworkServer> getServerFactory(final boolean original) {
        return (ServerFactory<NetworkServer>) (original ? default_server_factory : (server_factory != null ? server_factory : default_server_factory));
    }

    /**
     * Get the plugin account manager
     *
     * @return the plugin account manager
     */
    @Override
    public MultiAccountManager accountManager() {
        return null;
    }

    /**
     * Get a service
     *
     * @param name the service name
     * @return the service
     */
    @Override
    public PluginService getService(String name) {
        return null;
    }

    /**
     * Get the plugin module manager
     *
     * @return the plugin module manager
     */
    @Override
    public ModuleManager moduleManager() {
        return manager;
    }

    /**
     * Get the plugin premium data store
     *
     * @return the plugin premium store
     */
    @Override
    public PremiumDataStore premiumStore() {
        return null;
    }

    /**
     * Get the current server hash
     *
     * @return the server hash
     * @throws SecurityException if tried to be accessed from any
     *                           external source that's not the self plugin
     */
    @Override
    public ServerHash server() throws SecurityException {
        return null;
    }

    /**
     * Register a service
     *
     * @param name    the service name
     * @param service the plugin service to register
     * @throws UnsupportedOperationException if the service is already registered
     */
    @Override
    public void registerService(String name, PluginService service) throws UnsupportedOperationException {

    }

    /**
     * Unregister a service
     *
     * @param name the service name
     * @throws UnsupportedOperationException if the service is plugin internal
     */
    @Override
    public void unregisterService(String name) throws UnsupportedOperationException {

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
     * Define the plugin server factory
     *
     * @param factory the server factory
     */
    @Override
    public void setServerFactory(final ServerFactory<NetworkServer> factory) {
        server_factory = factory;
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void info(final String message, final Object... replaces) {
        logger().send(LogLevel.INFO, message, replaces);
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void warn(final String message, final Object... replaces) {
        logger().send(LogLevel.WARNING, message, replaces);
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void err(final String message, final Object... replaces) {
        logger().send(LogLevel.ERROR, message, replaces);
    }

    /**
     * Log something that is just informative
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logInfo(final String message, final Object... replaces) {
        logger().log(LogLevel.INFO, message, replaces);
    }

    /**
     * Log something that is important
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logWarn(final String message, final Object... replaces) {
        logger().log(LogLevel.WARNING, message, replaces);
    }

    /**
     * Log something that went wrong
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logErr(final String message, final Object... replaces) {
        logger().log(LogLevel.ERROR, message, replaces);
    }

    /**
     * Log an error
     *
     * @param error    the error
     * @param message  the message
     * @param replaces the message replaces
     */
    @Override
    public void log(final Throwable error, final String message, final Object... replaces) {
        logger().log(error, message, replaces);
    }

    @Override
    public InputStream loadResource(String s) {
        return DummyLockLogin.class.getResourceAsStream("/" + s);
    }
}
