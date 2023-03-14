package es.karmadev.locklogin.spigot.vault;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.PermissionConfiguration;
import es.karmadev.locklogin.spigot.SpigotPlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

@SuppressWarnings("unused")
public class VaultPermissionManager {

    private final Permission permission;
    private final Map<UUID, TransientPermissionData> trans = new ConcurrentHashMap<>();

    public VaultPermissionManager(final SpigotPlugin plugin) {
        RegisteredServiceProvider<Permission> provider = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        if (provider != null) {
            permission = provider.getProvider();
            if (!permission.isEnabled()) throw new IllegalStateException("Cannot initialize vault permission manager without a permission manager");
        } else {
            throw new IllegalStateException("Cannot initialize vault permission manager without a permission manager");
        }
    }

    public boolean hasPermission(final OfflinePlayer player, final PermissionObject permission) {
        if (player.isOnline()) return this.permission.playerHas(player.getPlayer(), permission.node());

        for (World world : Bukkit.getWorlds()) {
            if (this.permission.playerHas(world.getName(), player, permission.node())) return true;
        }

        try {
            return this.permission.playerHas(null, player, permission.node());
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public void removeAllPermission(final Player player) {
        boolean op = player.isOp();

        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        PermissionConfiguration permission_policy = configuration.permission();

        if (permission_policy.blockOperator() && op) player.setOp(false);
        String[] groups = permission.getPlayerGroups(player);
        List<String> permissions = new ArrayList<>();

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

        TransientPermissionData tdp = new TransientPermissionData(op, groups, permissions.toArray(new String[0]));
        trans.put(player.getUniqueId(), tdp);
    }

    public void restorePermissions(final Player player) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        PermissionConfiguration permission_policy = configuration.permission();

        TransientPermissionData tdp = trans.getOrDefault(player.getUniqueId(), null);
        if (tdp != null) {
            player.setOp(tdp.isOp() && !permission_policy.blockOperator());

            for (String group : tdp.getGroups()) {
                permission.playerAddGroup(player, group);
            }
            for (String node : tdp.getPermissions()) {
                permission.playerAdd(player, node);
            }
        }
    }
}

@AllArgsConstructor
class TransientPermissionData {

    @Getter
    private final boolean op;
    @Getter
    private final String[] groups;
    @Getter
    private final String[] permissions;
}
