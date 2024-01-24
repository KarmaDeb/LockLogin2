package es.karmadev.locklogin.spigot.event;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.file.section.MovementConfiguration;
import es.karmadev.locklogin.api.plugin.file.spawn.CancelPolicy;
import es.karmadev.locklogin.api.plugin.file.spawn.SpawnConfiguration;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.event.helper.EventHelper;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.SpawnLocationStorage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MovementHandler implements Listener {

    private final LockLoginSpigot spigot;

    public MovementHandler(final LockLoginSpigot spigot) {
        this.spigot = spigot;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(EntityPortalEnterEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) return;

        Player player = (Player) entity;
        NetworkClient client = UserDataHandler.fromEvent(EventHelper.asPlayerEvent(player, e));
        if (client == null) return;

        UserSession session = client.session();
        if (session.isLogged() && session.isTotpLogged() && session.isPinLogged()) {
            SpawnConfiguration spawn = spigot.configuration().spawn();
            Messages messages = spigot.messages();

            if (UserDataHandler.isTeleporting(player) && spawn.cancelWithPolicy(CancelPolicy.MOVEMENT)) {
                UserDataHandler.setTeleporting(player, null);
                client.sendMessage(messages.prefix() + messages.spawnCancelled());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        NetworkClient client = UserDataHandler.fromEvent(e);
        if (client == null) return;

        UserSession session = client.session();
        if (session.isLogged() && session.isTotpLogged() && session.isPinLogged()) {
            SpawnConfiguration spawn = spigot.configuration().spawn();
            Messages messages = spigot.messages();

            if (UserDataHandler.isTeleporting(player) && spawn.cancelWithPolicy(CancelPolicy.MOVEMENT)) {
                UserDataHandler.setTeleporting(player, null);
                client.sendMessage(messages.prefix() + messages.spawnCancelled());
            }

            return;
        }

        if (!e.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            e.setCancelled(true);
            /*
            Allow plugins to teleport clients, so we don't break them. Obviously, this
            requires them to make a good usage of Player#teleport (basically specify
            the teleport cause to be PLUGIN)
             */
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        //TODO: Optimize logic
        Player player = e.getPlayer();
        NetworkClient client = UserDataHandler.fromEvent(e);
        if (client == null) return;

        Location from = e.getFrom();
        Location to = e.getTo();

        Block source = from.getBlock();

        Configuration configuration = spigot.configuration();
        MovementConfiguration movement = configuration.movement();
        SpawnConfiguration spawn = configuration.spawn();
        Messages messages = spigot.messages();

        UserSession session = client.session();
        if (session.isLogged() && session.isTotpLogged() && session.isPinLogged()) {
            if (to == null) return;
            Block target = to.getBlock();
            if (source.getX() != target.getX() || source.getZ() != target.getZ() || source.getY() < target.getY()) {
                if (UserDataHandler.isTeleporting(player) && spawn.cancelWithPolicy(CancelPolicy.MOVEMENT)) {
                    UserDataHandler.setTeleporting(player, null);
                    client.sendMessage(messages.prefix() + messages.spawnCancelled());
                }
            }

            return;
        }

        if (movement.allow()) {
            int maxDistance = movement.distance();
            if (maxDistance <= 0) return; //Allow movement

            if (to == null) {
                e.setCancelled(true);
                return;
            }

            if (spawn.enabled()) {
                Location spawnLocation = SpawnLocationStorage.load();
                if (spawnLocation != null) {
                    if (spawnLocation.distance(player.getLocation()) > maxDistance) {
                        Location preTeleport = player.getLocation();
                        spawnLocation.setYaw(preTeleport.getYaw());
                        spawnLocation.setPitch(preTeleport.getPitch());

                        player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
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
