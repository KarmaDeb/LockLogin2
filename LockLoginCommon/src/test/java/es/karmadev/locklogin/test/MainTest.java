package es.karmadev.locklogin.test;

import es.karmadev.locklogin.api.event.entity.client.EntityCreatedEvent;
import es.karmadev.locklogin.api.event.handler.EventHandler;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.license.License;
import es.karmadev.locklogin.api.plugin.license.LicenseProvider;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.protection.type.*;
import es.karmadev.locklogin.common.plugin.web.SocketService;
import io.socket.client.Socket;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class MainTest {

    private final TestLockLogin plugin = new TestLockLogin();

    {
        plugin.sqlite.connect();
    }

    //Passed: 12/03/22 23:42
    @Test
    public void testEvents() {
        int id = 1;
        LocalNetworkClient client = plugin.network().getEntity(id);

        EventHandler handler = new TestListener();
        plugin.moduleManager().addEventHandler(handler);

        plugin.moduleManager().fireEvent(new EntityCreatedEvent(client));
    }

    //Passed: 12/03/22 23:42
    @Test
    public void testUserCreation() {
        String name = "KarmaDev";
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());

        UserFactory<LocalNetworkClient> factory = plugin.getUserFactory(false);
        LocalNetworkClient client = factory.create(name, uuid);

        System.out.println(client.id());
    }

    //Passed: 12/03/22 23:42
    @Test
    public void testUserFetch() {
        int id = 1;

        LocalNetworkClient client = plugin.network().getEntity(id);
        System.out.println(client);
    }

    //Passed: 12/03/22 23:42
    @Test
    public void testUserSession() {
        int id = 1;

        LocalNetworkClient client = plugin.network().getEntity(id);
        UserSession account = client.session();

        System.out.println(account);
    }

    //Passed: 12/03/22 23:42
    @Test
    public void testUserAccount() {
        int id = 1;

        LocalNetworkClient client = plugin.network().getEntity(id);
        UserAccount account = client.account();

        System.out.println(account);
    }

    //Passed: 12/03/22 23:42
    @Test @SuppressWarnings("all")
    public void testSocket() throws InterruptedException {
        SocketService service = new SocketService();
        plugin.info("WebSocket service at: {0}", service.getAddress());

        Socket client = service.getInstance();
        client.on("welcome", (data) -> plugin.info("Successfully connected to LockLogin web services!"));

        client.connect();
        synchronized (Thread.currentThread()) {
            while (!client.connected()) Thread.sleep(1);
        }
    }

    //Passed: 12/03/22 23:42
    @Test
    public void registerHash() throws UnnamedHashException {
        plugin.hasher().registerMethod(new SHA512Hash());
        plugin.hasher().registerMethod(new SHA256Hash());
        plugin.hasher().registerMethod(new BCryptHash());
        plugin.hasher().registerMethod(new Argon2I());
        plugin.hasher().registerMethod(new Argon2D());
        plugin.hasher().registerMethod(new Argon2ID());
    }

    //Passed: 12/03/22 23:42
    @Test
    public void testLicense() {
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
