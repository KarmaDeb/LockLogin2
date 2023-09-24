package es.karmadev.locklogin.common.api.runtime;

import es.karmadev.api.core.source.APISource;
import es.karmadev.api.core.source.SourceManager;
import es.karmadev.api.core.source.exception.UnknownProviderException;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.ModuleManager;
import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public class CRuntime extends LockLoginRuntime {

    DependencyManager manager;
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
        try {
            Path filePath = Paths.get(LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String path = PathUtilities.pathString(filePath, '/');

            APISource source;
            try {
                source = SourceManager.getProvider("LockLogin");
            } catch (UnknownProviderException ex) {
                throw new SecurityException(ex);
            }

            String pluginsFolder = PathUtilities.pathString(source.workingDirectory().getParent().toAbsolutePath(), '/');

            Path loaderJar = Paths.get(APISource.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String loader = PathUtilities.pathString(loaderJar, '/');

            Path serverJar = Paths.get(LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            String server = PathUtilities.pathString(serverJar, '/');

            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            String method = targetClazz.getSimpleName() + "#" + targetMethod;

            if (elements.length >= 5) {
                boolean bypass = false;
                for (int i = 0; i < 4; i++) {
                    StackTraceElement caller = elements[i];
                    String name = caller.getClassName();

                    if (name.equalsIgnoreCase("es.karmadev.locklogin.spigot.util.converter.SpigotModuleMaker")) {
                        bypass = true;
                        break;
                    }
                }

                if (bypass) return;
                /*
                Allow internal classes to bypass this restriction in order to allow its normal function,
                for instance, SpigotModuleMaker, to allow plugins to be implemented into a module, the plugin
                must somehow access (inheritable) to the module manager, which is "protected"
                 */

                StackTraceElement caller = elements[4]; //CRuntime and SubmissiveRuntime call counts!
                String name = caller.getClassName();
                try {
                    Class<?> clazz = Class.forName(name);
                    URL url = clazz.getResource('/' + name.replace('.', '/') + ".class");
                    if (url != null) {
                        String urlPath = url.getPath();

                        if (urlPath.startsWith("file:") && urlPath.contains("!")) {
                            String jarPath = urlPath.substring((urlPath.startsWith("file:/") ? 6 : 5), urlPath.indexOf('!')).replaceAll("%20", " ");
                            Path currentPath = Paths.get(jarPath);
                            jarPath = PathUtilities.pathString(currentPath, '/');

                            if (!path.equals(jarPath) && !loader.equals(jarPath) && !server.equals(jarPath)) {
                                if (jarPath.startsWith(pluginsFolder)) {
                                    Module mod = modManager.loader().getModule(currentPath);
                                    String pathName = PathUtilities.pathString(currentPath, '/');
                                    if (mod != null) {
                                        pathName = "(Module) " + mod.getDescription().getName();
                                        if (permission != PLUGIN_ONLY) {
                                            //source.logger().send(LogLevel.SUCCESS, "Allowed class {0}[{1}] to accessing protected method {2}", method, pathName, urlRoute);
                                            return;
                                        }
                                    }

                                    throw new SecurityException("Cannot allow source " + pathName + " to access method " + method);
                                }
                            }
                        }
                    }
                } catch (FileSystemNotFoundException | ClassNotFoundException ignored) {}
            }
        } catch (URISyntaxException ex) {
            throw new SecurityException(ex);
        }
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