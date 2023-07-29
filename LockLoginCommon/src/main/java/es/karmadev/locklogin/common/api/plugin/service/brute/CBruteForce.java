package es.karmadev.locklogin.common.api.plugin.service.brute;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.schedule.runner.async.AsyncTaskExecutor;
import es.karmadev.api.schedule.runner.event.TaskEvent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.api.security.brute.BruteForceService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class CBruteForce implements BruteForceService {

    private final DataDriver driver;
    private final Map<String, Long> schedulers = new ConcurrentHashMap<>();

    private boolean loaded = false;

    public CBruteForce(final DataDriver driver) {
        this.driver = driver;
    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "BruteForce";
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
        return false;
    }

    /**
     * Success authentication request
     *
     * @param address the address that performed
     *                the success request
     */
    @Override
    public void success(final InetAddress address) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            String rawAddress = address.getHostAddress();
            try (ResultSet result = statement.executeQuery("SELECT `id` FROM `brute` WHERE `address` = '" + rawAddress + "'")) {
                if (result.next()) {
                    int id = result.getInt("id");

                    if (!result.wasNull()) {
                        driver.close(null, statement);
                        statement = connection.createStatement();

                        statement.executeUpdate("UPDATE `brute` SET `tries` = 0, SET `blocked` = fase, SET `remaining` = 0 WHERE `id` = " + id);
                        return;
                    }
                }

                driver.close(null, statement);
                statement = connection.createStatement();
                statement.execute("INSERT INTO `brute` (`address`) VALUES ('" + rawAddress + "')");
            }
        } catch (SQLException ex) {
            LockLogin plugin = CurrentPlugin.getPlugin();
            plugin.log(ex, "Failed to pass success auth request in address {0}", address.getHostAddress());
        } finally {
            driver.close(connection, statement);
        }
    }

    /**
     * Fail authentication request
     *
     * @param address the address that performed
     *                the failed request
     */
    @Override
    public void fail(final InetAddress address) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            String rawAddress = address.getHostAddress();
            try (ResultSet result = statement.executeQuery("SELECT `id`,`tries` FROM `brute` WHERE `address` = '" + rawAddress + "'")) {
                if (result.next()) {
                    int id = result.getInt("id");
                    if (!result.wasNull()) {
                        int tries = result.getInt("tries");

                        if (!result.wasNull()) {
                            driver.close(null, statement);
                            statement = connection.createStatement();

                            statement.executeUpdate("UPDATE `brute` SET `tries` = " + (tries + 1) + " WHERE `id` = " + id);
                            return;
                        }
                    }
                }

                driver.close(null, statement);
                statement = connection.createStatement();
                statement.execute("INSERT INTO `brute` (`address`,`tries`) VALUES ('" + rawAddress + "'," + 1 + ")");
            }
        } catch (SQLException ex) {
            LockLogin plugin = CurrentPlugin.getPlugin();
            plugin.log(ex, "Failed to pass failed auth request in address {0}", address.getHostAddress());
        } finally {
            driver.close(connection, statement);
        }
    }

    /**
     * Block temporally the address
     *
     * @param address the addres to block
     * @param time    the block time
     */
    @Override
    public void block(final InetAddress address, final long time) {
        LockLogin plugin = CurrentPlugin.getPlugin();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            String rawAddress = address.getHostAddress();
            try (ResultSet result = statement.executeQuery("SELECT `id` FROM `brute` WHERE `address` = '" + rawAddress + "'")) {
                if (result.next()) {
                    int id = result.getInt("id");
                    if (!result.wasNull()) {
                        driver.close(null, statement);
                        statement = connection.createStatement();

                        statement.executeUpdate("UPDATE `brute` SET `blocked` = true WHERE `id` = " + id);

                        AsyncTaskExecutor executor = new AsyncTaskExecutor(time, TimeUnit.MINUTES);
                        executor.setRepeating(false);

                        executor.on(TaskEvent.END, () -> markUnblocked(address));
                        executor.on(TaskEvent.STOP, () -> markUnblocked(address));

                        executor.start();
                        schedulers.put(address.getHostAddress(), executor.id());
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.log(ex, "Failed to mark address {0} block status", address.getHostAddress());
        } finally {
            driver.close(connection, statement);
        }
    }

    /**
     * Unblock the address
     *
     * @param address the address to unblock
     */
    @Override
    public void unblock(final InetAddress address) {
        LockLogin plugin = CurrentPlugin.getPlugin();

        long id = schedulers.remove(address.getHostAddress());
        AsyncTaskExecutor executor = AsyncTaskExecutor.getExecutor(id);
        if (executor != null) {
            executor.stop();
        }
    }

    /**
     * Get the blocked addresses
     *
     * @return the blocked addresses
     */
    @Override
    public InetAddress[] blocked() {
        List<InetAddress> addresses = new ArrayList<>();

        Set<String> keys = schedulers.keySet();
        keys.forEach((key) -> {
            try {
                InetAddress address = InetAddress.getByName(key);
                addresses.add(address);
            } catch (UnknownHostException ignored) {}
        });

        return addresses.toArray(new InetAddress[0]);
    }

    /**
     * Toggle the panic mode on the client
     *
     * @param client the client
     * @param status the panic mode status
     */
    @Override
    public void togglePanic(final LocalNetworkClient client, final boolean status) {
        LockLogin plugin = CurrentPlugin.getPlugin();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `id` FROM `panic` WHERE `player` = " + client.id())) {
                if (result.next()) {
                    int id = result.getInt("id");
                    if (!result.wasNull()) {
                        driver.close(null, statement);
                        statement = connection.createStatement();

                        statement.executeUpdate("UPDATE `panic` SET `status` = " + status + " WHERE `id` = " + id);
                        return;
                    }
                }

                driver.close(null, statement);
                statement = connection.createStatement();

                statement.execute("INSERT INTO `panic` (`player`,`status`) VALUES (" + client.id() + ", " + status + ")");
            }
        } catch (SQLException ex) {
            plugin.log(ex, "Failed to mark panic status for client {0}", client.id());
        } finally {
            driver.close(connection, statement);
        }
    }

    /**
     * Get the authentication tries of
     * an address
     *
     * @param address the address
     * @return the address authentication requests
     */
    @Override
    public int tries(final InetAddress address) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            String rawAddress = address.getHostAddress();
            try (ResultSet result = statement.executeQuery("SELECT `tries` FROM `brute` WHERE `address` = '" + rawAddress + "'")) {
                if (result.next()) {
                    int tries = result.getInt("tries");
                    if (!result.wasNull()) {
                        return tries;
                    }
                }
            }
        } catch (SQLException ex) {
            LockLogin plugin = CurrentPlugin.getPlugin();
            plugin.log(ex, "Failed to request address {0} authentication tries", address.getHostAddress());
        } finally {
            driver.close(connection, statement);
        }

        return -1;
    }

    /**
     * Get the address temporal ban time left
     *
     * @param address the address
     * @return the address ban time left
     */
    @Override
    public long banTimeLeft(final InetAddress address) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        long id = schedulers.getOrDefault(address.getHostAddress(), null);

        AsyncTaskExecutor executor = AsyncTaskExecutor.getExecutor(id);
        if (executor == null) return -1;

        return executor.timeLeft();
    }

    /**
     * Get if the client is panicking
     *
     * @param client the client
     * @return if panicking
     */
    @Override
    public boolean isPanicking(final LocalNetworkClient client) {
        LockLogin plugin = CurrentPlugin.getPlugin();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `status` FROM `panic` WHERE `player` = " + client.id())) {
                if (result.next()) {
                    boolean status = result.getBoolean("status");
                    if (!result.wasNull()) {
                        return status;
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.log(ex, "Failed to check panic status for client {0}", client.id());
        } finally {
            driver.close(connection, statement);
        }

        return false;
    }

    /**
     * Get if the address is blocked
     *
     * @param address the address
     * @return if the address is blocked
     */
    @Override
    public boolean isBlocked(final InetAddress address) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            String rawAddress = address.getHostAddress();
            try (ResultSet result = statement.executeQuery("SELECT `blocked` FROM `brute` WHERE `address` = '" + rawAddress + "'")) {
                if (result.next()) {
                    boolean blocked = result.getBoolean("blocked");
                    if (!result.wasNull()) {
                        return blocked;
                    }
                }
            }
        } catch (SQLException ex) {
            LockLogin plugin = CurrentPlugin.getPlugin();
            plugin.log(ex, "Failed to request address {0} connection block status", address.getHostAddress());
        } finally {
            driver.close(connection, statement);
        }

        return false;
    }

    /**
     * Save the current brute force
     * status, to be loaded in the next
     * server start
     */
    @Override
    public void saveStatus() {
        LockLogin plugin = CurrentPlugin.getPlugin();

        for (String address : schedulers.keySet()) {
            long id = schedulers.getOrDefault(address, null);
            AsyncTaskExecutor scheduler = AsyncTaskExecutor.getExecutor(id);
            if (scheduler == null) return;

            Connection connection = null;
            Statement statement = null;
            try {
                connection = driver.retrieve();
                statement = connection.createStatement();

                try (ResultSet result = statement.executeQuery("SELECT `id` FROM `brute` WHERE `address` = '" + address + "'")) {
                    if (result.next()) {
                        int brute_id = result.getInt("id");
                        if (!result.wasNull()) {
                            driver.close(null, statement);
                            statement = connection.createStatement();

                            statement.executeUpdate("UPDATE `brute` SET `remaining` = " + scheduler.timeLeft() + " WHERE `id` = " + brute_id);
                        }
                    }
                }
            } catch (SQLException ex) {
                plugin.log(ex, "Failed to store address {0} brute force data", address);
            } finally {
                driver.close(connection, statement);
            }
        }
    }

    /**
     * Load the brute force status
     */
    @Override
    public void loadStatus() {
        if (loaded) return;
        loaded = true;

        LockLogin plugin = CurrentPlugin.getPlugin();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `id`,`address`,`blocked`,`remaining` FROM `brute`")) {
                while (result.next()) {
                    int id = result.getInt("id");
                    if (result.wasNull()) continue;

                    String address = result.getString("address");
                    if (ObjectUtils.isNullOrEmpty(address)) continue;

                    boolean blocked = result.getBoolean("blocked");
                    if (result.wasNull()) continue;

                    long remaining = result.getLong("remaining");
                    if (result.wasNull()) continue;

                    if (blocked) {
                        try {
                            InetAddress inet = InetAddress.getByName(address);
                            AsyncTaskExecutor executor = new AsyncTaskExecutor(remaining, TimeUnit.MINUTES);
                            executor.on(TaskEvent.END, () -> markUnblocked(inet));
                            executor.on(TaskEvent.STOP, () -> markUnblocked(inet));

                            schedulers.put(address, executor.id());
                        } catch (UnknownHostException ignored) {}
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.log(ex, "Failed to load stored brute force data");
            loaded = false;
        } finally {
            driver.close(connection, statement);
        }
    }

    /**
     * Mark an address as unblocked, internally
     *
     * @param address the address to mark as
     *                no longer blocked
     */
    private void markUnblocked(final InetAddress address) {
        LockLogin plugin = CurrentPlugin.getPlugin();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            String rawAddress = address.getHostAddress();
            try (ResultSet result = statement.executeQuery("SELECT `id` FROM `brute` WHERE `address` = '" + rawAddress + "'")) {
                if (result.next()) {
                    int id = result.getInt("id");
                    if (!result.wasNull()) {
                        driver.close(null, statement);
                        statement = connection.createStatement();

                        statement.executeUpdate("UPDATE `brute` SET `blocked` = false, `tries` = 0, `remaining` = 0 WHERE `id` = " + id);
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.log(ex, "Failed to mark address {0} block status", address.getHostAddress());
        } finally {
            driver.close(connection, statement);
        }
    }
}
