package es.karmadev.locklogin.common.api.user.storage.session;

import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.api.user.session.SessionFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CSessionFactory implements SessionFactory<CSession> {

    private final DataDriver driver;
    private final Set<CSession> session_cache = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        if (session_cache.stream().anyMatch((session) -> session != null && client.session() != null && session.id() == client.session().id())) {
            return session_cache.stream().filter((session) -> session.id() == client.session().id()).findFirst().orElse(null);
        }

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

                    CSession session = new CSession(client.id(), session_id, driver);
                    session_cache.add(session);
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
     * Get all the sessions on the server
     *
     * @return all the server sessions
     */
    @Override
    public Collection<CSession> getSessions() {
        List<CSession> offline = new ArrayList<>(session_cache);

        StringBuilder idIgnorer = new StringBuilder();
        for (CSession account : offline) {
            idIgnorer.append(account.id()).append(",");
        }
        String not_in = StringUtils.replaceLast(idIgnorer.toString(), ",", "");

        Connection connection = null;
        Statement statement = null;
        List<CSession> sessions = new ArrayList<>(session_cache);
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet fetch_result = statement.executeQuery("SELECT `id`,`session_id` FROM `user` WHERE `session_id` NOT IN (" + not_in + ")")) {
                while (fetch_result.next()) {
                    int session_id = fetch_result.getInt("session_id");
                    if (!fetch_result.wasNull()) {
                        int user_id = fetch_result.getInt("id");
                        if (!fetch_result.wasNull()) {
                            CSession account = new CSession(user_id, session_id, driver);

                            session_cache.add(account);
                            sessions.add(account);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }

        return Collections.unmodifiableList(sessions);
    }
}
