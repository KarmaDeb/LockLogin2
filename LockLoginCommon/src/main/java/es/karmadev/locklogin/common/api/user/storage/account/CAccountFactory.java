package es.karmadev.locklogin.common.api.user.storage.account;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.migration.AccountMigrator;
import es.karmadev.locklogin.common.api.user.storage.account.transiction.CMigrator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CAccountFactory implements AccountFactory<CAccount> {

    private final SQLDriver engine;
    private final Set<CAccount> account_cache = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final CMigrator migrator;

    public CAccountFactory(final SQLDriver engine) {
        this.engine = engine;
        this.migrator = new CMigrator(this);
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
            connection = engine.retrieve();
            statement = connection.createStatement();

            long now = Instant.now().toEpochMilli();

            try (ResultSet fetch_result = statement.executeQuery(QueryBuilder.createQuery()
                    .select(Table.USER, Row.ACCOUNT_ID)
                    .where(Row.ID, QueryBuilder.EQUALS, client.id()).build())) {
                if (fetch_result.next()) {
                    int account_id = fetch_result.getInt(1);

                    if (fetch_result.wasNull()) {
                        engine.close(null, statement);
                        statement = connection.createStatement();

                        statement.execute(QueryBuilder.createQuery()
                                .insert(Table.ACCOUNT, Row.CREATED_AT).values(now)
                                .build());
                        engine.close(null, statement);

                        statement = connection.createStatement();
                        try (ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
                            if (result.next()) {
                                account_id = result.getInt(1);

                                engine.close(null, statement);
                                statement = connection.createStatement();

                                statement.executeUpdate(QueryBuilder.createQuery()
                                        .update(Table.USER).set(Row.ACCOUNT_ID, account_id)
                                        .where(Row.ID, QueryBuilder.EQUALS, client.id()).build());
                            }
                        }
                    }

                    CAccount account = new CAccount(client.id(), account_id, engine);
                    account_cache.add(account);

                    return account;
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
     * Get all the accounts
     *
     * @return all the plugin accounts
     */
    @Override
    public CAccount[] getAllAccounts() {
        List<CAccount> offline = new ArrayList<>(account_cache);

        List<Integer> ids = new ArrayList<>();
        for (CAccount account : offline) {
            ids.add(account.id());
        }

        List<CAccount> accounts = new ArrayList<>(account_cache);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            try (ResultSet fetch_result = statement.executeQuery(QueryBuilder.createQuery().select(Table.USER, Row.ACCOUNT_ID, Row.ID)
                    .where(Row.ACCOUNT_ID, QueryBuilder.NOT_IN(ids)).build())) {
                while (fetch_result.next()) {
                    int account_id = fetch_result.getInt(1);
                    if (!fetch_result.wasNull()) {
                        int user_id = fetch_result.getInt(1);
                        if (!fetch_result.wasNull()) {
                            CAccount account = new CAccount(user_id, account_id, engine);
                            account_cache.add(account);
                            accounts.add(account);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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
