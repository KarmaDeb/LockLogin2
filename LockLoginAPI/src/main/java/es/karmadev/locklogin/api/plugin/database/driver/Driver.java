package es.karmadev.locklogin.api.plugin.database.driver;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.api.web.WebDownloader;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * LockLogin known drivers, downloaded drivers
 * are originally downloaded from:
 * <a href="https://dbschema.com/">DB Schema</a>
 */
@Getter
public enum Driver {

    /**
     * SQLite driver (default)
     */
    SQLite(
            true,
            "org.sqlite.JDBC",
            "https://karmadev.es/locklogin-repository/dependency/driver/v1/sqlite.zip",
            "jdbc:sqlite:{0}",
            "sqlite-jdbc"),

    /**
     * H2 driver (default)
     */
    H2(
            false, //Even though h2 supports local storage, you can also have it on a remote host
            "org.h2.Driver",
            "https://karmadev.es/locklogin-repository/dependency/driver/v1/h2.zip",
            new String[]{
                    "jdbc:h2:file:{0};MODE=mysql",
                    "jdbc:h2:tcp://{0}:{1}/~/{2};MODE=mysql"
            },
            "h2"),

    /**
     * MySQL driver
     */
    MySQL(
            false,
            "com.mysql.cj.jdbc.Driver",
            "https://karmadev.es/locklogin-repository/dependency/driver/v1/mysql.zip",
            "jdbc:mysql://{0}:{1}/{2}?useSSL={3}?verifyServerCertificate={4}",
            "mysql-connector"),

    /**
     * MariaDB driver
     */
    MariaDB(
            false,
            "org.mariadb.jdbc.Driver",
            "https://karmadev.es/locklogin-repository/dependency/driver/v1/mariadb.zip",
            "jdbc:mysql://{0}:{1}/{2}?useSSL={3}?verifyServerCertificate={4}",
            "mariadb-java-client"),

    /**
     * PostgreSQL driver
     */
    PostgreSQL(
            false,
                    "org.postgresql.Driver",
                    "https://karmadev.es/locklogin-repository/dependency/driver/v1/postgre.zip",
                    "jdbc:postgresql://{0}:{1}/{2}?useSSL={3}?verifyServerCertificate={4}",
                    "postgresql");

    private final String testClass;
    @Getter
    private final String downloadURL;
    private final String[] connections;
    @Getter
    private final boolean local;

    private final String[] injectJars;

    /**
     * Initialize the driver
     *
     * @param local the driver works locally instead
     *              of a server
     * @param test the test class name
     * @param url the download url
     * @param connections the connection URLs
     * @param inject the jar files to inject
     */
    Driver(final boolean local, final String test, final String url, final String[] connections, final String... inject) {
        this.local = local;
        this.testClass = test;
        this.downloadURL = url;
        this.connections = connections;
        this.injectJars = inject;
    }

    /**
     * Initialize the driver
     *
     * @param local if the driver works locally instead
     *              of a server
     * @param test the test class name
     * @param url the download url
     * @param connectionURL the connection URL
     * @param inject the jar files to inject
     */
    Driver(final boolean local, final String test, final String url, final String connectionURL, final String... inject) {
        this.local = local;
        testClass = test;
        downloadURL = url;
        connections = new String[]{connectionURL};
        injectJars = inject;
    }

    /**
     * Get the connection URL
     *
     * @param arguments the connection arguments
     * @return the connection URL
     */
    public String getConnection(final Object... arguments) {
        if (connections.length > 1) {
            /*
            If the driver supports different URL connections and we too,
            we must iterate through all the supported connection URLs and
            count how many arguments it supports and then return the first
            match that has the same amount of arguments as we provide. If
            we fail on this task, we will always return the first available
            connection URL, which is never null or empty
             */
            for (String str : connections) {
                Pattern argMatcher = Pattern.compile("\\{[0-9]*}");
                Matcher matcher = argMatcher.matcher(str);

                int count = 0;
                while (matcher.find()) {
                    count++;
                }

                if (count == arguments.length) {
                    return StringUtils.format(str, arguments);
                }
            }
        }

        return StringUtils.format(connections[0], arguments);
    }

    /**
     * Get if the jar file is included
     *
     * @param name the jar file name
     * @return if the jar file is included
     */
    public boolean includes(final String name) {
        return Arrays.stream(injectJars).anyMatch(name.toLowerCase()::startsWith);
    }

    /**
     * Test the driver
     */
    public void testDriver() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.getRuntime();

        plugin.logInfo("Validating the existence of a suitable driver for {0}", this.name());

        try {
            Class.forName(this.getTestClass());
            plugin.logInfo("Suitable driver for {0} found!", this.name());

            return;
        } catch (ClassNotFoundException ignored) {}

        Path driver_directory = plugin.workingDirectory().resolve("driver").resolve(this.name());

        boolean status = true;
        long start = System.currentTimeMillis();

        if (!Files.exists(driver_directory)) {
            plugin.logWarn("{0} driver not found. It will be downloaded", this.name());

            try {
                WebDownloader downloader = new WebDownloader(this.getDownloadURL());
                Path destination = plugin.workingDirectory().resolve("cache").resolve("driver.zip");
                downloader.download(destination);

                plugin.logInfo("Successfully downloaded driver {0}", this.name());

                try (ZipFile zip = new ZipFile(destination.toFile())) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();

                    do {
                        ZipEntry element = entries.nextElement();
                        String name = element.getName();

                        if (this.includes(name)) {
                            //plugin.info("Loading driver {0} dependency {1}", this.name(), name.replace(".jar", ""));
                            plugin.logInfo("Loaded driver {0} file {1}", this.name(), name);

                            InputStream stream = zip.getInputStream(element);

                            Path dependFile = driver_directory.resolve(name);
                            PathUtilities.createPath(dependFile);

                            Files.copy(stream, dependFile, StandardCopyOption.REPLACE_EXISTING);

                            runtime.dependencyManager().appendExternal(dependFile);
                        }
                    } while (entries.hasMoreElements());
                } catch (IOException err) {
                    plugin.log(err, "Failed to load {0} zip file driver from {1}", this.name(), this.getDownloadURL());
                    status = false;
                } finally {
                    long end = System.currentTimeMillis();
                    long elapsed = end - start;

                    if (status) {
                        plugin.logInfo("Finished driver testing after {0}ms", elapsed);
                    } else {
                        plugin.logErr("Failed driver testing after {0}ms", elapsed);
                    }
                }
            } catch (Throwable ex2) {
                throw new RuntimeException(ex2);
            }
        } else {
            plugin.logInfo("Loading driver {0}", this.name());

            try(Stream<Path> files = Files.list(driver_directory).filter((file) -> !Files.isDirectory(file) && PathUtilities.getExtension(file).equals("jar"))) {
                files.forEach((path) -> runtime.dependencyManager().appendExternal(path));
            } catch (IOException err) {
                plugin.log(err, "Failed to load {0} driver files", this.name());
                status = false;
            } finally {
                long end = System.currentTimeMillis();
                long elapsed = end - start;

                if (status) {
                    plugin.logInfo("Finished driver testing after {0}ms", elapsed);
                } else {
                    plugin.logErr("Failed driver testing after {0}ms", elapsed);
                }
            }
        }
    }
}
