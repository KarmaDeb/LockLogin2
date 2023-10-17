package es.karmadev.locklogin.spigot.event;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.MovementConfiguration;
import es.karmadev.locklogin.api.plugin.file.section.SpawnSection;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.SpawnLocationStorage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementHandler implements Listener {

    private final LockLoginSpigot spigot;

    public MovementHandler(final LockLoginSpigot spigot) {
        this.spigot = spigot;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        //TODO: Optimize logic
        Player player = e.getPlayer();
        NetworkClient client = UserDataHandler.fromEvent(e);
        if (client == null) return;

        UserSession session = client.session();
        if (session.isLogged() && session.isTotpLogged() && session.isPinLogged()) return;

        Location from = e.getFrom();
        Location to = e.getTo();

        Block source = from.getBlock();

        Configuration configuration = spigot.configuration();
        MovementConfiguration movement = configuration.movement();
        SpawnSection spawn = configuration.spawn();

        if (movement.allow()) {
            int maxDistance = movement.distance();
            if (maxDistance <= 0) return; //Allow movement

            if (to == null) {
                e.setCancelled(true);
                return;
            }

            if (spawn.enable()) {
                Location spawnLocation = SpawnLocationStorage.load();
                if (spawnLocation != null) {
                    if (spawnLocation.distance(player.getLocation()) > maxDistance) {
                        Location preTeleport = player.getLocation();
                        spawnLocation.setYaw(preTeleport.getYaw());
                        spawnLocation.setPitch(preTeleport.getPitch());

                        player.teleport(spawnLocation);
                    }

                    return;
                }
            }
        } else {
            if (to == null) return;
            Block target = to.getBlock();

            e.setCancelled(source.getX() != target.getX() || source.getZ() != target.getZ() || source.getY() < target.getY());
            return;
        }

        Block target = to.getBlock();
        if (!source.equals(target)) {
            e.setCancelled(source.getX() != target.getX() || source.getZ() != target.getZ() || source.getY() < target.getY());
        }
    }
}
