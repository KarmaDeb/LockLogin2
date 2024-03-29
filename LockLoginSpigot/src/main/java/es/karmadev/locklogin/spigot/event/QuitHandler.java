package es.karmadev.locklogin.spigot.event;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.user.session.service.SessionStoreService;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.user.auth.CProcessFactory;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.PlayerLocationStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitHandler implements Listener {

    private final LockLoginSpigot spigot;

    public QuitHandler(final LockLoginSpigot spigot) {
        this.spigot = spigot;
    }

    @EventHandler(priority = EventPriority.LOWEST) @SuppressWarnings("unchecked")
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        int networkId = UserDataHandler.getNetworkId(player);

        Bukkit.getScheduler().runTaskAsynchronously(spigot.plugin(), () -> {
            if (networkId > 0) {
                CPluginNetwork network = (CPluginNetwork) spigot.network();
                NetworkClient client = network.getPlayer(networkId);
                if (client != null) {
                    if (client.session().isPersistent()) {
                        PluginService sessionStoreProvider = spigot.getService("persistence");
                        if (sessionStoreProvider instanceof ServiceProvider) {
                            ServiceProvider<SessionStoreService> provider = (ServiceProvider<SessionStoreService>) sessionStoreProvider;
                            SessionStoreService service = provider.serve(spigot.driver());

                            if (service != null) {
                                service.saveSession(client);
                            }
                        }
                    }

                    client.getSessionChecker().cancel();
                    network.disconnectClient(client);

                    PlayerLocationStorage storage = new PlayerLocationStorage(client);
                    storage.assign(player.getLocation());

                    ((CProcessFactory) CurrentPlugin.getPlugin().getProcessFactory()).removeProgress(client);

                    spigot.getInjector().release(client);
                    spigot.getTotpHandler().destroyAll(client);

                    client.session().login(false);
                    client.session().totpLogin(false);
                    client.session().pinLogin(false);
                    //Logout, we don't care about session, as we (should) already stored its previous state

                    client.session().reset();
                    client.account().reset();
                    client.reset();

                    UserDataHandler.destroySettings(client);
                    //Reset caches
                }
            }

            /*
            We must run always the #handleDisconnect method, as a player
            could as been marked as ready to handle, but he might not have reach
            the network ID assignment
             */
            UserDataHandler.handleDisconnect(player);
        });
    }
}
