package es.karmadev.locklogin.common.api.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.api.plugin.database.Driver;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.RowType;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import ml.karmaconfigs.api.common.data.path.PathUtilities;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class CSQLDriver implements DataDriver {

    private HikariDataSource source;
    private boolean connected = false;

    private final Driver driver;

    public CSQLDriver(final Driver driver) {
        this.driver = driver;
    }

    /**
     * Connect to the driver
     */
    @Override
    public void connect() {
        if (!connected) {
            LockLogin plugin = CurrentPlugin.getPlugin();

            Path sql_file = plugin.workingDirectory().resolve("data").resolve("accounts.db");
            PathUtilities.create(sql_file);

            HikariConfig config = new HikariConfig();
            config.setPoolName("locklogin-" + driver.name());
            config.setDriverClassName(driver.getTestClass());
            config.setJdbcUrl(driver.getConnection(sql_file));
            config.setMinimumIdle(10);
            config.setMaximumPoolSize(50);
            config.setIdleTimeout(300 * 1000L);
            config.setConnectionTimeout(60000L);
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(6000000L);

            source = new HikariDataSource(config);
            plugin.info("Initialized LockLogin sqlite connection successfully");
            connected = true;

            Connection connection = null;
            Statement statement = null;
            try {
                connection = source.getConnection();
                statement = connection.createStatement();

                QueryBuilder accountCreteQuery = QueryBuilder.createQuery(driver)
                        .createTable(true, Table.ACCOUNT)
                        .withRow(Row.ID, RowType.INTEGER, QueryBuilder.NOT_NULL)
                        .withRow(Row.PASSWORD, RowType.LONGTEXT)
                        .withRow(Row.PIN, RowType.LONGTEXT)
                        .withRow(Row.TOKEN_2FA, RowType.VARCHAR, QueryModifier.of("(128)"))
                        .withRow(Row.PANIC, RowType.LONGTEXT)
                        .withRow(Row.STATUS_2FA, RowType.BOOLEAN)
                        .withRow(Row.CREATED_AT, RowType.TIMESTAMP, QueryBuilder.DEFAULT(QueryBuilder.CURRENT_TIMESTAMP(driver)))
                        .withPrimaryKey(Row.ID, true);

                QueryBuilder sessionCreateQuery = QueryBuilder.createQuery(driver)
                        .createTable(true, Table.SESSION)
                        .withRow(Row.ID, RowType.INTEGER, QueryBuilder.NOT_NULL)
                        .withRow(Row.LOGIN_CAPTCHA, RowType.BOOLEAN)
                        .withRow(Row.LOGIN_PASSWORD, RowType.BOOLEAN)
                        .withRow(Row.LOGIN_PIN, RowType.BOOLEAN)
                        .withRow(Row.LOGIN_2FA, RowType.BOOLEAN)
                        .withRow(Row.PERSISTENT, RowType.BOOLEAN)
                        .withRow(Row.CAPTCHA, RowType.VARCHAR, QueryModifier.of("(16)"))
                        .withRow(Row.CREATED_AT, RowType.TIMESTAMP, QueryBuilder.DEFAULT(QueryBuilder.CURRENT_TIMESTAMP(driver)))
                        .withPrimaryKey(Row.ID, true);

                QueryBuilder serverCreateQuery = QueryBuilder.createQuery(driver)
                        .createTable(true, Table.SERVER)
                        .withRow(Row.ID, RowType.INTEGER, QueryBuilder.NOT_NULL)
                        .withRow(Row.NAME, RowType.VARCHAR, QueryModifier.of("(32)"))
                        .withRow(Row.ADDRESS, RowType.VARCHAR, QueryModifier.of("(42)"))
                        .withRow(Row.PORT, RowType.INTEGER)
                        .withRow(Row.CREATED_AT, RowType.TIMESTAMP, QueryBuilder.DEFAULT(QueryBuilder.CURRENT_TIMESTAMP(driver)))
                        .withPrimaryKey(Row.ID, true);

                QueryBuilder userCreateQuery = QueryBuilder.createQuery(driver)
                        .createTable(true, Table.USER)
                        .withRow(Row.ID, RowType.INTEGER, QueryBuilder.NOT_NULL)
                        .withRow(Row.NAME, RowType.VARCHAR, QueryBuilder.NOT_NULL, QueryModifier.of("(16)"))
                        .withRow(Row.UUID, RowType.VARCHAR, QueryBuilder.NOT_NULL, QueryModifier.of("(36)"))
                        .withRow(Row.PREMIUM_UUID, RowType.VARCHAR, QueryModifier.of("(36)"), QueryBuilder.DEFAULT(QueryBuilder.NULL))
                        .withRow(Row.ACCOUNT_ID, RowType.INTEGER, QueryBuilder.DEFAULT(QueryBuilder.NULL))
                        .withRow(Row.SESSION_ID, RowType.INTEGER, QueryBuilder.DEFAULT(QueryBuilder.NULL))
                        .withRow(Row.STATUS, RowType.BOOLEAN, QueryBuilder.DEFAULT(QueryBuilder.FALSE))
                        .withRow(Row.CONNECTION_TYPE, RowType.INTEGER, QueryBuilder.DEFAULT(QueryBuilder.NUMBER(1)))
                        .withRow(Row.LAST_SERVER, RowType.INTEGER, QueryBuilder.DEFAULT(QueryBuilder.NULL))
                        .withRow(Row.PREV_SERVER, RowType.INTEGER, QueryBuilder.DEFAULT(QueryBuilder.NULL))
                        .withRow(Row.CREATED_AT, RowType.TIMESTAMP, QueryBuilder.DEFAULT(QueryBuilder.CURRENT_TIMESTAMP(driver)))
                        .withPrimaryKey(Row.ID, true)
                        .withForeign(Row.ACCOUNT_ID, JoinRow.at(Row.ID, Table.ACCOUNT), QueryModifier.of(" ON UPDATE CASCADE"), QueryModifier.of(" ON DELETE SET NULL"))
                        .withForeign(Row.SESSION_ID, JoinRow.at(Row.ID, Table.SESSION), QueryModifier.of(" ON UPDATE CASCADE"), QueryModifier.of(" ON DELETE SET NULL"))
                        .withForeign(Row.LAST_SERVER, JoinRow.at(Row.ID, Table.SERVER), QueryModifier.of(" ON UPDATE CASCADE"), QueryModifier.of(" ON DELETE SET NULL"))
                        .withForeign(Row.PREV_SERVER, JoinRow.at(Row.ID, Table.SERVER), QueryModifier.of(" ON UPDATE CASCADE"), QueryModifier.of(" ON DELETE SET NULL"));

                QueryBuilder bruteCreateQuery = QueryBuilder.createQuery(driver)
                        .createTable(true, Table.BRUTE_FORCE)
                        .withRow(Row.ID, RowType.INTEGER, QueryBuilder.NOT_NULL)
                        .withRow(Row.ADDRESS, RowType.VARCHAR, QueryBuilder.NOT_NULL, QueryModifier.of("(42)"))
                        .withRow(Row.TRIES, RowType.INTEGER, QueryBuilder.NOT_NULL, QueryBuilder.DEFAULT(QueryBuilder.NUMBER(0)))
                        .withRow(Row.BLOCKED, RowType.BOOLEAN, QueryBuilder.NOT_NULL,QueryBuilder.DEFAULT(QueryBuilder.FALSE))
                        .withRow(Row.REMAINING, RowType.LONG, QueryBuilder.NOT_NULL, QueryBuilder.DEFAULT(QueryBuilder.NUMBER(0)))
                        .withRow(Row.CREATED_AT, RowType.TIMESTAMP, QueryBuilder.NOT_NULL, QueryBuilder.DEFAULT(QueryBuilder.CURRENT_TIMESTAMP(driver)))
                        .withPrimaryKey(Row.ID, true);

                plugin.logInfo("Executing query <code>{0}</code>", accountCreteQuery.build());
                //"CREATE TABLE IF NOT EXISTS `account` ('id' INTEGER NOT NULL, 'password' TEXT, 'pin' TEXT, '2fa_token' TEXT, 'panic' TEXT, '2fa' BOOLEAN, 'created_at' NUMERIC, PRIMARY KEY('id' AUTOINCREMENT))"
                boolean acc_create = statement.execute(accountCreteQuery.queryType());

                plugin.logInfo("Executing query <code>{0}</code>", sessionCreateQuery.build());
                //"CREATE TABLE IF NOT EXISTS `session` ('id' INTEGER NOT NULL, 'captcha_login' BOOLEAN, 'pass_login' BOOLEAN, 'pin_login' BOOLEAN, '2fa_login' BOOLEAN, 'persistence' BOOLEAN, 'captcha' TEXT, 'created_at' NUMERIC, PRIMARY KEY('id' AUTOINCREMENT))"
                boolean sess_create = statement.execute(sessionCreateQuery.queryType());

                plugin.logInfo("Executing query <code>{0}</code>", serverCreateQuery.build());
                //"CREATE TABLE IF NOT EXISTS `server` ('id' INTEGER NOT NULL, 'name' TEXT, 'address' TEXT, 'port' INTEGER, 'created_at' NUMERIC, PRIMARY KEY('id' AUTOINCREMENT))"
                boolean serv_create = statement.execute(serverCreateQuery.build());

                plugin.logInfo("Executing query <code>{0}</code>", userCreateQuery.build());
                //"CREATE TABLE IF NOT EXISTS `user` ('id' INTEGER NOT NULL, 'name' TEXT, 'uuid' TEXT, 'account_id' INTEGER NULL, 'session_id' INTEGER NULL, 'type' INTEGER DEFAULT 1, 'last_server' INTEGER NULL, 'previous_server' INTEGER NULL, 'created_at' NUMERIC, PRIMARY KEY('id' AUTOINCREMENT), FOREIGN KEY('account_id') REFERENCES account('id') ON DELETE SET NULL, FOREIGN KEY('session_id') REFERENCES session('id') ON DELETE SET NULL, FOREIGN KEY('last_server') REFERENCES server('id') ON DELETE SET NULL, FOREIGN KEY('previous_server') REFERENCES server('id') ON DELETE SET NULL)"
                boolean user_create = statement.execute(userCreateQuery.build());

                plugin.logInfo("Executing query <code>{0}</code>", bruteCreateQuery.build());
                //"CREATE TABLE IF NOT EXISTS `brute` ('id' INTEGER NOT NULL, 'address' TEXT NOT NULL, 'tries' INTEGER DEFAULT 0, 'blocked' BOOLEAN DEFAULT false, 'remaining' NUMERIC DEFAULT 0, PRIMARY KEY('id' AUTOINCREMENT))"
                boolean brute_create = statement.execute(bruteCreateQuery.build());

                if (acc_create && sess_create && serv_create && user_create && brute_create) {
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
    }

    /**
     * Get if the driver is connected
     *
     * @return if the driver has been connected
     */
    @Override
    public boolean connected() {
        return connected;
    }

    /**
     * Retrieve a connection from the database
     *
     * @return the database connection
     * @throws SQLException if the connection was not
     *                      able to be established
     */
    @Override
    public Connection retrieve() throws SQLException {
        return source.getConnection();
    }
}
