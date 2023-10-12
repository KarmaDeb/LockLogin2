package es.karmadev.locklogin.common.api.client;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.plugin.CacheElement;
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
    protected final SQLDriver engine;

    public Function<String, Boolean> hasPermission;

    private final CacheElement<UserSession> session = new CacheElement<>();
    private final CacheElement<UserAccount> account = new CacheElement<>();

    public CLocalClient(final int id, final SQLDriver engine) {
        this.id = id;
        this.engine = engine;
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            QueryBuilder builder = QueryBuilder.createQuery()
                    .select(Table.USER, Row.NAME)
                    .where(Row.ID, "=", id);

            try (ResultSet result = statement.executeQuery(builder.build(""))) {
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

            QueryBuilder builder = QueryBuilder.createQuery()
                    .select(Table.USER, Row.ADDRESS, Row.PORT)
                    .where(Row.ID, "=", id);

            try (ResultSet result = statement.executeQuery(builder.build(""))) {
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

            QueryBuilder builder = QueryBuilder.createQuery()
                    .select(Table.USER, Row.CREATED_AT)
                    .where(Row.ID, "=", id);

            try (ResultSet result = statement.executeQuery(builder.build(""))) {
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
     * @return if the entity has the permission
     */
    @Override
    public boolean hasPermission(final PermissionObject permission) {
        return hasPermission != null && hasPermission.apply(permission.node());
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            QueryBuilder setAddress = QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.ADDRESS, address.getHostString())
                    .set(Row.PORT, address.getPort())
                    .where(Row.ID, "=", id);

            statement.executeUpdate(setAddress.build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection ,statement);
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            QueryBuilder setName = QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.NAME, name)
                    .where(Row.ID, "=", id);

            statement.executeUpdate(setName.build(""));
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            QueryBuilder builder = QueryBuilder.createQuery()
                    .select(Table.USER, Row.UUID)
                    .where(Row.ID, "=", id);

            try (ResultSet result = statement.executeQuery(builder.build(""))) {
                if (result.next()) {
                    return UUID.fromString(result.getString(1));
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
     * Get the connection online-mode unique identifier
     *
     * @return the connection online-mode unique
     * identifier
     */
    @Override
    public UUID onlineId() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            QueryBuilder builder = QueryBuilder.createQuery()
                    .select(Table.USER, Row.PREMIUM_UUID)
                    .where(Row.ID, "=", id);

            try (ResultSet result = statement.executeQuery(builder.build(""))) {
                if (result.next()) {
                    return UUID.fromString(result.getString(1));
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
     * Update the client unique ID
     *
     * @param id the client unique ID
     */
    @Override
    public void setUniqueId(final UUID id) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(QueryBuilder.createQuery().update(Table.USER)
                    .set(Row.UUID, id).where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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
            connection = engine.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.CONNECTION_TYPE)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build())) {

                if (result.next()) {
                    int type = result.getInt(1);
                    if (!result.wasNull()) {
                        return ConnectionType.byId(type);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.CONNECTION_TYPE, type.id())
                    .where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection ,statement);
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
        NetworkClient online = CurrentPlugin.getPlugin().network().getPlayer(id);
        if (online == null) {
            online = new COnlineClient(id, engine, null);
            CPluginNetwork network = (CPluginNetwork) CurrentPlugin.getPlugin().network();
            network.appendClient(online);
        }

        return online;
    }

    /**
     * Get the client previous server
     *
     * @return the client previous server
     */
    @Override
    public NetworkServer previousServer() {
        LockLogin plugin = CurrentPlugin.getPlugin();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.PREV_SERVER)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                if (result.next()) {
                    int server_id = result.getInt(1);
                    if (!result.wasNull()) {
                        CPluginNetwork network = (CPluginNetwork) plugin.network();

                        NetworkServer server = network.getServer(server_id);
                        if (server == null) {
                            server = new CServer(server_id, engine);
                            network.appendServer(server);
                        }

                        return server;
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

    /**
     * Get the client last server
     *
     * @return the last server
     */
    @Override
    public NetworkServer server() {
        LockLogin plugin = CurrentPlugin.getPlugin();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.LAST_SERVER)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                if (result.next()) {
                    int server_id = result.getInt(1);
                    if (!result.wasNull()) {
                        CPluginNetwork network = (CPluginNetwork) plugin.network();

                        NetworkServer server = network.getServer(server_id);
                        if (server == null) {
                            server = new CServer(server_id, engine);
                            network.appendServer(server);
                        }

                        return server;
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

    /**
     * Get the client account
     *
     * @return the client account
     */
    @Override
    public UserAccount account() {
        return account.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.USER, Row.ACCOUNT_ID)
                        .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                    if (result.next()) {
                        int account_id = result.getInt(1);
                        return new CAccount(id, account_id, engine);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            AccountFactory<UserAccount> factory = CurrentPlugin.getPlugin().getAccountFactory(false);
            return factory.create(this);
        });
    }

    /**
     * Get the client session
     *
     * @return the client session
     */
    @Override
    public UserSession session() {
        return session.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.USER, Row.SESSION_ID)
                        .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                    if (result.next()) {
                        int session_id = result.getInt(1);
                        if (!result.wasNull()) {
                            return new CSession(id, session_id, engine);
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            SessionFactory<UserSession> factory = CurrentPlugin.getPlugin().getSessionFactory(false);
            return factory.create(this);
        });
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.LAST_SERVER, server.id())
                    .where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection ,statement);
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.PREV_SERVER, server.id())
                    .where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection ,statement);
        }
    }

    /**
     * Get if the client is op
     *
     * @return if the client has op
     */
    @Override
    public boolean isOp() {
        return hasPermission != null && hasPermission.apply("op");
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

    /**
     * Reset the cache, implementations
     * should interpreter null as "everything"
     *
     * @param name the cache name to reset
     */
    @Override
    public void reset(final String name) {
        if (name == null) {
            session.assign(null);
            account.assign(null);
            return;
        }

        switch (name.toLowerCase()) {
            case "session":
                session.assign(null);
                break;
            case "account":
                account.assign(null);
                break;
        }
    }
}
