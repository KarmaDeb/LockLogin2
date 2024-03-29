package es.karmadev.locklogin.spigot;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.minecraft.MinecraftVersion;
import es.karmadev.api.schedule.runner.TaskRunner;
import es.karmadev.api.schedule.runner.async.AsyncTaskExecutor;
import es.karmadev.api.schedule.runner.event.TaskEvent;
import es.karmadev.api.spigot.core.KarmaPlugin;
import es.karmadev.api.spigot.server.SpigotServer;
import es.karmadev.api.version.BuildStatus;
import es.karmadev.api.version.Version;
import es.karmadev.api.version.checker.VersionChecker;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.CacheAble;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.UpdaterSection;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.common.CLockLogin;
import es.karmadev.locklogin.common.api.client.CLocalClient;
import es.karmadev.locklogin.common.plugin.internal.PluginPermissionManager;
import es.karmadev.locklogin.common.plugin.secure.logger.JavaLogger;
import es.karmadev.locklogin.common.plugin.secure.logger.Log4Logger;
import es.karmadev.locklogin.common.plugin.web.CMarketPlace;
import es.karmadev.locklogin.common.plugin.web.License;
import es.karmadev.locklogin.common.util.LockLoginJson;
import es.karmadev.locklogin.spigot.api.NMSPayload;
import es.karmadev.locklogin.spigot.command.helper.CommandHelper;
import es.karmadev.locklogin.spigot.event.*;
import es.karmadev.locklogin.spigot.event.helper.EventHelper;
import es.karmadev.locklogin.spigot.event.client.PlayerVersusHandler;
import es.karmadev.locklogin.spigot.event.window.InterfaceIOEvent;
import es.karmadev.locklogin.spigot.process.SpigotPinProcess;
import es.karmadev.locklogin.spigot.protocol.BungeeListener;
import es.karmadev.locklogin.spigot.protocol.ProtocolAssistant;
import es.karmadev.locklogin.spigot.util.storage.SpawnLocationStorage;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class SpigotPlugin extends KarmaPlugin {

    @Getter
    LockLoginSpigot spigot;

    @Getter
    private final CommandHelper commandHelper = new CommandHelper(getCommandMap());
    @Getter
    private VersionChecker checker;

    @Getter
    private ChatHandler chatHandler;
    @Getter
    private JoinHandler joinHandler;
    @Getter
    private MovementHandler movementHandler;
    @Getter
    private QuitHandler quitHandler;
    @Getter
    private IterationHandler iterationHandler;
    @Getter
    private PlayerVersusHandler playerVersusHandler;

    @Getter
    private InterfaceIOEvent UI_CloseOpenHandler; //Internal usage, we don't really care the name it has

    private final CLockLogin lockLogin;

    public SpigotPlugin() throws NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException {
        super(false);
        Field commandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        commandMap.setAccessible(true);
        CommandMap map = (CommandMap) commandMap.get(Bukkit.getServer());

        spigot = new LockLoginSpigot(this, map);
        this.lockLogin = new CLockLogin(spigot, workingDirectory());
    }

    /**
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
        long start = System.currentTimeMillis();
        if (spigot.boot) {
            try {
                NMSPayload.autoDetect();
                if (NMSPayload.isNotSupported()) {
                    logger().send(LogLevel.SEVERE, "Failed to boot LockLogin. Not supported version: {0}", SpigotServer.getVersion());
                    spigot.boot = false;

                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            } catch (RuntimeException ex) {
                logger().log(ex, "Failed to start LockLogin");
                MinecraftVersion version = SpigotServer.getVersion();

                logger().send(LogLevel.SEVERE, "Incompatible server version {0}. Minimum version is {1}",
                        version, MinecraftVersion.v1_13_X, MinecraftVersion.v1_20_X);
                spigot.boot = false;

                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            chatHandler = new ChatHandler(spigot);
            joinHandler = new JoinHandler(spigot);
            movementHandler = new MovementHandler(spigot);
            quitHandler = new QuitHandler(spigot);
            iterationHandler = new IterationHandler();
            playerVersusHandler = new PlayerVersusHandler(spigot);
            UI_CloseOpenHandler = new InterfaceIOEvent(spigot);
            EventHelper.setInstance(spigot);

            PluginManager pluginManager = Bukkit.getPluginManager();
            spigot.installDriver();

            //Register the vanilla plugin hash methods
            lockLogin.registerHash();

            logger().send(LogLevel.SUCCESS, "LockLogin has been booted");

            //Set up the clients
            setupClients();

            //Reorganize legacy directories with the new ones
            reorganizeDirectories();

            spigot.getSessionFactory(false).getSessions().forEach((session) -> {
                session.invalidate();
                session.captchaLogin(false);
                session.login(false);
                session.pinLogin(false);
                session.totpLogin(false);
            });

            ProtocolAssistant.registerListener();
            spigot.getRuntime().booted = true;

            Path pluginFile = spigot.getRuntime().file();
            try(JarFile jar = new JarFile(pluginFile.toFile())) {
                Enumeration<? extends ZipEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;

                    String name = entry.getName();
                    if (name.endsWith(".class")) {
                        String className = name.replace("/", ".").substring(0, name.length() - 6);
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(CacheAble.class)) {
                                CacheAble cacheAble = clazz.getDeclaredAnnotation(CacheAble.class);
                                try {
                                    Method precache = clazz.getDeclaredMethod("preCache");
                                    precache.invoke(clazz);

                                    spigot.logInfo("Cached {0}", cacheAble.name());
                                } catch (NoSuchMethodException | IllegalAccessException |
                                         InvocationTargetException ex) {
                                    ex.printStackTrace();
                                    spigot.log(ex, "Failed to cache {0}", cacheAble.name());
                                    //spigot.logWarn("Failed to cache {0}", cacheAble.name());
                                }
                            }
                        } catch (NoClassDefFoundError | ClassNotFoundException ex) {
                            spigot.logWarn("Couldn't find class: {0}", className);
                        }
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            CMarketPlace marketPlace = (CMarketPlace) spigot.getMarketPlace();
            int version = marketPlace.getVersion();
            int required = LockLoginJson.getMarketVersion();

            if (version > required) {
                /*
                If the marketplace version is over the required one, it means we
                haven't updated in a while, anyway, marketplace API should be backwards
                compatible (in most cases), but might still break some old versions of
                the plugin, so we blame the user about that
                 */
                spigot.logWarn("Plugin is using an out-dated marketplace version ({0}, we are on {1}), this could result in some things not working properly", version, required);
            }
            if (version < required) {
                /*
                If the marketplace version is under the required one, it means we
                are on a test version which is about to introduce new features to the
                marketplace API. So we advise the user that marketplace might not work
                or even the plugin itself
                 */
                spigot.logWarn("Inconsistent plugin and marketplace versions ({0}, we are on {1})! This usually means you are on a pre-build version of the plugin and should not be used on production", version, required);
            }
            if (version == required) {
                spigot.logInfo("Using LockLogin marketplace version {0}", version);
            }

            try {
                commandHelper.mapCommand(spigot);
            } catch (IOException | ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }

            //Register plugin events
            registerEvents(pluginManager);

            //Load LockLogin modules
            lockLogin.loadModules();

            //Migrate from legacy found data
            lockLogin.migrateLegacyData();

            Configuration configuration = spigot.configuration();
            UpdaterSection updater = configuration.updater();

            checker = new VersionChecker(this);

            if (updater.check()) {
                TaskRunner<Long> checkTask = new AsyncTaskExecutor(updater.interval(), TimeUnit.SECONDS);
                checkTask.setRepeating(true);
                checkTask.on(TaskEvent.RESTART, this::performVersionCheck);
            }

            performVersionCheck();
            logger().log(LogLevel.DEBUG, "LockLogin {0} initialized with {1} services", sourceVersion(), spigot.service_provider.size());

            try {
                Logger logger = (Logger) LogManager.getRootLogger();
                Log4Logger filter = new Log4Logger(this, commandHelper.getCommands());
                logger.addFilter(filter);
            } catch (ClassCastException ex) {
                logger().log(LogLevel.ERROR, "Failed to bind into server's logger, user sensitive information might be exposed!");
                java.util.logging.LogManager javaLM = java.util.logging.LogManager.getLogManager();
                Enumeration<String> names = javaLM.getLoggerNames();

                while (names.hasMoreElements()) {
                    String name = names.nextElement();
                    java.util.logging.Logger logger = javaLM.getLogger(name);

                    logger.setFilter(new JavaLogger(commandHelper.getCommands()));
                }
            }

            //Load marketplace resources
            lockLogin.arrangeMarketplace(marketPlace);

            spigot.configuration().reload(); //Perform a silent reload in order to apply changes from resources
            spigot.languagePackManager().setLang(spigot.configuration().language());

            spigot.messages().reload();
            SpawnLocationStorage.load(); //Precache spawn location, regardless if it's enabled or not

            long end = System.currentTimeMillis();
            long diff = end - start;
            long diff2 = spigot.getPostStartup().toEpochMilli() - spigot.getStartup().toEpochMilli();

            long rs = diff + diff2;
            logger().send(LogLevel.INFO, "LockLogin initialized in {0}ms ({1} seconds)", rs, TimeUnit.MILLISECONDS.toSeconds(rs));
            if (License.isLicensed()) {
                spigot.info("Thanks {0} for supporting Locklogin", License.getBuyer());
            }

            if (spigot.bungeeMode()) {
                PluginMessageListener messageListener = new BungeeListener(this);
                getServer().getMessenger().registerIncomingPluginChannel(this, "login:inject", messageListener);
                getServer().getMessenger().registerOutgoingPluginChannel(this, "login:inject");
            }
        } else {
            logger().send(LogLevel.WARNING, "LockLogin won't initialize due an internal error. Please report this to discord {0}", "https://discord.gg/77p8KZNfqE");
        }
    }

    @Override
    public void disable() {
        Bukkit.getScheduler().cancelTasks(this);
        commandHelper.unMap();

        Logger coreLogger = (Logger) LogManager.getRootLogger();
        Iterator<Filter> filters = coreLogger.getFilters();
        if (filters != null) {
            while (filters.hasNext()) {
                Filter filter = filters.next();
                if (filter.getClass().equals(Log4Logger.class))
                    filter.stop();
            }
        }

        if (!spigot.boot) {
            logger().send(LogLevel.SEVERE,
                    "LockLogin stopped because of an internal error. Check logs for more information");
        }
    }

    public void performVersionCheck() {
        checker.check().onComplete(() -> {
            if (checker.getStatus().equals(BuildStatus.OUTDATED)) {
                logger().send(LogLevel.INFO, "LockLogin has found a new version!");
                logger().send("");
                logger().send(LogLevel.INFO, "Current version is: {0}", sourceVersion());
                logger().send(LogLevel.INFO, "Latest version is: {0}", checker.getVersion());
                logger().send("");

                logger().send(LogLevel.INFO, "Download latest version from:");
                for (URL url : checker.getUpdateURLs()) {
                    logger().send("&b- &7{0}", url);
                }

                logger().send("");
                logger().send("&b------ &7Version history&b ------");
                for (Version version : checker.getVersionHistory()) {
                    String[] changelog = checker.getChangelog(version);
                    if (sourceVersion().compareTo(version) == 0) {
                        logger().send("&bVersion: &a(CURRENT) &3{0}&7:", version);
                    } else {
                        logger().send("&bVersion: &3{0}&7:", version);
                    }

                    for (String line : changelog) {
                        logger().send(line);
                    }
                }
            }
        });
    }

    private void setupClients() {
        PremiumDataStore store = spigot.premiumStore();
        PluginPermissionManager<OfflinePlayer, String> permissionManager = spigot.getPermissionManager();

        for (LocalNetworkClient local : spigot.network().getPlayers()) {
            if (local instanceof CLocalClient) {
                CLocalClient offline = (CLocalClient) local;
                offline.hasPermission = permissionObject -> {
                    UUID uniqueId = local.uniqueId();
                    if (local.connection().equals(ConnectionType.ONLINE)) {
                        UUID tmpId = store.onlineId(offline.name());
                        if (tmpId != null) uniqueId = tmpId;
                    }

                    OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(uniqueId);
                    if (permissionManager != null) {
                        return permissionManager.hasPermission(offlinePlayer, permissionObject);
                    }

                    if (offlinePlayer.isOnline()) {
                        Player player = offlinePlayer.getPlayer();
                        return player != null && player.hasPermission(permissionObject);
                    }

                    return false;
                };
            }
        }
    }

    private void reorganizeDirectories() {
        Path legacy_mod_directory = workingDirectory().resolve("plugin").resolve("modules");
        if (Files.exists(legacy_mod_directory)) {
            try(Stream<Path> files = Files.list(legacy_mod_directory).filter((file) -> !Files.isDirectory(file) && PathUtilities.getExtension(file).equals("jar"))) {
                if (files.findAny().isPresent()) {
                    logger().log(LogLevel.WARNING, "LockLogin has detected presence of legacy modules folder. Those won't be loaded as they used an unsupported plugin API. Please refer to {0} to update the modules and install them in the /mods plugin directory",
                            "https://reddo.es/karmadev/locklogin/community/products/");
                }
            } catch (IOException ignored) {}
        }
        Path modules_directory = workingDirectory().resolve("mods");
        PathUtilities.createDirectory(modules_directory);

        try(Stream<Path> files = Files.list(modules_directory).filter((file) -> !Files.isDirectory(file) && PathUtilities.getExtension(file).equals("jar"))) {
            if (files.findAny().isPresent()) {
                if (Files.exists(legacy_mod_directory)) {
                    PathUtilities.destroy(legacy_mod_directory);
                }
            }
        } catch (IOException ignored) {}
    }

    private void registerEvents(final PluginManager manager) {
        manager.registerEvents(chatHandler, this);
        manager.registerEvents(joinHandler, this);
        manager.registerEvents(movementHandler, this);
        manager.registerEvents(quitHandler, this);
        manager.registerEvents(iterationHandler, this);
        manager.registerEvents(playerVersusHandler, this);

        //PIN inventory events
        if (SpigotPinProcess.createDummy().isEnabled()) {
            manager.registerEvents(UI_CloseOpenHandler, this);
        }
    }

    private SimpleCommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (SimpleCommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            spigot.log(e, "An error occurred while obtaining the command map");
        }

        return null;
    }

    @Override
    public @Nullable URI sourceUpdateURI() {
        return lockLogin.getUpdateURI();
    }
}
