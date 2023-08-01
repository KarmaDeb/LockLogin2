package es.karmadev.locklogin.spigot.event;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.user.auth.CProcessFactory;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.PlayerLocationStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitHandler implements Listener {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        int networkId = UserDataHandler.getNetworkId(player);
        if (networkId > 0) {
            CPluginNetwork network = (CPluginNetwork) plugin.network();
            NetworkClient client = network.getPlayer(networkId);

            client.getSessionChecker().cancel();
            network.disconnectClient(client);

            PlayerLocationStorage storage = new PlayerLocationStorage(client);
            storage.assign(player.getLocation());

            ((CProcessFactory) CurrentPlugin.getPlugin().getProcessFactory()).removeProgress(client);
        }
    }
}
