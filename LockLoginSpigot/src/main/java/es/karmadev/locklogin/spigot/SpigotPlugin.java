package es.karmadev.locklogin.spigot;

import es.karmadev.api.core.KarmaAPI;
import es.karmadev.api.core.KarmaPlugin;
import es.karmadev.api.core.source.exception.AlreadyRegisteredException;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.version.Version;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.common.api.client.CLocalClient;
import es.karmadev.locklogin.common.api.dependency.CPluginDependency;
import es.karmadev.locklogin.common.api.plugin.service.SpartanService;
import es.karmadev.locklogin.common.api.protection.type.*;
import es.karmadev.locklogin.spigot.command.LoginCommand;
import es.karmadev.locklogin.spigot.command.RegisterCommand;
import es.karmadev.locklogin.spigot.event.ChatHandler;
import es.karmadev.locklogin.spigot.event.JoinHandler;
import es.karmadev.locklogin.spigot.event.QuitHandler;
import es.karmadev.locklogin.spigot.protocol.ProtocolAssistant;
import es.karmadev.locklogin.spigot.vault.VaultPermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class SpigotPlugin extends KarmaPlugin {

    LockLoginSpigot spigot;

    public SpigotPlugin() throws NoSuchFieldException, IllegalAccessException, AlreadyRegisteredException {
        super(false);
        Field commandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        commandMap.setAccessible(true);
        CommandMap map = (CommandMap) commandMap.get(Bukkit.getServer());

        spigot = new LockLoginSpigot(this, map);
    }

    /**
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
        logger().send(LogLevel.WARNING, "Preparing to inject dependencies. Please wait...");
        long start = System.currentTimeMillis();
        CPluginDependency.load();

        PluginManager pluginManager = getServer().getPluginManager();
        boolean boot = true;
        for (LockLoginDependency dependency : CPluginDependency.getAll()) {
            if (dependency.isPlugin()) {
                String name = dependency.name();
                Version version = Version.parse(dependency.version().plugin());

                if (name.equalsIgnoreCase("KarmaAPI")) {
                    Version platform = Version.parse(dependency.version().project());

                    Version API_VERSION = Version.parse(KarmaAPI.VERSION);
                    Version PLUGIN_VERSION = Version.parse(KarmaAPI.BUILD);

                    if (API_VERSION.equals(platform)) {
                        if (API_VERSION.compareTo(version) >= 1) {
                            logger().send(LogLevel.INFO, "KarmaAPI detected successfully. Version {0}[{1}] of {2}[{3}] (required)", API_VERSION, PLUGIN_VERSION, platform, version);
                        } else {
                            logger().send(LogLevel.SEVERE, "Cannot load LockLogin as required dependency (KarmaAPI) is out of date ({0}). Yours: {1}", version, PLUGIN_VERSION);
                            boot = false;
                            break;
                        }
                    } else {
                        logger().send(LogLevel.SEVERE, "Cannot load LockLogin as required dependency (KarmaAPI) is not in the required build ({0}). Yours: {1}", platform, API_VERSION);
                        boot = false;
                        break;
                    }
                } else {
                    if (pluginManager.isPluginEnabled(name)) {
                        Plugin plugin = pluginManager.getPlugin(name);
                        if (plugin != null) {
                            Version pluginVersion = Version.parse(plugin.getDescription().getVersion());

                            if (pluginVersion.compareTo(version) < 0) {
                                logger().send(LogLevel.SEVERE, "Plugin dependency {0} was found but is out of date ({1} > {2}). LockLogin will still try to hook into its API, but there may be some errors", name, version, pluginVersion);
                            } else {
                                logger().send(LogLevel.INFO, "Plugin dependency {0} has been successfully hooked", name);
                                if (name.equalsIgnoreCase("Spartan")) {
                                    spigot.registerService("spartan", new SpartanService());
                                }
                            }
                        }
                    }
                }
            } else {
                //console().send("Injecting dependency {0}", Level.INFO, dependency.name());
                spigot.getRuntime().dependencyManager().append(dependency);
            }
        }

        if (boot) {
            spigot.installDriver();

            LockLoginHasher hasher = spigot.hasher();

            try {
                hasher.registerMethod(new SHA512Hash());
                hasher.registerMethod(new SHA256Hash());
                hasher.registerMethod(new BCryptHash());
                hasher.registerMethod(new Argon2I());
                hasher.registerMethod(new Argon2D());
                hasher.registerMethod(new Argon2ID());
            } catch (UnnamedHashException ex) {
                ex.printStackTrace();
                return;
            }

            logger().send(LogLevel.SUCCESS, "LockLogin has been booted");

            PremiumDataStore store = spigot.premiumStore();
            VaultPermissionManager vaultTmpManager = null;
            if (pluginManager.isPluginEnabled("Vault")) {
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

            long end = System.currentTimeMillis();
            long diff = end - start;
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

            PluginCommand login = getCommand("login");
            PluginCommand register = getCommand("register");
            if (login != null) {
                login.setExecutor(new LoginCommand());
            }
            if (register != null) {
                register.setExecutor(new RegisterCommand());
            }

            PluginManager manager = getServer().getPluginManager();
            manager.registerEvents(new JoinHandler(), this);
            manager.registerEvents(new ChatHandler(), this);
            manager.registerEvents(new QuitHandler(), this);
        } else {
            logger().send(LogLevel.WARNING, "LockLogin won't initialize due an internal error. Please report this to discord {0}", "https://discord.gg/77p8KZNfqE");
        }
    }

    @Override
    public void disable() {

    }
}
