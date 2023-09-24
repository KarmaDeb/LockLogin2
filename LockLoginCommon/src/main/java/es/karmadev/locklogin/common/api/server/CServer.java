package es.karmadev.locklogin.common.api.server;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannel;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor @SuppressWarnings("unused")
public class CServer implements NetworkServer {

    private final int id;
    private final SQLDriver engine;
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
            connection = engine.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery(
                    QueryBuilder.createQuery()
                            .select(Table.SERVER, Row.NAME)
                            .where(Row.ID, QueryBuilder.EQUALS, id).build()
            )) {
                if (result.next()) {
                    return result.getString(1);
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
     * Get the entity address
     *
     * @return the entity address
     */
    @Override
    public InetSocketAddress address() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.SERVER, Row.ADDRESS, Row.PORT)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                if (result.next()) {
                    String address = result.getString(1);
                    int port = result.getInt(2);
                    if (!result.wasNull()) {
                        if (address == null) address = "127.0.0.1";
                        return InetSocketAddress.createUnresolved(address, port);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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
            connection = engine.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.SERVER, Row.CREATED_AT)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                if (result.next()) {
                    long millis = result.getLong(1);
                    return Instant.ofEpochMilli(millis);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.SERVER).set(Row.NAME, name)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.SERVER)
                    .set(Row.ADDRESS, address.getHostString())
                    .set(Row.PORT, address.getPort())
                    .where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection ,statement);
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
     * Get all the offline clients that
     * are connected in this server
     *
     * @return all the offline clients
     */
    @Override
    public Collection<LocalNetworkClient> offlineClients() {
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

    /**
     * When a packet is received
     *
     * @param packet the packet
     */
    @Override
    public void onReceive(IncomingPacket packet) {

    }

    /**
     * When a packet is sent
     *
     * @param packet the packet to send
     */
    @Override
    public void onSend(OutgoingPacket packet) {

    }

    /**
     * Send a message to the client
     *
     * @param message the message to send
     */
    @Override
    public void sendMessage(String message) {

    }

    /**
     * Send an actionbar to the client
     *
     * @param actionbar the actionbar to send
     */
    @Override
    public void sendActionBar(String actionbar) {

    }

    /**
     * Send a title to the client
     *
     * @param title    the title
     * @param subtitle the subtitle
     * @param fadeIn   the title fade in time
     * @param showTime the title show time
     * @param fadeOut  the title fade out time
     */
    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int showTime, int fadeOut) {

    }
}
