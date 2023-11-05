package es.karmadev.locklogin.spigot.permission.vault;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.PermissionConfiguration;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.plugin.internal.PluginPermissionManager;
import es.karmadev.locklogin.spigot.SpigotPlugin;
import es.karmadev.locklogin.spigot.permission.TransientPermissionData;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VaultPM implements PluginPermissionManager<OfflinePlayer, String> {

    private final Permission permission;
    private final Map<UUID, TransientPermissionData<String, String>> transientPermissions = new ConcurrentHashMap<>();

    public VaultPM(final SpigotPlugin plugin) {
        RegisteredServiceProvider<Permission> provider = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        if (provider != null) {
            permission = provider.getProvider();
            if (!permission.isEnabled()) throw new IllegalStateException("Cannot initialize vault permission manager without a permission manager");
        } else {
            throw new IllegalStateException("Cannot initialize vault permission manager without a permission manager");
        }
    }

    /**
     * Get if the player has the permission
     *
     * @param player     the player
     * @param permission the permission
     * @return if the player has the permission
     */
    @Override
    public <T extends OfflinePlayer> boolean hasPermission(final T player, final String permission) {
        if (player.isOnline()) return this.permission.playerHas(player.getPlayer(), permission);

        for (World world : Bukkit.getWorlds()) {
            if (this.permission.playerHas(world.getName(), player, permission)) return true;
        }

        try {
            return this.permission.playerHas(null, player, permission);
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Remove all the permissions from the
     * player
     *
     * @param offlinePlayer the player
     */
    @Override
    public <T extends OfflinePlayer> void removeAllPermission(final T offlinePlayer) {
        Player onlinePlayer = null;
        if (offlinePlayer instanceof Player) {
            onlinePlayer = (Player) offlinePlayer;
        } else {
            if (offlinePlayer.isOnline()) {
                onlinePlayer = offlinePlayer.getPlayer();
            }
        }

        if (onlinePlayer == null) return;

        Player player = onlinePlayer;
        boolean op = player.isOp();

        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        PermissionConfiguration permission_policy = configuration.permission();

        if (permission_policy.blockOperator() && op) player.setOp(false);

        String[] groups = permission.getPlayerGroups(player);
        List<String> permissions = new ArrayList<>();
        if (permission_policy.removeEveryPermission()) {
            player.getEffectivePermissions().forEach((bukkitPermission) -> {
                String node = bukkitPermission.getPermission();
                boolean contains = false;
                for (String group : groups) {
                    try {
                        if (permission.groupHas((String) null, group, node)) {
                            contains = true;
                            break;
                        }
                    } catch (NullPointerException ex) {
                        for (World world : Bukkit.getWorlds()) {
                            if (permission.groupHas(world, group, node)) {
                                contains = true;
                                break;
                            }
                        }
                    }
                }

                permission.playerRemove(player, node);
                if (!contains) permissions.add(node);
            });
            for (String group : groups) permission.playerRemoveGroup(player, group);
        }

        TransientPermissionData<String, String> tdp = new TransientPermissionData<>(op, groups, permissions.toArray(new String[0]));
        transientPermissions.put(player.getUniqueId(), tdp);
    }

    /**
     * Restore all the permissions from the
     * player
     *
     * @param offlinePlayer the player
     */
    @Override
    public <T extends OfflinePlayer> void restorePermissions(final T offlinePlayer) {
        Player onlinePlayer = null;
        if (offlinePlayer instanceof Player) {
            onlinePlayer = (Player) offlinePlayer;
        } else {
            if (offlinePlayer.isOnline()) {
                onlinePlayer = offlinePlayer.getPlayer();
            }
        }

        if (onlinePlayer == null) return;

        Player player = onlinePlayer;
        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        PermissionConfiguration permission_policy = configuration.permission();

        TransientPermissionData<String, String> tdp = transientPermissions.getOrDefault(player.getUniqueId(), null);
        if (tdp != null) {
            player.setOp(tdp.isOp() && !permission_policy.blockOperator());

            for (String group : tdp.getGroups()) {
                if (permission.playerInGroup(player, group)) continue;
                permission.playerAddGroup(player, group);
            }

            for (String node : tdp.getPermissions()) {
                if (permission.playerHas(player, node)) continue;
                if (node.matches("(\\*|minecraft\\.\\*|bukkit\\.\\*)") && !permission_policy.allowWildcard()) continue;

                permission.playerAdd(player, node);
            }

            transientPermissions.remove(player.getUniqueId());
        }
    }

    /**
     * Apply the grants to the player
     *
     * @param offlinePlayer the player
     */
    @Override
    public <T extends OfflinePlayer> void applyGrants(final T offlinePlayer) {
        Player onlinePlayer = null;
        if (offlinePlayer instanceof Player) {
            onlinePlayer = (Player) offlinePlayer;
        } else {
            if (offlinePlayer.isOnline()) {
                onlinePlayer = offlinePlayer.getPlayer();
            }
        }

        if (onlinePlayer == null) return;

        Player player = onlinePlayer;
        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        PermissionConfiguration permission_policy = configuration.permission();

        int networkId = UserDataHandler.getNetworkId(player);
        if (networkId <= 0) return;

        NetworkClient entity = plugin.network().getPlayer(networkId);
        UserSession session = entity.session();

        if (session.isLogged()) {
            for (String grant : permission_policy.unLoggedGrants()) {
                if (grant.matches("(\\*|minecraft\\.\\*|bukkit\\.\\*)")) continue; //Uhm... no

                if (grant.startsWith("[group]")) {
                    permission.playerRemoveGroup(player, grant.substring(7));
                    continue;
                }

                permission.playerRemove(player, grant);
            }
            for (String grant : permission_policy.loggedGrants()) {
                if (grant.matches("(\\*|minecraft\\.\\*|bukkit\\.\\*)")) continue; //Uhm... no

                if (grant.startsWith("[group]")) {
                    permission.playerAddGroup(player, grant.substring(7));
                    continue;
                }

                permission.playerAdd(player, grant);
            }

            return;
        }

        for (String grant : permission_policy.unLoggedGrants()) {
            if (grant.matches("(\\*|minecraft\\.\\*|bukkit\\.\\*)")) continue; //Uhm... no

            if (grant.startsWith("[group]")) {
                permission.playerAddGroup(player, grant.substring(7));
                continue;
            }

            permission.playerAdd(player, grant);
        }
    }
}
