package es.karmadev.locklogin.spigot;

import es.karmadev.api.core.KarmaAPI;
import es.karmadev.api.database.DatabaseManager;
import es.karmadev.api.database.model.JsonDatabase;
import es.karmadev.api.database.model.json.JsonConnection;
import es.karmadev.api.file.FileEncryptor;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.api.file.yaml.handler.YamlHandler;
import es.karmadev.api.kson.JsonInstance;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.kson.io.JsonReader;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.api.minecraft.text.component.Component;
import es.karmadev.api.minecraft.text.component.ComponentBuilder;
import es.karmadev.api.minecraft.text.component.title.Times;
import es.karmadev.api.spigot.reflection.packet.MessagePacket;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.api.version.Version;
import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.ModuleConverter;
import es.karmadev.locklogin.api.extension.module.ModuleManager;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.api.plugin.ServerHash;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.database.Database;
import es.karmadev.locklogin.api.plugin.file.language.LanguagePackManager;
import es.karmadev.locklogin.api.plugin.marketplace.MarketPlace;
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
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.client.CPremiumDataStore;
import es.karmadev.locklogin.common.api.database.sql.CSQLDriver;
import es.karmadev.locklogin.common.api.dependency.CPluginDependency;
import es.karmadev.locklogin.common.api.extension.CModuleManager;
import es.karmadev.locklogin.common.api.packet.COutPacket;
import es.karmadev.locklogin.common.api.plugin.CPluginHash;
import es.karmadev.locklogin.common.api.plugin.file.CPluginConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.lang.InternalPack;
import es.karmadev.locklogin.common.api.plugin.service.backup.CLocalBackup;
import es.karmadev.locklogin.common.api.plugin.service.brute.CBruteForce;
import es.karmadev.locklogin.common.api.plugin.service.floodgate.CFloodGate;
import es.karmadev.locklogin.common.api.plugin.service.mail.CMailService;
import es.karmadev.locklogin.common.api.plugin.service.name.CNameProvider;
import es.karmadev.locklogin.common.api.plugin.service.password.CPasswordProvider;
import es.karmadev.locklogin.common.api.protection.CPluginHasher;
import es.karmadev.locklogin.common.api.protection.totp.CTotpService;
import es.karmadev.locklogin.common.api.protection.type.SHA512Hash;
import es.karmadev.locklogin.common.api.runtime.CRuntime;
import es.karmadev.locklogin.common.api.server.CServerFactory;
import es.karmadev.locklogin.common.api.user.CUserFactory;
import es.karmadev.locklogin.common.api.user.auth.CProcessFactory;
import es.karmadev.locklogin.common.api.user.session.service.SessionStoreProvider;
import es.karmadev.locklogin.common.api.user.storage.account.CAccountFactory;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionFactory;
import es.karmadev.locklogin.common.plugin.internal.PluginPermissionManager;
import es.karmadev.locklogin.common.plugin.secure.totp.TotpGlobalHandler;
import es.karmadev.locklogin.common.plugin.web.CMarketPlace;
import es.karmadev.locklogin.spigot.command.module.SpigotCommandManager;
import es.karmadev.locklogin.spigot.permission.luckperms.LuckPermsPM;
import es.karmadev.locklogin.spigot.permission.vault.VaultPM;
import es.karmadev.locklogin.spigot.process.SpigotLoginProcess;
import es.karmadev.locklogin.spigot.process.SpigotPinProcess;
import es.karmadev.locklogin.spigot.process.SpigotRegisterProcess;
import es.karmadev.locklogin.spigot.process.SpigotTotpProcess;
import es.karmadev.locklogin.spigot.protocol.injector.ClientInjector;
import es.karmadev.locklogin.spigot.protocol.injector.NMSHelper;
import es.karmadev.locklogin.spigot.util.converter.SpigotModuleMaker;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LockLoginSpigot implements LockLogin, NetworkServer {

    private final CSQLDriver driver;
    private final SpigotPlugin plugin;

    private final CMarketPlace marketPlace = new CMarketPlace();
    private final CModuleManager moduleManager = new CModuleManager();
    private final CRuntime runtime = new CRuntime();
    private CPluginNetwork network;
    private CPremiumDataStore premiumDataStore;
    private final CPluginHasher hasher;
    private final CPluginConfiguration configuration;
    private final InternalPack messages;
    private final CProcessFactory process_factory;
    private final SpigotModuleMaker moduleMaker;
    @Getter
    private final TotpGlobalHandler totpHandler = new TotpGlobalHandler();

    @Getter
    private PluginPermissionManager<OfflinePlayer, String> permissionManager;
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
    private Instant postStartup;

    @Getter
    private final ClientInjector injector;

    public boolean boot = true;
    private final KeyPair pair;
    @Getter
    private PrivateKey sharedSecret;

    public LockLoginSpigot(final SpigotPlugin plugin, final CommandMap map) throws RuntimeException {
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

        plugin.logger().log(LogLevel.WARNING, "Preparing to inject dependencies. Please wait...");
        CPluginDependency.load();

        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
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
                            plugin.logger().log(LogLevel.INFO, "KarmaAPI detected successfully. Version {0}[{1}] of {2}[{3}] (required)", API_VERSION, PLUGIN_VERSION, platform, version);
                        } else {
                            plugin.logger().log(LogLevel.SEVERE, "Cannot load LockLogin as required dependency (KarmaAPI) is out of date ({0}). Yours: {1}", version, PLUGIN_VERSION);
                            boot = false;
                            break;
                        }
                    } else {
                        plugin.logger().log(LogLevel.SEVERE, "Cannot load LockLogin as required dependency (KarmaAPI) is not in the required build ({0}). Yours: {1}", platform, API_VERSION);
                        boot = false;
                        break;
                    }
                } else {
                    if (pluginManager.isPluginEnabled(name)) {
                        Plugin tmpPlugin = pluginManager.getPlugin(name);
                        if (tmpPlugin != null) {
                            Version pluginVersion = Version.parse(tmpPlugin.getDescription().getVersion());

                            if (pluginVersion.compareTo(version) < 0) {
                                plugin.logger().log(LogLevel.SEVERE, "Plugin dependency {0} was found but is out of date ({1} > {2}). LockLogin will still try to hook into its API, but there may be some errors", name, version, pluginVersion);
                            } else {
                                plugin.logger().log(LogLevel.INFO, "Plugin dependency {0} has been successfully hooked", name);
                                /*if (name.equalsIgnoreCase("Spartan")) {
                                    registerService("spartan", new SpartanService());
                                }*/
                            }
                        }
                    }
                }
            } else {
                //console().send("Injecting dependency {0}", Level.INFO, dependency.name());
                runtime.dependencyManager().append(dependency);
            }
        }

        configuration = new CPluginConfiguration(this);

        KeyPair pair = null;
        try {
            Path keyStore = workingDirectory().resolve("cache").resolve("keys.json");
            IvParameterSpec spec = new IvParameterSpec(configuration.secretKey().iv());

            FileEncryptor encryptor = new FileEncryptor(keyStore, configuration.secretKey().token());
            if (Files.exists(keyStore)) {
                encryptor.decrypt(spec);

                JsonObject data = JsonReader.read(PathUtilities.read(keyStore)).asObject();

                if (data.hasChild("public") && data.hasChild("private")) {
                    byte[] publicBytes = Base64.getDecoder().decode(data.getChild("public")
                            .asNative().getString());
                    byte[] privateBytes = Base64.getDecoder().decode(data.getChild("private")
                            .asNative().getString());

                    KeyFactory factory = KeyFactory.getInstance("RSA");

                    X509EncodedKeySpec publicKey = new X509EncodedKeySpec(publicBytes);
                    PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec(privateBytes);

                    PublicKey pub = factory.generatePublic(publicKey);
                    PrivateKey pri = factory.generatePrivate(privateKey);

                    pair = new KeyPair(pub, pri);
                } else {
                    logErr("Cannot initialize LockLogin because the key storage is invalid");
                    boot = false;
                }

                encryptor.encrypt(spec);
            } else {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);

                pair = generator.generateKeyPair();

                PathUtilities.createPath(keyStore);
                JsonObject object = JsonObject.newObject("", "");
                object.put("public", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
                object.put("private", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));

                String raw = object.toString(false);
                PathUtilities.write(keyStore, raw);

                encryptor.encrypt(spec);
            }
        } catch (Exception ex) {
            boot = false;
        }

        this.pair = pair;
        if (!boot) {
            driver = null;
            messages = null;
            hasher = null;
            process_factory = null;
            moduleMaker = null;
            injector = null;
            return; //We won't boot
        }

        messages = new InternalPack();
        hasher = new CPluginHasher();

        messages.setLang(configuration.language());

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
        }

        registerService("totp", new CTotpService());
        registerService("mailer", new CMailService());
        registerService("persistence", new SessionStoreProvider());

        driver = new CSQLDriver(configuration.database().driver());
        CurrentPlugin.updateState();

        SpigotCommandManager manager = new SpigotCommandManager(this, map);
        moduleManager.onCommandRegistered = manager;
        moduleManager.onCommandUnregistered = manager;

        process_factory = new CProcessFactory();
        process_factory.register(SpigotRegisterProcess.class);
        process_factory.register(SpigotLoginProcess.class);
        process_factory.register(SpigotPinProcess.class);
        process_factory.register(SpigotTotpProcess.class);

        SpigotRegisterProcess.setStatus(configuration.authSettings().register());
        SpigotLoginProcess.setStatus(configuration.authSettings().login());
        SpigotPinProcess.setStatus(configuration.authSettings().pin());
        SpigotTotpProcess.setStatus(configuration.authSettings().totp());

        moduleMaker = new SpigotModuleMaker();
        injector = new ClientInjector();

        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "ll:test", (s, player, bytes) ->
                logInfo("Received: {0}", Arrays.toString(bytes)));
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "ll:test");
        postStartup = Instant.now();

        if (pluginManager.isPluginEnabled("LuckPerms")) {
            permissionManager = new LuckPermsPM(plugin);
        } else if (pluginManager.isPluginEnabled("Vault")) {
            permissionManager = new VaultPM(plugin);
        }
    }

    void installDriver() {
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

        Path databaseData = plugin.workingDirectory().resolve("cache").resolve("database.json");

        Database database = configuration.database();
        if (Files.exists(databaseData)) {
            JsonInstance databaseCache = JsonReader.read(PathUtilities.read(databaseData));
            if (!databaseCache.isObjectType()) return;

            JsonObject object = databaseCache.asObject();
            boolean modifications = false;
            for (Table table : Table.values()) {
                String rawName = table.name();

                JsonObject tableObject = JsonObject.newObject("", table.name());
                if (object.hasChild(rawName) && object.getChild(rawName).isObjectType()) {
                    tableObject = object.getChild(rawName).asObject();
                }

                String tableValue = database.tableName(table);
                if (tableObject.hasChild("name") && tableObject.getChild("name").isNativeType() &&
                        tableObject.getChild("name").asNative().isString()) {
                    tableValue = tableObject.getChild("name").asNative().getString();
                }

                String tableName = database.tableName(table);
                if (!tableName.equals(tableValue)) {
                    logInfo("Detected outdated table name, renaming {0} to {1}", tableValue, tableName);

                    Connection connection = null;
                    Statement statement = null;
                    try {
                        connection = driver.retrieve();
                        statement = connection.createStatement();

                        statement.executeUpdate(QueryBuilder.createQuery().alter(table).rename(tableValue).build());
                        tableObject.put("name", tableName);
                        modifications = true;
                    } catch (SQLException ex) {
                        log(ex, "Failed to rename table {0} from {1} to {2}", table.name(), tableValue, tableName);
                        return;
                    } finally {
                        driver.close(connection, statement);
                    }
                }

                for (Row row : table.getUsableRows()) {
                    String columnValue = database.columnName(table, row);
                    if (tableObject.hasChild(row.name()) && tableObject.getChild(row.name()).isNativeType() &&
                            tableObject.getChild(row.name()).asNative().isString()) {
                        columnValue = tableObject.getChild(row.name()).asNative().getString();
                    }

                    String columnName = database.columnName(table, row);
                    if (!columnName.equals(columnValue)) {
                        logInfo("Detected outdated column name at {0}, renaming {1} to {2}", tableName, columnValue, columnName);

                        Connection connection = null;
                        Statement statement = null;
                        try {
                            connection = driver.retrieve();
                            DatabaseMetaData meta = connection.getMetaData();
                            ResultSet columns = meta.getColumns(null, null, tableName, columnValue);

                            statement = connection.createStatement();

                            if (!columns.next()) {
                                logErr("Column {0} not found at {1}, plugin cannot proceed", columnValue, tableName, columnName);
                                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                                return;
                            }

                            try {
                                columns.close();
                            } catch (SQLException ignored) {}

                            statement.executeUpdate(QueryBuilder.createQuery().alter(table).rename(row, columnValue).build());
                            tableObject.put(row.name(), columnName);
                            modifications = true;
                        } catch (SQLException ex) {
                            log(ex, "Failed to rename column {0} from {1} to {2} at {3}", row.name(), columnValue, columnName, tableName);
                            return;
                        } finally {
                            driver.close(connection, statement);
                        }
                    }
                }

                object.put(table.name(), tableObject);
            }

            if (modifications) {
                String raw = object.toString(false);
                PathUtilities.write(databaseData, raw);
            }
        } else {
            JsonObject object = JsonObject.newObject("", "");
            for (Table table : Table.values()) {
                String name = database.tableName(table);

                JsonObject tableObject = JsonObject.newObject("", table.name());
                tableObject.put("name", name);

                for (Row row : table.getUsableRows()) {
                    String rowName = database.columnName(table, row);
                    tableObject.put(row.name(), rowName);
                }

                object.put(table.name(), tableObject);
            }

            String raw = object.toString(false);
            PathUtilities.write(databaseData, raw);
        }
    }

    /**
     * Get the plugin communication keys
     *
     * @return the plugin keys
     */
    public KeyPair getCommunicationKeys() {
        return pair;
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
    public SpigotPlugin plugin() throws SecurityException {
        return plugin;
    }

    /**
     * Get the plugin marketplace
     *
     * @return the plugin marketplace
     */
    @Override
    public MarketPlace getMarketPlace() {
        return marketPlace;
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
        try {
            YamlFileHandler yaml = YamlHandler.load(spigot_yml.toPath());
            //KarmaYamlManager yaml = new KarmaYamlManager(spigot_yml);

            return yaml.getBoolean("settings.bungeecord", false);
        } catch (IOException ex) {
            plugin.logger().log(LogLevel.SEVERE, "Failed to retrieve bungeecord mode", ex);
            return false;
        }
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
        return plugin.workingDirectory();
    }

    /**
     * Get the plugin runtime
     *
     * @return the plugin runtime
     */
    @Override
    public CRuntime getRuntime() {
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
     * Get the plugin language pack manager
     *
     * @return the plugin language pack
     * manager
     */
    @Override
    public LanguagePackManager languagePackManager() {
        return messages;
    }

    /**
     * Get the plugin auth process factory
     *
     * @return the process factory
     */
    @Override
    public ProcessFactory getProcessFactory() {
        return process_factory;
    }

    /**
     * Get the module converter
     *
     * @return the module converter
     */
    @Override @SuppressWarnings("unchecked")
    public <T> ModuleConverter<T> getConverter() {
        return (ModuleConverter<T>) moduleMaker;
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
        Path data = plugin.workingDirectory().resolve("cache").resolve("server.kf");
        if (Files.exists(data)) {
            Pattern hashPattern = Pattern.compile("'hash' -> [\"'].*[\"']");
            Pattern timePattern = Pattern.compile("'time' -> [0-9]*");

            List<String> lines = PathUtilities.readAllLines(data);

            String hash = null;
            long time = Long.MIN_VALUE;
            for (String line : lines) {
                Matcher hashMatcher = hashPattern.matcher(line);
                Matcher timeMatcher = timePattern.matcher(line);

                int end = line.length() - 1;
                if (hashMatcher.find()) {
                    int being = hashMatcher.start() + "'hash' -> '".length();
                    hash = line.substring(being, end);
                    continue;
                }

                if (timeMatcher.find()) {
                    int being = timeMatcher.start() + "'time' -> ".length();
                    String value = line.substring(being);

                    try {
                        time = Long.parseLong(value);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (hash == null || time == Long.MIN_VALUE) {
                SHA512Hash sha = new SHA512Hash();
                String random = StringUtils.generateString(32);

                HashResult result = sha.hash(random);
                hash = new String(result.product().product());
                time = Instant.now().toEpochMilli();
            }

            final String hash_value = hash;
            final long hash_creation = time;

            JsonDatabase database = (JsonDatabase) DatabaseManager.getEngine("json").orElse(new JsonDatabase());
            JsonConnection connection = database.grabConnection("cache/info.json");

            JsonConnection server = connection.createTable("server");
            server.set("hash", hash_value);
            server.set("time", hash_creation);
            server.setPrettySave(true);

            if (server.save()) {
                plugin.logger().log(LogLevel.DEBUG, "Successfully migrated from legacy KarmaMain to JsonDatabase");
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
        provider_account_factory = factory;
    }

    /**
     * Define the plugin session factory
     *
     * @param factory the account session factory
     */
    @Override
    public void setSessionFactory(final SessionFactory<UserSession> factory) {
        provider_session_factory = factory;
    }

    /**
     * Define the plugin user factory
     *
     * @param factory the user factory
     */
    @Override
    public void setUserFactory(final UserFactory<LocalNetworkClient> factory) {
        provider_user_factory = factory;
    }

    /**
     * Define the plugin server factory
     *
     * @param factory the server factory
     */
    @Override
    public void setServerFactory(final ServerFactory<NetworkServer> factory) {
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
        plugin.logger().send(LogLevel.INFO, message, replaces);
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void warn(final String message, final Object... replaces) {
        plugin.logger().send(LogLevel.WARNING, message, replaces);
    }

    /**
     * Print a message
     *
     * @param message  the message to print
     * @param replaces the message replaces
     */
    @Override
    public void err(final String message, final Object... replaces) {
        plugin.logger().send(LogLevel.ERROR, message, replaces);
    }

    /**
     * Log something that is just informative
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logInfo(final String message, final Object... replaces) {
        plugin.logger().log(LogLevel.INFO, message, replaces);
    }

    /**
     * Log something that is important
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logWarn(final String message, final Object... replaces) {
        plugin.logger().log(LogLevel.WARNING, message, replaces);
    }

    /**
     * Log something that went wrong
     *
     * @param message  the log message
     * @param replaces the message replaces
     */
    @Override
    public void logErr(final String message, final Object... replaces) {
        plugin.logger().log(LogLevel.ERROR, message, replaces);
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
        plugin.logger().log(error, message, replaces);
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
    public Collection<NetworkClient> getConnected() {
        return network.getOnlinePlayers();
    }

    /**
     * Get all the offline clients that
     * are connected in this server
     *
     * @return all the offline clients
     */
    @Override
    public Collection<LocalNetworkClient> getOfflineClients() {
        return network.getPlayers().stream().filter((account) -> !account.online()).collect(Collectors.toList());
    }

    /**
     * Get a channel name
     *
     * @param name the channel name
     * @return the channel name
     */
    @Override
    public NetworkChannel getChannel(final String name) {
        return null;
    }

    /**
     * Register a network channel
     *
     * @param channel the channel
     */
    @Override
    public void registerChannel(final NetworkChannel channel) {

    }

    /**
     * When a packet is received
     *
     * @param packet the packet
     */
    public void onReceive(final IncomingPacket packet) {
        final String identifier = packet.getSequence("identifier");

        final COutPacket out = new COutPacket(packet.getType());
        out.addProperty("replying", packet.id());

        switch (packet.getType()) {
            case HELLO:
                byte[] publicKey = pair.getPublic().getEncoded();
                String sharedKey = Base64.getEncoder().encodeToString(publicKey);

                out.addProperty("key", sharedKey);
                break;
            case CHANNEL_INIT:
                if (this.sharedSecret == null) {
                    byte[] sharedSecret = Base64.getDecoder().decode(packet.getSequence("key"));
                    try {
                        KeyFactory factory = KeyFactory.getInstance("RSA");
                        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(sharedSecret);

                        this.sharedSecret = factory.generatePrivate(spec);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException ignored) {
                    }
                }
                break;
            case CHANNEL_CLOSE:
                this.sharedSecret = null;
                logInfo("Received channel close message from proxy");
                break;
            case CONNECTION_INIT:
                out.addProperty("hello", "world!");
                break;
        }

        Object[] payloads = NMSHelper.createPayloads(identifier, out);

        Player bridge = null;
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player != null && player.isOnline()) {
                bridge = player;
                break;
            }
        }

        for (Object payload : payloads) {
            try {
                NMSHelper.sendPacket(bridge, payload);
            } catch (InvocationTargetException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * When a packet is sent
     *
     * @param packet the packet to send
     */
    public void onSend(final OutgoingPacket packet) {

    }

    /**
     * Send a message to the client
     *
     * @param message the message to send
     */
    @Override
    public void sendMessage(final String message) {
        plugin.logger().send(message);
    }

    /**
     * Send an actionbar to the client
     *
     * @param actionbar the actionbar to send
     */
    @Override
    public void sendActionBar(final String actionbar) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            Component[] components = ComponentBuilder.builder()
                    .actionbar(Colorize.colorize(actionbar)).build();

            MessagePacket packet = new MessagePacket(components);
            packet.send(online);
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
            ComponentBuilder builder = Component.builder();
            if (title != null) {
                builder.title(Colorize.colorize(title));
            }
            if (subtitle != null) {
                builder.subtitle(Colorize.colorize(subtitle));
            }

            builder.fadeIn(Times.exact(fadeIn))
                    .show(Times.exact(showTime))
                    .fadeOut(Times.exact(fadeOut));

            MessagePacket packet = new MessagePacket(builder.build());
            packet.send(online);
        }
    }

    @Override
    public InputStream loadResource(final String s) {
        return LockLoginSpigot.class.getResourceAsStream("/" + s);
    }
}
