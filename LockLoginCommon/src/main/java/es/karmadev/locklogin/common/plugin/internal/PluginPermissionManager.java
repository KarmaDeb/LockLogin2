package es.karmadev.locklogin.common.plugin.internal;

/**
 * Represents a plugin permission manager. Used by
 * LockLogin to manage permissions without the need to
 * know which permission plugin we are using
 * @param <PlayerType>> the player type
 * @param <PermissionType> the permission type
 */
public interface PluginPermissionManager<PlayerType, PermissionType> {

    /**
     * Get if the player has the permission
     *
     * @param player the player
     * @param permission the permission
     * @return if the player has the permission
     * @param <T> the player type
     */
    <T extends PlayerType> boolean hasPermission(final T player, final PermissionType permission);

    /**
     * Remove all the permissions from the
     * player
     *
     * @param player the player
     * @param <T> the player type
     */
    <T extends PlayerType> void removeAllPermission(final T player);

    /**
     * Restore all the permissions from the
     * player
     *
     * @param player the player
     * @param <T> the player type
     */
    <T extends PlayerType> void restorePermissions(final T player);

    /**
     * Apply the grants to the player
     *
     * @param player the player
     * @param <T> the player type
     */
    <T extends PlayerType> void applyGrants(T player);
}
