package es.karmadev.locklogin.common.api.client;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CPremiumDataStore implements PremiumDataStore {

    private final DataDriver driver;
    private final Map<String, UUID> cached = new ConcurrentHashMap<>();

    public CPremiumDataStore(final DataDriver driver) {
        this.driver = driver;

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `name`,`premium_uuid` FROM `user` WHERE `premium_uuid` IS NOT NULL")) {
                while (result.next()) {
                    String name = result.getString("name");
                    String uniqueId = result.getString("premium_uuid");

                    if (!ObjectUtils.areNullOrEmpty(false, name, uniqueId)) {
                        UUID id = UUID.fromString(uniqueId);
                        cached.put(name, id);
                    }
                }
            }
        } catch (SQLException ignored) {} finally {
            driver.close(connection, statement);
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
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `premium_uuid` FROM `user` WHERE `name` = '" + name + "'")) {
                if (result.next()) {
                    String uniqueId = result.getString("premium_uuid");
                    if (!ObjectUtils.isNullOrEmpty(uniqueId)) {
                        cache = UUID.fromString(uniqueId);
                        cached.put(name, cache);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
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
            connection = driver.retrieve();
            statement = connection.createStatement();

            UUID cache = cached.getOrDefault(name, null);
            if (onlineId.equals(cache) || onlineId.equals(offline)) return;

            if (cache == null) {
                driver.close(null, statement);
                statement = connection.createStatement();

                statement.executeUpdate("UPDATE `user` SET `premium_uuid` = '" + onlineId + "' WHERE `name` = '" + name + "'");
            } else {
                statement.execute("INSERT INTO `user` (`name`,`uuid`,`premium_uuid`) VALUES ('" + name + "','" + offline + "','" + onlineId + "')");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }
    }
}
