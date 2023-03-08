package es.karmadev.locklogin.spigot;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.CPluginNetwork;
import es.karmadev.locklogin.common.dependency.CPluginDependency;
import es.karmadev.locklogin.common.protection.CPluginHasher;
import es.karmadev.locklogin.common.protection.type.SHA256Hash;
import es.karmadev.locklogin.common.protection.type.SHA512Hash;
import es.karmadev.locklogin.common.runtime.CRuntime;
import es.karmadev.locklogin.common.user.SQLiteDriver;
import es.karmadev.locklogin.common.user.storage.account.CAccountFactory;
import es.karmadev.locklogin.common.user.storage.session.CSessionFactory;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.comparator.VersionComparator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LockLoginSpigot extends KarmaPlugin implements LockLogin {

    private final LockLoginRuntime runtime = new CRuntime();
    private final CPluginNetwork network = new CPluginNetwork();
    private final LockLoginHasher hasher;

    private final SQLiteDriver sqlite = new SQLiteDriver();
    private final CAccountFactory default_account_factory = new CAccountFactory(sqlite);
    private final CSessionFactory default_session_factory = new CSessionFactory(sqlite);
    private AccountFactory<UserAccount> account_factory = null;
    private SessionFactory<UserSession> session_factory = null;

    public LockLoginSpigot() {
        super(false);
        Class<CurrentPlugin> instance = CurrentPlugin.class;
        try {
            Method initialize = instance.getDeclaredMethod("initialize", LockLogin.class);
            initialize.setAccessible(true);

            initialize.invoke(instance, this);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        hasher = new CPluginHasher();
    }

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

        File pluginFile = getFile();
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
    public AccountFactory<UserAccount> getAccountFactory(final boolean original) {
        return (AccountFactory<UserAccount>) (original ? default_account_factory : (account_factory != null ? account_factory : default_account_factory));
    }

    /**
     * Get the plugin session factory
     *
     * @param original retrieve the plugin default
     *                 session factory
     * @return the plugin session factory
     */
    @Override
    public SessionFactory<UserSession> getSessionFactory(final boolean original) {
        return (SessionFactory<UserSession>) (original ? default_session_factory : (session_factory != null ? session_factory : default_session_factory));
    }

    /**
     * Get the plugin user factory
     *
     * @param original retrieve the plugin default
     *                 user factory
     * @return the plugin user factory
     */
    @Override
    public UserFactory<LocalNetworkClient> getUserFactory(final boolean original) {
        return null;
    }

    /**
     * Define the plugin account factory
     *
     * @param factory the account factory
     */
    @Override
    public void setAccountFactory(final AccountFactory<UserAccount> factory) {
        account_factory = factory;
    }

    /**
     * Define the plugin session factory
     *
     * @param factory the account session factory
     */
    @Override
    public void setSessionFactory(final SessionFactory<UserSession> factory) {
        session_factory = factory;
    }

    /**
     * Define the plugin user factory
     *
     * @param factory the user factory
     */
    @Override
    public void setUserFactory(UserFactory<LocalNetworkClient> factory) {

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
     * Log something that is just informative
     *
     * @param message the log message
     */
    @Override
    public void logInfo(final String message) {
        logger().scheduleLog(Level.INFO, message);
    }

    /**
     * Log something that is important
     *
     * @param message the log message
     */
    @Override
    public void logWarn(final String message) {
        logger().scheduleLog(Level.WARNING, message);
    }

    /**
     * Log something that went wrong
     *
     * @param message the log message
     */
    @Override
    public void logErr(final String message) {
        logger().scheduleLog(Level.GRAVE, message);
    }

    /**
     * Log an error
     *
     * @param error   the error
     * @param message the message
     */
    @Override
    public void log(final Throwable error, final String message) {
        logger().scheduleLog(Level.GRAVE, error);
        logger().scheduleLog(Level.INFO, message);
    }

    /**
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
        try {
            hasher.registerMethod(new SHA512Hash());
            hasher.registerMethod(new SHA256Hash());
        } catch (UnnamedHashException ex) {
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
