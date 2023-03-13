package es.karmadev.locklogin.spigot;

import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.common.api.dependency.CPluginDependency;
import es.karmadev.locklogin.common.api.protection.type.*;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.comparator.VersionComparator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class SpigotPlugin extends KarmaPlugin {

    LockLoginSpigot spigot;

    public SpigotPlugin() {
        super(false);
        spigot = new LockLoginSpigot(this);
        spigot.getDriver().connect();
    }

    /**
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
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
                console().send("Injecting dependency \"{0}\"", Level.INFO, dependency.name());
                spigot.runtime().dependencyManager().append(dependency);
            }
        }

        if (boot) {
            console().send("LockLogin has been booted", Level.OK);
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
