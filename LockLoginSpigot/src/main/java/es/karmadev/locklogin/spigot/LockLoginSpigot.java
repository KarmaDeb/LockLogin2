package es.karmadev.locklogin.spigot;

import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
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
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.backup.BackupService;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.SQLiteDriver;
import es.karmadev.locklogin.common.api.extension.CModuleManager;
import es.karmadev.locklogin.common.api.plugin.file.CPluginConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.lang.InternalPack;
import es.karmadev.locklogin.common.api.plugin.service.backup.CLocalBackup;
import es.karmadev.locklogin.common.api.protection.CPluginHasher;
import es.karmadev.locklogin.common.api.runtime.CRuntime;
import es.karmadev.locklogin.common.api.server.CServerFactory;
import es.karmadev.locklogin.common.api.user.CUserFactory;
import es.karmadev.locklogin.common.api.user.storage.account.CAccountFactory;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionFactory;
import es.karmadev.locklogin.common.api.web.license.CLicenseProvider;
import lombok.Getter;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.enums.Level;

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
import java.util.stream.Stream;

public class LockLoginSpigot implements LockLogin {

    private final KarmaPlugin plugin;

    @Getter
    private final SQLiteDriver driver = new SQLiteDriver();

    private final CModuleManager moduleManager = new CModuleManager();
    private final CRuntime runtime = new CRuntime(moduleManager);
    private final CPluginNetwork network = new CPluginNetwork(driver);
    private final CPluginHasher hasher;
    private final CPluginConfiguration configuration;
    private final InternalPack messages;

    private final CAccountFactory default_account_factory = new CAccountFactory(driver);
    private final CSessionFactory default_session_factory = new CSessionFactory(driver);
    private final CUserFactory default_user_factory = new CUserFactory(driver);
    private final CServerFactory default_server_factory = new CServerFactory(driver);

    private AccountFactory<? extends UserAccount> provider_account_factory = null;
    private SessionFactory<? extends UserSession> provider_session_factory = null;
    private UserFactory<? extends LocalNetworkClient> provider_user_factory = null;
    private ServerFactory<? extends NetworkServer> provider_server_factory = null;

    private final Map<String, PluginService> service_provider = new ConcurrentHashMap<>();
    private final CLicenseProvider license_provider;

    private License license;

    public LockLoginSpigot(final KarmaPlugin plugin) {
        this.plugin = plugin;

        Class<CurrentPlugin> instance = CurrentPlugin.class;
        try {
            Method initialize = instance.getDeclaredMethod("initialize", LockLogin.class);
            initialize.setAccessible(true);

            initialize.invoke(instance, this);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        hasher = new CPluginHasher();
        configuration = new CPluginConfiguration();
        messages = new InternalPack();
        license_provider = new CLicenseProvider();
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
        return plugin;
    }

    /**
     * Get if the plugin is running in
     * BungeeCord mode
     *
     * @return if the plugin is in bungee mode
     */
    @Override
    public boolean bungeeMode() {
        File server_folder = plugin.getServer().getWorldContainer();
        File spigot_yml = new File(server_folder, "spigot.yml");
        KarmaYamlManager yaml = new KarmaYamlManager(spigot_yml);

        return yaml.getBoolean("settings.bungeecord", false);
    }

    /**
     * Get if the plugin is running in
     * online mode
     *
     * @return if the server is online mode
     */
    @Override
    public boolean onlineMode() {
        return plugin.getServer().getOnlineMode();
    }

    /**
     * Get the plugin build type
     *
     * @return the plugin build
     */
    @Override
    public BuildType build() {
        return BuildType.RELEASE;
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

        File pluginFile = plugin.getSourceFile();
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
        return plugin.getDataPath();
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
        return messages.getMessenger();
    }

    /**
     * Get the plugin account factory
     *
     * @param original retrieve the plugin default
     *                 account factory
     * @return the plugin account factory
     */
    @Override @SuppressWarnings("unchecked")
    public AccountFactory<? extends UserAccount> getAccountFactory(final boolean original) {
        if (original || provider_account_factory == null) {
            return default_account_factory;
        }

        return provider_account_factory;
    }

    /**
     * Get the plugin session factory
     *
     * @param original retrieve the plugin default
     *                 session factory
     * @return the plugin session factory
     */
    @Override @SuppressWarnings("unchecked")
    public SessionFactory<? extends UserSession> getSessionFactory(final boolean original) {
        if (original || provider_session_factory == null) {
            return default_session_factory;
        }

        return provider_session_factory;
    }

    /**
     * Get the plugin user factory
     *
     * @param original retrieve the plugin default
     *                 user factory
     * @return the plugin user factory
     */
    @Override @SuppressWarnings("unchecked")
    public UserFactory<? extends LocalNetworkClient> getUserFactory(final boolean original) {
        if (original || provider_user_factory == null) {
            return default_user_factory;
        }

        return provider_user_factory;
    }

    /**
     * Get the plugin server factory
     *
     * @param original retrieve the plugin default
     *                 server factory
     * @return the plugin server factory
     */
    @Override @SuppressWarnings("unchecked")
    public ServerFactory<? extends NetworkServer> getServerFactory(final boolean original) {
        if (original || provider_server_factory == null) {
            return default_server_factory;
        }

        return provider_server_factory;
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
     * Get a backup service
     *
     * @param name the service name
     * @return the backup service
     */
    @Override
    public BackupService getBackupService(final String name) {
        PluginService service = service_provider.getOrDefault(name, null);
        if (service instanceof BackupService) {
            return (BackupService) service;
        }

        return null;
    }

    /**
     * Get a service
     *
     * @param name the service name
     * @return the service
     */
    @Override
    public PluginService getService(final String name) {
        return service_provider.getOrDefault(name, null);
    }

    /**
     * Get the license provider
     *
     * @return the license provider
     */
    @Override
    public LicenseProvider licenseProvider() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY);
        return license_provider;
    }

