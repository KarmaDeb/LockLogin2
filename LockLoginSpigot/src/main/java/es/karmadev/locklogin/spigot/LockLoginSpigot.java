package es.karmadev.locklogin.spigot;

import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.manager.ModuleManager;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannel;
import es.karmadev.locklogin.api.plugin.ServerHash;
import es.karmadev.locklogin.api.plugin.database.Driver;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.license.License;
import es.karmadev.locklogin.api.plugin.license.LicenseProvider;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.client.CPremiumDataStore;
import es.karmadev.locklogin.common.api.extension.CModuleManager;
import es.karmadev.locklogin.common.api.plugin.CPluginHash;
import es.karmadev.locklogin.common.api.plugin.file.CPluginConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.lang.InternalPack;
import es.karmadev.locklogin.common.api.plugin.service.backup.CLocalBackup;
import es.karmadev.locklogin.common.api.plugin.service.brute.CBruteForce;
import es.karmadev.locklogin.common.api.plugin.service.floodgate.CFloodGate;
import es.karmadev.locklogin.common.api.plugin.service.name.CNameProvider;
import es.karmadev.locklogin.common.api.protection.CPluginHasher;
import es.karmadev.locklogin.common.api.protection.type.SHA512Hash;
import es.karmadev.locklogin.common.api.runtime.CRuntime;
import es.karmadev.locklogin.common.api.server.CServerFactory;
import es.karmadev.locklogin.common.api.sql.CSQLDriver;
import es.karmadev.locklogin.common.api.user.CUserFactory;
import es.karmadev.locklogin.common.api.user.storage.account.CAccountFactory;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionFactory;
import es.karmadev.locklogin.common.api.web.license.CLicenseProvider;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.bukkit.reflection.BarMessage;
import ml.karmaconfigs.api.bukkit.reflection.TitleMessage;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LockLoginSpigot implements LockLogin, KarmaSource, NetworkServer {

    private final CSQLDriver driver;
    private final KarmaPlugin plugin;

    private final CModuleManager moduleManager = new CModuleManager();
    private final CRuntime runtime = new CRuntime(moduleManager);
    private CPluginNetwork network;
    private CPremiumDataStore premiumDataStore;
    private final CPluginHasher hasher;
    private final CPluginConfiguration configuration;
    private final InternalPack messages;

    private CAccountFactory default_account_factory;
    private CSessionFactory default_session_factory;
    private CUserFactory default_user_factory;
    private CServerFactory default_server_factory;

    private AccountFactory<? extends UserAccount> provider_account_factory = null;
    private SessionFactory<? extends UserSession> provider_session_factory = null;
    private UserFactory<? extends LocalNetworkClient> provider_user_factory = null;
    private ServerFactory<? extends NetworkServer> provider_server_factory = null;

    private final Map<String, PluginService> service_provider = new ConcurrentHashMap<>();
    private final CLicenseProvider license_provider;

    private License license;

    private final Instant startup = Instant.now();

    public LockLoginSpigot(final KarmaPlugin plugin, final CommandMap map) {
        this.plugin = plugin;

        Class<CurrentPlugin> instance = CurrentPlugin.class;
        try {
            Method initialize = instance.getDeclaredMethod("initialize", LockLogin.class);
            initialize.setAccessible(true);

            initialize.invoke(instance, this);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        configuration = new CPluginConfiguration();
        messages = new InternalPack();
        hasher = new CPluginHasher();
        license_provider = new CLicenseProvider();

        CLocalBackup backup_service = new CLocalBackup();
        CNameProvider name_service = new CNameProvider();

        registerService("name", name_service);
        registerService("backup", backup_service);
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");

            CFloodGate floodgate_service = new CFloodGate();
            registerService("floodgate", floodgate_service);
        } catch (ClassNotFoundException ex) {
            plugin.logger().scheduleLog(Level.INFO, "Ignoring FloodGate service compatibility");
            plugin.console().send("Failed to detect FloodGate API. FloodGate service will be disabled", Level.WARNING);
        }

        driver = new CSQLDriver();
        CurrentPlugin.updateState();

        SpigotCommandManager manager = new SpigotCommandManager(this, map);
        moduleManager.onCommandRegistered = manager;
        moduleManager.onCommandUnregistered = manager;
    }

    void installDriver() {
        driver.testDriver(Driver.SQLite);
        driver.connect();

        if (network == null) network = new CPluginNetwork(driver);
        if (premiumDataStore == null) premiumDataStore = new CPremiumDataStore(driver);
        if (default_account_factory == null) default_account_factory = new CAccountFactory(driver);
        if (default_session_factory == null) default_session_factory = new CSessionFactory(driver);
        if (default_user_factory == null) default_user_factory = new CUserFactory(driver);
        if (default_server_factory == null) default_server_factory = new CServerFactory(driver);

        CBruteForce brute_service = new CBruteForce(driver);
        registerService("bruteforce", brute_service);

        CurrentPlugin.updateState();
    }

    /**
     * Get the plugin data driver
     *
     * @return the plugin data driver
     */
    public CSQLDriver driver() {
        return driver;
    }

    /**
     * Get the LockLogin plugin instance
     *
     * @return the locklogin plugin
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public KarmaPlugin plugin() throws SecurityException {
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
    public CRuntime runtime() throws SecurityException {
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
        return moduleManager;
    }

    /**
     * Get the plugin premium data store
     *
     * @return the plugin premium store
     */
    @Override
    public PremiumDataStore premiumStore() {
        return premiumDataStore;
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY);
        Path data = getDataPath().resolve("cache").resolve("server.kf");
        KarmaMain persistent_hash = new KarmaMain(data);

        Element<?> hash = persistent_hash.get("hash");
        Element<?> creation = persistent_hash.get("time");

        if (hash.isElementNull() || creation.isElementNull() || !hash.getAsPrimitive().isString() || !creation.getAsPrimitive().isNumber()) {
            SHA512Hash sha = new SHA512Hash();
            String random = TokenGenerator.generateToken();

            HashResult result = sha.hash(random);
            hash = new KarmaPrimitive(new String(result.product().product()));
            creation = new KarmaPrimitive(Instant.now().toEpochMilli());

            persistent_hash.set("hash", hash);
            persistent_hash.set("time", creation);

            persistent_hash.save();
        }
        String hash_value = hash.getAsString();
        long hash_creation = creation.getAsLong();

        return CPluginHash.of(hash_value, hash_creation);
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
            plugin.logger().scheduleLog(Level.WARNING, "Tried to register duplicated service name {0}", name);
            throw new UnsupportedOperationException("Cannot register service " + name + " because it's already defined by another service");
        }
        Stream<PluginService> filtered_services = service_provider.values().stream().filter((registered) -> service.getClass().equals(registered.getClass()));
        if (filtered_services.findAny().isPresent()) {
            plugin.logger().scheduleLog(Level.WARNING, "Tried to registered duplicated service provider {0} under name {1}", service.getClass().getName(), name);
            throw new UnsupportedOperationException("Cannot register service " + name + " because it's already registered under other service name");
        }

        service_provider.put(name, service);
        String serviceClass = service.getClass().getName();
        if (service instanceof ServiceProvider) {
            ServiceProvider<?> provider = (ServiceProvider<?>) service;
            serviceClass = provider.getService().getName();
        }

        plugin.logger().scheduleLog(Level.INFO, "Registered service {0} for provider {1}", name, serviceClass);
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
        //if (service instanceof CLocalBackup) throw new UnsupportedOperationException("Cannot unregister plugin internal service: " + name);

        service_provider.remove(name);
        plugin.logger().scheduleLog(Level.INFO, "Unregistered service {0} on provider {1}", name, service.getClass().getName());
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

    /**
     * Karma source name
     *
     * @return the source name
     */
    @Override
    public String name() {
        return "LockLoginSpigot";
    }

    /**
     * Get the entity address
     *
     * @return the entity address
     */
    @Override
    public InetSocketAddress address() {
        return InetSocketAddress.createUnresolved("127.0.0.1", plugin.getServer().getPort());
    }

    /**
     * Get when the entity was created
     *
     * @return the entity creation date
     */
    @Override
    public Instant creation() {
        return startup;
    }

    /**
     * Get if this entity has the specified permission
     *
     * @param permission the permission
     * @return if the entity has the permission
     */
    @Override
    public boolean hasPermission(final PermissionObject permission) {
        return true;
    }

    /**
     * Karma source version
     *
     * @return the source version
     */
    @Override
    public String version() {
        return plugin.version();
    }

    /**
     * Karma source description
     *
     * @return the source description
     */
    @Override
    public String description() {
        return "LockLoginSpigot is the spigot version of LockLogin";
    }

    /**
     * Karma source authors
     *
     * @return the source authors
     */
    @Override
    public String[] authors() {
        return plugin.authors();
    }

    /**
     * Karma source update URL
     *
     * @return the source update URL
     */
    @Override
    public String updateURL() {
        return plugin.updateURL();
    }

    /**
     * Get the server id
     *
     * @return the server id
     */
    @Override
    public int id() {
        return 0;
    }

    /**
     * Update the server name
     *
     * @param name the server name
     */
    @Override
    public void setName(final String name) {
        throw new UnsupportedOperationException("Cannot define bukkit server name");
    }

    /**
     * Update the server address
     *
     * @param address the server new address
     */
    @Override
    public void setAddress(final InetSocketAddress address) {
        throw new UnsupportedOperationException("Cannot define bukkit server address");
    }

    /**
     * Get all the clients that are connected
     * in this server
     *
     * @return all the connected clients
     */
    @Override
    public Collection<NetworkClient> connected() {
        return network.getOnlinePlayers();
    }

    /**
     * Get all the offline clients that
     * are connected in this server
     *
     * @return all the offline clients
     */
    @Override
    public Collection<LocalNetworkClient> offlineClients() {
        return network.getPlayers().stream().filter((account) -> !account.online()).collect(Collectors.toList());
    }

    /**
     * Get the server packet queue
     *
     * @return the server packet queue
     */
    @Override
    public NetworkChannel channel() {
        return null;
    }

    /**
     * Send a message to the client
     *
     * @param message the message to send
     */
    @Override
    public void sendMessage(final String message) {
        plugin.console().send(message);
    }

    /**
     * Send an actionbar to the client
     *
     * @param actionbar the actionbar to send
     */
    @Override
    public void sendActionBar(final String actionbar) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            BarMessage bar = new BarMessage(online, actionbar);
            bar.send(false);
        }
    }

    /**
     * Send a title to the client
     *
     * @param title    the title
     * @param subtitle the subtitle
     * @param fadeIn   the title fade in time
     * @param showTime the title show time
     * @param fadeOut  the title fade out time
     */
    @Override
    public void sendTitle(final String title, final String subtitle, final int fadeIn, final int showTime, final int fadeOut) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            TitleMessage bar = new TitleMessage(online, title, subtitle);
            bar.send(fadeIn, showTime, fadeOut);
        }
    }
}
