package es.karmadev.locklogin.common.api;

import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.common.api.client.CLocalClient;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CPluginNetwork implements PluginNetwork {

    private final SQLDriver engine;
    private final Set<NetworkClient> clients = ConcurrentHashMap.newKeySet();
    private final Set<NetworkServer> servers = ConcurrentHashMap.newKeySet();
    private final Set<LocalNetworkClient> offline_cache = ConcurrentHashMap.newKeySet();

    public CPluginNetwork(final SQLDriver engine) {
        this.engine = engine;

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
     * Disconnect a client
     *
     * @param client the client to disconnect
     */
    public void disconnectClient(final NetworkClient client) {
        clients.remove(client);
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            QueryBuilder builder = QueryBuilder.createQuery()
                    .select(Table.USER, Row.ID).where(Row.ID, "=", id);

            try (ResultSet result = statement.executeQuery(builder.build(""))) {
                if (result.next()) {
                    CLocalClient cl = new CLocalClient(id, engine);
                    offline_cache.add(cl);

                    return cl;
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
     * Get an offline client
     *
     * @param uniqueId the client unique id
     * @return the client
     */
    @Override
    public LocalNetworkClient getOfflinePlayer(final UUID uniqueId) {
        if (offline_cache.stream().anyMatch((offline) -> uniqueId.equals(offline.uniqueId()) || uniqueId.equals(offline.onlineId()))) {
            return offline_cache.stream().filter(
                    (offline) -> offline != null && (uniqueId.equals(offline.uniqueId()) || uniqueId.equals(offline.onlineId())))
                    .findAny()
                    .orElse(null);
        }

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            QueryBuilder builder = QueryBuilder.createQuery()
                    .select(Table.USER, Row.ID)
                    .where(Row.UUID, "=", uniqueId).or()
                    .where(Row.PREMIUM_UUID, "=", uniqueId);

            try (ResultSet result = statement.executeQuery(builder.build(""))) {
                if (result.next()) {
                    int id = result.getInt(1);

                    CLocalClient cl = new CLocalClient(id, engine);
                    offline_cache.add(cl);

                    return cl;
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

        List<Integer> ids = new ArrayList<>();
        for (LocalNetworkClient client : offline) {
            ids.add(client.id());
        }

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            QueryBuilder builder = QueryBuilder.createQuery()
                    .select(Table.USER, Row.ID)
                    .where(Row.ID, QueryBuilder.NOT_IN(ids));

            try (ResultSet result = statement.executeQuery(builder.build(""))) {
                while (result.next()) {
                    int id = result.getInt(1);
                    if (!result.wasNull()) {
                        CLocalClient cl = new CLocalClient(id, engine);
                        offline_cache.add(cl);
                        offline.add(cl);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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
