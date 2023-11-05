package es.karmadev.locklogin.api.plugin.permission;

import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.NonFinal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static lombok.ToString.Exclude;

@Accessors(fluent = true)
@Getter
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

    DummyPermission(final String node, final boolean inheritance) {
        this.node = node;
        this.inheritance = inheritance;
    }

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
        PermissionObject[] child = new PermissionObject[0];
        for (PermissionObject sub : children) {
            child = Arrays.copyOf(child, child.length + 1);
            child[child.length - 1] = sub;
        }

        return child;
    }

    /**
     * Get the parent permissions
     *
     * @return the parent
     */
    @Override
    public PermissionObject[] parent() {
        PermissionObject[] supper = new PermissionObject[0];
        for (PermissionObject sup : parent) {
            supper = Arrays.copyOf(supper, supper.length + 1);
            supper[supper.length - 1] = sup;
        }

        return supper;
    }

    /**
     * Get the top level permission
     *
     * @return the top level permission
     */
    @Override
    public PermissionObject topLevel() {
        return topLevel;
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
        for (PermissionObject child : children) {
            if (child.node().equals(permission.node())) return true;
        }

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
        for (PermissionObject supper : parent) {
            if (supper.node().equals(permission.node())) return true;
        }

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
        DummyPermission n = new DummyPermission(node, inheritance);
        n.topLevel = LockLoginPermission.PERMISSION_ADMINISTRATOR;

        return n;
    }
}
