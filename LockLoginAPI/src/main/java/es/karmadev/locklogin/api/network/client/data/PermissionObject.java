package es.karmadev.locklogin.api.network.client.data;

import es.karmadev.locklogin.api.network.NetworkEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * @param ignoredNodes the nodes to ignore
     * @return if the specified permission applies to
     * this one or vice-versa
     */
    default boolean isPermissible(final NetworkEntity entity, final String... ignoredNodes) {
        if (entity.hasPermission(this)) return true;

        List<String> ignored = new ArrayList<>(Arrays.asList(ignoredNodes));
        ignored.add(node());

        PermissionObject top = topLevel();
        if (top.inheritance() && entity.hasPermission(top)) return true;

        for (PermissionObject child : top.children()) {
            if (!ignored.contains(child.node())) {
                if (child.isPermissible(entity, ignored.toArray(new String[0]))) return true;
            }
        }

        return false;
    }
}
