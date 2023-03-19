package es.karmadev.locklogin.api.plugin.database;

import lombok.Getter;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.util.Arrays;

/**
 * LockLogin known drivers
 */
public enum Driver {
    /**
     * SQLite driver (default)
     */
    SQLite(
            "org.sqlite.JDBC",
            "https://dbschema.com/jdbc-drivers/SqliteJdbcDriver.zip",
            "jdbc:sqlite:{0}",
            "sqlite-jdbc"),

    /**
     * MySQL driver
     */
    MySQL(
            "com.mysql.cj.jdbc.Driver",
            "https://dbschema.com/jdbc-drivers/MySqlJdbcDriver.zip",
            "jdbc:mysql://{0}:{1}/{2}?useSSL={3}?verifyServerCertificate={4}",
            "mysql-connector"),

    /**
     * MariaDB driver
     */
    MariaDB(
            "org.mariadb.jdbc.Driver",
            "https://dbschema.com/jdbc-drivers/MariaDbJdbcDriver.zip",
            "",
            "mariadb-java-client"),

    /**
     * MongoDB driver
     */
    MongoDB(
            "com.wisecoders.dbschema.mongodb.JdbcDriver",
            "https://dbschema.com/jdbc-drivers/MongoDbJdbcDriver.zip",
            "",
            "bson",
            "bson-record",
            "graal-sdk",
            "icu4j",
            "js",
            "js-scriptengine",
            "mongodb-driver-core",
            "mongodb-driver-async",
            "mongodjdbc",
            "regex",
            "truffle-api");

    @Getter
    private final String testClass;
    @Getter
    private final String downloadURL;
    private final String connection;

    private final String[] injectJars;

    /**
     * Initialize the driver
     *
     * @param test the test class name
     * @param url the download url
     * @param inject the jar files to inject
     */
    Driver(final String test, final String url, final String connectionURL, final String... inject) {
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
        return StringUtils.formatString(connection, arguments);
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
}
