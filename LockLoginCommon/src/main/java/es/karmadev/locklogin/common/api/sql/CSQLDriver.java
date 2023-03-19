package es.karmadev.locklogin.common.api.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.api.plugin.database.Driver;
import es.karmadev.locklogin.api.plugin.database.query.JoinRow;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.query.QueryModifier;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.RowType;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.plugin.file.Database;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CSQLDriver implements DataDriver {

    private HikariDataSource source;
    private boolean connected = false;

    /**
     * Connect to the driver
     */
    @Override
    public void connect() {
        if (!connected) {
            LockLogin plugin = CurrentPlugin.getPlugin();
            Database database = plugin.configuration().database();
            Driver driver = database.driver();

            Path sql_file = plugin.workingDirectory().resolve("data").resolve("accounts.db");

            HikariConfig config = new HikariConfig();
            config.setPoolName("locklogin-" + driver.name());
            config.setDriverClassName(driver.getTestClass());
            if (driver.equals(Driver.SQLite)) {
                config.setJdbcUrl(driver.getConnection(sql_file));
            } else {
                config.setJdbcUrl(driver.getConnection(database.host(), database.port(), database.database(), database.ssl(), database.verifyCertificates()));
                config.setUsername(database.username());
                config.setPassword(database.password());
            }

            config.setMinimumIdle(database.minimumConnections());
            config.setMaximumPoolSize(database.maximumConnections());
            config.setIdleTimeout(database.unusedTimeout() * 1000L);
            config.setConnectionTimeout(database.connectionTimeout() * 1000L);
            config.setConnectionTestQuery(database.testQuery());
            config.setMaxLifetime(database.maximumLifetime() * 1000L);
            config.setLeakDetectionThreshold(database.leakDetection() * 1000L);

            source = new HikariDataSource(config);
            plugin.info("Initialized LockLogin sqlite connection successfully");
            connected = true;

            Connection connection = null;
            Statement statement = null;
            try {
                connection = source.getConnection();
                statement = connection.createStatement();

                /*if (driver.equals(Driver.SQLite)) {
                    statement.execute("CREATE TABLE IF NOT EXISTS `sqlite_master` (`type` TEXT, `name` TEXT, `tbl_name` TEXT, `rootpage` INTEGER, `sql` TEXT)");
                }*/

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

                List<Table> tables = new ArrayList<>(Arrays.asList(Table.values()));
                try (ResultSet result = statement.executeQuery("SELECT `name` AS 'table', `sql` AS 'sequence' FROM `sqlite_master` WHERE `type`='table'")) {
                    while (result.next()) {
                        String tableName = result.getString("table");
                        String sqlQuery = result.getString("sequence");

                        if (!StringUtils.isNullOrEmpty(tableName) && !StringUtils.isNullOrEmpty(sqlQuery)) {
                            for (Table table : Table.values()) {
                                String tbName = database.tableName(table);
                                if (tbName.equals(tableName)) {
                                    tables.remove(table);
                                }
                            }
                        }
                    }
                }

                boolean acc_create = true;
                boolean sess_create = true;
                boolean serv_create = true;
                boolean user_create = true;
                boolean brute_create = true;
                for (Table table : Table.values()) {
                    if (tables.contains(table)) {
                        switch (table) {
                            case ACCOUNT:
                                plugin.logInfo("Executing query <code>{0}</code>", accountCreteQuery.build());
                                try {
                                    statement.execute(accountCreteQuery.build());
                                } catch (SQLException ex) {
                                    plugin.log(ex, "An error occurred while executing query");
                                    acc_create = false;
                                }
                                break;
                            case SESSION:
                                plugin.logInfo("Executing query <code>{0}</code>", sessionCreateQuery.build());
                                try {
                                    statement.execute(sessionCreateQuery.build());
                                } catch (SQLException ex) {
                                    plugin.log(ex, "An error occurred while executing query");
                                    sess_create = false;
                                }
                                break;
                            case SERVER:
                                plugin.logInfo("Executing query <code>{0}</code>", serverCreateQuery.build());
                                try {
                                    statement.execute(serverCreateQuery.build());
                                } catch (SQLException ex) {
                                    plugin.log(ex, "An error occurred while executing query");
                                    serv_create = false;
                                }
                                break;
                            case USER:
                                plugin.logInfo("Executing query <code>{0}</code>", userCreateQuery.build());
                                try {
                                    statement.execute(userCreateQuery.build());
                                } catch (SQLException ex) {
                                    plugin.log(ex, "An error occurred while executing query");
                                    user_create = false;
                                }
                                break;
                            case BRUTE_FORCE:
                                plugin.logInfo("Executing query <code>{0}</code>", bruteCreateQuery.build());
                                try {
                                    statement.execute(bruteCreateQuery.build());
                                } catch (SQLException ex) {
                                    plugin.log(ex, "An error occurred while executing query");
                                    brute_create = false;
                                }
                                break;
                        }
                    }
                }

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
     * Fetch the existing tables
     *
     * @return the existing tables
     */
    @Override
    public List<Table> fetchTables() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Database database = plugin.configuration().database();

        List<Table> tables = new ArrayList<>();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = source.getConnection();
            statement = connection.createStatement();

            try (ResultSet result = statement.executeQuery("SELECT `name` AS 'table' FROM `sqlite_master` WHERE `type`='table'")) {
                while (result.next()) {
                    String tableName = result.getString("table");
                    if (!StringUtils.isNullOrEmpty(tableName)) {
                        for (Table table : Table.values()) {
                            String tbName = database.tableName(table);
                            if (tbName.equals(tableName) && !tables.contains(table)) {
                                tables.add(table);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.log(ex, "Failed to fetch created tables");
            plugin.info("An error occurred while executing a sql query");
        } finally {
            close(connection, statement);
        }

        return Collections.unmodifiableList(tables);
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
