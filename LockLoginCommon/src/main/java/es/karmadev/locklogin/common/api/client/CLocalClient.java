package es.karmadev.locklogin.common.api.client;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.server.CServer;
import es.karmadev.locklogin.common.api.user.storage.account.CAccount;
import es.karmadev.locklogin.common.api.user.storage.session.CSession;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

public class CLocalClient implements LocalNetworkClient {

    protected final int id;
    protected final DataDriver pool;

    public Function<PermissionObject, Boolean> hasPermission;

    public CLocalClient(final int id, final DataDriver pool) {
        this.id = id;
        this.pool = pool;
    }

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
            try (ResultSet result = statement.executeQuery("SELECT `name` FROM `user` WHERE `id` = " + id)) {
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
            try (ResultSet result = statement.executeQuery("SELECT `address`,`port` FROM `user` WHERE `id` = " + id)) {
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
            try (ResultSet result = statement.executeQuery("SELECT `created_at` FROM `user` WHERE `id` = " + id)) {
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
        return hasPermission != null && hasPermission.apply(permission);
    }

    /**
     * Get the entity id
     *
     * @return the entity id
     */
    @Override
    public int id() {
        return id;
    }

    /**
     * Update the connection address
     *
     * @param address the connection address
     */
    @Override
    public void setAddress(final InetSocketAddress address) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `user` SET `address` = '" + address.getHostString() + "' WHERE `id` = " + id);
            statement.executeUpdate("UPDATE `user` SET `port` = " + address.getPort() + " WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection ,statement);
        }
    }

    /**
     * Set the connection name
     *
     * @param name the connection name
     */
    @Override
    public void setName(final String name) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `user` SET `name` = '" + name + "' WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the connection unique identifier
     *
     * @return the connection unique identifier
     */
    @Override
    public UUID uniqueId() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `uuid` FROM `user` WHERE `id` = " + id)) {
                if (result.next()) {
                    return UUID.fromString(result.getString("uuid"));
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
     * Update the client unique ID
     *
     * @param id the client unique ID
     */
    @Override
    public void setUniqueId(final UUID id) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `user` SET `uuid` = '" + id + "' WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the client connection
     *
     * @return the client connection type
     */
    @Override
    public ConnectionType connection() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `type` FROM `user` WHERE `id` = " + id)) {
                if (result.next()) {
                    int type = result.getInt("type");
                    if (!result.wasNull()) {
                        return ConnectionType.byId(type);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return ConnectionType.OFFLINE;
    }

    /**
     * Set the client connection type
     *
     * @param type the connection type
     */
    @Override
    public void setConnection(final ConnectionType type) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `user` SET `type` = " + type.id() + " WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection ,statement);
        }
    }

    /**
     * Get if the client is online
     *
     * @return if the client is online
     */
    @Override
    public boolean online() {
        return CurrentPlugin.getPlugin().network().getPlayer(id) != null;
    }

    /**
     * Get the network client
     *
     * @return the network client
     */
    @Override
    public NetworkClient client() {
        return CurrentPlugin.getPlugin().network().getPlayer(id);
    }

    /**
     * Get the client previous server
     *
     * @return the client previous server
     */
    @Override
    public NetworkServer previousServer() {
        Connection connection = null;
        Statement statement = null;
        LockLogin plugin = CurrentPlugin.getPlugin();

        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `previous_server` WHERE `id` = " + id)) {
                if (result.next()) {
                    int server_id = result.getInt("previous_server");
                    if (!result.wasNull()) {
                        CPluginNetwork network = (CPluginNetwork) plugin.network();

                        NetworkServer server = network.getServer(server_id);
                        if (server == null) {
                            server = new CServer(server_id, pool);
                            network.appendServer(server);
                        }

                        return server;
                    }
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
     * Get the client last server
     *
     * @return the last server
     */
    @Override
    public NetworkServer server() {
        Connection connection = null;
        Statement statement = null;
        LockLogin plugin = CurrentPlugin.getPlugin();

        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `last_server` FROM `user` WHERE `id` = " + id)) {
                if (result.next()) {
                    int server_id = result.getInt("last_server");
                    if (!result.wasNull()) {
                        CPluginNetwork network = (CPluginNetwork) plugin.network();

                        NetworkServer server = network.getServer(server_id);
                        if (server == null) {
                            server = new CServer(server_id, pool);
                            network.appendServer(server);
                        }

                        return server;
                    }
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
     * Get the client account
     *
     * @return the client account
     */
    @Override
    public UserAccount account() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `account_id` FROM `user` WHERE `id` = " + id)) {
                if (result.next()) {
                    int account_id = result.getInt("account_id");
                    return new CAccount(id, account_id, pool);
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
     * Get the client session
     *
     * @return the client session
     */
    @Override
    public UserSession session() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `session_id` FROM `user` WHERE `id` = " + id)) {
                if (result.next()) {
                    int session_id = result.getInt("session_id");
                    return new CSession(id, session_id, pool);
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
     * Set the client server
     *
     * @param server the server to set on
     *               If the client is online, we will move him
     *               to this server, otherwise he will join it
     *               when he joins the server
     */
    @Override
    public void setServer(final NetworkServer server) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `user` SET `last_server` = " + server.id() + " WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection ,statement);
        }
    }

    /**
     * Force the client previous server
     *
     * @param server the new previous server
     */
    @Override
    public void forcePreviousServer(final NetworkServer server) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `user` SET `previous_server` = " + server.id() + " WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection ,statement);
        }
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     * @apiNote In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * The string output is not necessarily stable over time or across
     * JVM invocations.
     * @implSpec The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode() + "[" + name() + ";" + uniqueId() + ":" + id + "]";
    }
}
