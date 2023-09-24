package es.karmadev.locklogin.test;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.common.api.protection.type.*;

import java.util.UUID;

public class Main {

    public static void main(String[] args) throws Throwable {
        DummyLockLogin locklogin = new DummyLockLogin();
        SQLDriver driver = locklogin.driver();

        LockLoginHasher hasher = locklogin.hasher();
        hasher.registerMethod(new SHA512Hash());
        hasher.registerMethod(new SHA256Hash());
        /*hasher.registerMethod(new Argon2I());
        hasher.registerMethod(new Argon2D());
        hasher.registerMethod(new Argon2ID());
        hasher.registerMethod(new BCryptHash());*/

        driver.connect();
        UserFactory<LocalNetworkClient> userFactory = locklogin.getUserFactory(true);

        String name = "KarmaDev";
        String password = "12345678";
        UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        LocalNetworkClient client = userFactory.create(name, offline);

        //LocalNetworkClient client = userFactory.create(name, offline);
        System.out.println("User: " + client.id());

        AccountFactory<UserAccount> accountFactory = locklogin.getAccountFactory(true);
        UserAccount account = accountFactory.create(client);

        //System.out.println("Account: " + account.id());
        //account.setPassword(password);

        HashResult passwordResult = account.password();
        System.out.println(passwordResult.verify(password));
    }
}
