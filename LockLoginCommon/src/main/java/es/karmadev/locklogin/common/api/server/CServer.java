package es.karmadev.locklogin.common.api.server;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannel;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.common.api.server.channel.SChannel;
import es.karmadev.locklogin.common.util.ActionListener;
import es.karmadev.locklogin.common.util.action.ServerEntityAction;
import lombok.AllArgsConstructor;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor @SuppressWarnings("unused")
public class CServer implements NetworkServer {

    private final int id;
    private final DataDriver pool;
    private final SChannel channel = new SChannel();

    private final Set<LocalNetworkClient> offline_clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<NetworkClient> online_clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public final ActionListener<LocalNetworkClient> listener = ServerEntityAction.builder()
            .onClientConnect((client) -> {
                offline_clients.remove(client);

                //Any action performed on an offline client should remove its online client instance from a related object refference
                Stream<NetworkClient> filtered = online_clients.stream().filter((online) -> client.id() == client.id());
                filtered.collect(Collectors.toList()).forEach(online_clients::remove);
            })
            .onClientDisconnect((client) -> {
                offline_clients.remove(client);

                //Any action performed on an offline client should remove its online client instance from a related object refference
                Stream<NetworkClient> filtered = online_clients.stream().filter((online) -> client.id() == client.id());
                filtered.collect(Collectors.toList()).forEach(online_clients::remove);
            })
            .onOnlineConnect((client) -> {
                online_clients.add(client);

                //Any action performed on an online client should remove its offline client instance from a related object refference
                Stream<LocalNetworkClient> filtered = offline_clients.stream().filter((offline) -> offline.id() == client.id());
                filtered.collect(Collectors.toList()).forEach(offline_clients::remove);
            })
            .onOnlineDisconnect((client) -> {
                online_clients.remove(client);

                //Any action performed on an online client should remove its offline client instance from a related object refference
                Stream<LocalNetworkClient> filtered = offline_clients.stream().filter((offline) -> offline.id() == client.id());
                filtered.collect(Collectors.toList()).forEach(offline_clients::remove);
            }).build();

    /**
     * Get the entity name
     *
     * @return the entity name
     */
    @Override
    public String name() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `name` FROM `server` WHERE `id` = " + id)) {
                if (result.next()) {
                    return result.getString("name");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return null;
    }

    /**
     * Get the entity address
     *
     * @return the entity address
     */
    @Override
    public InetSocketAddress address() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `address`,`port` FROM `server` WHERE `id` = " + id)) {
                if (result.next()) {
                    String address = result.getString("address");
                    int port = result.getInt("port");
                    if (!result.wasNull()) {
                        if (address == null) address = "127.0.0.1";
                        return InetSocketAddress.createUnresolved(address, port);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return InetSocketAddress.createUnresolved("127.0.0.1", new Random().nextInt(65565));
    }

    /**
     * Get when the entity was created
     *
     * @return the entity creation date
     */
    @Override
    public Instant creation() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `created_at` FROM `server` WHERE `id` = " + id)) {
                if (result.next()) {
                    long millis = result.getLong("created_at");
                    return Instant.ofEpochMilli(millis);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return Instant.now();
    }

    /**
     * Get if this entity has the specified permission
     *
     * @param permission the permission
     * @return if the entity has the perimssion
     */
    @Override
    public boolean hasPermission(final PermissionObject permission) {
        return true;
    }

    /**
     * Get the server id
     *
     * @return the server id
     */
    @Override
    public int id() {
        return id;
    }

    /**
     * Update the server name
     *
     * @param name the server name
     */
    @Override
    public void setName(final String name) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `server` SET `name` = '" + name + "' WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Update the server address
     *
     * @param address the server new address
     */
    @Override
    public void setAddress(final InetSocketAddress address) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `server` SET `address` = '" + address.getHostString() + "' WHERE `id` = " + id);
            statement.executeUpdate("UPDATE `server` SET `port` = " + address.getPort() + " WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection ,statement);
        }
    }

    /**
     * Get all the clients that are connected
     * in this server
     *
     * @return all the connected clients
     */
    @Override
    public Collection<NetworkClient> connected() {
        return null;
    }

    /**
     * Get all the online clients that
     * are connected in this server
     *
     * @return all the connected clients
     */
    @Override
    public Collection<NetworkClient> onlineClients() {
        return null;
    }

    /**
     * Get the server packet queue
     *
     * @return the server packet queue
     */
    @Override
    public NetworkChannel channel() {
        return channel;
    }
}
