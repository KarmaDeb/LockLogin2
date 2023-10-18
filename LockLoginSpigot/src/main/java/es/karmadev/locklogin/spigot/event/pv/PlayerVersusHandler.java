package es.karmadev.locklogin.spigot.event.pv;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.file.spawn.CancelPolicy;
import es.karmadev.locklogin.api.plugin.file.spawn.SpawnConfiguration;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class PlayerVersusHandler implements Listener {

    private final LockLoginSpigot plugin;

    public PlayerVersusHandler(final LockLoginSpigot plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerVersus(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();

        if (entity instanceof Player && damager instanceof Player) {
            Player attacker = (Player) entity;
            Player victim = (Player) damager;
            if (handlePVP(attacker, victim)) {
                e.setCancelled(true);
            }
            return;
        }

        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (handlePVE(player, e.getDamager(), true)) {
                e.setCancelled(true);
            }
            return;
        }
        if (damager instanceof Player) {
            Player player = (Player) damager;
            if (handlePVE(player, entity, false)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerVersusBlock(EntityDamageByBlockEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (handlePVE(player)) {
                e.setCancelled(true);
            }
        }
    }

    private boolean handlePVP(final Player attacker, final Player victim) {
        int attackerId = UserDataHandler.getNetworkId(attacker);
        int victimId = UserDataHandler.getNetworkId(victim);
        if (attackerId <= 0 || victimId <= 0) return false;

        NetworkClient attackerClient = plugin.network().getPlayer(attackerId);
        NetworkClient victimClient = plugin.network().getPlayer(attackerId);
        if (attackerClient == null || victimClient == null) return false;


        UserSession attackerSession = attackerClient.session();
        UserSession victimSession = victimClient.session();

        boolean attackerLogged = false;
        boolean victimLogged = false;
        SpawnConfiguration config = plugin.configuration().spawn();
        Messages messages = plugin.messages();

        if (attackerSession.isLogged() && attackerSession.isTotpLogged() && attackerSession.isPinLogged()) {
            attackerLogged = true;

            if (UserDataHandler.isTeleporting(attacker)) {
                if (config.cancelWithPolicy(CancelPolicy.PVP)) {
                    UserDataHandler.setTeleporting(attacker, null);
                    attackerClient.sendMessage(messages.prefix() + messages.spawnCancelled());
                }
            }
        }
        if (victimSession.isLogged() && victimSession.isTotpLogged() && victimSession.isPinLogged()) {
            victimLogged = true;

            if (UserDataHandler.isTeleporting(victim)) {
                if (config.cancelWithPolicy(CancelPolicy.PVP)) {
                    UserDataHandler.setTeleporting(attacker, null);
                    victimClient.sendMessage(messages.prefix() + messages.spawnCancelled());
                }
            }
        }

        if (attackerLogged && victimLogged) return false; //Simply allow
        if (attackerLogged) {
            attackerClient.sendMessage(messages.prefix() + messages.notVerified(victimClient));
        }
        return true;
    }

    private boolean handlePVE(final Player subject1, final Entity subject2, final boolean entityToPlayer) {
        int networkId = UserDataHandler.getNetworkId(subject1);
        if (networkId <= 0) return false;

        NetworkClient client = plugin.network().getPlayer(networkId);
        if (client == null) return false;

        UserSession session = client.session();
        if (session.isLogged() && session.isPinLogged() && session.isTotpLogged()) {
            if (UserDataHandler.isTeleporting(subject1)) {
                SpawnConfiguration config = plugin.configuration().spawn();
                Messages messages = plugin.messages();

                if (config.cancelWithPolicy(CancelPolicy.PVE)) {
                    UserDataHandler.setTeleporting(subject1, null);
                    client.sendMessage(messages.prefix() + messages.spawnCancelled());
                }
            }

            return false;
        }

        if (entityToPlayer) {
            Vector direction = subject1.getLocation().toVector().subtract(subject2.getLocation().toVector()).normalize();
            Vector yMod = new Vector(0, 0.25, 0);
            direction.add(yMod);

            subject1.setVelocity(yMod);
            //Throw the entity at the player's opposite direction
        }

        return true;
    }

    private boolean handlePVE(final Player victim) {
        int networkId = UserDataHandler.getNetworkId(victim);
        if (networkId <= 0) return false;

        NetworkClient client = plugin.network().getPlayer(networkId);
        if (client == null) return false;

        if (client.session().isLogged() && client.session().isTotpLogged() && client.session().isPinLogged()) {
            //We're fully logged int
            if (UserDataHandler.isTeleporting(victim)) {
                SpawnConfiguration config = plugin.configuration().spawn();
                Messages messages = plugin.messages();

                if (config.cancelWithPolicy(CancelPolicy.PVE)) {
                    UserDataHandler.setTeleporting(victim, null);
                    client.sendMessage(messages.prefix() + messages.spawnCancelled());
                }
            }

            return false;
        }

        return true;
    }
}
