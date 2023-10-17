package es.karmadev.locklogin.spigot.util.converter;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.extension.ModuleConverter;
import es.karmadev.locklogin.api.extension.module.ModuleLoader;
import es.karmadev.locklogin.api.extension.module.PluginModule;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SpigotModuleMaker implements ModuleConverter<JavaPlugin> {

    private final static LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final static ModuleLoader loader = spigot.moduleManager().loader();
    private final ConcurrentMap<JavaPlugin, SpigotModule> modules = new ConcurrentHashMap<>();

    /**
     * Extend the plugin into a plugin module
     *
     * @param plugin the plugin to extend
     * @return the extended module
     */
    @Override
    public PluginModule<JavaPlugin> implement(final JavaPlugin plugin) {
        if (!modules.containsKey(plugin)) {
            Path caller = pluginFile(plugin);
            SpigotModule sm = modules.computeIfAbsent(plugin, (existing) -> new SpigotModule(plugin, caller, this, spigot.network(), spigot.moduleManager()));
            if (loader.enable(sm)) {
                spigot.info("Implemented plugin {0} as a module (it can now access LockLogin API)", plugin.getName());
                modules.put(plugin, sm);
            }
        }

        return modules.get(plugin);
    }

    /**
     * Undo the plugin module extension
     *
     * @param module the extension
     */
    @Override
    public void dispose(final PluginModule<JavaPlugin> module) {
        if (modules.containsKey(module.getPlugin())) {
            loader.unload(module);
            JavaPlugin plugin = module.getPlugin();

            modules.remove(plugin);
            spigot.info("Disposed plugin {0} as a module (it can no longer access LockLogin API)", plugin.getName());
        }
    }

    /**
     * Undo the plugin module extension
     *
     * @param plugin the plugin owning the
     *               extension
     */
    @Override
    public void dispose(final JavaPlugin plugin) {
        SpigotModule sm = modules.get(plugin);
        if (sm != null) dispose(sm);
    }

    /**
     * Get if the plugin is extended
     *
     * @param plugin the plugin
     * @return if the plugin is extended
     */
    @Override
    public boolean isImplemented(JavaPlugin plugin) {
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
