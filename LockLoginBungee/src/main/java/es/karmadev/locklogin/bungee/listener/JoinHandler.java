package es.karmadev.locklogin.bungee.listener;

import es.karmadev.locklogin.bungee.LockLoginBungee;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

@AllArgsConstructor
public class JoinHandler implements Listener {

    private final LockLoginBungee plugin;

    @EventHandler
    public void onLogin(ServerConnectedEvent e) {
        Server server = e.getServer();
        ServerInfo info = server.getInfo();
        plugin.getConnectedServers().put(info.getName(), server);

        ProxiedPlayer player = e.getPlayer();
        UUID playerId = player.getUniqueId();

        info.sendData("login:inject", playerId.toString().getBytes());
    }
}
