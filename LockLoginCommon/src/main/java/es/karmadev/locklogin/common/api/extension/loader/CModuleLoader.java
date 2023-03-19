package es.karmadev.locklogin.common.api.extension.loader;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.extension.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.manager.ModuleLoader;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.common.api.extension.CModuleManager;
import es.karmadev.locklogin.common.api.extension.command.CCommandMap;
import es.karmadev.locklogin.common.api.extension.command.CModCommand;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CModuleLoader implements ModuleLoader {

    private final Set<CModData> loadedModules = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Module, CModData> unloadedModulesData = new ConcurrentHashMap<>();

    private final CModuleManager manager;

    public CModuleLoader(final CModuleManager manager) {
        this.manager = manager;
    }

    /**
     * Load a module
     *
     * @param module the module to load
     */
    @Override
    public void load(final Module module) {
        if (unloadedModulesData.containsKey(module)) {
            CModData disabled = unloadedModulesData.remove(module);
            if (disabled != null) {
                loadedModules.add(disabled);

                KarmaYamlManager module_yaml = disabled.getModuleYML();
                mapCommands(module_yaml, module);

                module.onLoad();
            }
        }
    }

    /**
     * Try to load a module from a file
     *
     * @param file the module file
     * @return the loaded module
     * @throws IllegalStateException if the module is not in a valid
     *                               directory or is not a valid module
     */
    @Override
    public Module load(final Path file) throws IllegalStateException {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.runtime();

        File modFile = file.toFile();

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("module.yml");
            if (entry == null) plugin.logErr("Cannot load module from file {0} (nonexistent module.yml)", PathUtilities.getPrettyPath(file));

            try (InputStream stream = jar.getInputStream(entry)) {
                KarmaYamlManager module_yaml = new KarmaYamlManager(stream);
                String clazz = module_yaml.getString("main", null);

                if (!StringUtils.isNullOrEmpty(clazz)) {
                    runtime.dependencyManager().appendExternal(file);

                    try {
                        Class<? extends Module> main = Class.forName(clazz).asSubclass(Module.class);

                        Constructor<? extends Module> constructor = main.getDeclaredConstructor();
                        constructor.setAccessible(true);

                        Module module = constructor.newInstance();
                        mapCommands(module_yaml, module);

                        CModData data = new CModData(file, module, module_yaml);
                        loadedModules.add(data);

                        module.onLoad();
                    } catch (Exception ex) {
                        plugin.log(ex, "An unexpected error occurred while fetching a module from {0}", PathUtilities.getPrettyPath(file));
                    }
                }
            }
        } catch (IOException ex) {
            plugin.log(ex, "Failed to read module from file: {0}", PathUtilities.getPrettyPath(file));
        }

        return null;
    }

    /**
     * Try to get a module by its file
     *
     * @param file the module file
     * @return the module
     */
    @Override
    public Module get(final Path file) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.runtime();

        File modFile = file.toFile();

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("module.yml");
            if (entry == null) plugin.logErr("Cannot load module from file {0} (nonexistent module.yml)", PathUtilities.getPrettyPath(file));

            try (InputStream stream = jar.getInputStream(entry)) {
                KarmaYamlManager module_yaml = new KarmaYamlManager(stream);
                String clazz = module_yaml.getString("main", null);

                if (!StringUtils.isNullOrEmpty(clazz)) {
                    URL[] urls = new URL[]{
                            modFile.toURI().toURL(),
                            runtime.file().toUri().toURL()
                    };
                    try (URLClassLoader loader = new URLClassLoader(urls)) {
                        try {
                            Class<? extends Module> main = loader.loadClass(clazz).asSubclass(Module.class);

                            Class<Module> moduleClass = Module.class;
                            Method init = moduleClass.getDeclaredMethod("initialize", File.class, KarmaYamlManager.class, Class.class);
                            init.setAccessible(true);

                            return (Module) init.invoke(moduleClass, modFile, module_yaml, main);
                        } catch (Exception ex) {
                            plugin.log(ex, "An unexpected error occurred while fetching a module from {0}", PathUtilities.getPrettyPath(file));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            plugin.log(ex, "Failed to read module from file: {0}", PathUtilities.getPrettyPath(file));
        }

        return null;
    }

    /**
     * Unload a module
     *
     * @param module the module to unload
     */
    @Override
    public void unload(final Module module) {
        CModData mod_data = null;
        for (CModData data : loadedModules) {
            Module dataMod = data.getModule();
            if (dataMod.id().equals(module.id())) {
                mod_data = data;
            }
        }

        if (mod_data != null) {
            EventHandlerList[] handlers = manager.getHandlers(module);
            for (EventHandlerList handler : handlers) handler.unregisterAll(module);

            CCommandMap commandMap = manager.commands();
            commandMap.unregisterAll(module);

            module.onUnload();
            loadedModules.remove(mod_data);
            unloadedModulesData.put(module, mod_data);
        }
    }

    /**
     * Find a module
     *
     * @param name the module name
     * @return the module
     */
    @Override
    public Module findByName(final String name) {
        Module match = null;

        for (CModData data : loadedModules) {
            Module module = data.getModule();

            if (module.name().equals(name)) {
                match = module;
                break;
            }
        }

        return match;
    }

    /**
     * Find a module
     *
     * @param file the module file
     * @return the module
     */
    @Override
    public Module findByFile(final Path file) {
        Module match = null;

        for (CModData data : loadedModules) {
            if (data.getFile().equals(file)) {
                match = data.getModule();
                break;
            }
        }

        return match;
    }

    /**
     * Find a module by a class member
     *
     * @param clazz the class
     * @return the module owning that class
     */
    @Override
    public Module findByClass(final Class<?> clazz) {
        Module match = null;

        for (CModData data : loadedModules) {
            if (data.ownsClass(clazz)) {
                match = data.getModule();
                break;
            }
        }

        return match;
    }

    /**
     * Find an unloaded module by its name
     *
     * @param name the module name
     * @return the module
     */
    @Override
    public Module findUnloaded(final String name) {
        Module match = null;

        for (Module data : unloadedModulesData.keySet()) {
            if (data.name().equals(name)) {
                match = data;
                break;
            }
        }

        return match;
    }

    /**
     * Find an unloaded module by its
     * module file
     *
     * @param file the module file
     * @return the module
     */
    @Override
    public Module findUnloaded(final Path file) {
        Module match = null;

        for (Module data : unloadedModulesData.keySet()) {
            if (data.getSourceFile().equals(file.toFile())) {
                match = data;
                break;
            }
        }

        return match;
    }

    /**
     * Find an unloaded module by a class
     * member
     *
     * @param clazz the class
     * @return the module owning that class
     */
    @Override
    public Module findUnloaded(final Class<?> clazz) {
        Module match = null;

        for (Module data : unloadedModulesData.keySet()) {
            CModData mod_data = unloadedModulesData.getOrDefault(data, null);
            if (mod_data != null) {
                if (mod_data.ownsClass(clazz)) {
                    match = data;
                    break;
                }
            }
        }

        return match;
    }

    /**
     * Get all the loaded modules
     *
     * @return the loaded modules
     */
    @Override
    public Module[] getModules() {
        List<Module> modules = new ArrayList<>();

        loadedModules.forEach((finder) -> modules.add(finder.getModule()));
        return modules.toArray(new Module[0]);
    }

    /**
     * Map the commands of the module
     *
     * @param yaml the module internal yaml file
     * @param module the module
     */
    private void mapCommands(final KarmaYamlManager yaml, final Module module) {
        if (yaml.isSet("commands")) {
            KarmaYamlManager command_section = yaml.getSection("commands");
            for (String key : command_section.getKeySet()) {
                KarmaYamlManager command_data = command_section.getSection(key);
                String description = command_data.getString("description", "A module command");
                List<String> aliases = command_data.getStringList("aliases");

                ModuleCommand command = new CModCommand(module, key, description, aliases.toArray(new String[0]));
                manager.commands().register(module, command);
            }
        }
    }
}
