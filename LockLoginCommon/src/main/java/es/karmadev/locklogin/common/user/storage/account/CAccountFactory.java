package es.karmadev.locklogin.common.user.storage.account;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.migration.AccountMigrator;
import es.karmadev.locklogin.common.SQLiteDriver;
import es.karmadev.locklogin.common.user.storage.account.transiction.CMigrator;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CAccountFactory implements AccountFactory<CAccount> {

    private final SQLiteDriver driver;
    private final Set<CAccount> account_cache = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final CMigrator migrator;

    public CAccountFactory(final SQLiteDriver driver) {
        this.driver = driver;
        this.migrator = new CMigrator(driver);
    }

    /**
     * Create an account for the specified client
     *
     * @param client the client to generate
     *               the account for
     * @return the client account
     */
    @Override
    public CAccount create(final LocalNetworkClient client) {
        if (account_cache.stream().anyMatch((account) -> account != null && client.account() != null && account.id() == client.account().id())) {
            return account_cache.stream().filter((account) -> account.id() == client.account().id()).findFirst().orElse(null);
        }

        Connection connection = null;
        Statement statement = null;
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery("SELECT `account_id` FROM `user` WHERE `id` = " + client.id())) {
                if (fetch_result.next()) {
                    int account_id = fetch_result.getInt("account_id");
                    if (fetch_result.wasNull()) {
                        driver.close(null, statement);
                        statement = connection.createStatement();

                        statement.execute("INSERT INTO `account` (`created_at`) VALUES (" + now + ")");
                        driver.close(null, statement);

                        statement = connection.createStatement();
                        try (ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
                            if (result.next()) {
                                account_id = result.getInt(1);

                                driver.close(null, statement);
                                statement = connection.createStatement();

                                statement.executeUpdate("UPDATE `user` SET `account_id` = " + account_id + " WHERE `id` = " + client.id());
                            }
                        }
                    }

                    CAccount account = new CAccount(client.id(), account_id, driver);
                    account_cache.add(account);

                    return account;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }

        return null;
    }

    /**
     * Get all the accounts
     *
     * @return all the plugin accounts
     */
    @Override
    public CAccount[] getAllAccounts() {
        List<CAccount> offline = new ArrayList<>(account_cache);

        StringBuilder idIgnorer = new StringBuilder();
        for (CAccount account : offline) {
            idIgnorer.append(account.id()).append(",");
        }
        String not_in = StringUtils.replaceLast(idIgnorer.toString(), ",", "");

        Connection connection = null;
        Statement statement = null;
        List<CAccount> accounts = new ArrayList<>(account_cache);
        try {
            connection = driver.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery("SELECT `id`,`account_id` FROM `user` WHERE `account_id` NOT IN (" + not_in + ")")) {
                while (fetch_result.next()) {
                    int account_id = fetch_result.getInt("account_id");
                    if (!fetch_result.wasNull()) {
                        int user_id = fetch_result.getInt("id");
                        if (!fetch_result.wasNull()) {
                            CAccount account = new CAccount(user_id, account_id, driver);
                            account_cache.add(account);
                            accounts.add(account);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            driver.close(connection, statement);
        }

        return accounts.toArray(new CAccount[0]);
    }

    /**
     * Get the account migrator of this factory
     *
     * @return the factory account migrator
     */
    @Override
    public AccountMigrator<CAccount> migrator() {
        return migrator;
    }
}
