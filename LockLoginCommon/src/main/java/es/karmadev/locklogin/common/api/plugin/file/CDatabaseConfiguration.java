package es.karmadev.locklogin.common.api.plugin.file;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.api.file.yaml.handler.YamlHandler;
import es.karmadev.api.file.yaml.handler.YamlReader;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.database.Driver;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.plugin.file.Database;

import java.io.IOException;
import java.nio.file.Path;

public class CDatabaseConfiguration implements Database {

    private final Driver driver;
    private final YamlFileHandler yaml;

    /**
     * Initialize the plugin configuration
     */
    public CDatabaseConfiguration(final Driver driver) {
        this.driver = driver;
        final Path file = CurrentPlugin.getPlugin().workingDirectory().resolve("database.yml");

        LockLogin plugin = CurrentPlugin.getPlugin();
        PathUtilities.copy(plugin, "plugin/yaml/database.yml", file);

        try {
            YamlReader reader = new YamlReader(plugin.loadResource("plugin/yaml/database.yml"));
            yaml = YamlHandler.load(file, reader);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        yaml.validate();
    }

    /**
     * Get the plugin database driver
     *
     * @return the plugin driver
     */
    @Override
    public Driver driver() {
        return driver;
    }

    /**
     * Get the database name
     *
     * @return the database
     */
    @Override
    public String database() {
        return yaml.getString("Database.Name", "locklogin");
    }

    /**
     * Get the SQL server host
     *
     * @return the sql host
     */
    @Override
    public String host() {
        return yaml.getString("Database.Host", "127.0.0.1");
    }

    /**
     * Get the SQL server port
     *
     * @return the SQL port
     */
    @Override
    public int port() {
        return yaml.getInteger("Database.Port", 3306);
    }

    /**
     * Get the SQL server username
     *
     * @return the account name
     */
    @Override
    public String username() {
        return yaml.getString("Database.User", "root");
    }

    /**
     * Get the SQL server password
     *
     * @return the account password
     */
    @Override
    public String password() {
        return yaml.getString("Database.Pass", "");
    }

    /**
     * Get if the connection is performed
     * under ssl
     *
     * @return if the connection uses SSL
     */
    @Override
    public boolean ssl() {
        return yaml.getBoolean("Database.UseSSL", false);
    }

    /**
     * Get if the connection verifies server
     * certificates
     *
     * @return if the connection is safe
     */
    @Override
    public boolean verifyCertificates() {
        return yaml.getBoolean("Database.ValidateCerts", false);
    }

    /**
     * Get the test query
     *
     * @return the test query
     */
    @Override
    public String testQuery() {
        return yaml.getString("Database.TestQuery", "SELECT 1");
    }

    /**
     * Get the sql connection timeout
     *
     * @return the connection timeout
     */
    @Override
    public int connectionTimeout() {
        return yaml.getInteger("Pooling.ConnectionTimeout", 60);
    }

    /**
     * Get the connection being used timeout
     *
     * @return the connection use timeout
     */
    @Override
    public int unusedTimeout() {
        return yaml.getInteger("Pooling.UnusedTimeout", 10);
    }

    /**
     * Get the database leak detection threshold
     *
     * @return the database leak detection
     */
    @Override
    public int leakDetection() {
        return yaml.getInteger("Pooling.LeakDetection", 20);
    }

    /**
     * Get the connection maximum lifetime
     *
     * @return the connection lifetime
     */
    @Override
    public int maximumLifetime() {
        return yaml.getInteger("Pooling.MaximumLifeTime", 30);
    }

    /**
     * Get the minimum amount of connections
     *
     * @return the minimum amount of connections
     */
    @Override
    public int minimumConnections() {
        return yaml.getInteger("Pooling.MinimumConnections", 10);
    }

    /**
     * Get the maximum amount of connections
     *
     * @return the maximum amount of connections
     */
    @Override
    public int maximumConnections() {
        return yaml.getInteger("Pooling.MaximumConnections", 50);
    }

    /**
     * Get the name of a table
     *
     * @param table the table
     * @return the table name
     */
    @Override
    public String tableName(final Table table) {
        return yaml.getString("Tables." + table.name + ".Table", table.name);
    }

    /**
     * Get the column name of a table
     *
     * @param table the table
     * @param row   the row
     * @return the column name
     */
    @Override
    public String columnName(final Table table, final Row row) {
        return yaml.getString("Tables." + table.name + ".Columns." + row.name, row.name);
    }
}
