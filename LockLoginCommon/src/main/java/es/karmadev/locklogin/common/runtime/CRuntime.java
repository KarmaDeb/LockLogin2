package es.karmadev.locklogin.common.runtime;

import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public class CRuntime extends LockLoginRuntime {

    private final DependencyManager manager = new CDependencyManager();

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
        String path = LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Path pluginsFolder = APISource.loadProvider("LockLogin").getDataPath().getParent();
        String loader = APISource.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (loader.startsWith("/")) {
            loader = loader.substring(1);
        }

        String server = LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        if (server.startsWith("/")) {
            server = server.substring(1);
        }

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

                        if (!jarPath.equals(path) && !loader.equals(jarPath) && !jarPath.equals(server)) {
                            if (jarPath.startsWith(pluginsFolder.toString())) {
                                return Paths.get(jarPath);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return Paths.get(path);
    }

    /**
     * Verify the runtime integrity
     *
     * @param permission the minimum permission authorization level
     * @throws SecurityException if the integrity fails to check
     */
    @Override
    public void verifyIntegrity(final int permission) throws SecurityException {
        if (permission < ANY) return;

        String path = LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Path pluginsFolder = APISource.loadProvider("LockLogin").getDataPath().getParent();
        String loader = APISource.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (loader.startsWith("/")) {
            loader = loader.substring(1);
        }

        CodeSource source = LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource();
        if (source == null) return;

        String server = source.getLocation().getFile();
        if (server.startsWith("/")) {
            server = server.substring(1);
        }

        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String method = "";
        for (StackTraceElement element : elements) {
            String name = element.getClassName();
            try {
                Class<?> clazz = Class.forName(name);
                URL url = clazz.getResource('/' + name.replace('.', '/') + ".class");
                if (url != null) {
                    String urlPath = url.getPath();
                    if (urlPath.startsWith("file:") && urlPath.contains("!")) {
                        String jarPath = urlPath.substring((urlPath.startsWith("file:/") ? 6 : 5), urlPath.indexOf('!')).replaceAll("%20", " ");
                        if (jarPath.equals(path) && StringUtils.isNullOrEmpty(method)) {
                            method = element.getClassName() + "#" + element.getMethodName();
                        }

                        if (!jarPath.equals(path) && !jarPath.equals(loader) && !jarPath.equals(server)) {
                            if (jarPath.startsWith(pluginsFolder.toString())) {
                                Path caller = Paths.get(jarPath);

                                Module mod = manager.find(caller);
                                String pathName = PathUtilities.getPrettyPath(caller);
                                if (mod != null) {
                                    pathName = "(Module) " + mod.name();
                                    if (permission == PLUGIN_AND_MODULES) break;
                                    if (permission == MODULE_ONLY) {
                                        //TODO: Check if the module owns the class
                                    }
                                }

                                throw new SecurityException("Cannot access API method " + method + " from an unsafe source. " + pathName);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
