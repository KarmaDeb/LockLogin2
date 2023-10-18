package es.karmadev.locklogin.spigot.event.helper;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;

public class EventHelper {

    public static PlayerEvent asPlayerEvent(final Player player, final EntityEvent event) {
        if (event instanceof Cancellable) {
            return new CancellablePE(player, (Cancellable) event);
        }

        return new SimplePE(player, event);
    }
}
