package es.karmadev.locklogin.common.api.user;

import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.common.api.client.CLocalClient;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

public class CUserFactory implements UserFactory<CLocalClient> {

    private final SQLDriver engine;

    public CUserFactory(final SQLDriver engine) {
        this.engine = engine;
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            //"SELECT `id` FROM `user` WHERE `name` = '" + name + "' OR `uuid` = '" + uniqueId + "'"
            try (ResultSet fetch_result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.ID)
                    .where(Row.NAME, QueryBuilder.EQUALS, name).or()
                    .where(Row.UUID, QueryBuilder.EQUALS, uniqueId).build())) {
                if (fetch_result.next()) {
                    int id = fetch_result.getInt("id");
                    return new CLocalClient(id, engine);
                } else {
                    engine.close(null, statement);
                    statement = connection.createStatement();

                    //"INSERT INTO `user` (`name`,`uuid`,`account_id`,`session_id`,`created_at`) VALUES ('" + name + "','" + uniqueId + "'," + account + "," + session + "," + now + ")"
                    statement.execute(QueryBuilder.createQuery()
                            .insert(Table.USER, Row.NAME, Row.UUID, Row.ACCOUNT_ID, Row.SESSION_ID, Row.CREATED_AT)
                            .values(name, uniqueId, account, session, now).build());
                    return create(name, uniqueId, account, session);
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
     * Create a user without an account and
     * session
     *
     * @param name     the username
     * @param uniqueId the user unique id
     * @return the new created user
     */
    @Override
    public CLocalClient create(final String name, final UUID uniqueId) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.ID)
                    .where(Row.NAME, QueryBuilder.EQUALS, name).or()
                    .where(Row.UUID, QueryBuilder.EQUALS, uniqueId).build())) {
                if (fetch_result.next()) {
                    int id = fetch_result.getInt("id");
                    return new CLocalClient(id, engine);
                } else {
                    engine.close(null, statement);
                    statement = connection.createStatement();

                    //"INSERT INTO `user` (`name`,`uuid`,`created_at`) VALUES ('" + name + "','" + uniqueId + "'," + now + ")"
                    statement.execute(QueryBuilder.createQuery()
                            .insert(Table.USER, Row.NAME, Row.UUID, Row.CREATED_AT)
                            .values(name, uniqueId, now).build());
                    engine.close(null, statement);

                    try (ResultSet insert_result = statement.executeQuery("SELECT last_insert_rowid()")) {
                        if (insert_result.next()) {
                            int id = insert_result.getInt(1);
                            return new CLocalClient(id, engine);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }

        return null;
    }
}
