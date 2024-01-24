package es.karmadev.locklogin.spigot.permission.luckperms;

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
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LuckPermsPM implements PluginPermissionManager<OfflinePlayer, String> {

    private final LuckPerms permission;
    private final Map<UUID, TransientPermissionData<Group, Node>> transientPermissions = new ConcurrentHashMap<>();

    public LuckPermsPM(final SpigotPlugin plugin) {
        RegisteredServiceProvider<LuckPerms> provider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            permission = provider.getProvider();
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
        PlayerAdapter<OfflinePlayer> adapter = this.permission.getPlayerAdapter(OfflinePlayer.class);
        User user = adapter.getUser(player);

        return user.data().contains(Node.builder(permission).build(), NodeEqualityPredicate.ONLY_KEY).asBoolean();
        //I love how easy it is on LuckPerms, no loops, no null casts, just a simple check
    }

    /**
     * Remove all the permissions from the
     * player
     *
     * @param player the player
     */
    @Override
    public <T extends OfflinePlayer> void removeAllPermission(final T player) {
        boolean op = player.isOp();

        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        PermissionConfiguration permission_policy = configuration.permission();

        if (permission_policy.blockOperator() && op) player.setOp(false);

        Set<Group> groupList = new HashSet<>();
        Set<Node> nodeList = new HashSet<>();
        if (permission_policy.removeEveryPermission()) {
            UserManager manager = this.permission.getUserManager();
            GroupManager groupManager = this.permission.getGroupManager();

            PlayerAdapter<OfflinePlayer> adapter = this.permission.getPlayerAdapter(OfflinePlayer.class);
            User user = adapter.getUser(player);

            Group primary = groupManager.getGroup(user.getPrimaryGroup());
            Collection<Group> groups = user.getInheritedGroups(user.getQueryOptions());

            for (Group group : groups) {
                if (group.equals(primary)) continue;
                groupList.add(group);
            }

            for (Node node : user.getNodes(NodeType.PERMISSION)) { //We only want permissions
                if (groups.stream().anyMatch((g) -> g.data()
                        .contains(node, NodeEqualityPredicate.ONLY_KEY).asBoolean())) continue;

                nodeList.add(node); //Add only independent permissions (non-group granted)
            }

            manager.modifyUser(user.getUniqueId(), (modifiable) -> modifiable.data().clear((node) -> {
                nodeList.add(node);
                return true;
            }));
        }

        TransientPermissionData<Group, Node> tdp = new TransientPermissionData<>(op, groupList.toArray(new Group[0]), nodeList.toArray(new Node[0]));
        transientPermissions.put(player.getUniqueId(), tdp);
    }

    /**
     * Restore all the permissions from the
     * player
     *
     * @param player the player
     */
    @Override
    public <T extends OfflinePlayer> void restorePermissions(final T player) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        PermissionConfiguration permission_policy = configuration.permission();

        TransientPermissionData<Group, Node> tdp = transientPermissions.getOrDefault(player.getUniqueId(), null);
        if (tdp != null) {
            if (tdp.isOp() && !permission_policy.blockOperator()) player.setOp(true);

            UserManager manager = permission.getUserManager();
            PlayerAdapter<OfflinePlayer> adapter = permission.getPlayerAdapter(OfflinePlayer.class);
            User user = adapter.getUser(player);

            manager.modifyUser(user.getUniqueId(), (modifiable) -> {
                for (Group group : tdp.getGroups()) {
                    InheritanceNode inheritanceGroup = InheritanceNode.builder(group).build();
                    if (modifiable.data().contains(inheritanceGroup, NodeEqualityPredicate.ONLY_KEY).asBoolean())
                        continue;

                    DataMutateResult result = modifiable.data().add(inheritanceGroup);
                    if (!result.wasSuccessful()) {
                        plugin.warn("Failed to grant group {0} to client {1}",
                                group.getName(),
                                modifiable.getUsername());
                    }
                }

                for (Node node : tdp.getPermissions()) {
                    if (node.getKey().matches("(\\*|minecraft\\.\\*|bukkit\\.\\*)") &&
                            !permission_policy.allowWildcard()) continue;

                    if (modifiable.data().contains(node, NodeEqualityPredicate.ONLY_KEY).asBoolean())
                        continue;

                    DataMutateResult result = modifiable.data().add(node);
                    if (!result.wasSuccessful()) {
                        plugin.warn("Failed to grant node {0} to client {1}",
                                node.getKey(),
                                modifiable.getUsername());
                    }
                }
            });

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

        UserManager manager = permission.getUserManager();
        PlayerAdapter<OfflinePlayer> adapter = permission.getPlayerAdapter(OfflinePlayer.class);
        User user = adapter.getUser(player);

        manager.modifyUser(user.getUniqueId(), (modifiable) -> {
            if (session.isLogged()) {
                for (String grant : permission_policy.unLoggedGrants()) {
                    if (grant.matches("(\\*|minecraft\\.\\*|bukkit\\.\\*)")) continue; //Uhm... no

                    if (grant.startsWith("[group]")) {
                        String groupName = grant.substring(7);
                        Group group = permission.getGroupManager().getGroup(groupName);
                        if (group == null) continue;

                        modifiable.data().remove(InheritanceNode.builder(group).build());
                        continue;
                    }

                    modifiable.data().remove(Node.builder(grant).build());
                }
                for (String grant : permission_policy.loggedGrants()) {
                    if (grant.matches("(\\*|minecraft\\.\\*|bukkit\\.\\*)")) continue; //Uhm... no

                    if (grant.startsWith("[group]")) {
                        String groupName = grant.substring(7);
                        Group group = permission.getGroupManager().getGroup(groupName);
                        if (group == null) continue;

                        modifiable.data().add(InheritanceNode.builder(group).build());
                        continue;
                    }

                    modifiable.data().add(Node.builder(grant).build());
                }
                return;
            }

            for (String grant : permission_policy.unLoggedGrants()) {
                if (grant.matches("(\\*|minecraft\\.\\*|bukkit\\.\\*)")) continue; //Uhm... no

                if (grant.startsWith("[group]")) {
                    String groupName = grant.substring(7);
                    Group group = permission.getGroupManager().getGroup(groupName);
                    if (group == null) continue;

                    modifiable.data().add(InheritanceNode.builder(group).build());
                    continue;
                }

                modifiable.data().add(Node.builder(grant).build());
            }
        });
    }
}
