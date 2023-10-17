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

    public boolean booted = false;

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
        /*Path plugin = file();

        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements.length >= 5) {
            StackTraceElement caller = elements[4]; //CRuntime and SubmissiveRuntime call counts!
            String name = caller.getClassName();
            try {
                Class<?> clazz = Class.forName(name);
                URL url = clazz.getResource('/' + name.replace('.', '/') + ".class");
                if (url != null) {
                    String urlPath = url.getPath();

                    if (urlPath.startsWith("file:") && urlPath.contains("!")) {
                        String jarPath = urlPath.substring((urlPath.startsWith("file:/") ? 6 : 5), urlPath.indexOf('!')).replaceAll("%20", " ");
                        return Paths.get(jarPath);
                    }
                }
            } catch (FileSystemNotFoundException | ClassNotFoundException ignored) {
            }
        }*/

        return file();
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
        /*try {
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
        }*/
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