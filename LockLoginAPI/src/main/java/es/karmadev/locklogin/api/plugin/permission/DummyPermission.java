package es.karmadev.locklogin.api.plugin.permission;

import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.NonFinal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static lombok.ToString.Exclude;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
@SuppressWarnings("unused")
public class DummyPermission implements PermissionObject {

    String node;
    boolean inheritance;

    @NonFinal
    PermissionObject topLevel;

    @Exclude
    Set<PermissionObject> children = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Exclude
    Set<PermissionObject> parent = Collections.newSetFromMap(new ConcurrentHashMap<>());


    /**
     * Set the top level permission
     *
     * @param top the top permission
     * @return this dummy permission
     */
    public DummyPermission setTopLevel(final PermissionObject top) {
        topLevel = top;
        return this;
    }

    /**
     * Add a permission children
     *
     * @param permission the children
     * @return the modified permission
     */
    @Override
    public PermissionObject addChildren(final PermissionObject... permission) {
        children.addAll(Arrays.asList(permission));
        return this;
    }

    /**
     * Add a permission parent
     *
     * @param permission the parent
     * @return the modified permission
     */
    @Override
    public PermissionObject addParent(final PermissionObject... permission) {
        parent.addAll(Arrays.asList(permission));
        return this;
    }

    /**
     * Get the children permissions
     *
     * @return the children
     */
    @Override
    public PermissionObject[] children() {
        return children.toArray(new PermissionObject[0]);
    }

    /**
     * Get the parent permissions
     *
     * @return the parent
     */
    @Override
    public PermissionObject[] parent() {
        return parent.toArray(new PermissionObject[0]);
    }

    /**
     * Get the top level permission
     *
     * @return the top level permission
     */
    @Override
    public PermissionObject topLevel() {
        return null;
    }

    /**
     * Get if this permission is children of
     * the specified one
     *
     * @param permission the permission
     * @return if children
     */
    @Override
    public boolean isChildOf(final PermissionObject permission) {
        return false;
    }

    /**
     * Get if this permission is children of
     * the specified one
     *
     * @param permission the permission
     * @return if parent
     */
    @Override
    public boolean isParentOf(final PermissionObject permission) {
        return false;
    }

    /**
     * Create a dummy permission
     *
     * @param node the permission node
     * @param inheritance if the permission follows inheritance
     * @return the permission
     */
    public static DummyPermission of(final String node, final boolean inheritance) {
        return DummyPermission.of(node, inheritance, LockLoginPermission.LOCKLOGIN);
    }
}
