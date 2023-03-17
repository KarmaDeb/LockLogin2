package es.karmadev.locklogin.common.api.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import ml.karmaconfigs.api.common.data.path.PathUtilities;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL Pooling
 */
public final class SQLiteDriver {

    private HikariDataSource source;

    /**
     * Test if the environment is ready to run SQL
     */
    public void testDrivers() {
        testDrivers("MongoDB", "", "");
    }

    public void connect() {
        LockLogin plugin = CurrentPlugin.getPlugin();

        Path sql_file = plugin.workingDirectory().resolve("data").resolve("accounts.db");
        PathUtilities.create(sql_file);

        HikariConfig config = new HikariConfig();
        config.setPoolName("locklogin");
        config.setDriverClassName("org");
        config.setJdbcUrl("jdbc:sqlite:" + sql_file);
        config.setMinimumIdle(10);
        config.setMaximumPoolSize(50);
        config.setIdleTimeout(300 * 1000L);
        config.setConnectionTimeout(60000L);
        config.setConnectionTestQuery("SELECT 1");
        config.setLeakDetectionThreshold(6000000L);

        source = new HikariDataSource(config);
        plugin.info("Initialized LockLogin sqlite connection successfully");

        Connection connection = null;
        Statement statement = null;
        try {
            connection = source.getConnection();
            statement = connection.createStatement();

            boolean acc_create = statement.execute("CREATE TABLE IF NOT EXISTS `account` ('id' INTEGER NOT NULL, 'password' TEXT, 'pin' TEXT, '2fa_token' TEXT, 'panic' TEXT, '2fa' BOOLEAN, 'created_at' NUMERIC, PRIMARY KEY('id' AUTOINCREMENT))");
            boolean sess_create = statement.execute("CREATE TABLE IF NOT EXISTS `session` ('id' INTEGER NOT NULL, 'captcha_login' BOOLEAN, 'pass_login' BOOLEAN, 'pin_login' BOOLEAN, '2fa_login' BOOLEAN, 'persistence' BOOLEAN, 'captcha' TEXT, 'created_at' NUMERIC, PRIMARY KEY('id' AUTOINCREMENT))");
            boolean serv_create = statement.execute("CREATE TABLE IF NOT EXISTS `server` ('id' INTEGER NOT NULL, 'name' TEXT, 'address' TEXT, 'port' INTEGER, 'created_at' NUMERIC, PRIMARY KEY('id' AUTOINCREMENT))");
            boolean user_create = statement.execute("CREATE TABLE IF NOT EXISTS `user` ('id' INTEGER NOT NULL, 'name' TEXT, 'uuid' TEXT, 'account_id' INTEGER NULL, 'session_id' INTEGER NULL, 'type' INTEGER DEFAULT 1, 'last_server' INTEGER NULL, 'previous_server' INTEGER NULL, 'created_at' NUMERIC, PRIMARY KEY('id' AUTOINCREMENT), FOREIGN KEY('account_id') REFERENCES account('id') ON DELETE SET NULL, FOREIGN KEY('session_id') REFERENCES session('id') ON DELETE SET NULL, FOREIGN KEY('last_server') REFERENCES server('id') ON DELETE SET NULL, FOREIGN KEY('previous_server') REFERENCES server('id') ON DELETE SET NULL)");
            boolean premium_create = statement.execute("CREATE TABLE IF NOT EXISTS `premium` ('id' INTEGER NOT NULL, 'name' TEXT, 'uuid' TEXT, PRIMARY KEY('id' AUTOINCREMENT))");
            boolean panic_create = statement.execute("CREATE TABLE IF NOT EXISTS `panic` ('id' INTEGER NOT NULL, 'player' INTEGER NOT NULL, 'status' BOOLEAN DEFAULT false, PRIMARY KEY('id' AUTOINCREMENT), FOREIGN KEY('player') REFERENCES user('id') ON DELETE SET NULL)");
            boolean brute_create = statement.execute("CREATE TABLE IF NOT EXISTS `brute` ('id' INTEGER NOT NULL, 'address' TEXT NOT NULL, 'tries' INTEGER DEFAULT 0, 'blocked' BOOLEAN DEFAULT false, 'remaining' NUMERIC DEFAULT 0, PRIMARY KEY('id' AUTOINCREMENT))");

            if (acc_create && sess_create && serv_create && user_create && premium_create && panic_create && brute_create) {
                plugin.info("Successfully setup LockLogin sqlite tables");
            } else {
                plugin.info("An error occurred while running SQL queries. Some tables were not able to be created");

                if (!acc_create) {
                    plugin.err("Failed to create accounts table");
                    plugin.logErr("Couldn't create table: account");
                }
                if (!sess_create) {
                    plugin.err("Failed to create sessions table");
                    plugin.logErr("Couldn't create table: session");
                }
                if (!serv_create) {
                    plugin.err("Failed to create servers table");
                    plugin.logErr("Couldn't create table: server");
                }
                if (!user_create) {
                    plugin.err("Failed to create users table");
                    plugin.logErr("Couldn't create table: user");
                }
                if (!premium_create) {
                    plugin.err("Failed to create premiums table");
                    plugin.logErr("Couldn't create table: premium");
                }
                if (panic_create) {
                    plugin.err("Failed to create panic mode table");
                    plugin.logErr("Couldn't create table: panic");
                }
                if (!brute_create) {
                    plugin.err("Failed to create brute force table");
                    plugin.logErr("Couldn't create table: brute");
                }
            }
        } catch (SQLException ex) {
            plugin.log(ex, "An exception has raised when setting up LockLogin sqlite database");
            plugin.err("Failed to setup LockLogin sqlite connection");
        } finally {
            close(connection, statement);
        }
    }

    /**
     * Retrieve a new connection
     *
     * @return the connection to retrieve
     * @throws SQLException as part of {@link HikariDataSource#getConnection()}
     */
    public Connection retrieve() throws SQLException {
        return source.getConnection();
    }

    /**
     * Closes an statement and its connection
     *
     * @param connection the connection to close
     * @param statement the statement to close
     */
    public void close(final Connection connection, final Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (Throwable ignored) {}
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable ignored) {}
        }
    }

    private void testDrivers(final String name, final String testClass, final String downloadURL) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.info("Verifying for {0} driver", name);

        try {
            Class.forName(testClass);
            plugin.info("Driver {0} found!", name);
        } catch (ClassNotFoundException ex) {

        }
    }
}
