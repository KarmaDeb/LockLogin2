package es.karmadev.locklogin.common.server;

import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannel;
import es.karmadev.locklogin.common.server.channel.SChannel;
import es.karmadev.locklogin.common.SQLiteDriver;
import lombok.AllArgsConstructor;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Random;

@AllArgsConstructor
public class CServer implements NetworkServer {

    private final int id;
    private final SQLiteDriver pool;
    private final SChannel channel = new SChannel();

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
     * Get the server packet queue
     *
     * @return the server packet queue
     */
    @Override
    public NetworkChannel channel() {
        return channel;
    }
}
