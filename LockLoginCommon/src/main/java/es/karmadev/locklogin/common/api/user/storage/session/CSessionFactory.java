package es.karmadev.locklogin.common.api.user.storage.session;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.user.session.SessionFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CSessionFactory implements SessionFactory<CSession> {

    private final SQLDriver engine;
    private final Set<CSession> session_cache = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public CSessionFactory(final SQLDriver engine) {
        this.engine = engine;
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.SESSION_ID)
                    .where(Row.ID, QueryBuilder.EQUALS, client.id()).build())) {
                if (fetch_result.next()) {
                    int session_id = fetch_result.getInt(1);
                    if (fetch_result.wasNull()) {
                        engine.close(null, statement);
                        statement = connection.createStatement();

                        statement.execute(QueryBuilder.createQuery()
                                .insert(Table.SESSION, Row.CREATED_AT).values(now).build());
                        engine.close(null, statement);

                        statement = connection.createStatement();
                        try (ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
                            if (result.next()) {
                                session_id = result.getInt(1);

                                engine.close(null, statement);
                                statement = connection.createStatement();

                                statement.executeUpdate(QueryBuilder.createQuery()
                                        .update(Table.USER).set(Row.SESSION_ID, session_id)
                                        .where(Row.ID, QueryBuilder.EQUALS, client.id()).build());
                            }
                        }
                    }

                    CSession session = new CSession(client.id(), session_id, engine);
                    session_cache.add(session);

                    return session;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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

        List<Integer> ids = new ArrayList<>();
        for (CSession account : offline) {
            ids.add(account.id());
        }
        List<CSession> sessions = new ArrayList<>(session_cache);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            try (ResultSet fetch_result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.SESSION_ID, Row.ID)
                    .where(Row.SESSION_ID, QueryBuilder.NOT_IN(ids)).build())) {
                while (fetch_result.next()) {
                    int session_id = fetch_result.getInt(1);
                    if (!fetch_result.wasNull()) {
                        int user_id = fetch_result.getInt(1);
                        if (!fetch_result.wasNull()) {
                            CSession account = new CSession(user_id, session_id, engine);

                            session_cache.add(account);
                            sessions.add(account);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }

        return Collections.unmodifiableList(sessions);
    }
}
