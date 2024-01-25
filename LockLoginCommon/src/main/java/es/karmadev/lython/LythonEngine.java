package es.karmadev.lython;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.CurrentPlugin;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * LockLogin python engine, using Jython (which
 * uses python)
 */
public class LythonEngine implements Runnable {

    private final Path pythonScript;

    /**
     * Initialize the lython engine
     *
     * @param scriptPath the python script path
     */
    public LythonEngine(final Path scriptPath) {
        this.pythonScript = scriptPath;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (!Files.exists(pythonScript) || Files.isDirectory(pythonScript)) return;

        String raw = PathUtilities.read(pythonScript);
        if (ObjectUtils.isNullOrEmpty(raw)) return;

        Path path = Paths.get("D:\\.dev\\.m2\\repository\\org\\python\\jython-standalone\\2.7.3\\jython-standalone-2.7.3.jar");
        try {
            URL[] jython = new URL[]{path.toUri().toURL()};

            String[] allowed = new String[]{"es.karmadev.locklogin.api"};
            try (RestrictedClassLoader loader = new RestrictedClassLoader(jython, allowed, PathUtilities.pathString(pythonScript))) {
                try {
                    Class<?> jythonInterpreterClass = loader.request("org.python.util.PythonInterpreter");
                    loader.restrict();

                    Object instance = jythonInterpreterClass.getDeclaredConstructor().newInstance();
                    Method exec = jythonInterpreterClass.getMethod("exec", String.class);

                    exec.invoke(instance, raw);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static class Hello {
        public static class Waver {
            public void hello() {
                System.out.println("Hello from java!");
            }
        }
    }
}

class RestrictedClassLoader extends URLClassLoader {

    private final List<String> allowed;
    private final String scriptPath;

    private boolean open = true;

    /**
     * Constructs a new URLClassLoader for the specified URLs using the
     * default delegation parent {@code ClassLoader}. The URLs will
     * be searched in the order specified for classes and resources after
     * first searching in the parent class loader. Any URL that ends with
     * a '/' is assumed to refer to a directory. Otherwise, the URL is
     * assumed to refer to a JAR file which will be downloaded and opened
     * as needed.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @throws SecurityException    if a security manager exists and its
     *                              {@code checkCreateClassLoader} method doesn't allow
     *                              creation of a class loader.
     * @throws NullPointerException if {@code urls} is {@code null}.
     * @see SecurityManager#checkCreateClassLoader
     */
    public RestrictedClassLoader(final URL[] urls, final String[] allowed, final String scriptPath) {
        super(urls, null);
        this.allowed = Arrays.asList(allowed);
        this.scriptPath = scriptPath;
    }

    Class<?> request(final String clazz) throws ClassNotFoundException {
        if (!open) throw new ClassNotFoundException(clazz);
        return super.loadClass(clazz, false);
    }

    void restrict() {
        open = false;
    }

    /**
     * Loads the class with the specified <a href="#name">binary name</a>.  The
     * default implementation of this method searches for classes in the
     * following order:
     *
     * <ol>
     *
     *   <li><p> Invoke {@link #findLoadedClass(String)} to check if the class
     *   has already been loaded.  </p></li>
     *
     *   <li><p> Invoke the {@link #loadClass(String) <tt>loadClass</tt>} method
     *   on the parent class loader.  If the parent is <tt>null</tt> the class
     *   loader built-in to the virtual machine is used, instead.  </p></li>
     *
     *   <li><p> Invoke the {@link #findClass(String)} method to find the
     *   class.  </p></li>
     *
     * </ol>
     *
     * <p> If the class was found using the above steps, and the
     * <tt>resolve</tt> flag is true, this method will then invoke the {@link
     * #resolveClass(Class)} method on the resulting <tt>Class</tt> object.
     *
     * <p> Subclasses of <tt>ClassLoader</tt> are encouraged to override {@link
     * #findClass(String)}, rather than this method.  </p>
     *
     * <p> Unless overridden, this method synchronizes on the result of
     * {@link #getClassLoadingLock <tt>getClassLoadingLock</tt>} method
     * during the entire class loading process.
     *
     * @param name    The <a href="#name">binary name</a> of the class
     * @param resolve If <tt>true</tt> then resolve the class
     * @return The resulting <tt>Class</tt> object
     * @throws ClassNotFoundException If the class could not be found
     */
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        if (this.allowed.stream().noneMatch(name::startsWith))
            throw new SecurityException("Script " + scriptPath + " tried to access restricted resource " + name);

        return super.loadClass(name, resolve);
    }
}
