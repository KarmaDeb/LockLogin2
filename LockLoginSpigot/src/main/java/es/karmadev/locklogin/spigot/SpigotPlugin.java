package es.karmadev.locklogin.spigot;

import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.common.api.client.CLocalClient;
import es.karmadev.locklogin.common.api.dependency.CPluginDependency;
import es.karmadev.locklogin.common.api.protection.type.*;
import es.karmadev.locklogin.spigot.vault.VaultPermissionManager;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.comparator.VersionComparator;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

public class SpigotPlugin extends KarmaPlugin {

    LockLoginSpigot spigot;

    public SpigotPlugin() {
        super(false);
        spigot = new LockLoginSpigot(this);
    }

    /**
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
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
                        boot = false;
                        break;
                    }
                } else {
                    if (pluginManager.isPluginEnabled(name)) {
                        Plugin plugin = pluginManager.getPlugin(name);
                        if (plugin != null) {
                            String pluginVersion = plugin.getDescription().getVersion();

                            VersionComparator comparator = new VersionComparator(pluginVersion, version);
                            if (!comparator.isUpToDate()) {
                                console().send("Plugin dependency {0} was found but is out of date ({1} > {2}). LockLogin will still try to hook into its API, but there may be some errors", Level.WARNING, name, version, pluginVersion);
                            } else {
                                console().send("Plugin dependency {0} has been successfully hooked", Level.INFO, name);
                            }
                        }
                    }
                }
            } else {
                console().send("Injecting dependency {0}", Level.INFO, dependency.name());
                spigot.runtime().dependencyManager().append(dependency);
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

            console().send("LockLogin has been booted", Level.OK);

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
                                return player != null && player.hasPermission(permissionObject.node());
                            }
                        }


                        return false;
                    };
                }
            }

            Path legacy_mod_directory = getDataPath().resolve("plugin").resolve("modules");
            if (Files.exists(legacy_mod_directory)) {
                try(Stream<Path> files = Files.list(legacy_mod_directory).filter((file) -> !Files.isDirectory(file) && PathUtilities.getExtension(file).equals("jar"))) {
                    if (files.findAny().isPresent()) {
                        console().send("LockLogin has detected presence of legacy modules folder. Those won't be loaded as they used an unsupported plugin API. Please refer to {0} to update the modules and install them in the /mods plugin directory",
                                Level.WARNING,
                                "https://reddo.es/karmadev/locklogin/community/products/");
                    }
                } catch (IOException ignored) {}
            }
            Path modules_directory = getDataPath().resolve("mods");
            PathUtilities.createDirectory(modules_directory);

            try(Stream<Path> files = Files.list(modules_directory).filter((file) -> !Files.isDirectory(file) && PathUtilities.getExtension(file).equals("jar"))) {
                if (files.findAny().isPresent()) {
                    if (Files.exists(legacy_mod_directory)) {
                        PathUtilities.destroy(legacy_mod_directory);
                    }
                }
            } catch (IOException ignored) {}
        } else {
            console().send("LockLogin won't initialize due an internal error. Please report this to discord {0}", Level.WARNING, "https://discord.gg/77p8KZNfqE");
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
