package es.karmadev.locklogin.common.api.client;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CPremiumDataStore implements PremiumDataStore {

    private final SQLDriver engine;
    private final Map<String, UUID> cached = new ConcurrentHashMap<>();

    public CPremiumDataStore(final SQLDriver engine) {
        this.engine = engine;

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.NAME, Row.PREMIUM_UUID)
                    .where(Row.PREMIUM_UUID, QueryBuilder.IS_NOT, QueryBuilder.NULL).build())) {
                while (result.next()) {
                    String name = result.getString(1);
                    String uniqueId = result.getString(2);

                    if (!ObjectUtils.areNullOrEmpty(false, name, uniqueId)) {
                        UUID id = UUID.fromString(uniqueId);
                        cached.put(name, id);
                    }
                }
            }
        } catch (SQLException ignored) {} finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get the client id
     *
     * @param name the client name
     * @return the client id
     */
    @Override
    public UUID onlineId(final String name) {
        UUID cache = cached.getOrDefault(name, null);
        if (cache != null) return cache;

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.PREMIUM_UUID)
                    .where(Row.NAME, QueryBuilder.EQUALS, name).build())) {
                if (result.next()) {
                    String uniqueId = result.getString(1);
                    if (!ObjectUtils.isNullOrEmpty(uniqueId)) {
                        cache = UUID.fromString(uniqueId);
                        cached.put(name, cache);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }

        return cache;
    }

    /**
     * Save the client online id
     *
     * @param name     the client name
     * @param onlineId the client online id
     */
    @Override
    public void saveId(final String name, final UUID onlineId) {
        if (name == null || onlineId == null) return;

        UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            UUID cache = cached.getOrDefault(name, null);
            if (onlineId.equals(cache) || onlineId.equals(offline)) return;

            engine.close(null, statement);
            statement = connection.createStatement();

            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.PREMIUM_UUID, onlineId)
                    .where(Row.NAME, QueryBuilder.EQUALS, name).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }
}
