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
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * LockLogin known drivers
 */
@Getter
public enum Driver {

    /**
     * SQLite driver (default)
     */
    SQLite(
            true,
            "org.sqlite.JDBC",
            "https://dbschema.com/jdbc-drivers/SqliteJdbcDriver.zip",
            "jdbc:sqlite:{0}",
            "sqlite-jdbc"),

    /**
     * MySQL driver
     */
    MySQL(
            false,
            "com.mysql.cj.jdbc.Driver",
            "https://dbschema.com/jdbc-drivers/MySqlJdbcDriver.zip",
            "jdbc:mysql://{0}:{1}/{2}?useSSL={3}?verifyServerCertificate={4}",
            "mysql-connector"),

    /**
     * MariaDB driver
     */
    MariaDB(
            false,
            "org.mariadb.jdbc.Driver",
            "https://dbschema.com/jdbc-drivers/MariaDbJdbcDriver.zip",
            "jdbc:mysql://{0}:{1}/{2}?useSSL={3}?verifyServerCertificate={4}",
            "mariadb-java-client"),

    /**
     * MongoDB driver
     */
    MongoDB(
            false,
            "com.wisecoders.dbschema.mongodb.JdbcDriver",
            "https://dbschema.com/jdbc-drivers/MongoDbJdbcDriver.zip",
            /*
            That's actually a bad practice, we should host our own .zip file, so if
            the version changes, that won't affect us as we will always be downloading
            the defined dependency version
             */
            "mongodb://{0}:{1}",
            "bson",
            "bson-record",
            "graal-sdk",
            "icu4j",
            "js",
            "js-scriptengine",
            "mongodb-driver-core",
            "mongodb-driver-sync",
            "mongodjdbc",
            "regex",
            "truffle-api");

    private final String testClass;
    @Getter
    private final String downloadURL;
    private final String connection;
    @Getter
    private final boolean local;

    private final String[] injectJars;

    /**
     * Initialize the driver
     *
     * @param local if the driver works locally instead
     *              of a server
     * @param test the test class name
     * @param url the download url
     * @param inject the jar files to inject
     */
    Driver(final boolean local, final String test, final String url, final String connectionURL, final String... inject) {
        this.local = local;
        testClass = test;
        downloadURL = url;
        connection = connectionURL;
        injectJars = inject;
    }

    /**
     * Get the connection URL
     *
     * @param arguments the connection arguments
     * @return the connection URL
     */
    public String getConnection(final Object... arguments) {
        return StringUtils.format(connection, arguments);
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

        plugin.info("Validating the existence of a suitable driver for {0}", this.name());

        try {
            Class.forName(this.getTestClass());
            plugin.info("Suitable driver for {0} found!", this.name());

            return;
        } catch (ClassNotFoundException ignored) {}

        Path driver_directory = plugin.workingDirectory().resolve("driver").resolve(this.name());

        boolean status = true;
        long start = System.currentTimeMillis();

        if (!Files.exists(driver_directory)) {
            plugin.warn("{0} driver not found. It will be downloaded", this.name());

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
                            plugin.info("Loading driver {0} dependency {1}", this.name(), name.replace(".jar", ""));
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
                        plugin.info("Finished driver testing after {0}ms", elapsed);
                    } else {
                        plugin.err("Failed driver testing after {0}ms", elapsed);
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
                    plugin.info("Finished driver testing after {0}ms", elapsed);
                } else {
                    plugin.err("Failed driver testing after {0}ms", elapsed);
                }
            }
        }
    }
}
