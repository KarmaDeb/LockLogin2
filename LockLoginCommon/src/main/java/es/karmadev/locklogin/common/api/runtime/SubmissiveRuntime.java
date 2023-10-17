package es.karmadev.locklogin.common.api.runtime;

import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

import java.nio.file.Path;
import java.nio.file.Paths;

@Deprecated
public class SubmissiveRuntime extends LockLoginRuntime {

    private final DependencyManager manager = new CDependencyManager();
    private CRuntime runtime;

    public boolean booted = false;

    /**
     * Get the plugin runtime dependency manager
     *
     * @return the dependency manager
     */
    @Override
    public DependencyManager dependencyManager() {
        return (runtime == null ? manager : runtime.dependencyManager());
    }

    /**
     * Get the plugin file path
     *
     * @return the plugin file path
     */
    @Override
    public Path file()  {
        if (runtime != null) return runtime.file();

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
        /*if (runtime != null) return runtime.caller();
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

        return plugin;*/
        return file();
    }

    /**
     * Verify the runtime integrity
     *
     * @param permission the minimum permission authorization level
     * @param clazz      the clazz that is verifying integrity
     * @param method     the method that is verifying integrity
     * @throws SecurityException if the integrity fails to check
     */
    @Override
    public void verifyIntegrity(final int permission, final Class<?> clazz, final String method) throws SecurityException {
        /*if (runtime == null) return;
        runtime.verifyIntegrity(permission, clazz, method);*/
    }

    /**
     * Get if the runtime is completely booted. Meaning
     * the plugin is ready to handle everything
     *
     * @return the plugin boot status
     */
    @Override
    public boolean booting() {
        return (runtime != null ? runtime.booted : !booted);
    }

    /**
     * Become the normal CRuntime
     */
    public void becomeCRuntime() {
        if (runtime == null) {
            runtime = new CRuntime();
            runtime.manager = manager;
        }
    }
}
