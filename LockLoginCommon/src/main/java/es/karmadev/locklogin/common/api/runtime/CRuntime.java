package es.karmadev.locklogin.common.api.runtime;

import es.karmadev.api.core.source.APISource;
import es.karmadev.api.core.source.SourceManager;
import es.karmadev.api.core.source.exception.UnknownProviderException;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.extension.manager.ModuleManager;
import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

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
        String path = LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        try {
            Path pluginsFolder = SourceManager.getProvider("LockLogin").workingDirectory().getParent();
            String loader = APISource.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
            if (loader.startsWith("/")) {
                loader = loader.substring(1);
            }

            CodeSource source = LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource();
            if (source == null) return Paths.get(path);

            String server = source.getLocation().getFile();
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
        } catch (UnknownProviderException ignored) {}

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
        if (permission < 0) return;

        String path = LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        try {
            Path pluginsFolder = SourceManager.getProvider("LockLogin").workingDirectory().getParent();
            String loader = APISource.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
            if (loader.startsWith("/")) {
                loader = loader.substring(1);
            }

            CodeSource source = LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource();
            if (source == null) throw new SecurityException("Cannot validate runtime integrity. Are we running in a test environment?");

            String server = source.getLocation().getFile();
            if (server.startsWith("/")) {
                server = server.substring(1);
            }

            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            String method = "";
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
                            if (jarPath.equals(path) && ObjectUtils.isNullOrEmpty(method)) {
                                method = element.getClassName() + "#" + element.getMethodName();
                            }

                            if (!jarPath.equals(path) && !jarPath.equals(loader) && !jarPath.equals(server)) {
                                if (jarPath.startsWith(pluginsFolder.toString())) {
                                    Path caller = Paths.get(jarPath);

                                    Module mod = modManager.loader().findByFile(caller);
                                    String pathName = PathUtilities.pathString(caller);
                                    if (mod != null) {
                                        pathName = "(Module) " + mod.sourceName();
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
