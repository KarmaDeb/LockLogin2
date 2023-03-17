package es.karmadev.locklogin.api.plugin.permission;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Official LockLogin permissions
 */
@SuppressWarnings("unused")
public final class LockLoginPermission {

    /**
     * LockLogin administrators permission
     * <p>locklogin.administrator</p>
     */
    final static PermissionObject LOCKLOGIN = DummyPermission.of("locklogin.administrator", true);

    /**
     * LockLogin reload permission
     * <p>locklogin.reload</p>
     */
    public final static PermissionObject PERMISSION_RELOAD = DummyPermission.of("locklogin.reload", false);

    /**
     * LockLogin location management permission
     * <p>locklogin.location</p>
     */
    public final static PermissionObject PERMISSION_LOCATION = DummyPermission.of("locklogin.location", true);

    /**
     * LockLogin spawn management permission
     * <p>locklogin.location.spawn</p>
     */
    public final static PermissionObject PERMISSION_LOCATION_SPAWN = DummyPermission.of("locklogin.location.spawn", false);

    /**
     * LockLogin client last location management permission
     * <p>locklogin.location.client</p>
     */
    public final static PermissionObject PERMISSION_LOCATION_CLIENT = DummyPermission.of("locklogin.location.client", false);

    /**
     * LockLogin client info permission
     * <p>locklogin.info</p>
     */
    public final static PermissionObject PERMISSION_INFO = DummyPermission.of("locklogin.info", true);

    /**
     * LockLogin client info request permission
     * <p>locklogin.info.request</p>
     */
    public final static PermissionObject PERMISSION_INFO_REQUEST = DummyPermission.of("locklogin.info.request", false);

    /**
     * LockLogin client potential alt permission
     * <p>locklogin.info.al</p>
     */
    public final static PermissionObject PERMISSION_INFO_ALT = DummyPermission.of("locklogin.info.alt", true);

    /**
     * LockLogin client potential alt alert permission
     * <p>locklogin.info.alt.alert</p>
     */
    public final static PermissionObject PERMISSION_INFO_ALT_ALERT = DummyPermission.of("locklogin.info.alt.alert", false);

    /**
     * LockLogin client account management permission
     * <p>locklogin.account</p>
     */
    public final static PermissionObject PERMISSION_ACCOUNT = DummyPermission.of("locklogin.account", true);

    /**
     * LockLogin client session close permission
     * <p>locklogin.account.close</p>
     */
    public final static PermissionObject PERMISSION_ACCOUNT_CLOSE = DummyPermission.of("locklogin.account.close", false);

    /**
     * LockLogin client account removal permission
     * <p>locklogin.account.remove</p>
     */
    public final static PermissionObject PERMISSION_ACCOUNT_REMOVE = DummyPermission.of("locklogin.account.remove", false);

    /**
     * LockLogin client account password management permission
     * <p>locklogin.account.password</p>
     */
    public final static PermissionObject PERMISSION_ACCOUNT_PASSWORD = DummyPermission.of("locklogin.account.password", false);

    /**
     * LockLogin client account pin management permission
     * <p>locklogin.account.pin</p>
     */
    public final static PermissionObject PERMISSION_ACCOUNT_PIN = DummyPermission.of("locklogin.account.pin", false);

    /**
     * LockLogin client account 2fa management permission
     * <p>locklogin.account.2fa</p>
     */
    public final static PermissionObject PERMISSION_ACCOUNT_2FA = DummyPermission.of("locklogin.account.2fa", false);

    /**
     * LockLogin client account creation permission
     * <p>locklogin.account.create</p>
     */
    public final static PermissionObject PERMISSION_ACCOUNT_CREATE = DummyPermission.of("locklogin.account.create", false);

    /**
     * LockLogin force 2fa status permission
     * <p>locklogin.force2fa</p>
     */
    public final static PermissionObject PERMISSION_FORCE_2FA = DummyPermission.of("locklogin.force2fa", false);

    /**
     * LockLogin alias management permission
     * <p>locklogin.alias</p>
     */
    public final static PermissionObject PERMISSION_ALIAS = DummyPermission.of("locklogin.alias", false);

    /**
     * LockLogin module management permission
     * <p>locklogin.module</p>
     */
    public final static PermissionObject PERMISSION_MODULE = DummyPermission.of("locklogin.module", true);

    /**
     * LockLogin module loading permission
     * <p>locklogin.module.load</p>
     */
    public final static PermissionObject PERMISSION_MODULE_LOAD = DummyPermission.of("locklogin.module.load", false);

