package es.karmadev.locklogin.spigot.event.helper;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SimplePE extends PlayerEvent {

    private final EntityEvent parent;

    public SimplePE(final Player player, final EntityEvent parent) {
        super(player);
        this.parent = parent;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return parent.getHandlers();
    }
}
