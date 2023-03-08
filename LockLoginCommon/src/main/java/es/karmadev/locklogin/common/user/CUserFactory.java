package es.karmadev.locklogin.common.user;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.client.CLocalClient;
import es.karmadev.locklogin.common.user.storage.session.CSession;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

public class CUserFactory implements UserFactory<CLocalClient> {

    private final SQLiteDriver driver;

    public CUserFactory(final SQLiteDriver driver) {
        this.driver = driver;
    }

    /**
     * Create a new user
     *
     * @param name     the user name
     * @param uniqueId the user unique id
     * @param account  the user account
     * @param session  the user session
     * @return the new created user
     */
    @Override
    public CLocalClient create(final String name, final UUID uniqueId, final int account, final int session) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery("SELECT `id` FROM `user` WHERE `name` = '" + name + "' OR `uuid` = '" + uniqueId + "'")) {
                if (fetch_result.next()) {
                    int id = fetch_result.getInt("id");
                    return new CLocalClient(id, driver);
                } else {
                    driver.close(null, statement);
                    statement = connection.createStatement();

                    statement.execute("INSERT INTO `user` (`name`,`uuid`,`account_id`,`session_id`,`created_at`) VALUES ('" + name + "','" + uniqueId + "'," + account + "," + session + "," + now + ")");
                    return create(name, uniqueId, account, session);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }

        return null;
    }

    /**
     * Create an user without an account and
     * session
     *
     * @param name     the user name
     * @param uniqueId the user unique id
     * @return the new created user
     */
    @Override
    public CLocalClient create(final String name, final UUID uniqueId) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery("SELECT `id` FROM `user` WHERE `name` = '" + name + "' OR `uuid` = '" + uniqueId + "'")) {
                if (fetch_result.next()) {
                    int id = fetch_result.getInt("id");
                    return new CLocalClient(id, driver);
                } else {
                    driver.close(null, statement);
                    statement = connection.createStatement();

                    statement.execute("INSERT INTO `user` (`name`,`uuid`,`created_at`) VALUES ('" + name + "','" + uniqueId + "'," + now + ")");
                    driver.close(null, statement);

                    try (ResultSet insert_result = statement.executeQuery("SELECT last_insert_rowid()")) {
                        if (insert_result.next()) {
                            int id = insert_result.getInt(1);
                            return new CLocalClient(id, driver);
                        }
                    }
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