    /**
     * Get the current license
     *
     * @return the license
     */
    @Override
    public License license() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        return license;
    }

    /**
     * Get the plugin module manager
     *
     * @return the plugin module manager
     */
    @Override
    public ModuleManager moduleManager() {
        return null;
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
     * Register a service
     *
     * @param name    the service name
     * @param service the plugin service to register
     * @throws UnsupportedOperationException if the service is already registered
     */
    @Override
    public void registerService(final String name, final PluginService service) throws UnsupportedOperationException {
        if (service_provider.containsKey(name)) {
            throw new UnsupportedOperationException("Cannot register service " + name + " because it's already defined by another service");
        }
        Stream<PluginService> filtered_services = service_provider.values().stream().filter((registered) -> service.getClass().equals(registered.getClass()));
        if (filtered_services.findAny().isPresent()) {
            throw new UnsupportedOperationException("Cannot register service " + name + " because it's already registered under other service name");
        }

        service_provider.put(name, service);
    }

    /**
     * Unregister a service
     *
     * @param name the service name
     * @throws UnsupportedOperationException if the service is plugin internal
     * @throws NullPointerException if the service does not exist
     */
    @Override
    public void unregisterService(final String name) throws UnsupportedOperationException, NullPointerException {
        PluginService service = service_provider.getOrDefault(name, null);
        if (service == null) throw new NullPointerException("Cannot unregister service " + name + " because it does not exist");
        if (service instanceof CLocalBackup) throw new UnsupportedOperationException("Cannot unregister plugin internal service: " + name);

        service_provider.remove(name);
    }

    /**
     * Updates the plugin license
     *
     * @param new_license the new plugin license
     * @throws SecurityException if the action was not performed by the plugin
     */
    @Override
    public void updateLicense(final License new_license) throws SecurityException {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY);
        license = new_license;
    }

    /**
     * Define the plugin account factory
     *
     * @param factory the account factory
     */
    @Override
    public void setAccountFactory(final AccountFactory<UserAccount> factory) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        provider_account_factory = factory;
    }

    /**
     * Define the plugin session factory
     *
     * @param factory the account session factory
     */
    @Override
    public void setSessionFactory(final SessionFactory<UserSession> factory) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        provider_session_factory = factory;
    }

    /**
     * Define the plugin user factory
     *
     * @param factory the user factory
     */
    @Override
    public void setUserFactory(final UserFactory<LocalNetworkClient> factory) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        provider_user_factory = factory;
    }

    /**
     * Define the plugin server factory
     *
     * @param factory the server factory
     */
    @Override
    public void setServerFactory(final ServerFactory<NetworkServer> factory) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        provider_server_factory = factory;
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void info(final String message, final Object... replaces) {
        plugin.console().send(message, Level.INFO, replaces);
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void warn(final String message, final Object... replaces) {
        plugin.console().send(message, Level.WARNING, replaces);
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void err(final String message, final Object... replaces) {
        plugin.console().send(message, Level.GRAVE, replaces);
    }

    /**
     * Log something that is just informative
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logInfo(final String message, final Object... replaces) {
        plugin.logger().scheduleLog(Level.INFO, message, replaces);
    }

    /**
     * Log something that is important
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logWarn(final String message, final Object... replaces) {
        plugin.logger().scheduleLog(Level.WARNING, message, replaces);
    }

    /**
     * Log something that went wrong
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logErr(final String message, final Object... replaces) {
        plugin.logger().scheduleLog(Level.GRAVE, message, replaces);
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
        plugin.logger().scheduleLog(Level.GRAVE, error);
        plugin.logger().scheduleLog(Level.INFO, message, replaces);
    }
}
