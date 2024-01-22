package es.karmadev.locklogin.bungee.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class JoinHandler implements Listener {

    @EventHandler
    public void onLogin(ServerConnectedEvent e) {
        ProxiedPlayer player = e.getPlayer();
        UUID playerId = player.getUniqueId();

        e.getServer().getInfo().sendData("login:inject", playerId.toString().getBytes());
    }
}
