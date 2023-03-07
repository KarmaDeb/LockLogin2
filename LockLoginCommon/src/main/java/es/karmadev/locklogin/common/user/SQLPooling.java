package es.karmadev.locklogin.common.user;

import java.sql.Connection;
import java.sql.Statement;

/**
 * SQL Pooling
 */
public interface SQLPooling {

    /**
     * Retrieve a new connection
     *
     * @return the connection to retrieve
     */
    Connection retrieve();

    /**
     * Closes an statement and its connection
     *
     * @param connection the connection to close
     * @param statement the statement to close
     */
    default void close(final Connection connection, final Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (Throwable ignored) {}
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable ignored) {}
        }
    }
}
