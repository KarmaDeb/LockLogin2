package es.karmadev.locklogin.spigot.event.iteration;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.function.Consumer;

public class IterationHandler implements Listener {

    private final Consumer<PlayerEvent> eventConsumer = (e) -> {
        NetworkClient client = UserDataHandler.fromEvent(e);

        if (client == null) return;

        UserSession session = client.session();
        if (session.isLogged() && session.isPinLogged() && session.isTotpLogged()) return;

        if (e instanceof Cancellable) {
            Cancellable cancellable = (Cancellable) e;
            cancellable.setCancelled(true);
        }
    };

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClientIterate(PlayerInteractEvent e) {
        eventConsumer.accept(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClientIterate(PlayerInteractAtEntityEvent e) {
        eventConsumer.accept(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClientIterate(PlayerInteractEntityEvent e) {
        eventConsumer.accept(e);
    }
}
