package es.karmadev.locklogin.test;

import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.event.entity.client.EntityCreatedEvent;
import es.karmadev.locklogin.api.event.handler.EventHandler;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.database.Driver;
import es.karmadev.locklogin.api.plugin.database.query.JoinRow;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.query.QueryModifier;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.RowType;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.plugin.service.name.CNameProvider;
import es.karmadev.locklogin.common.api.plugin.service.name.CNameValidator;
import es.karmadev.locklogin.common.api.protection.type.*;
//import org.junit.Assert;
//import org.junit.Test;

import java.util.UUID;

//import static org.junit.Assert.assertFalse;

public class MainTest {

    private final static TestLockLogin plugin = new TestLockLogin();

    static {
        plugin.sqlite.connect();
    }

    //@Test
    public void testNames() {
        CNameProvider provider = new CNameProvider();
        String[] checkNames = new String[]{
                "Notch",
                "Steve",
                "KarmaDev",
                "Mr_Pro99",
                "Álvaro55.25,wad2",
                "SeñorHost"
        };

        for (String name : checkNames) {
            CNameValidator validator = provider.serve();
            validator.validate(name);

            if (validator.isValid()) {
                System.out.println(name);
            } else {
                System.out.println(ConsoleColor.parse("(Invalid) " + name + ": " + validator.invalidCharacters() + "&r"));
            }
        }
    }

    //@Test
    public void testLoad() {
        String raw = "rO0ABXNyADllcy5rYXJtYWRldi5sb2NrbG9naW4uY29tbW9uLmFwaS5wbHVnaW4uZmlsZS5DU2VjcmV0U3RvcmVoaQ9Ss0JS7AIAAlsAAml2dAACW0JbAAV0b2tlbnEAfgABeHB1cgACW0Ks8xf4BghU4AIAAHhwAAABAK4uBfBZD0hBUBIHi9lo67sRGgQgnDCP11NQRyUyiFij1USUqhxsfWcmZTbOlQJRuPpF2CgGWZX/RoG49n1SJpw3I6xkRT1JDdlKOjwtT/+HN5LKFHdar11Zb8vRyDsT3P2fCW2t0YKCItzhsvVnXC0Hd9RUjFmXNOvtOWgQXeRSrRlPQQaIy4JAVRSiZwq467P3Ur7wfRcstsCxsSsYgQBbagGMtRQQCid8ASyzOWiEZcFieOg2glZxCwuGEpBGepXmiZjV1wh6Va24qCDO0sEYdeOIsfbA2K0rO5THUr0SCHySx9KN354TBE9kvj1hThgzqOogEYsx0xZ9TayNoZx1cQB+AAMAAAAgBFLz6lqzPMkSrsPD3D0GYcf3BcBCKgf5N7+KZypceIQ=";
        Object load = StringUtils.load(raw);

        System.out.println(load);
    }



    //@Test
    public void testQueryBuilderSQLITE() {
        Driver driver = Driver.SQLite;

        QueryBuilder accountCreteQuery = QueryBuilder.createQuery(driver)
                .createTable(true, Table.ACCOUNT)
                .withRow(Row.ID, RowType.INTEGER, QueryBuilder.NOT_NULL)
                .withRow(Row.PASSWORD, RowType.BLOB)
                .withRow(Row.PIN, RowType.BLOB)
                .withRow(Row.TOKEN_2FA, RowType.VARCHAR, QueryModifier.of("(128)"))
                .withRow(Row.PANIC, RowType.BLOB)
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

        QueryBuilder alterQuery = QueryBuilder.createQuery(driver)
                        .alter(Table.ACCOUNT).add(Row.TOKEN_2FA, RowType.BOOLEAN);

        System.out.println(accountCreteQuery.build(");"));
        System.out.println(sessionCreateQuery.build(");"));
        System.out.println(serverCreateQuery.build(");"));
        System.out.println(userCreateQuery.build(");"));
        System.out.println(bruteCreateQuery.build(");"));
        System.out.println(alterQuery.build(";"));

        //Assert.assertTrue(true);
    }

    //@Test
    public void testQueryBuilderMySQL() {
        Driver driver = Driver.MySQL;

        QueryBuilder accountCreteQuery = QueryBuilder.createQuery(driver)
                .createTable(true, Table.ACCOUNT)
                .withRow(Row.ID, RowType.INTEGER, QueryBuilder.NOT_NULL)
                .withRow(Row.PASSWORD, RowType.BLOB)
                .withRow(Row.PIN, RowType.BLOB)
                .withRow(Row.TOKEN_2FA, RowType.VARCHAR, QueryModifier.of("(128)"))
                .withRow(Row.PANIC, RowType.BLOB)
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

        QueryBuilder alterQuery = QueryBuilder.createQuery(driver)
                .alter(Table.ACCOUNT).add(Row.TOKEN_2FA, RowType.BOOLEAN);

        System.out.println(accountCreteQuery.build(");"));
        System.out.println(sessionCreateQuery.build(");"));
        System.out.println(serverCreateQuery.build(");"));
        System.out.println(userCreateQuery.build(");"));
        System.out.println(bruteCreateQuery.build(");"));
        System.out.println(alterQuery.build(";"));

        //Assert.assertTrue(true);
    }

    //@Test
    public void testEvents() {
        int id = 1;
        LocalNetworkClient client = plugin.network().getEntity(id);

        EventHandler handler = new TestListener();
        plugin.moduleManager().addEventHandler(handler);

        plugin.moduleManager().fireEvent(new EntityCreatedEvent(client));
    }


    //Passed: 12/03/22 23:42
    //@Test
    public void testUserCreation() {
        String name = "KarmaDev";
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());

        UserFactory<LocalNetworkClient> factory = plugin.getUserFactory(false);
        LocalNetworkClient client = factory.create(name, uuid);

        System.out.println(client.id());
    }

    //Passed: 12/03/22 23:42
    //@Test
    public void testUserFetch() {
        int id = 1;

        LocalNetworkClient client = plugin.network().getEntity(id);
        System.out.println(client);
    }

    //Passed: 12/03/22 23:42
    //@Test
    public void testUserSession() {
        int id = 1;

        LocalNetworkClient client = plugin.network().getEntity(id);
        UserSession account = client.session();

        System.out.println(account);
    }

    //Passed: 12/03/22 23:42
    //@Test
    public void testUserAccount() {
        int id = 1;

        LocalNetworkClient client = plugin.network().getEntity(id);
        UserAccount account = client.account();

        System.out.println(account);
    }

    //Passed: 12/03/22 23:42
    //@Test
    public void registerHash() {
        boolean error = false;
        try {
            plugin.hasher().registerMethod(new SHA512Hash());
            plugin.hasher().registerMethod(new SHA256Hash());
            plugin.hasher().registerMethod(new BCryptHash());
            plugin.hasher().registerMethod(new Argon2I());
            plugin.hasher().registerMethod(new Argon2D());
            plugin.hasher().registerMethod(new Argon2ID());
        } catch (UnnamedHashException ex) {
            error = true;
        }

        //assertFalse(error);
    }
}
