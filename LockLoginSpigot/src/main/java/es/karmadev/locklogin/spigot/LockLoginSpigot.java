package es.karmadev.locklogin.spigot;

import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.command.CommandRegistrar;
import es.karmadev.locklogin.api.extension.manager.ModuleManager;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.license.License;
import es.karmadev.locklogin.api.plugin.license.LicenseProvider;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.backup.BackupService;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.dependency.CPluginDependency;
import es.karmadev.locklogin.common.api.extension.CModuleManager;
import es.karmadev.locklogin.common.api.extension.loader.CModuleLoader;
import es.karmadev.locklogin.common.api.plugin.file.CPluginConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.lang.InternalPack;
import es.karmadev.locklogin.common.api.protection.CPluginHasher;
import es.karmadev.locklogin.common.api.protection.type.*;
import es.karmadev.locklogin.common.api.runtime.CRuntime;
import es.karmadev.locklogin.common.api.SQLiteDriver;
import es.karmadev.locklogin.common.api.server.CServerFactory;
import es.karmadev.locklogin.common.api.user.CUserFactory;
import es.karmadev.locklogin.common.api.user.storage.account.CAccountFactory;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionFactory;
import es.karmadev.locklogin.common.api.web.license.CLicenseProvider;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.comparator.VersionComparator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LockLoginSpigot extends KarmaPlugin implements LockLogin {

    private final ModuleManager manager = new CModuleManager();

    public final CommandRegistrar modCommands = null;

    private final SQLiteDriver sqlite = new SQLiteDriver();

    private final LockLoginRuntime runtime = new CRuntime(manager);
    private final CPluginNetwork network = new CPluginNetwork(sqlite);
    private final CAccountFactory default_account_factory = new CAccountFactory(sqlite);
    private final CSessionFactory default_session_factory = new CSessionFactory(sqlite);
    private final CUserFactory default_user_factory = new CUserFactory(sqlite);
    private final CServerFactory default_server_factory = new CServerFactory(sqlite);
    private final Map<String, BackupService> backup_services = new ConcurrentHashMap<>();
    private final CLicenseProvider license_provider = new CLicenseProvider();
    private final Configuration configuration = new CPluginConfiguration();

    private InternalPack pack = new InternalPack();
    private AccountFactory<UserAccount> account_factory = null;
    private SessionFactory<UserSession> session_factory = null;
    private UserFactory<LocalNetworkClient> user_factory = null;
    private ServerFactory<NetworkServer> server_factory = null;
    private License license;

    private final LockLoginHasher hasher;


    public LockLoginSpigot() {
        super(false);
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
     * Get if the plugin is running in
     * BungeeCord mode
     *
     * @return if the plugin is in bungee mode
     */
    @Override
    public boolean bungeeMode() {
        File server_folder = getServer().getWorldContainer();
        File spigot_yml = new File(server_folder, "spigot.yml");
        KarmaYamlManager yaml = new KarmaYamlManager(spigot_yml);

        return yaml.getBoolean("settings.bungeecord", false);
    }

    /**
     * Get the plugin build type
     *
     * @return the plugin build
     */
    @Override
    public BuildType build() {
        return null;
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY);

        File pluginFile = getFile();
        try(JarFile jarFile = new JarFile(pluginFile)) {
            JarEntry entry = jarFile.getJarEntry(name);
            try (InputStream stream = jarFile.getInputStream(entry)) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = stream.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    baos.flush();

                    return new ByteArrayInputStream(baos.toByteArray());
                }
            }
        } catch (Throwable ignored) {}

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
     * Get a backup service
     *
     * @param name the service name
     * @return the backup service
     */
    @Override
    public BackupService getBackupService(final String name) {
        return backup_services.getOrDefault(name, null);
    }

    /**
     * Get the license provider
     *
     * @return the license provider
     */
    @Override
    public LicenseProvider licenseProvider() {
        return license_provider;
    }

    /**
     * Get the current license
     *
     * @return the license
     */
    @Override
    public License license() {
        return license;
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
     * Updates the plugin license
     *
     * @param new_license the new plugin license
     * @throws SecurityException if the action was not performed by the plugin
     */
    @Override
    public void updateLicense(final License new_license) throws SecurityException {
        license = new_license;
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
     * Register a backup service
     *
     * @param name    the service name
     * @param service the backup service
     */
    @Override
    public void registerBackupService(final String name, final BackupService service) {
        backup_services.put(name, service);
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
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
        try {
            hasher.registerMethod(new SHA512Hash());
            hasher.registerMethod(new SHA256Hash());
            hasher.registerMethod(new BCryptHash());
            hasher.registerMethod(new Argon2I());
            hasher.registerMethod(new Argon2D());
            hasher.registerMethod(new Argon2ID());
        } catch (UnnamedHashException ex) {
            ex.printStackTrace();
        }

        console().send("Preparing to inject dependencies. Please wait...", Level.WARNING);
        CPluginDependency.load();

        PluginManager pluginManager = getServer().getPluginManager();
        boolean boot = true;
        for (LockLoginDependency dependency : CPluginDependency.getAll()) {
            if (dependency.isPlugin()) {
                String name = dependency.name();
                String version = dependency.version().plugin();

                if (name.equalsIgnoreCase("KarmaAPI")) {
                    String platform = dependency.version().project();

                    String API_VERSION = KarmaAPI.getVersion();
                    String PLUGIN_VERSION = KarmaAPI.getPluginVersion();

                    if (API_VERSION.equals(platform)) {
                        VersionComparator comparator = new VersionComparator(PLUGIN_VERSION.replace("-", "."), version.replace("-", "."));
                        if (comparator.isUpToDate()) {
                            console().send("KarmaAPI detected successfully. Version {0}[{1}] of {2}[{3}] (required)", API_VERSION, PLUGIN_VERSION, platform, version);
                        } else {
                            console().send("Cannot load LockLogin as required dependency (KarmaAPI) is out of date ({0}). Yours: {1}", Level.GRAVE, version, PLUGIN_VERSION);
                            boot = false;
                            break;
                        }
                    } else {
                        console().send("Cannot load LockLogin as required dependency (KarmaAPI) is not in the required build ({0}). Yours: {1}", Level.GRAVE, platform, API_VERSION);
                        boot = true;
                        break;
                    }
                } else {
                    if (pluginManager.isPluginEnabled(name)) {
                        Plugin plugin = pluginManager.getPlugin(name);
                        String pluginVersion = plugin.getDescription().getVersion();

                        VersionComparator comparator = new VersionComparator(pluginVersion, version);
                        if (!comparator.isUpToDate()) {
                            console().send("Plugin dependency {0} was found but is out of date ({1} > {2}). LockLogin will still try to hook into its API, but there may be some errors", Level.WARNING, name, version, pluginVersion);
                        } else {
                            console().send("Plugin dependency {0} has been successfully hooked", Level.INFO, name);
                        }
                    }
                }
            } else {
                console().send("Injecting dependency \"{0}\"", Level.INFO, dependency.name());
                runtime.dependencyManager().append(dependency);
            }
        }

        if (boot) {

        }
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