    /**
     * LockLogin module unloading permission
     * <p>locklogin.module.unload</p>
     */
    public final static PermissionObject PERMISSION_MODULE_UNLOAD = DummyPermission.of("locklogin.module.unload", false);

    /**
     * LockLogin module reloading permission
     * <p>locklogin.module.reload</p>
     */
    public final static PermissionObject PERMISSION_MODULE_RELOAD = DummyPermission.of("locklogin.module.reload", false);

    /**
     * LockLogin module listing permission
     * <p>locklogin.module.list</p>
     */
    public final static PermissionObject PERMISSION_MODULE_LIST = DummyPermission.of("locklogin.module.list", false);

    /**
     * LockLogin join in unauthorized server permission
     * For example, a logged player trying to join the auth server. Without
     * this permission, it will be impossible for him
     * <p>locklogin.join.unauthorized</p>
     */
    public final static PermissionObject PERMISSION_JOIN_UNAUTHORIZED = DummyPermission.of("locklogin.join.unauthorized", false);

    /**
     * LockLogin unsafe password permission
     * <p>locklogin.unsafe</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE = DummyPermission.of("locklogin.unsafe", false);

    /**
     * LockLogin unsafe bypass permission
     * <p>locklogin.unsafe.bypass</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE_BYPASS = DummyPermission.of("locklogin.unsafe.bypass", true);

    /**
     * LockLogin unsafe bypass permission
     * <p>locklogin.unsafe.bypass.common</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE_BYPASS_COMMON = DummyPermission.of("locklogin.unsafe.bypass.common", false);

    /**
     * LockLogin unsafe bypass permission
     * <p>locklogin.unsafe.bypass.length</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE_BYPASS_LENGTH = DummyPermission.of("locklogin.unsafe.bypass.length", false);

    /**
     * LockLogin unsafe bypass permission
     * <p>locklogin.unsafe.bypass.special</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE_BYPASS_SPECIAL = DummyPermission.of("locklogin.unsafe.bypass.special", false);

    /**
     * LockLogin unsafe bypass permission
     * <p>locklogin.unsafe.bypass.numbers</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE_BYPASS_NUMBERS = DummyPermission.of("locklogin.unsafe.bypass.numbers", false);

    /**
     * LockLogin unsafe bypass permission
     * <p>locklogin.unsafe.bypass.lowercase</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE_BYPASS_LOWER = DummyPermission.of("locklogin.unsafe.bypass.lowercase", false);

    /**
     * LockLogin unsafe bypass permission
     * <p>locklogin.unsafe.bypass.uppercase</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE_BYPASS_UPPER = DummyPermission.of("locklogin.unsafe.bypass.uppercase", false);

    /**
     * LockLogin unsafe password used notification permission
     * <p>locklogin.unsafe.warning</p>
     */
    public final static PermissionObject PERMISSION_UNSAFE_WARNING = DummyPermission.of("locklogin.unsafe.warning", false);

    private final static Map<String, ModulePermission> module_node_map = new LinkedHashMap<>();
    private final static Map<String, PermissionObject> official_node_map = new LinkedHashMap<>();

