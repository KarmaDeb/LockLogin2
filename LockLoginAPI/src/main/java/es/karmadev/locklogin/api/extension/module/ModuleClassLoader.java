package es.karmadev.locklogin.api.extension.module;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.extension.module.command.CommandRegistrar;
import es.karmadev.locklogin.api.extension.module.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.module.exception.InvalidModuleException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * Module class loader, allowing different modules
 * to share classes
 */
final class ModuleClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    //private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    private final ModuleManager loader;
    private final ModuleDescription description;
    private final Path file;
    private final Path dataFolder;
    private final JarFile jar;
    AbstractModule module;

    ModuleClassLoader(@NotNull final ModuleManager loader,
                      @Nullable final ClassLoader parent,
                      @NotNull final ModuleDescription description,
                      @NotNull final Path dataFolder,
                      @NotNull final Path file) throws IOException, InvalidModuleException {
        super(new URL[]{file.toUri().toURL()}, parent);

        this.loader = loader;
        this.description = description;
        this.file = file;
        this.dataFolder = dataFolder;
        this.jar = new JarFile(file.toFile());
        /*this.manifest = jar.getManifest();
        this.url = file.toUri().toURL();*/

        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.getMain(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidModuleException("Cannot find module class \"" + description.getMain() + "\".");
            }

            Class<? extends AbstractModule> moduleClass;
            try {
                moduleClass = jarClass.asSubclass(AbstractModule.class);
            } catch (ClassCastException ex) {
                throw new InvalidModuleException("Main class \"" + description.getMain() + "\" does not extend AbstractModule");
            }

            module = moduleClass.getConstructor().newInstance();
            module.invokeInitialization();

            if (description.yaml.containsKey("commands")) {
                Object value = description.yaml.get("commands");
                if (value instanceof Map) {
                    Map<?, ?> unknownMap = (Map<?, ?>) value;
                    for (Object key : unknownMap.keySet()) {
                        Object mapValue = unknownMap.get(key);
                        if (key instanceof String && mapValue instanceof Map) {
                            Map<?, ?> mapData = (Map<?, ?>) mapValue;

                            String commandName = (String) key;
                            String cmdDescription;
                            List<String> cmdAliases = new ArrayList<>();

                            if (!mapData.containsKey("description")) continue;
                            cmdDescription = String.valueOf(mapData.get("description"));

                            if (mapData.containsKey("aliases")) {
                                Object aliasesObject = mapData.get("aliases");
                                if (aliasesObject instanceof List) {
                                    List<?> list = (List<?>) aliasesObject;
                                    list.forEach((obj) -> cmdAliases.add(String.valueOf(obj)));
                                }
                            }

                            ModuleCommand command = new AbstractCommand(
                                    module, commandName, cmdDescription, cmdAliases.toArray(new String[0]));

                            try {
                                CommandRegistrar registrar = CurrentPlugin.getPlugin().moduleManager().commands();

                                Method register = registrar.getClass().getDeclaredMethod("register", Module.class, ModuleCommand.class);
                                register.invoke(registrar, module, command);
                            } catch (ReflectiveOperationException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                }
            }

            module.onLoad();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new InvalidModuleException("No public module constructor found", ex);
        } catch (InstantiationException ex) {
            throw new InvalidModuleException("Abnormal module type", ex);
        } catch (IllegalArgumentException ex) {
            throw new InvalidModuleException("No no-args module constructor found", ex);
        }
    }

    @Nullable
    @Override
    public URL getResource(final String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        return findResources(name);
    }

    @Override
    public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true);
    }

    Class<?> loadClass0(@NotNull String name, boolean resolve, boolean checkGlobal) throws ClassNotFoundException {
        try {
            Class<?> result = super.loadClass(name, resolve);
            if (checkGlobal || result.getClassLoader() == this) {
                return result;
            }
        } catch (ClassNotFoundException ignored) {}

        if (checkGlobal) {
            // This ignores the libraries of other plugins, unless they are transitive dependencies.
            Class<?> result = loader.loader().getClassByName(name, resolve, description);

            if (result != null) {
                return result;
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    public void close() throws IOException {
        module = null;
        try {
            super.close();
        } finally {
            jar.close();
        }
    }

    /**
     * Initialize the module
     */
    synchronized void initialize() {
        module.init(loader, description, file, dataFolder, this);
    }
}
