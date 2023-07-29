package es.karmadev.locklogin.api.plugin.database;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.web.WebDownloader;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * LockLogin data driver
 */
public interface DataDriver {

    /**
     * Connect to the driver
     */
    void connect();

    /**
     * Get if the driver is connected
     *
     * @return if the driver has been connected
     */
    boolean connected();

    /**
     * Retrieve a connection from the database
     *
     * @return the database connection
     * @throws SQLException if the connection was not
     * able to be established
     */
    Connection retrieve() throws SQLException;

    /**
     * Fetch the existing tables
     *
     * @return the existing tables
     */
    List<Table> fetchTables();

    /**
     * Close the connection and its statement
     *
     * @param connection the connection to close
     * @param statement the statement to close
     */
    default void close(final Connection connection, final Statement statement) {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {}

        try {
            if (statement != null) statement.close();
        } catch (SQLException ignored) {}
    }

    /**
     * Test a driver
     *
     * @param driver the driver to test
     */
    default void testDriver(final Driver driver) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.runtime();

        plugin.info("Validating the existence of a suitable driver for {0}", driver.name());

        try {
            Class.forName(driver.getTestClass());
            plugin.info("Suitable driver for {0} found!", driver.name());
        } catch (ClassNotFoundException ex) {
            plugin.warn("{0} driver not found. It will be downloaded", driver.name());
            plugin.logInfo("Download/Loading driver {0}", driver.name());

            Path driver_directory = plugin.workingDirectory().resolve("driver").resolve(driver.name());

            boolean status = true;
            long start = System.currentTimeMillis();

            if (!Files.exists(driver_directory)) {
                try {
                    WebDownloader downloader = new WebDownloader(driver.getDownloadURL());
                    Path destination = plugin.workingDirectory().resolve("cache").resolve("driver.zip");
                    downloader.download(destination);

                    /*ResourceDownloader downloader = ResourceDownloader.toCache((KarmaSource) plugin.plugin(), "driver.zip", driver.getDownloadURL(), "cache");
                    downloader.download();*/

                    plugin.logInfo("Successfully downloaded driver {0}", driver.name());

                    try (ZipFile zip = new ZipFile(destination.toFile())) {
                        Enumeration<? extends ZipEntry> entries = zip.entries();

                        do {
                            ZipEntry element = entries.nextElement();
                            String name = element.getName();

                            if (driver.includes(name)) {
                                plugin.info("Loading driver {0} dependency {1}", driver.name(), name.replace(".jar", ""));
                                plugin.logInfo("Loaded driver {0} file {1}", driver.name(), name);

                                InputStream stream = zip.getInputStream(element);

                                Path dependFile = driver_directory.resolve(name);
                                PathUtilities.createPath(dependFile);
                                Files.copy(stream, dependFile, StandardCopyOption.REPLACE_EXISTING);

                                runtime.dependencyManager().appendExternal(dependFile);
                            }
                        } while (entries.hasMoreElements());
                    } catch (IOException err) {
                        plugin.log(err, "Failed to load {0} zip file driver from {1}", driver.name(), driver.getDownloadURL());
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
                try(Stream<Path> files = Files.list(driver_directory).filter((file) -> !Files.isDirectory(file) && PathUtilities.getExtension(file).equals("jar"))) {
                    files.forEach(runtime.dependencyManager()::appendExternal);
                } catch (IOException err) {
                    plugin.log(err, "Failed to load {0} driver files", driver.name());
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
}
