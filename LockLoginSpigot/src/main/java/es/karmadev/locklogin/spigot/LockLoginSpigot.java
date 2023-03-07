package es.karmadev.locklogin.spigot;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.CPluginNetwork;
import es.karmadev.locklogin.common.dependency.CPluginDependency;
import es.karmadev.locklogin.common.protection.CPluginHasher;
import es.karmadev.locklogin.common.runtime.CRuntime;
import es.karmadev.locklogin.common.user.storage.account.CAccountFactory;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.comparator.VersionComparator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.nio.file.Path;

public class LockLoginSpigot extends KarmaPlugin implements LockLogin {

    private final LockLoginRuntime runtime = new CRuntime();
    private final CPluginNetwork network = new CPluginNetwork();
    private final LockLoginHasher hasher = new CPluginHasher();

    private final CAccountFactory default_account_factory = new CAccountFactory();
    private AccountFactory<? extends UserAccount> account_factory = null;

    /**
     * Get the LockLogin plugin instance
     *
     * @return the locklogin plugin
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public Object plugin() throws SecurityException {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        return this;
    }

    /**
     * Get the plugin working directory
     *
     * @return the plugin working directory
     */
    @Override
    public Path workingDirectory() {
        return getDataPath();
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
        return null;
    }

    /**
     * Get the plugin messages
     *
     * @return the plugin messages
     */
    @Override
    public Messages messages() {
        return null;
    }

    /**
     * Get the plugin account factory
     *
     * @param original retrieve the plugin default
     *                 account factory
     * @return the plugin account factory
     */
    @Override
    public AccountFactory<? extends UserAccount> getAccountFactory(final boolean original) {
        return (original ? default_account_factory : (account_factory != null ? account_factory : default_account_factory));
    }

    /**
     * Get the plugin session factory
     *
     * @param original retrieve the plugin default
     *                 session factory
     * @return the plugin session factory
     */
    @Override
    public SessionFactory<? extends UserSession> getSessionFactory(final boolean original) {
        return null;
    }

    /**
     * Define the plugin account factory
     *
     * @param factory the account factory
     */
    @Override
    public void setAccountFactory(final AccountFactory<? extends UserAccount> factory) {
        account_factory = factory;
    }

    /**
     * Define the plugin session factory
     *
     * @param factory the account session factory
     */
    @Override
    public void setSessionFactory(final SessionFactory<? extends UserSession> factory) {

    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void info(final String message, final Object... replaces) {
        console().send(message, Level.INFO, replaces);
    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void warn(final String message, final Object... replaces) {
        console().send(message, Level.WARNING, replaces);
    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void err(final String message, final Object... replaces) {
        console().send(message, Level.GRAVE, replaces);
    }

    /**
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
        Class<CurrentPlugin> instance = CurrentPlugin.class;
        try {
            Method initialize = instance.getDeclaredMethod("initialize", LockLogin.class);
            initialize.setAccessible(true);

            initialize.invoke(instance, this);
        } catch (Throwable ex) {
            ex.printStackTrace();
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
                        boot = true;
                        break;
                    }
                } else {
                    if (pluginManager.isPluginEnabled(name)) {
                        Plugin plugin = pluginManager.getPlugin(name);
                        String pluginVersion = plugin.getDescription().getVersion();

                        VersionComparator comparator = new VersionComparator(pluginVersion, version);
                        if (!comparator.isUpToDate()) {
                            console().send("Plugin dependency {0} was found but is out of date ({1} > {2}). LockLogin will still try to hook into its API, but there may be some errors", Level.WARNING, name, version, pluginVersion);
                        } else {
                            console().send("Plugin dependency {0} has been successfully hooked", Level.INFO, name);
                        }
                    }
                }
            } else {
                console().send("Injecting dependency \"{0}\"", Level.INFO, dependency.name());
                runtime.dependencyManager().append(dependency);
            }
        }

        if (boot) {

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
