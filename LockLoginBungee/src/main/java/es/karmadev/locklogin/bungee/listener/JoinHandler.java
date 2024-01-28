package es.karmadev.locklogin.bungee.listener;

import es.karmadev.locklogin.bungee.LockLoginBungee;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
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

        ProxiedPlayer player = e.getPlayer();
        UUID playerId = player.getUniqueId();

        info.sendData("login:inject", playerId.toString().getBytes());
    }

    @EventHandler
    public void onDisconnect(ServerDisconnectEvent e) {
        ServerInfo info = e.getTarget();
        if (info.getPlayers().isEmpty()) {
            String name = info.getName();
            plugin.logInfo("Unloading shared server data for server ", name);
            plugin.getProtocol().forget(name);
        }
    }

    @EventHandler
    public void onKickedOut(ServerKickEvent e) {
        if (e.isCancelled()) return;

        ServerInfo info = e.getKickedFrom();
        if (info.getPlayers().isEmpty()) {
            String name = info.getName();
            plugin.logInfo("Unloading shared server data for server ", name);
            plugin.getProtocol().forget(name);
        }
    }
}
