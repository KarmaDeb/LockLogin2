package es.karmadev.locklogin.api.extension.module;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.extension.module.exception.InvalidDescriptionException;
import es.karmadev.locklogin.api.extension.module.exception.InvalidModuleException;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * LockLogin module loader
 */
public final class ModuleLoader {

    private final Set<Module> loadedModules = ConcurrentHashMap.newKeySet();
    private final Set<Module> enabledModules = ConcurrentHashMap.newKeySet();
    private final Set<ModuleClassLoader> loaders = ConcurrentHashMap.newKeySet();

    /**
     * Get a module by its name
     *
     * @param name the module name
     * @return the module
     */
    public Module getModule(final String name) {
        return loadedModules.stream().filter((module) -> module.getName().equalsIgnoreCase(name)).findAny().orElse(null);
    }

    /**
     * Get a module by its file
     *
     * @param file the module file
     * @return the module
     */
    public Module getModule(final Path file) {
        return loadedModules.stream().filter((module) -> module.getFile().equals(file)).findAny().orElse(null);
    }

    /**
     * Enables the module
     *
     * @param module the module to enable
     * @return if the module was able to be enabled
     */
    public boolean enable(final Module module) {
        if (module instanceof PluginModule<?>) loadedModules.add(module);
        if (!loadedModules.contains(module)) throw new UnsupportedOperationException("Cannot enable non-loaded module (" + module.getDescription().getName() + ")");

        if (module instanceof AbstractModule) {
            AbstractModule abs = (AbstractModule) module;
            if (abs.isEnabled()) return false; //Already enabled

            enabledModules.add(module);
            loaders.add((ModuleClassLoader) abs.getClassLoader());
            abs.setEnabled(true);
            return true;
        }

        if (enabledModules.contains(module)) return false; //Already enabled
        module.onEnable();
        enabledModules.add(module);

        return true;
    }

    /**
     * Disables the module
     *
     * @param module the module to disable
     */
    public void disable(final Module module) {
        if (!loadedModules.contains(module)) throw new UnsupportedOperationException("Cannot disable a non-loaded module (" + module.getDescription().getName() + ")");

        if (module instanceof AbstractModule) {
            AbstractModule abs = (AbstractModule) module;
            if (!abs.isEnabled()) return;

            abs.setEnabled(false);
            enabledModules.remove(module);
            loaders.remove((ModuleClassLoader) abs.getClassLoader());
            return;
        }

        if (enabledModules.contains(module)) return;
        module.onDisable();
        enabledModules.remove(module);
    }

    /**
     * Try to load a module from a file
     *
     * @param file the module file
     * @return the loaded module
     * @throws InvalidModuleException if the file is not in a valid module
     */
    @NotNull
    public Module load(final Path file) throws InvalidModuleException {
        if (!Files.exists(file)) throw new InvalidModuleException("Cannot load module " + file.toAbsolutePath() + " because it does not exist");

        final ModuleDescription description;
        try {
            description = loadDescription(file);
        } catch (InvalidDescriptionException ex) {
            throw new InvalidModuleException(ex);
        }

        Path parent = file.toAbsolutePath().getParent();
        Path dataFolder = parent.resolve(description.getName());

        if (Files.exists(dataFolder) && !Files.isDirectory(dataFolder)) {
            throw new InvalidModuleException(String.format("Module data folder `%s` (of module `%s`[%s]) exists but is not a directory",
                    dataFolder.toAbsolutePath(),
                    description.getName(),
                    file.toAbsolutePath()));
        }

        for (String depends : description.getDepends()) {
            Module module = getModule(depends);

            if (module == null) {
                throw new InvalidModuleException("Unknown module dependency " + depends + ". Please install it in order to use " + description.getName());
            }
            if (!module.isEnabled()) {
                throw new InvalidModuleException("Module dependency " + depends + " for " + description.getName() + " found and loaded, but not enabled. Module " + description.getName() + " won't be loaded");
            }
        }
        boolean allOptsFound = true;
        for (String optional : description.getOptDepends()) {
            Module module = getModule(optional);

            if (module == null) {
                allOptsFound = false;
                break;
            }
        }

        if (!allOptsFound) {
            CurrentPlugin.getPlugin().warn("Module {0} does not have all its optional dependencies {1}, it's highly recommended for you to install them in order to ensure a complete module functionality",
                    description.getName(),
                    description.getOptDepends());
        }

        final ModuleClassLoader loader;
        try {
            loader = new ModuleClassLoader(this, getClass().getClassLoader(), description, dataFolder, file);
        } catch (InvalidModuleException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new InvalidModuleException(ex);
        }

        loaders.add(loader);
        return loader.module;
    }

    /**
     * Get the description of a module file
     *
     * @param file the file
     * @return the module description
     * @throws InvalidDescriptionException if the file is not a valid module
     */
    public ModuleDescription loadDescription(final Path file) throws InvalidDescriptionException {
        if (file == null) throw new InvalidDescriptionException("Cannot load description from null module file");

        JarFile jar = null;
        InputStream stream = null;
        try {
            jar = new JarFile(file.toFile());
            JarEntry entry = jar.getJarEntry("module.yml");

            if (entry == null) {
                throw new InvalidDescriptionException("Module file does not contain module.yml");
            }

            stream = jar.getInputStream(entry);
            return new ModuleDescription(stream);
        } catch (IOException | YAMLException ex) {
            throw new InvalidDescriptionException(ex);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ignored) {}
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
    }

    Class<?> getClassByName(final String name, final boolean resolve, final ModuleDescription description) {
        for (ModuleClassLoader loader : loaders) {
            try {
                return loader.loadClass0(name, resolve, false);
            } catch (ClassNotFoundException ignored) {}
        }

        return null;
    }

    /**
     * Get and only get the enabled modules
     *
     * @return the enabled modules
     */
    public Collection<Module> getEnabledModules() {
        return Collections.unmodifiableSet(enabledModules);
    }

    /**
     * Get all the modules, regardless if they are enabled
     * or not
     *
     * @return all the loaded modules
     */
    public Collection<Module> getModules() {
        return Collections.unmodifiableSet(loadedModules);
    }

    /**
     * Unloads a module
     *
     * @param module the module to unload
     */
    public void unload(final Module module) {
        if (module instanceof PluginModule) {
            if (enabledModules.contains(module)) {
                enabledModules.remove(module);
                module.onDisable();
            }

            loadedModules.remove(module);
            return; //Stop execution here please
        }

        throw new UnsupportedOperationException("Cannot unload any module but PluginModules");
    }
}
