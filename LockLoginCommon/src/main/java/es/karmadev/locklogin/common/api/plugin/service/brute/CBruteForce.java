package es.karmadev.locklogin.common.api.plugin.service.brute;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.security.brute.BruteForceService;
import es.karmadev.locklogin.common.api.SQLiteDriver;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.errors.IllegalTimerAccess;
import ml.karmaconfigs.api.common.timer.scheduler.errors.TimerNotFound;

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

@SuppressWarnings("unused")
public class CBruteForce implements BruteForceService {

    private final SQLiteDriver driver;
    private final Map<String, Integer> schedulers = new ConcurrentHashMap<>();


    public CBruteForce(final SQLiteDriver driver) {
        this.driver = driver;
    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "bruteforce";
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

                        KarmaSource source = (KarmaSource) plugin.plugin();
                        SimpleScheduler scheduler = new SourceScheduler(source, time, SchedulerUnit.MINUTE, false)
                                .endAction(() -> markUnblocked(address))
                                .cancelAction((t) -> markUnblocked(address));

                        scheduler.start();
                        schedulers.put(address.getHostAddress(), scheduler.getId());
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

        Integer id = schedulers.remove(address.getHostAddress());
        if (id != null) {
            try {
                KarmaSource source = (KarmaSource) plugin.plugin();
                SimpleScheduler scheduler = new SourceScheduler(source, id);
                scheduler.cancel();
            } catch (TimerNotFound | IllegalTimerAccess ex) {
                plugin.log(ex, "Failed to cancel block scheduler");
            }
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
        Integer id = schedulers.getOrDefault(address.getHostAddress(), null);

        if (id != null) {
            try {
                SimpleScheduler scheduler = new SourceScheduler((KarmaSource) plugin.plugin(), id);
                return scheduler.getTime(SchedulerUnit.SECOND);
            } catch (TimerNotFound | IllegalTimerAccess ex) {
                plugin.log(ex, "Failed to access address timer {0}", address.getHostAddress());
            }
        }

        return -1;
    }

    /**
     * Get if the client is panicking
     *
     * @param client the client
     * @return if panicking
     */
    @Override
    public boolean isPanicking(final LocalNetworkClient client) {
        return false;
    }

    /**
     * Save the current brute force
     * status, to be loaded in the next
     * server start
     */
    @Override
    public boolean saveStatus() {
        return false;
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
