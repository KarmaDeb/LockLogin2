package es.karmadev.locklogin.test;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.protection.type.*;
import ml.karmaconfigs.api.common.string.random.RandomString;

import java.util.UUID;

public class Main {

    public static void main(String[] args) throws UnnamedHashException {
        TestLockLogin c = new TestLockLogin();
        LockLogin plugin = CurrentPlugin.getPlugin();

        c.sqlite.connect();
        CurrentPlugin.getPlugin().info("Success");

        plugin.hasher().registerMethod(new SHA512Hash());
        plugin.hasher().registerMethod(new SHA256Hash());
        plugin.hasher().registerMethod(new BCryptHash());
        plugin.hasher().registerMethod(new Argon2I());
        plugin.hasher().registerMethod(new Argon2D());
        plugin.hasher().registerMethod(new Argon2ID());

        UserFactory<LocalNetworkClient> clientFactory = plugin.getUserFactory(false);
        AccountFactory<UserAccount> accountFactory = plugin.getAccountFactory(false);
        SessionFactory<UserSession> sessionFactory = plugin.getSessionFactory(false);
        CurrentPlugin.getPlugin().warn("Using    user factory: {0}", clientFactory.getClass().getCanonicalName());
        CurrentPlugin.getPlugin().warn("Using account factory: {0}", accountFactory.getClass().getCanonicalName());
        CurrentPlugin.getPlugin().warn("Using session factory: {0}", sessionFactory.getClass().getCanonicalName());

        LocalNetworkClient created = clientFactory.create("KarmaDev", UUID.nameUUIDFromBytes(("OfflinePlayer:KarmaDev").getBytes()));
        UserAccount account = accountFactory.create(created);
        UserSession session = sessionFactory.create(created);

        session.captchaLogin(false);
        session.login(false);
        session.pinLogin(false);
        session._2faLogin(false);
        session.persistent(false);
        session.setCaptcha(new RandomString().create());
        account.setPassword("test");
        account.setPin("test");
        account.set2FA("test");
        account.setPanic("test");
        account.set2FA(true);

        HashResult rs = account.password();
        System.out.println(rs.hasher().name());
        System.out.println(rs.hasher().verify("test", rs));
        System.out.println(rs.hasher().verify("test2", rs));
        System.out.println(rs.hasher().verify("tEst", rs));
    }
}
