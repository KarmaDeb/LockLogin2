package es.karmadev.locklogin.bungee;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import es.karmaconfigs.api.common.karma.file.KarmaMain;
import es.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import es.karmaconfigs.api.common.karma.file.element.types.Element;
import es.karmadev.api.bungee.core.KarmaPlugin;
import es.karmadev.api.core.KarmaAPI;
import es.karmadev.api.database.DatabaseManager;
import es.karmadev.api.database.model.JsonDatabase;
import es.karmadev.api.database.model.json.JsonConnection;
import es.karmadev.api.file.FileEncryptor;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.api.version.Version;
import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.ModuleConverter;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.ModuleManager;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannel;
import es.karmadev.locklogin.api.plugin.ServerHash;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyType;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.auth.ProcessFactory;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.bungee.packet.PacketDataHandler;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.client.CPremiumDataStore;
import es.karmadev.locklogin.common.api.dependency.CPluginDependency;
import es.karmadev.locklogin.common.api.extension.CModuleManager;
import es.karmadev.locklogin.common.api.plugin.CPluginHash;
import es.karmadev.locklogin.common.api.plugin.file.CPluginConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.lang.InternalPack;
import es.karmadev.locklogin.common.api.plugin.service.SpartanService;
import es.karmadev.locklogin.common.api.plugin.service.backup.CLocalBackup;
import es.karmadev.locklogin.common.api.plugin.service.brute.CBruteForce;
import es.karmadev.locklogin.common.api.plugin.service.floodgate.CFloodGate;
import es.karmadev.locklogin.common.api.plugin.service.name.CNameProvider;
import es.karmadev.locklogin.common.api.plugin.service.password.CPasswordProvider;
import es.karmadev.locklogin.common.api.protection.CPluginHasher;
import es.karmadev.locklogin.common.api.protection.type.SHA512Hash;
import es.karmadev.locklogin.common.api.runtime.SubmissiveRuntime;
import es.karmadev.locklogin.common.api.server.CServerFactory;
import es.karmadev.locklogin.common.api.database.sql.CSQLDriver;
import es.karmadev.locklogin.common.api.user.CUserFactory;
import es.karmadev.locklogin.common.api.user.auth.CProcessFactory;
import es.karmadev.locklogin.common.api.user.storage.account.CAccountFactory;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionFactory;
import es.karmadev.locklogin.common.util.Task;
import lombok.Getter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LockLoginBungee implements LockLogin, NetworkServer {

    private final CSQLDriver driver;
    private final BungeePlugin plugin;

    private final CModuleManager moduleManager = new CModuleManager();
    private final SubmissiveRuntime runtime = new SubmissiveRuntime(moduleManager);
    private CPluginNetwork network;
    private CPremiumDataStore premiumDataStore;
    private final CPluginHasher hasher;
    private final CPluginConfiguration configuration;
    private final InternalPack messages;
    private final CProcessFactory process_factory;
    //private final SpigotModuleMaker moduleMaker;

    private CAccountFactory default_account_factory;
    private CSessionFactory default_session_factory;
    private CUserFactory default_user_factory;
    private CServerFactory default_server_factory;

    private AccountFactory<? extends UserAccount> provider_account_factory = null;
    private SessionFactory<? extends UserSession> provider_session_factory = null;
    private UserFactory<? extends LocalNetworkClient> provider_user_factory = null;
    private ServerFactory<? extends NetworkServer> provider_server_factory = null;

    final Map<String, PluginService> service_provider = new ConcurrentHashMap<>();

    @Getter
    private final Instant startup = Instant.now();
    @Getter
    //private final ClientInjector injector;

    public boolean boot = true;
    private final KeyPair pair;
    @Getter
    private final PrivateKey sharedSecret;

    public LockLoginBungee(final BungeePlugin plugin) throws RuntimeException {
        this.plugin = plugin;

        Class<CurrentPlugin> instance = CurrentPlugin.class;
        try {
            Method initialize = instance.getDeclaredMethod("initialize", LockLogin.class);
            initialize.setAccessible(true);

            initialize.invoke(instance, this);
        } catch (Throwable ex) {
            plugin.logger().log(LogLevel.SEVERE, "Failed to initialize LockLogin", ex);
            throw new RuntimeException("Couldn't initialize LockLogin");
        }

        plugin.logger().send(LogLevel.WARNING, "Preparing to inject dependencies. Please wait...");
        CPluginDependency.load();

        PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();
        for (LockLoginDependency dependency : CPluginDependency.getAll()) {
            if (dependency.type().equals(DependencyType.PLUGIN)) {
                String name = dependency.name();
                Version version = Version.parse(dependency.version().plugin());

                if (name.equalsIgnoreCase("KarmaAPI")) {
                    Version platform = Version.parse(dependency.version().project());

                    Version API_VERSION = Version.parse(KarmaAPI.VERSION);
                    Version PLUGIN_VERSION = Version.parse(KarmaAPI.BUILD);

                    if (API_VERSION.equals(platform)) {
                        if (API_VERSION.compareTo(version) >= 1) {
                            plugin.logger().send(LogLevel.INFO, "KarmaAPI detected successfully. Version {0}[{1}] of {2}[{3}] (required)", API_VERSION, PLUGIN_VERSION, platform, version);
                        } else {
                            plugin.logger().send(LogLevel.SEVERE, "Cannot load LockLogin as required dependency (KarmaAPI) is out of date ({0}). Yours: {1}", version, PLUGIN_VERSION);
                            boot = false;
                            break;
                        }
                    } else {
                        plugin.logger().send(LogLevel.SEVERE, "Cannot load LockLogin as required dependency (KarmaAPI) is not in the required build ({0}). Yours: {1}", platform, API_VERSION);
                        boot = false;
                        break;
                    }
                } else {
                    Plugin tmpPlugin = pluginManager.getPlugin(name);
                    if (tmpPlugin != null) {
                        Version pluginVersion = Version.parse(tmpPlugin.getDescription().getVersion());

                        if (pluginVersion.compareTo(version) < 0) {
                            plugin.logger().send(LogLevel.SEVERE, "Plugin dependency {0} was found but is out of date ({1} > {2}). LockLogin will still try to hook into its API, but there may be some errors", name, version, pluginVersion);
                        } else {
                            plugin.logger().send(LogLevel.INFO, "Plugin dependency {0} has been successfully hooked", name);
                            if (name.equalsIgnoreCase("Spartan")) {
                                registerService("spartan", new SpartanService());
                            }
                        }
                    }
                }
            } else {
                //console().send("Injecting dependency {0}", Level.INFO, dependency.name());
                runtime.dependencyManager().append(dependency);
            }
        }

        configuration = new CPluginConfiguration();

        KeyPair pair = null;
        PrivateKey sharedSecret = null;
        try {
            Path keyStore = workingDirectory().resolve("cache").resolve("keys.json");
            IvParameterSpec spec = new IvParameterSpec(configuration.secretKey().iv());

            FileEncryptor encryptor = new FileEncryptor(keyStore, configuration.secretKey().token());
            if (Files.exists(keyStore)) {
                encryptor.decrypt(spec);

                Gson gson = new GsonBuilder().create();
                JsonObject data = gson.fromJson(PathUtilities.read(keyStore), JsonObject.class);

                if (data.has("public") && data.has("private") && data.has("shared")) {
                    byte[] publicBytes = Base64.getDecoder().decode(data.get("public").getAsString());
                    byte[] privateBytes = Base64.getDecoder().decode(data.get("private").getAsString());
                    byte[] sharedBytes = Base64.getDecoder().decode(data.get("shared").getAsString());

                    KeyFactory factory = KeyFactory.getInstance("RSA");

                    X509EncodedKeySpec publicKey = new X509EncodedKeySpec(publicBytes);
                    PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec(privateBytes);
                    PKCS8EncodedKeySpec sharedKey = new PKCS8EncodedKeySpec(sharedBytes);

                    PublicKey pub = factory.generatePublic(publicKey);
                    PrivateKey pri = factory.generatePrivate(privateKey);
                    sharedSecret = factory.generatePrivate(sharedKey);

                    pair = new KeyPair(pub, pri);
                } else {
                    err("Cannot initialize LockLogin because the key storage is invalid");
                    boot = false;
                }

                encryptor.encrypt(spec);
            } else {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                KeyPairGenerator sharedGenerator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                sharedGenerator.initialize(2048);

                KeyPair sharedPair = sharedGenerator.generateKeyPair();
                pair = generator.generateKeyPair();
                sharedSecret = sharedPair.getPrivate();

                PathUtilities.createPath(keyStore);
                JsonObject object = new JsonObject();
                object.addProperty("public", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
                object.addProperty("private", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
                object.addProperty("shared", Base64.getEncoder().encodeToString(sharedSecret.getEncoded()));

                String raw = new GsonBuilder().create().toJson(object);
                PathUtilities.write(keyStore, raw);

                encryptor.encrypt(spec);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            boot = false;
        }

        /*if (boot) {
            Driver dr = configuration.database().driver();
            if (dr.isLocal()) {
                err("LockLogin won't start, as this is BungeeCord, LockLogin needs a SQL server to work properly");
                boot = false;
            }
        }*/

        this.pair = pair;
        this.sharedSecret = sharedSecret;
        if (!boot) {
            driver = null;
            messages = null;
            hasher = null;
            process_factory = null;
            /*moduleMaker = null;
            injector = null;*/
            return; //We won't boot
        }

        runtime.becomeCRuntime();
        messages = new InternalPack();
        hasher = new CPluginHasher();

        CLocalBackup backup_service = new CLocalBackup();
        CNameProvider name_service = new CNameProvider();
        CPasswordProvider password_service = new CPasswordProvider();

        registerService("name", name_service);
        registerService("backup", backup_service);
        registerService("password", password_service);
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");

            CFloodGate floodgate_service = new CFloodGate();
            registerService("floodgate", floodgate_service);
        } catch (ClassNotFoundException ex) {
            plugin.logger().log(LogLevel.INFO, "Ignoring FloodGate service compatibility");
            plugin.logger().send(LogLevel.WARNING, "Failed to detect FloodGate API. FloodGate service will be disabled");
        }

        driver = new CSQLDriver(configuration.database().driver());
        CurrentPlugin.updateState();

        BungeeCommandManager manager = new BungeeCommandManager();
        moduleManager.onCommandRegistered = manager;
        moduleManager.onCommandUnregistered = manager;

        process_factory = new CProcessFactory();
        /*process_factory.register(SpigotAccountProcess.class);
        process_factory.register(SpigotPinProcess.class);

        SpigotAccountProcess.setStatus(configuration.enableAuthentication());
        SpigotPinProcess.setStatus(configuration.enablePin());

        moduleMaker = new SpigotModuleMaker();
        injector = new ClientInjector();*/

        plugin.getProxy().registerChannel("ll:test");
    }

    void installDriver() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY, LockLoginBungee.class, "installDriver()");
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
     * Get the plugin communication keys
     *
     * @return the plugin keys
     */
    public KeyPair getCommunicationKeys() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY, LockLoginBungee.class, "getCommunicationKeys");
        return pair;
    }

    /**
     * Get the plugin data driver
     *
     * @return the plugin data driver
     */
    public CSQLDriver driver() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "driver()");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "plugin()");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "bungeeMode()");
        return true;
    }

    /**
     * Get if the plugin is running in
     * online mode
     *
     * @return if the server is online mode
     */
    @Override
    public boolean onlineMode() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "onlineMode()");
        return plugin.getProxy().getConfig().isOnlineMode();
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY, LockLoginBungee.class, "load(String)");

        File pluginFile = plugin.runtime().getFile().toFile();
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "workingDirectory()");
        return plugin.workingDirectory();
    }

    /**
     * Get the plugin runtime
     *
     * @return the plugin runtime
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public SubmissiveRuntime getRuntime() throws SecurityException {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "getRuntime()");
        return runtime;
    }

    /**
     * Get the plugin network
     *
     * @return the plugin network
     */
    @Override
    public PluginNetwork network() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "network()");
        return network;
    }

    /**
     * Get the plugin hasher
     *
     * @return the plugin hasher
     */
    @Override
    public LockLoginHasher hasher() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "hasher()");
        return hasher;
    }

    /**
     * Get the plugin configuration
     *
     * @return the plugin configuration
     */
    @Override
    public Configuration configuration() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "configuration()");
        return configuration;
    }

    /**
     * Get the plugin messages
     *
     * @return the plugin messages
     */
    @Override
    public Messages messages() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "messages()");
        return messages.getMessenger();
    }

    /**
     * Get the plugin auth process factory
     *
     * @return the process factory
     */
    @Override
    public ProcessFactory getProcessFactory() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "getProcessFactory()");
        return process_factory;
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
    @Override @SuppressWarnings("unchecked")
    public AccountFactory<? extends UserAccount> getAccountFactory(final boolean original) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "getAccountFactory(boolean)");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "getSessionFactory(boolean)");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "getUserFactory(boolean)");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "getServerFactory(boolean)");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "accountManager()");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "getService(String)");
        return service_provider.getOrDefault(name, null);
    }

    /**
     * Get the plugin module manager
     *
     * @return the plugin module manager
     */
    @Override
    public ModuleManager moduleManager() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "moduleManager()");
        return moduleManager;
    }

    /**
     * Get the plugin premium data store
     *
     * @return the plugin premium store
     */
    @Override
    public PremiumDataStore premiumStore() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "premiumStore()");
        return premiumDataStore;
    }

    /**
     * Get the current server hash
     *
     * @return the server hash
     * @throws SecurityException if tried to be accessed from any
     *                           external source that's not the self plugin
     */
    @Override @SuppressWarnings("deprecation")
    public ServerHash server() throws SecurityException {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY, LockLoginBungee.class, "server()");

        Path data = plugin.workingDirectory().resolve("cache").resolve("server.kf");
        if (Files.exists(data)) {
            KarmaMain persistent_hash = new KarmaMain(data);

            Element<?> hash = persistent_hash.get("hash");
            Element<?> creation = persistent_hash.get("time");

            if (hash.isElementNull() || creation.isElementNull() || !hash.getAsPrimitive().isString() || !creation.getAsPrimitive().isNumber()) {
                SHA512Hash sha = new SHA512Hash();
                String random = StringUtils.generateString(32);

                HashResult result = sha.hash(random);
                hash = new KarmaPrimitive(new String(result.product().product()));
                creation = new KarmaPrimitive(Instant.now().toEpochMilli());

                persistent_hash.set("hash", hash);
                persistent_hash.set("time", creation);

                persistent_hash.save();
            }
            String hash_value = hash.getAsString();
            long hash_creation = creation.getAsLong();

            JsonDatabase database = (JsonDatabase) DatabaseManager.getEngine("json").orElse(new JsonDatabase());
            JsonConnection connection = database.grabConnection("cache/info.json");

            JsonConnection server = connection.createTable("server");
            server.set("hash", hash_value);
            server.set("time", hash_creation);
            server.setPrettySave(true);

            if (server.save()) {
                plugin.logger().send(LogLevel.DEBUG, "Successfully migrated from legacy KarmaMain to JsonDatabase");
                PathUtilities.destroy(data);
            }
        }

        JsonDatabase database = (JsonDatabase) DatabaseManager.getEngine("json").orElse(new JsonDatabase());
        JsonConnection connection = database.grabConnection("cache/info.json");

        JsonConnection server = connection.createTable("server");
        if (!server.isSet("hash") || !server.isSet("time")) {
            SHA512Hash sha = new SHA512Hash();
            String random = StringUtils.generateString(32);

            HashResult result = sha.hash(random);
            String hash_value = new String(result.product().product());
            long hash_creation = Instant.now().toEpochMilli();

            server.set("hash", hash_value);
            server.set("time", hash_creation);
            server.setPrettySave(true);

            server.save();
        }

        return CPluginHash.of(server.getString("hash"), server.getNumber("time").longValue());
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "registerService(String, PluginService)");

        if (service_provider.containsKey(name)) {
            plugin.logger().log(LogLevel.WARNING, "Tried to register duplicated service name {0}", name);
            throw new UnsupportedOperationException("Cannot register service " + name + " because it's already defined by another service");
        }

        Stream<PluginService> filtered_services = service_provider.values().stream().filter((registered) -> service.getClass().equals(registered.getClass()));
        if (filtered_services.findAny().isPresent()) {
            plugin.logger().log(LogLevel.WARNING, "Tried to registered duplicated service provider {0} under name {1}", service.getClass().getName(), name);
            throw new UnsupportedOperationException("Cannot register service " + name + " because it's already registered under other service name");
        }

        service_provider.put(name, service);
        String serviceClass = service.getClass().getName();
        if (service instanceof ServiceProvider) {
            ServiceProvider<?> provider = (ServiceProvider<?>) service;
            serviceClass = provider.getService().getName();
        }

        plugin.logger().log(LogLevel.INFO, "Registered service {0} for provider {1}", name, serviceClass);
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "unregisterService(String)");

        PluginService service = service_provider.getOrDefault(name, null);
        if (service == null) throw new NullPointerException("Cannot unregister service " + name + " because it does not exist");
        //if (service instanceof CLocalBackup) throw new UnsupportedOperationException("Cannot unregister plugin internal service: " + name);

        service_provider.remove(name);
        plugin.logger().log(LogLevel.INFO, "Unregistered service {0} on provider {1}", name, service.getClass().getName());
    }

    /**
     * Define the plugin account factory
     *
     * @param factory the account factory
     */
    @Override
    public void setAccountFactory(final AccountFactory<UserAccount> factory) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "setAccountFactory(AccountFactory)");
        provider_account_factory = factory;
    }

    /**
     * Define the plugin session factory
     *
     * @param factory the account session factory
     */
    @Override
    public void setSessionFactory(final SessionFactory<UserSession> factory) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "setSessionFactory(SessionFactory)");
        provider_session_factory = factory;
    }

    /**
     * Define the plugin user factory
     *
     * @param factory the user factory
     */
    @Override
    public void setUserFactory(final UserFactory<LocalNetworkClient> factory) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "setUserFactory(UserFactory)");
        provider_user_factory = factory;
    }

    /**
     * Define the plugin server factory
     *
     * @param factory the server factory
     */
    @Override
    public void setServerFactory(final ServerFactory<NetworkServer> factory) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "setServerFactory(ServerFactory)");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "info(String, Object[])");
        Path caller = runtime.caller();
        Module module = moduleManager.loader().getModule(caller);

        if (module != null) {
            plugin.logger().send(LogLevel.INFO, "({0}): {1}", module.getName(), StringUtils.format(message, replaces));
        } else {
            plugin.logger().send(LogLevel.INFO, message, replaces);
        }
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void warn(final String message, final Object... replaces) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "warn(String, Object[])");
        Path caller = runtime.caller();
        Module module = moduleManager.loader().getModule(caller);

        if (module != null) {
            plugin.logger().send(LogLevel.WARNING, "({0}): {1}", module.getName(), StringUtils.format(message, replaces));
        } else {
            plugin.logger().send(LogLevel.WARNING, message, replaces);
        }
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void err(final String message, final Object... replaces) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "err(String, Object[])");
        Path caller = runtime.caller();
        Module module = moduleManager.loader().getModule(caller);

        if (module != null) {
            plugin.logger().send(LogLevel.ERROR, "({0}): {1}", module.getName(), StringUtils.format(message, replaces));
        } else {
            plugin.logger().send(LogLevel.ERROR, message, replaces);
        }
    }

    /**
     * Log something that is just informative
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logInfo(final String message, final Object... replaces) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "logInfo(String, Object[])");
        Path caller = runtime.caller();
        Module module = moduleManager.loader().getModule(caller);

        if (module != null) {
            plugin.logger().log(LogLevel.INFO, "({0}): {1}", module.getName(), StringUtils.format(message, replaces));
        } else {
            plugin.logger().log(LogLevel.INFO, message, replaces);
        }
    }

    /**
     * Log something that is important
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logWarn(final String message, final Object... replaces) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "logWarn(String, Object[])");
        Path caller = runtime.caller();
        Module module = moduleManager.loader().getModule(caller);

        if (module != null) {
            plugin.logger().log(LogLevel.WARNING, "({0}): {1}", module.getName(), StringUtils.format(message, replaces));
        } else {
            plugin.logger().log(LogLevel.WARNING, message, replaces);
        }
    }

    /**
     * Log something that went wrong
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logErr(final String message, final Object... replaces) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "logErr(String, Object[])");
        Path caller = runtime.caller();
        Module module = moduleManager.loader().getModule(caller);

        if (module != null) {
            plugin.logger().log(LogLevel.ERROR, "({0}): {1}", module.getName(), StringUtils.format(message, replaces));
        } else {
            plugin.logger().log(LogLevel.ERROR, message, replaces);
        }
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "log(Throwable, String, Object[])");
        Path caller = runtime.caller();
        Module module = moduleManager.loader().getModule(caller);

        if (module != null) {
            plugin.logger().log(error, "({0}): {1}", module.getName(), StringUtils.format(message, replaces));
        } else {
            plugin.logger().log(error, message, replaces);
        }
    }

    /**
     * Karma source name
     *
     * @return the source name
     */
    @Override
    public String name() {
        return "LockLoginBungee";
    }

    /**
     * Get the entity address
     *
     * @return the entity address
     */
    @Override
    public InetSocketAddress address() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "address()");
        return InetSocketAddress.createUnresolved("127.0.0.1", 25565);
    }

    /**
     * Get when the entity was created
     *
     * @return the entity creation date
     */
    @Override
    public Instant creation() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "creation()");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "connected()");
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "offlineClients()");
        return network.getPlayers().stream().filter((account) -> !account.online()).collect(Collectors.toList());
    }

    /**
     * Get the server packet queue
     *
     * @return the server packet queue
     */
    @Override
    public NetworkChannel channel() {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "channel()");
        return null;
    }

    /**
     * When a packet is received
     *
     * @param packet the packet
     */
    @Override
    public void onReceive(final IncomingPacket packet) {
        DataType type = packet.getType();
        switch (type) {
            case HELLO:
                byte[] keyBytes = Base64.getDecoder().decode(packet.getSequence("key"));
                try {
                    KeyFactory factory = KeyFactory.getInstance("RSA");
                    EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
                    PublicKey serverKey = factory.generatePublic(keySpec);

                    ServerInfo info = ProxyServer.getInstance().getServerInfo(packet.getSequence("server"));
                    PacketDataHandler.assignKey(info, serverKey);

                    //info("Stored server {0} key", info.getName());
                } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                    ex.printStackTrace();
                }
                break;
            case CONNECTION_INIT:
                break;
        }

        Task<IncomingPacket> taskExecutor = PacketDataHandler.getTask(packet);
        if (taskExecutor != null) taskExecutor.apply(packet);
    }

    /**
     * When a packet is sent
     *
     * @param packet the packet to send
     */
    @Override
    public void onSend(final OutgoingPacket packet) {

    }

    /**
     * Send a message to the client
     *
     * @param message the message to send
     */
    @Override
    public void sendMessage(final String message) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "sendMessage(String)");
        plugin.logger().send(message);
    }

    /**
     * Send an actionbar to the client
     *
     * @param actionbar the actionbar to send
     */
    @Override
    public void sendActionBar(final String actionbar) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "sendActionBar(String)");

        for (ProxiedPlayer online : ProxyServer.getInstance().getPlayers()) {
            online.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ColorComponent.parse(actionbar)));
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
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "sendTitle(String, String, int, int, int)");

        for (ProxiedPlayer online : ProxyServer.getInstance().getPlayers()) {
            Title bungeeTitle = ProxyServer.getInstance().createTitle();
            bungeeTitle.title(TextComponent.fromLegacyText(ColorComponent.parse(title)));
            bungeeTitle.subTitle(TextComponent.fromLegacyText(ColorComponent.parse(subtitle)));
            bungeeTitle.fadeIn(fadeIn);
            bungeeTitle.stay(showTime);
            bungeeTitle.fadeOut(fadeOut);

            bungeeTitle.send(online);
            //online.sendTitle(bungeeTitle);
        }
    }

    @Override
    public InputStream loadResource(final String s) {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginBungee.class, "loadResource(String)");
        return LockLoginBungee.class.getResourceAsStream("/" + s);
    }
}
