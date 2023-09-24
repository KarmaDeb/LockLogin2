package es.karmadev.locklogin.spigot;

import es.karmadev.api.logger.log.BoundedLogger;
import es.karmadev.api.spigot.core.KarmaPlugin;
import es.karmadev.api.core.source.exception.AlreadyRegisteredException;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.ModuleLoader;
import es.karmadev.locklogin.api.extension.module.exception.InvalidModuleException;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
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
import es.karmadev.locklogin.spigot.command.helper.CommandHelper;
import es.karmadev.locklogin.spigot.event.ChatHandler;
import es.karmadev.locklogin.spigot.event.JoinHandler;
import es.karmadev.locklogin.spigot.event.MovementHandler;
import es.karmadev.locklogin.spigot.event.QuitHandler;
import es.karmadev.locklogin.spigot.protocol.ProtocolAssistant;
import es.karmadev.locklogin.spigot.vault.VaultPermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SpigotPlugin extends KarmaPlugin {

    LockLoginSpigot spigot;
    private final CommandHelper commandHelper = new CommandHelper(getCommandMap());

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
        if (spigot.boot) {
            PluginManager pluginManager = Bukkit.getPluginManager();
            spigot.installDriver();

            //Register the vanilla plugin hash methods
            registerHash();

            logger().send(LogLevel.SUCCESS, "LockLogin has been booted");

            //Set up the clients
            setupClients(pluginManager);

            //Reorganize legacy directories with the new ones
            reorganizeDirectories();

            long end = System.currentTimeMillis();
            long diff = end - spigot.getStartup().toEpochMilli();
            logger().send(LogLevel.INFO, "LockLogin initialized in {0}ms ({1} seconds)", diff, TimeUnit.MILLISECONDS.toSeconds(diff));

            spigot.getSessionFactory(false).getSessions().forEach((session) -> {
                session.invalidate();
                session.captchaLogin(false);
                session.login(false);
                session.pinLogin(false);
                session._2faLogin(false);
            });

            ProtocolAssistant.registerListener();
            spigot.getRuntime().booted = true;

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
                logger().send(LogLevel.INFO, "Found legacy accounts folder, preparing to migrate existing data");

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

                            logger().send(LogLevel.WARNING, "Ignoring migration of {0} because there's missing data [{1}]",
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
                            logger().send(LogLevel.SUCCESS, "Successfully migrated account of {0}", name);
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

            logger().log(LogLevel.DEBUG, "LockLogin initialized with {0} services", spigot.service_provider.size());
        } else {
            logger().send(LogLevel.WARNING, "LockLogin won't initialize due an internal error. Please report this to discord {0}", "https://discord.gg/77p8KZNfqE");
        }
    }

    @Override
    public void disable() {
        commandHelper.unMap();
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
                    logger().send(LogLevel.WARNING, "LockLogin has detected presence of legacy modules folder. Those won't be loaded as they used an unsupported plugin API. Please refer to {0} to update the modules and install them in the /mods plugin directory",
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
        manager.registerEvents(new JoinHandler(), this);
        manager.registerEvents(new ChatHandler(), this);
        manager.registerEvents(new QuitHandler(), this);
        manager.registerEvents(new MovementHandler(), this);
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
                    spigot.info("Loaded module {0}", module.getName());
                } catch (InvalidModuleException ex) {
                    spigot.log(ex, "Failed to load file {0} as module", PathUtilities.getName(modFile));
                    spigot.err("Failed to load file {0} as a module file. Does it contains a module.yml?", PathUtilities.pathString(modFile));
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
}
