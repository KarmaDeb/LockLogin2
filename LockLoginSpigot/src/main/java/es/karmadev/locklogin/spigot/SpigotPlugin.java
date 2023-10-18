package es.karmadev.locklogin.spigot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import es.karmadev.api.core.source.exception.AlreadyRegisteredException;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.logger.log.BoundedLogger;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.schedule.runner.TaskRunner;
import es.karmadev.api.schedule.runner.async.AsyncTaskExecutor;
import es.karmadev.api.schedule.runner.event.TaskEvent;
import es.karmadev.api.spigot.core.KarmaPlugin;
import es.karmadev.api.version.BuildStatus;
import es.karmadev.api.version.Version;
import es.karmadev.api.version.checker.VersionChecker;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.api.web.url.domain.WebDomain;
import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.ModuleLoader;
import es.karmadev.locklogin.api.extension.module.exception.InvalidModuleException;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.CacheAble;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.UpdaterSection;
import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.AccountField;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.account.migration.AccountMigrator;
import es.karmadev.locklogin.api.user.account.migration.Transitional;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.common.api.client.CLocalClient;
import es.karmadev.locklogin.common.api.protection.type.*;
import es.karmadev.locklogin.common.api.user.storage.account.transiction.CTransitional;
import es.karmadev.locklogin.common.plugin.secure.logger.JavaLogger;
import es.karmadev.locklogin.common.plugin.secure.logger.Log4Logger;
import es.karmadev.locklogin.common.plugin.web.CMarketPlace;
import es.karmadev.locklogin.common.plugin.web.local.CStoredResource;
import es.karmadev.locklogin.common.plugin.web.manifest.ResourceManifest;
import es.karmadev.locklogin.common.util.LockLoginJson;
import es.karmadev.locklogin.spigot.command.helper.CommandHelper;
import es.karmadev.locklogin.spigot.event.ChatHandler;
import es.karmadev.locklogin.spigot.event.JoinHandler;
import es.karmadev.locklogin.spigot.event.MovementHandler;
import es.karmadev.locklogin.spigot.event.QuitHandler;
import es.karmadev.locklogin.spigot.event.ui.InterfaceIOEvent;
import es.karmadev.locklogin.spigot.process.SpigotPinProcess;
import es.karmadev.locklogin.spigot.protocol.ProtocolAssistant;
import es.karmadev.locklogin.spigot.util.storage.SpawnLocationStorage;
import es.karmadev.locklogin.spigot.vault.VaultPermissionManager;
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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class SpigotPlugin extends KarmaPlugin {

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
    private InterfaceIOEvent UI_CloseOpenHandler; //Internal usage, we don't really care the name it has

    public SpigotPlugin() throws NoSuchFieldException, IllegalAccessException, AlreadyRegisteredException {
        super(false);
        Field commandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        commandMap.setAccessible(true);
        CommandMap map = (CommandMap) commandMap.get(Bukkit.getServer());

        spigot = new LockLoginSpigot(this, map);
        BoundedLogger logger = (BoundedLogger) logger();
        logger.setLogToConsole(false); //Prevent from logging into console
    }

    /**
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
        long start = System.currentTimeMillis();
        if (spigot.boot) {
            chatHandler = new ChatHandler(spigot);
            joinHandler = new JoinHandler(spigot);
            movementHandler = new MovementHandler(spigot);
            quitHandler = new QuitHandler(spigot);
            UI_CloseOpenHandler = new InterfaceIOEvent(spigot);

            PluginManager pluginManager = Bukkit.getPluginManager();
            spigot.installDriver();

            //Register the vanilla plugin hash methods
            registerHash();

            logger().send(LogLevel.SUCCESS, "LockLogin has been booted");

            //Set up the clients
            setupClients(pluginManager);

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
            loadModules();

            Path legacyUserDirectory = spigot.workingDirectory().resolve("data").resolve("accounts");
            if (Files.exists(legacyUserDirectory) && Files.isDirectory(legacyUserDirectory)) {
                logger().log(LogLevel.INFO, "Found legacy accounts folder, preparing to migrate existing data");

                try(Stream<Path> files = Files.list(legacyUserDirectory).filter((file) ->
                        !Files.isDirectory(file) && PathUtilities.getExtension(file).equals("lldb"))) {

                    Pattern namePattern = Pattern.compile("'player' -> [\"'].*[\"']");
                    Pattern passPattern = Pattern.compile("'password' -> [\"'].*[\"']");
                    Pattern tokenPattern = Pattern.compile("'token' -> [\"'].*[\"']");
                    Pattern pinPattern = Pattern.compile("'pin' -> [\"'].*[\"']");
                    Pattern gAuthPattern = Pattern.compile("'2fa' -> [true-false]");
                    Pattern panicPattern = Pattern.compile("'panic' -> [\"'].*[\"']");

                    Path migratedDirectory = spigot.workingDirectory().resolve("cache").resolve("migrations");
                    PathUtilities.createDirectory(migratedDirectory);

                    files.forEachOrdered((file) -> {
                        String fileName = PathUtilities.getName(file, true);

                        Path migrationFile = migratedDirectory.resolve(fileName);
                        List<String> lines = PathUtilities.readAllLines(file);

                        String name = null;
                        String legacyPassword = null;
                        String gAuth = null;
                        String legacyPin = null;
                        boolean gAuthStatus = false;
                        String legacyPanic = null;

                        for (String line : lines) {
                            Matcher nameMatcher = namePattern.matcher(line);
                            Matcher passMatcher = passPattern.matcher(line);
                            Matcher tokenMatcher = tokenPattern.matcher(line);
                            Matcher pinMatcher = pinPattern.matcher(line);
                            Matcher authMatcher = gAuthPattern.matcher(line);
                            Matcher panicMatcher = panicPattern.matcher(line);

                            final int ending = line.length() - 1;
                            if (nameMatcher.find()) {
                                int being = nameMatcher.start() + "'player' -> '".length();
                                name = line.substring(being, ending);

                                continue;
                            }
                            if (passMatcher.find()) {
                                int being = passMatcher.start() + "'password' -> '".length();
                                legacyPassword = line.substring(being, ending);

                                continue;
                            }
                            if (tokenMatcher.find()) {
                                int being = tokenMatcher.start() + "'token' -> '".length();
                                gAuth = line.substring(being, ending);

                                continue;
                            }
                            if (pinMatcher.find()) {
                                int being = pinMatcher.start() + "'pin' -> '".length();
                                legacyPin = line.substring(being, ending);

                                continue;
                            }
                            if (authMatcher.find()) {
                                int being = authMatcher.start() + "'2fa' -> ".length();
                                String rawValue = line.substring(being, ending + 1);

                                gAuthStatus = Boolean.parseBoolean(rawValue);
                                continue;
                            }
                            if (panicMatcher.find()) {
                                int being = panicMatcher.start() + "'panic' -> '".length();
                                legacyPanic = line.substring(being, ending);
                            }
                        }

                        if (name == null || legacyPassword == null ||
                                gAuth == null || legacyPin == null || legacyPanic == null) {

                            logger().log(LogLevel.WARNING, "Ignoring migration of {0} because there's missing data [{1}]",
                                    (name != null ? name : PathUtilities.getName(file, false)));
                            return;
                        }

                        UUID uniqueId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
                        Transitional legacy = CTransitional.fromLegacy(
                                name,
                                uniqueId,
                                legacyPassword,
                                legacyPin,
                                gAuth,
                                legacyPanic,
                                gAuthStatus
                        );

                        UserFactory<? extends LocalNetworkClient> userFactory = spigot.getUserFactory(true);
                        LocalNetworkClient client = userFactory.create(name, uniqueId);

                        AccountFactory<? extends UserAccount> factory = spigot.getAccountFactory(true);
                        AccountMigrator<? extends UserAccount> migrator = factory.migrator();

                        UserAccount migrated = migrator.migrate(client, legacy, AccountField.EMAIL);
                        if (migrated != null) {
                            logger().log(LogLevel.SUCCESS, "Successfully migrated account of {0}", name);
                            try {
                                Files.move(file, migrationFile, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException ex) {
                                logger().log(ex, "Failed to move legacy player {0} file into migrations folder {1}, account might be processed and migrated on the next server boot unless moved manually",
                                        PathUtilities.pathString(file, '/'), PathUtilities.pathString(migrationFile, '/'));
                            }
                        }
                    });
                } catch (IOException ex) {
                    logger().log(ex, "Failed to migrate legacy accounts");
                }
            }

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

            Path resourcesDirectory = spigot.workingDirectory().resolve("marketplace").resolve("resources");
            try(Stream<Path> files = Files.list(resourcesDirectory).filter(Files::isDirectory)) {
                logger().log(LogLevel.INFO, "Preparing to load marketplace resources");
                Gson gson = new GsonBuilder().create();

                Pattern idPattern = Pattern.compile("id=[0-9]*");
                Pattern categoryPattern = Pattern.compile("category=[A-Z]*");
                Pattern namePattern = Pattern.compile("name=.*");
                Pattern descriptionPattern = Pattern.compile("description=.*");
                Pattern publisherPattern = Pattern.compile("publisher=[a-zA-Z0-9_-]{3,16}");
                Pattern versionPattern = Pattern.compile("version=.*");
                Pattern downloadPattern = Pattern.compile("download=[0-9]*");

                files.forEach((directory) -> {
                    Path resourceMeta = directory.resolve("resource.meta");
                    Path manifest = directory.resolve("manifest.json");

                    if (!Files.exists(resourceMeta) || !Files.exists(manifest)) return;
                    if (Files.isDirectory(resourceMeta) || Files.isDirectory(manifest)) return;

                    JsonElement element = gson.fromJson(PathUtilities.read(manifest), JsonElement.class);

                    ResourceManifest rm = new ResourceManifest();
                    if (rm.read(spigot, element)) {
                        List<String> rawData = PathUtilities.readAllLines(resourceMeta);
                        int id = -1;
                        Category category = null;
                        String name = null;
                        String description = null;
                        String publisher = null;
                        String rsVersion = null;
                        Instant download = null;

                        for (String line : rawData) {
                            Matcher matcher = idPattern.matcher(line);
                            if (matcher.matches()) {
                                id = Integer.parseInt(matcher.group().split("=")[1]);
                            } else if (categoryPattern.matcher(line).matches()) {
                                try {
                                    category = Category.valueOf(line.split("=")[1]);
                                } catch (IllegalArgumentException ignored) {}
                            } else if (namePattern.matcher(line).matches()) {
                                name = line.split("=")[1];
                            } else if (descriptionPattern.matcher(line).matches()) {
                                description = line.split("=")[1];
                            } else if (publisherPattern.matcher(line).matches()) {
                                publisher = line.split("=")[1];
                            } else if (versionPattern.matcher(line).matches()) {
                                rsVersion = line.split("=")[1];
                            } else if (downloadPattern.matcher(line).matches()) {
                                download = Instant.ofEpochMilli(Long.parseLong(line.split("=")[1]));
                            }
                        }

                        if (id > 0 && !ObjectUtils.areNullOrEmpty(false, category, name, description,
                                publisher, rsVersion, download)) {
                            CStoredResource resource = CStoredResource.of(
                                id, false, category, name, description, publisher, rsVersion, download, rm
                            );
                            resource.load();
                            marketPlace.getManager().getResourceSet().add(resource);
                        }
                    }
                });

                spigot.configuration().reload(); //Perform a silent reload in order to apply changes from resources
                spigot.languagePackManager().setLang(spigot.configuration().language());

                spigot.messages().reload();
                SpawnLocationStorage.load(); //Precache spawn location, regardless if it's enabled or not

                long end = System.currentTimeMillis();
                long diff = end - start;
                long diff2 = spigot.getPostStartup().toEpochMilli() - spigot.getStartup().toEpochMilli();

                long rs = diff + diff2;
                logger().send(LogLevel.INFO, "LockLogin initialized in {0}ms ({1} seconds)", rs, TimeUnit.MILLISECONDS.toSeconds(rs));
            } catch (IOException ex) {
                logger().log(ex, "Failed to load resources");
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

    private void registerHash() {
        LockLoginHasher hasher = spigot.hasher();

        try {
            hasher.registerMethod(new SHA512Hash());
            hasher.registerMethod(new SHA256Hash());
            hasher.registerMethod(new BCryptHash());
            hasher.registerMethod(new Argon2I());
            hasher.registerMethod(new Argon2D());
            hasher.registerMethod(new Argon2ID());
        } catch (UnnamedHashException ex) {
            spigot.log(ex, "An error occurred while registering default plugin hashes");
        }
    }

    private void setupClients(final PluginManager manager) {
        PremiumDataStore store = spigot.premiumStore();
        VaultPermissionManager vaultTmpManager = null;
        if (manager.isPluginEnabled("Vault")) {
            try {
                vaultTmpManager = new VaultPermissionManager(this);
            } catch (IllegalStateException ignored) {}
        }

        VaultPermissionManager vaultManager = vaultTmpManager;
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
                    if (vaultManager != null) {
                        return vaultManager.hasPermission(offlinePlayer, permissionObject);
                    } else {
                        if (offlinePlayer.isOnline()) {
                            Player player = offlinePlayer.getPlayer();
                            return player != null && player.hasPermission(permissionObject);
                        }
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

        //PIN inventory events
        if (SpigotPinProcess.createDummy().isEnabled()) {
            manager.registerEvents(UI_CloseOpenHandler, this);
        }
    }

    private void loadModules() {
        Path modsFolder = workingDirectory().resolve("mods");
        if (!Files.isDirectory(modsFolder)) PathUtilities.destroy(modsFolder);

        PathUtilities.createDirectory(modsFolder);
        try(Stream<Path> mods = Files.list(modsFolder).filter((path) -> !Files.isDirectory(path))) {
            ModuleLoader loader = spigot.moduleManager().loader();
            mods.forEach((modFile) -> {
                try {
                    Module module = loader.load(modFile);

                    if (loader.enable(module)) {
                        spigot.info("Loaded module {0}", module.getName());
                    } else {
                        spigot.warn("Failed to load module {0}", module.getName());
                    }
                } catch (InvalidModuleException ex) {
                    spigot.log(ex, "Failed to load file {0} as module", PathUtilities.getName(modFile));
                    //spigot.err("Failed to load file {0} as a module file. Does it contains a module.yml?", PathUtilities.pathString(modFile));
                }
            });
        } catch (IOException ex) {
            spigot.log(ex, "An error occurred while loading modules");
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
        URI[] uris = LockLoginJson.getUpdateURIs();
        for (URI uri : uris) {
            try {
                URL url = uri.toURL();
                WebDomain domain = URLUtilities.getDomain(url);
                if (domain == null) continue;

                String host = String.format("%s://%s.%s/", domain.protocol(), domain.root(), domain.tld());
                URL hostURL = URLUtilities.fromString(host);

                if (hostURL == null) continue;

                BuildType type = LockLoginJson.getChannel();
                String flName = type.name().toLowerCase() + ".json";

                return URLUtilities.append(url, flName).toURI();
            } catch (MalformedURLException | URISyntaxException ignored) {}
        }

        return null;
    }
}
