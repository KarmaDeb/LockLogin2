package es.karmadev.locklogin.common.api.user.storage.session;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.plugin.database.DataDriver;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public class CSessionFactory implements SessionFactory<CSession> {

    private final DataDriver driver;

    public CSessionFactory(final DataDriver driver) {
        this.driver = driver;
    }

    /**
     * Create an account for the specified client
     *
     * @param client the client to generate
     *               the account for
     * @return the client account
     */
    @Override
    public CSession create(final LocalNetworkClient client) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery("SELECT `session_id` FROM `user` WHERE `id` = " + client.id())) {
                if (fetch_result.next()) {
                    int session_id = fetch_result.getInt("session_id");
                    if (fetch_result.wasNull()) {
                        driver.close(null, statement);
                        statement = connection.createStatement();

                        statement.execute("INSERT INTO `session` (`created_at`) VALUES (" + now + ")");
                        driver.close(null, statement);

                        statement = connection.createStatement();
                        try (ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
                            if (result.next()) {
                                session_id = result.getInt(1);

                                driver.close(null, statement);
                                statement = connection.createStatement();

                                statement.executeUpdate("UPDATE `user` SET `session_id` = " + session_id + " WHERE `id` = " + client.id());
                            }
                        }
                    }

                    return new CSession(client.id(), session_id, driver);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }

        return null;
    }
}
