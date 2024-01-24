package es.karmadev.locklogin.api.plugin.database.driver.engine;

import es.karmadev.locklogin.api.plugin.database.driver.Driver;
import es.karmadev.locklogin.api.plugin.database.schema.Table;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Represents a SQL driver
 */
public interface SQLDriver {

    /**
     * Get the engine driver
     *
     * @return the driver
     */
    Driver getDriver();

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
     * Close the underlying database connection
     */
    void close();

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
}
