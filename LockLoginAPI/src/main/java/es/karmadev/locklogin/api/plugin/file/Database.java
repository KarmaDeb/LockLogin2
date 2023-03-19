package es.karmadev.locklogin.api.plugin.file;

import es.karmadev.locklogin.api.plugin.database.Driver;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;

/**
 * LockLogin database configuration
 */
public interface Database {

    /**
     * Get the plugin database driver
     *
     * @return the plugin driver
     */
    Driver driver();

    /**
     * Get the database name
     *
     * @return the database
     */
    String database();

    /**
     * Get the SQL server host
     *
     * @return the sql host
     */
    String host();

    /**
     * Get the SQL server port
     *
     * @return the SQL port
     */
    int port();

    /**
     * Get the SQL server username
     *
     * @return the account name
     */
    String username();

    /**
     * Get the SQL server password
     *
     * @return the account password
     */
    String password();

    /**
     * Get if the connection is performed
     * under ssl
     *
     * @return if the connection uses SSL
     */
    boolean ssl();

    /**
     * Get if the connection verifies server
     * certificates
     *
     * @return if the connection is safe
     */
    boolean verifyCertificates();

    /**
     * Get the test query
     *
     * @return the test query
     */
    String testQuery();

    /**
     * Get the sql connection timeout
     *
     * @return the connection timeout
     */
    int connectionTimeout();

    /**
     * Get the connection being used timeout
     *
     * @return the connection use timeout
     */
    int unusedTimeout();

    /**
     * Get the database leak detection threshold
     *
     * @return the database leak detection
     */
    int leakDetection();

    /**
     * Get the connection maximum lifetime
     *
     * @return the connection lifetime
     */
    int maximumLifetime();

    /**
     * Get the minimum amount of connections
     *
     * @return the minimum amount of connections
     */
    int minimumConnections();

    /**
     * Get the maximum amount of connections
     *
     * @return the maximum amount of connections
     */
    int maximumConnections();

    /**
     * Get the name of a table
     *
     * @param table the table
     * @return the table name
     */
    String tableName(final Table table);

    /**
     * Get the column name of a table
     *
     * @param table the table
     * @param row the row
     * @return the column name
     */
    String columnName(final Table table, final Row row);
}

