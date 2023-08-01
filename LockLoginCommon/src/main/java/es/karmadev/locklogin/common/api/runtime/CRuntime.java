package es.karmadev.locklogin.common.api.runtime;

import es.karmadev.api.core.source.APISource;
import es.karmadev.api.core.source.SourceManager;
import es.karmadev.api.core.source.exception.UnknownProviderException;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.manager.ModuleManager;
import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CRuntime extends LockLoginRuntime {

    private final DependencyManager manager = new CDependencyManager();
    private final ModuleManager modManager;

    public boolean booted = false;

    public CRuntime(final ModuleManager manager) {
        modManager = manager;
    }

    /**
     * Get the plugin runtime dependency manager
     *
     * @return the dependency manager
     */
    @Override
    public DependencyManager dependencyManager() {
        return manager;
    }

    /**
     * Get the plugin file path
     *
     * @return the plugin file path
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public Path file() throws SecurityException {
        String path = LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return Paths.get(path);
    }

    /**
     * Get the current caller
     *
     * @return the caller
     */
    @Override
    public Path caller() {
        Path plugin = file();

        try {
            Path pluginsFolder = SourceManager.getProvider("LockLogin").workingDirectory().toAbsolutePath().getParent();
            String loaderPath = APISource.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
            if (loaderPath.startsWith("/")) {
                loaderPath = loaderPath.substring(1);
            }

            Path loader = Paths.get(loaderPath);
            CodeSource source = LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource();
            if (source == null) return plugin;

            String serverPath = source.getLocation().getFile();
            if (serverPath.startsWith("/")) {
                serverPath = serverPath.substring(1);
            }

            String pluginsPath = PathUtilities.pathString(pluginsFolder, '/');

            Path server = Paths.get(serverPath);
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : elements) {
                String name = element.getClassName();
                try {
                    Class<?> clazz = Class.forName(name);
                    URL url = clazz.getResource('/' + name.replace('.', '/') + ".class");
                    if (url != null) {
                        String urlPath = url.getPath();
                        if (urlPath.startsWith("file:") && urlPath.contains("!")) {
                            String jarPath = urlPath.substring((urlPath.startsWith("file:/") ? 6 : 5), urlPath.indexOf('!')).replaceAll("%20", " ");
                            Path path = Paths.get(jarPath);
                            jarPath = PathUtilities.pathString(path, '/');

                            if (!plugin.equals(path) && !loader.equals(path) && !path.equals(server)) {
                                if (jarPath.startsWith(pluginsPath)) {
                                    return path;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (UnknownProviderException ignored) {}

        return plugin;
    }

    /**
     * Verify the runtime integrity
     *
     * @param permission the minimum permission authorization level
     * @param targetClazz      the clazz that is verifying integrity
     * @param targetMethod     the method that is verifying integrity
     * @throws SecurityException if the integrity fails to check
     */
    @Override
    public void verifyIntegrity(final int permission, final Class<?> targetClazz, String targetMethod) throws SecurityException {
        if (permission < 0) return;

        Path plugin = file();
        try {
            Path pluginsFolder = SourceManager.getProvider("LockLogin").workingDirectory().toAbsolutePath().getParent();
            String loaderPath = APISource.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
            if (loaderPath.startsWith("/")) {
                loaderPath = loaderPath.substring(1);
            }

            Path loader = Paths.get(loaderPath);
            CodeSource source = LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource();
            if (source == null) throw new SecurityException("Cannot validate runtime integrity. Are we running in a test environment?");

            String serverPath = source.getLocation().getFile();
            if (serverPath.startsWith("/")) {
                serverPath = serverPath.substring(1);
            }

            String pluginsPath = PathUtilities.pathString(pluginsFolder, '/');

            Path server = Paths.get(serverPath);
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            String method = targetClazz.getSimpleName() + "#" + targetMethod;
            Class<?> sourceClass = null;
            for (StackTraceElement element : elements) {
                String name = element.getClassName();
                try {
                    Class<?> clazz = Class.forName(name);
                    if (sourceClass == null) sourceClass = clazz;

                    URL url = clazz.getResource('/' + name.replace('.', '/') + ".class");
                    if (url != null) {
                        String urlPath = url.getPath();
                        if (urlPath.startsWith("file:") && urlPath.contains("!")) {
                            String jarPath = urlPath.substring((urlPath.startsWith("file:/") ? 6 : 5), urlPath.indexOf('!')).replaceAll("%20", " ");
                            Path jar = Paths.get(jarPath);
                            jarPath = PathUtilities.pathString(jar, '/');

                            if (!jarPath.equals(PathUtilities.pathString(plugin, '/')) && !jar.equals(loader) && !jar.equals(server)) {
                                if (jarPath.startsWith(pluginsPath)) {
                                    Path caller = Paths.get(jarPath);

                                    Module mod = modManager.loader().findByFile(caller);
                                    String pathName = PathUtilities.pathString(caller);

                                    if (mod != null) {
                                        pathName = "(Module) " + mod.sourceName();
                                        if (permission == PLUGIN_AND_MODULES) break;
                                        if (permission == MODULE_ONLY) {
                                            Module src = modManager.loader().findByClass(targetClazz);
                                            if (!src.equals(mod)) {
                                                throw new SecurityException("Cannot access module method " + method + " from an unsafe source. " + pathName);
                                            }

                                            break;
                                        }
                                    }

                                    throw new SecurityException("Cannot access API method " + method + " from an unsafe source. " + pathName);
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
        } catch (UnknownProviderException ignored) {}
    }

    /**
     * Get if the runtime is completely booted. Meaning
     * the plugin is ready to handle everything
     *
     * @return the plugin boot status
     */
    @Override
    public boolean booting() {
        return !booted;
    }
}
