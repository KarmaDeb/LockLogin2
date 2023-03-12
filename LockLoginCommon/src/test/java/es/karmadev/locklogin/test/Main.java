package es.karmadev.locklogin.test;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.network.server.ServerFactory;
import es.karmadev.locklogin.api.plugin.license.License;
import es.karmadev.locklogin.api.plugin.license.LicenseProvider;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.protection.type.*;
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.utils.url.URLUtils;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

        /* client = plugin.network().getEntity(1);
        UserSession session = client.session();

        session.captchaLogin(false);
        session.login(true);
        session.pinLogin(false);
        session._2faLogin(false);
        session.persistent(false);
        session.setCaptcha(new RandomString().create());
        account.setPassword("test");
        account.setPin("test");
        account.set2FA("test");
        account.setPanic("test");
        account.set2FA(true);*/

        Path license = Paths.get("D:\\Documentos\\TestUnits\\Minecraft\\Proxy\\auth\\plugins\\LockLogin\\cache\\license.dat");
        LicenseProvider provider = plugin.licenseProvider();
        License data = provider.load(license);
        if (data == null) {
            data = provider.update(license);
            if (data != null) {
                plugin.info("Updated license automatically");
                data.forceInstall();
            }
        }
        plugin.updateLicense(data);

        String[] fields = provider.update(data);
        if (fields.length > 0) {
            plugin.warn("Detected {0} changes on the license. The license has been updated automatically; The changes included:", fields.length);
            for (String field : fields) {
                plugin.info(field);
            }
        }

        System.out.println(plugin.license().version());
        System.out.println(plugin.license().owner().name());
    }
}
