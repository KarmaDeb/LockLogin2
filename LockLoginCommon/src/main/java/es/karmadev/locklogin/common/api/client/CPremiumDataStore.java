package es.karmadev.locklogin.common.api.client;

import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.common.api.SQLiteDriver;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CPremiumDataStore implements PremiumDataStore {

    private final SQLiteDriver driver;
    private final Map<String, UUID> cached = new ConcurrentHashMap<>();

    public CPremiumDataStore(final SQLiteDriver driver) {
        this.driver = driver;

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `name`,`uuid` FROM `premium`")) {
                while (result.next()) {
                    String name = result.getString("name");
                    String uniqueId = result.getString("uuid");

                    if (!StringUtils.areNullOrEmpty(name, uniqueId)) {
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

            try (ResultSet result = statement.executeQuery("SELECT `uuid` FROM `premium` WHERE `name` = '" + name + "'")) {
                if (result.next()) {
                    String uniqueId = result.getString("uuid");
                    if (!StringUtils.isNullOrEmpty(uniqueId)) {
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

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            UUID cache = cached.getOrDefault(name, null);
            UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
            if (onlineId.equals(cache) || onlineId.equals(offline)) return;

            if (cache == null) {
                driver.close(null, statement);
                statement = connection.createStatement();

                statement.executeUpdate("UPDATE `premium` SET `uuid` = '" + onlineId + "' WHERE `name` = '" + name + "'");
            } else {
                statement.execute("INSERT INTO `premium` (`name`,`uuid`) VALUES ('" + name + "','" + onlineId + "')");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }
    }
}
