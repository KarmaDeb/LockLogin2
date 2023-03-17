package es.karmadev.locklogin.common.api.server;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.api.plugin.database.DataDriver;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public class CServerFactory implements ServerFactory<CServer> {

    private final DataDriver driver;

    public CServerFactory(final DataDriver driver) {
        this.driver = driver;
    }

    /**
     * Create a new server
     *
     * @param name    the server name
     * @param address the server address
     * @return the server
     */
    @Override
    public CServer create(final String name, final InetSocketAddress address) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Connection connection = null;
        Statement statement = null;

        CPluginNetwork network = (CPluginNetwork) plugin.network();
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery("SELECT `id` FROM `server` WHERE `name` = '" + name + "' OR `address` = '" + address.getHostString() + "' AND `port` = " + address.getPort())) {
                if (fetch_result.next()) {
                    int id = fetch_result.getInt("id");

                    CServer stored = (CServer) network.getServer(id);
                    if (stored == null) {
                        stored = new CServer(id, driver);
                        network.appendServer(stored);
                    }

                    driver.close(connection, statement);

                    stored.setAddress(address);
                    stored.setName(name);

                    return stored;
                } else {
                    driver.close(null, statement);
                    statement = connection.createStatement();

                    statement.execute("INSERT INTO `server` (`name`,`address`,`port`,`created_at`) VALUES ('" + name + "','" + address.getHostString() + "'," + address.getPort() + "," + now + ")");
                    driver.close(null, statement);

                    try (ResultSet insert_result = statement.executeQuery("SELECT last_insert_rowid()")) {
                        if (insert_result.next()) {
                            int id = insert_result.getInt(1);
                            CServer server = new CServer(id, driver);
                            network.appendServer(server);

                            return server;
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }

        return null;
    }
}
