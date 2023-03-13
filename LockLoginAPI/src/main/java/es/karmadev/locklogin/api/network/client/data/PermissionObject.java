package es.karmadev.locklogin.api.network.client.data;

import es.karmadev.locklogin.api.network.NetworkEntity;

/**
 * LockLogin permission object
 */
@SuppressWarnings("unused")
public interface PermissionObject {

    /**
     * Get the permission node
     *
     * @return the permission node
     */
    String node();

    /**
     * Add a permission children
     *
     * @param permission the children
     * @return the modified permission
     */
    PermissionObject addChildren(final PermissionObject... permission);

    /**
     * Add a permission parent
     *
     * @param permission the parent
     * @return the modified permission
     */
    PermissionObject addParent(final PermissionObject... permission);

    /**
     * Get the children permissions
     *
     * @return the children
     */
    PermissionObject[] children();

    /**
     * Get the parent permissions
     *
     * @return the parent
     */
    PermissionObject[] parent();

    /**
     * Get the top level permission
     *
     * @return the top level permission
     */
    PermissionObject topLevel();

    /**
     * Get if the permission inherits from
     * his parents
     *
     * @return if having this permissions grants
     * the upper permissions
     */
    boolean inheritance();

    /**
     * Get if this permission is children of
     * the specified one
     *
     * @param permission the permission
     * @return if children
     */
    boolean isChildOf(final PermissionObject permission);

    /**
     * Get if this permission is children of
     * the specified one
     *
     * @param permission the permission
     * @return if parent
     */
    boolean isParentOf(final PermissionObject permission);

    /**
     * Get if the specified permission is permissible
     *
     * @param entity the entity to check permission with
     * @return if the specified permission applies to
     * this one or vice-versa
     */
    default boolean isPermissible(final NetworkEntity entity) {
        if (entity.hasPermission(this)) return true;

        PermissionObject top = topLevel();
        if (top.inheritance() && entity.hasPermission(top)) return true;

        for (PermissionObject child : top.children()) {
            if (child.node().equals(node())) continue;
            if (child.isPermissible(entity)) return true;
        }

        return false;
    }
}
