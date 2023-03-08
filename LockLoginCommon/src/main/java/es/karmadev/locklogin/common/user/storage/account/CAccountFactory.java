package es.karmadev.locklogin.common.user.storage.account;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.user.SQLiteDriver;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

public class CAccountFactory implements AccountFactory<CAccount> {

    private final SQLiteDriver driver;

    public CAccountFactory(final SQLiteDriver driver) {
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
    public CAccount create(final LocalNetworkClient client) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery("SELECT `account_id` FROM `user` WHERE `id` = " + client.id())) {
                if (fetch_result.next()) {
                    int account_id = fetch_result.getInt("account_id");
                    if (fetch_result.wasNull()) {
                        driver.close(null, statement);
                        statement = connection.createStatement();

                        statement.execute("INSERT INTO `account` (`created_at`) VALUES (" + now + ")");
                        driver.close(null, statement);

                        statement = connection.createStatement();
                        try (ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
                            if (result.next()) {
                                account_id = result.getInt(1);

                                driver.close(null, statement);
                                statement = connection.createStatement();

                                statement.executeUpdate("UPDATE `user` SET `account_id` = " + account_id + " WHERE `id` = " + client.id());
                            }
                        }
                    }

                    return new CAccount(client.id(), account_id, driver);
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
