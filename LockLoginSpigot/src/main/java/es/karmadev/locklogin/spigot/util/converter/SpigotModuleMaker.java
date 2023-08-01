package es.karmadev.locklogin.spigot.util.converter;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.extension.ModuleConverter;
import es.karmadev.locklogin.api.extension.plugin.PluginModule;
import es.karmadev.locklogin.common.api.extension.loader.CModuleLoader;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SpigotModuleMaker implements ModuleConverter<JavaPlugin> {

    private final static LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final static CModuleLoader loader = (CModuleLoader) spigot.moduleManager().loader();
    private final ConcurrentMap<JavaPlugin, SpigotModule> modules = new ConcurrentHashMap<>();

    /**
     * Extend the plugin into a plugin module
     *
     * @param plugin the plugin to extend
     * @return the extended module
     */
    @Override
    public PluginModule<JavaPlugin> extend(final JavaPlugin plugin) {
        if (!modules.containsKey(plugin)) {
            Path caller = pluginFile(plugin);
            loader.loadPlugin(caller);

            SpigotModule sm = modules.computeIfAbsent(plugin, (mod) -> new SpigotModule(plugin, caller));

            loader.load(sm);
            spigot.info("Loaded {0} as a plugin module", PathUtilities.pathString(caller, '/'));
        }

        return modules.get(plugin);
    }

    /**
     * Undo the plugin module extension
     *
     * @param module the extension
     */
    @Override
    public void retract(final PluginModule<JavaPlugin> module) {
        CModuleLoader loader = (CModuleLoader) spigot.moduleManager().loader();
        loader.unload(module);
    }

    /**
     * Undo the plugin module extension
     *
     * @param plugin the plugin owning the
     *               extension
     */
    @Override
    public void retract(JavaPlugin plugin) {
        SpigotModule sm = modules.get(plugin);
        if (sm != null) retract(sm);
    }

    /**
     * Get if the plugin is extended
     *
     * @param plugin the plugin
     * @return if the plugin is extended
     */
    @Override
    public boolean isExtended(JavaPlugin plugin) {
        return modules.containsKey(plugin);
    }

    private Path pluginFile(final JavaPlugin plugin) {
        String path = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return Paths.get(path);
    }


}
