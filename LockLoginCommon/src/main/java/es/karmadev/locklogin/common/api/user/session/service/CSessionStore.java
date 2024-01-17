package es.karmadev.locklogin.common.api.user.session.service;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.Cached;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.CacheContainer;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.api.user.session.service.SessionCache;
import es.karmadev.locklogin.api.user.session.service.SessionStoreService;
import es.karmadev.locklogin.common.api.plugin.CacheElement;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class CSessionStore implements SessionStoreService, Cached {

    boolean grantedThroughServiceProvider = false;
    private final SQLDriver driver;

    private final static ConcurrentMap<InetSocketAddress, CacheContainer<SessionCache>> sessions = new ConcurrentHashMap<>();

    CSessionStore(final SQLDriver driver) {
        this.driver = driver;
    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "session store";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return true;
    }

    /**
     * Get the session of an address
     *
     * @param address the address
     * @return the session
     */
    @Override
    public SessionCache getSession(final InetSocketAddress address) {
        if (!grantedThroughServiceProvider) return null;
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot get a session with a null LockLogin instance");

        Configuration configuration = plugin.configuration();
        if (!configuration.session().enable()) return null;

        int timeout = configuration.session().timeout();
        long exp = Math.max(0, TimeUnit.MINUTES.toMillis(timeout));

        CacheContainer<SessionCache> cacheElement = sessions.computeIfAbsent(address, (c) -> new CacheElement<>(timeout, TimeUnit.MINUTES));

        return cacheElement.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = driver.retrieve();
                statement = connection.createStatement();

                String query = QueryBuilder.createQuery(driver.getDriver())
                        .select(Table.SESSION_STORE, Row.USER_ID, Row.LOGIN_PASSWORD, Row.LOGIN_TOTP, Row.LOGIN_PIN, Row.CREATED_AT)
                        .where(Row.ADDRESS, QueryBuilder.EQUALS, address.getHostString()).build();

                try (ResultSet rs = statement.executeQuery(query)) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        boolean password = rs.getBoolean(2);
                        boolean totp = rs.getBoolean(3);
                        boolean pin = rs.getBoolean(4);
                        long creation = rs.getLong(5);

                        long now = System.currentTimeMillis();
                        if (exp > 0 && now > creation + exp) {
                            String deleteQuery = QueryBuilder.createQuery(driver.getDriver())
                                    .delete(Table.SESSION_STORE).where(Row.USER_ID, QueryBuilder.EQUALS, id).build();
                            statement.execute(deleteQuery);

                            return null; //Expired
                        }

                        LocalNetworkClient client = plugin.network().getOfflinePlayer(id);
                        if (client == null) {
                            throw new IllegalStateException("Unexpected client value from client id: " + id);
                        }

                        return new CSessionCache(client, address, password, totp, pin);
                    }
                }
            } catch (SQLException ex) {
                plugin.log(ex, "Failed to retrieve session store for {0}", address);
            } finally {
                driver.close(connection, statement);
            }

            return null;
        });
    }

    /**
     * Save the session of a client
     *
     * @param client the client to save
     *               session for
     */
    @Override
    public void saveSession(final LocalNetworkClient client) {
        if (!grantedThroughServiceProvider) return;
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot save a session with a null LockLogin instance");

        Configuration configuration = plugin.configuration();
        if (!configuration.session().enable()) return;

        int timeout = configuration.session().timeout();
        CacheContainer<SessionCache> cacheElement = sessions.computeIfAbsent(client.address(), (c) -> new CacheElement<>(timeout, TimeUnit.MINUTES));

        UserSession session = client.session();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            String query = QueryBuilder.createQuery(driver.getDriver())
                    .select(Table.SESSION_STORE, Row.ID)
                    .where(Row.USER_ID, QueryBuilder.EQUALS, client.id()).build();

            try (ResultSet rs = statement.executeQuery(query)) {
                SessionCache cache = new CSessionCache(client, client.address(), session.isLogged(), session.isTotpLogged(), session.isPinLogged());
                if (rs.next()) {
                    int id = rs.getInt(1);
                    String updateQuery = QueryBuilder.createQuery(driver.getDriver())
                            .update(Table.SESSION_STORE)
                            .set(Row.ADDRESS, client.address().getHostString())
                            .set(Row.LOGIN_PASSWORD, session.isLogged())
                            .set(Row.LOGIN_TOTP, session.isTotpLogged())
                            .set(Row.LOGIN_PIN, session.isPinLogged())
                            .where(Row.ID, QueryBuilder.EQUALS, id).build();

                    statement.executeUpdate(updateQuery);
                } else {
                    String insertQuery = QueryBuilder.createQuery(driver.getDriver())
                            .insert(Table.SESSION_STORE, Row.USER_ID, Row.ADDRESS, Row.LOGIN_PASSWORD, Row.LOGIN_TOTP, Row.LOGIN_PIN)
                            .values(client.id(), client.address().getHostString(), session.isLogged(), session.isTotpLogged(), session.isPinLogged()).build();

                    statement.execute(insertQuery);
                }

                cacheElement.assign(cache);
            }
        } catch (SQLException ex) {
            plugin.log(ex, "Failed to retrieve session store for {0}", client.address());
        } finally {
            driver.close(connection, statement);
        }
    }

    /**
     * Reset the cache, implementations
     * should interpreter null as "everything"
     *
     * @param name the cache name to reset
     */
    @Override
    public void reset(final String name) {
        if (!grantedThroughServiceProvider) return;
        sessions.clear();
    }
}
