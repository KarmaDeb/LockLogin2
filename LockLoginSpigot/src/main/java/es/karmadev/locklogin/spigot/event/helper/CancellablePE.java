package es.karmadev.locklogin.spigot.event.helper;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.NotNull;

public class CancellablePE extends SimplePE implements Cancellable {

    private final Cancellable parent;

    public CancellablePE(final Player player, final Cancellable parent) {
        super(player, (EntityEvent) parent);
        this.parent = parent;
    }

    /**
     * Gets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins
     *
     * @return true if this event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return parent.isCancelled();
    }

    /**
     * Sets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins.
     *
     * @param cancel true if you wish to cancel this event
     */
    @Override
    public void setCancelled(final boolean cancel) {
        parent.setCancelled(cancel);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return ((Event) parent).getHandlers();
    }
}