    static {
        PERMISSION_LOCATION.addChildren(
                PERMISSION_LOCATION_SPAWN.addParent(PERMISSION_LOCATION),
                PERMISSION_LOCATION_CLIENT.addParent(PERMISSION_LOCATION)
        );

        PERMISSION_INFO.addChildren(
                PERMISSION_INFO_REQUEST.addParent(PERMISSION_INFO),
                PERMISSION_INFO_ALT.addParent(PERMISSION_INFO),
                PERMISSION_INFO_ALT_ALERT.addParent(PERMISSION_INFO, PERMISSION_INFO_ALT)
        );

        PERMISSION_ACCOUNT.addChildren(
                PERMISSION_ACCOUNT_CLOSE.addParent(PERMISSION_ACCOUNT),
                PERMISSION_ACCOUNT_REMOVE.addParent(PERMISSION_ACCOUNT),
                PERMISSION_ACCOUNT_PASSWORD.addParent(PERMISSION_ACCOUNT),
                PERMISSION_ACCOUNT_PIN.addParent(PERMISSION_ACCOUNT),
                PERMISSION_ACCOUNT_2FA.addParent(PERMISSION_ACCOUNT),
                PERMISSION_ACCOUNT_CREATE.addParent(PERMISSION_ACCOUNT)
        );

        PERMISSION_MODULE.addChildren(
                PERMISSION_MODULE_LOAD.addParent(PERMISSION_MODULE),
                PERMISSION_MODULE_UNLOAD.addParent(PERMISSION_MODULE),
                PERMISSION_MODULE_RELOAD.addParent(PERMISSION_MODULE),
                PERMISSION_MODULE_LIST.addParent(PERMISSION_MODULE)
        );

        PERMISSION_UNSAFE.addChildren(
                PERMISSION_UNSAFE_BYPASS.addParent(PERMISSION_UNSAFE)
                        .addChildren(
                                PERMISSION_UNSAFE_BYPASS_COMMON.addParent(
                                        PERMISSION_UNSAFE_BYPASS),
                                PERMISSION_UNSAFE_BYPASS_LENGTH.addParent(
                                        PERMISSION_UNSAFE_BYPASS),
                                PERMISSION_UNSAFE_BYPASS_SPECIAL.addParent(
                                        PERMISSION_UNSAFE_BYPASS),
                                PERMISSION_UNSAFE_BYPASS_NUMBERS.addParent(
                                        PERMISSION_UNSAFE_BYPASS),
                                PERMISSION_UNSAFE_BYPASS_LOWER.addParent(
                                        PERMISSION_UNSAFE_BYPASS),
                                PERMISSION_UNSAFE_BYPASS_UPPER.addParent(
                                        PERMISSION_UNSAFE_BYPASS)
                        ),
                PERMISSION_UNSAFE_WARNING.addParent(PERMISSION_UNSAFE)
        );

        Field[] fields = LockLoginPermission.class.getDeclaredFields(); //This is the easiest way
        for (Field field : fields) {
            if (field.getType().equals(PermissionObject.class)) {
                try {
                    PermissionObject permission = (PermissionObject) field.get(LockLoginPermission.class);
                    if (!permission.equals(LOCKLOGIN)) {
                        LOCKLOGIN.addChildren(permission.addParent(LOCKLOGIN));
                    }

                    official_node_map.put(permission.node(), permission);
                } catch (IllegalAccessException ignored) {} //We shouldn't receive this, we know it's public and static
            }
        }
    }

    /**
     * Get a permission by its node name
     *
     * @param node the permission node
     * @return the permission
     */
    public static PermissionObject byNode(final String node) {
        PermissionObject permission = official_node_map.getOrDefault(node, null);
        if (permission == null) {
            ModulePermission modPermission = module_node_map.getOrDefault(node, null);
            if (modPermission != null && modPermission.getModule().isLoaded()) {
                return modPermission.getPermission();
            }
        }

        return null;
    }

    /**
     * Register a permission
     *
     * @param permission the permission to register
     * @return the permission
     *
     * @throws SecurityException if a non-module or unloaded module tries
     * to register a permission
     */
    public static boolean register(final PermissionObject permission) throws SecurityException {
        if (!module_node_map.containsKey(permission.node()) &&
                module_node_map.values().stream().noneMatch((np) -> np.getPermission().node().equals(permission.node()))) {
            LockLogin plugin = CurrentPlugin.getPlugin();
            plugin.runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

            Path caller = plugin.runtime().caller();
            if (caller == null) throw new SecurityException("Cannot maintain a secure permission policy with a null API iterator");

            Module module = plugin.moduleManager().loader().load(caller);
            if (module == null || !module.isLoaded()) throw new SecurityException("Cannot maintain a secure permission policy with an invalid API iterator");

            module_node_map.put(permission.node(), new ModulePermission(module, permission.addParent(LOCKLOGIN.addChildren(permission))));
            return true;
        }

        return false;
    }

    /**
     * Unregister a permission
     *
     * @param permission the permission to unregister
     * @return the permission
     * @throws SecurityException if a non-module or unloaded module tries to unregister a permission, or if
     * a module tries to modify the permissions of another module
     */
    public static boolean unregister(final PermissionObject permission) throws SecurityException {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

        Path caller = plugin.runtime().caller();
        if (caller == null) throw new SecurityException("Cannot maintain a secure permission policy with a null API iterator");

        Module module = plugin.moduleManager().loader().load(caller);
        if (module == null || !module.isLoaded()) throw new SecurityException("Cannot maintain a secure permission policty with an invalid API iterator");

        ModulePermission modPermission = module_node_map.getOrDefault(permission.node(), null);
        if (modPermission != null) {
            if (!modPermission.getModule().equals(module)) throw new SecurityException("Cannot modify other module permissions!");
            module_node_map.remove(permission.node());

            return true;
        }

        return false;
    }
}

@AllArgsConstructor
class ModulePermission {

    @Getter
    private final Module module;
    @Getter
    private final PermissionObject permission;
}