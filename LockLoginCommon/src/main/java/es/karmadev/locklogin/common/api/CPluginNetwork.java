package es.karmadev.locklogin.common.api;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.common.api.client.CLocalClient;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CPluginNetwork implements PluginNetwork {

    private final SQLiteDriver driver;
    private final Set<NetworkClient> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<NetworkServer> servers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<LocalNetworkClient> offline_cache = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public CPluginNetwork(final SQLiteDriver driver) {
        this.driver = driver;

        CurrentPlugin.whenAvailable((plugin) -> {
            Collection<LocalNetworkClient> offline_clients = getPlayers();
            plugin.info("There're {0} accounts in this server (not all of them may be registered)", offline_clients.size());
        });
    }

    /**
     * Append a client
     *
     * @param client the client to append
     */
    public void appendClient(final NetworkClient client) {
        clients.add(client);
    }

    /**
     * Append a server
     *
     * @param server the server to append
     */
    public void appendServer(final NetworkServer server) {
        servers.add(server);
    }

    /**
     * Get a client
     *
     * @param id the client id
     * @return the client
     */
    @Override
    public NetworkClient getPlayer(final int id) {
        return clients.stream().filter((client) -> client.id() == id).findFirst().orElse(null);
    }

    /**
     * Get a client
     *
     * @param name the client name
     * @return the client
     */
    @Override
    public NetworkClient getPlayer(final String name) {
        return clients.stream().filter((client) -> client.name().equals(name)).findFirst().orElse(null);
    }

    /**
     * Get a client
     *
     * @param uniqueId the client unique id
     * @return the client
     */
    @Override
    public NetworkClient getPlayer(final UUID uniqueId) {
        return clients.stream().filter((client) -> client.uniqueId().equals(uniqueId)).findFirst().orElse(null);
    }

    /**
     * Get a client
     *
     * @param id the client id
     * @return the client
     */
    @Override
    public LocalNetworkClient getEntity(final int id) {
        if (offline_cache.stream().anyMatch((offline) -> offline.id() == id)) {
            return offline_cache.stream().filter((offline) -> offline.id() == id).findFirst().orElse(null);
        }

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `id` FROM `user` WHERE `id` = " + id)) {
                if (result.next()) {
                    CLocalClient cl = new CLocalClient(id, driver);
                    offline_cache.add(cl);

                    return cl;
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
     * Get an offline client
     *
     * @param uniqueId the client unique id
     * @return the client
     */
    @Override
    public LocalNetworkClient getOfflinePlayer(final UUID uniqueId) {
        if (offline_cache.stream().anyMatch((offline) -> offline.uniqueId().equals(uniqueId))) {
            return offline_cache.stream().filter((offline) -> offline.uniqueId().equals(uniqueId)).findFirst().orElse(null);
        }

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `id` FROM `user` WHERE `uuid` = '" + uniqueId + "'")) {
                if (result.next()) {
                    int id = result.getInt("id");

                    CLocalClient cl = new CLocalClient(id, driver);
                    offline_cache.add(cl);

                    return cl;
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
     * Get a server
     *
     * @param id the server id
     * @return the server
     */
    @Override
    public NetworkServer getServer(final int id) {
        return servers.stream().filter((server) -> server.id() == id).findFirst().orElse(null);
    }

    /**
     * Get a server
     *
     * @param name the server name
     * @return the server
     */
    @Override
    public NetworkServer getServer(final String name) {
        return servers.stream().filter((server) -> server.name().equals(name)).findFirst().orElse(null);
    }

    /**
     * Get all the online players
     *
     * @return the online players
     */
    @Override
    public Collection<NetworkClient> getOnlinePlayers() {
        List<NetworkClient> entities = new ArrayList<>();
        clients.forEach((cl) -> {
            if (cl.online()) entities.add(cl.client());
        });

        return entities;
    }

    /**
     * Get all the players
     *
     * @return all the players
     */
    @Override
    public Collection<LocalNetworkClient> getPlayers() {
        List<LocalNetworkClient> offline = new ArrayList<>(offline_cache);

        StringBuilder idIgnorer = new StringBuilder();
        for (LocalNetworkClient client : offline) {
            idIgnorer.append(client.id()).append(",");
        }
        String not_in = StringUtils.replaceLast(idIgnorer.toString(), ",", "");

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `id` FROM `user` WHERE `id` NOT IN (" + not_in + ")")) {
                while (result.next()) {
                    int id = result.getInt("id");
                    if (!result.wasNull()) {
                        CLocalClient cl = new CLocalClient(id, driver);
                        offline_cache.add(cl);
                        offline.add(cl);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }

        return offline;
    }

    /**
     * Get all the servers
     *
     * @return the servers
     */
    @Override
    public Collection<NetworkServer> getServers() {
        return new ArrayList<>(servers);
    }
}
