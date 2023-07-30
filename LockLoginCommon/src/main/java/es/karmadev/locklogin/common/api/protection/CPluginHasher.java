package es.karmadev.locklogin.common.api.protection;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CPluginHasher implements LockLoginHasher {

    private final Set<PluginHash> hashes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final VirtualID virtualID = new CVirtualId();

    /**
     * Register a new hash method
     *
     * @param hash the hash method
     * @throws UnnamedHashException if the hash method is unnamed
     */
    @Override
    public void registerMethod(final PluginHash hash) throws UnnamedHashException {
        if (hash == null || ObjectUtils.isNullOrEmpty(hash.name())) throw new UnnamedHashException();
        LockLogin plugin = CurrentPlugin.getPlugin();

        if (hashes.stream().noneMatch((stored) -> stored.name().equals(hash.name()))) {
            plugin.info("Validating hash {0}", hash.name());
            plugin.logInfo("Preparing to register hash {0}", hash.name());

            String test = StringUtils.generateString();
            try {
                HashResult result = hash.hash(test);
                if (result == null) {
                    plugin.warn("Cannot register hashing method {0} because it does not provide a valid hasher", hash.name());
                    plugin.logErr("Failed to register hash method {0}. It does not provide a valid hasher", hash.name());

                    return;
                } else {
                    plugin.logInfo("Hash {0} passed check #1", hash.name());
                }

                VirtualizedInput input = result.product();
                String virtual = new String(input.product());

                String raw = virtual;
                try {
                    int[] references = input.references();
                    int startIndex = references[0];

                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < virtual.length(); i++) {
                        int realIndex = references[i + 1];
                        builder.append(virtual.charAt(realIndex));
                    }

                    raw = builder.substring(startIndex);
                } catch (IndexOutOfBoundsException ignored) {
                    /*
                    Only possible if virtual ID is disabled. We won't call the configuration during
                    this process for security reasons. So we can't really determine if the virtual ID is
                    enabled. That's why, we will just try to resolve the virtualized input, if not, then
                    it means that it hasn't been virtualized
                     */
                }

                if (raw.equalsIgnoreCase(test)) {
                    plugin.warn("Cannot register hashing method {0} because it's not safe for production use (THIS HASHING METHOD DOES NOT HASH)", hash.name());
                    plugin.logErr("Failed to register hash method {0}. It does not perform any type of hash", hash.name());

                    return;
                } else {
                    plugin.logInfo("Hash {0} passed check #2", hash.name());
                }

                if (!hash.verify(test, result)) {
                    plugin.warn("Cannot register hashing method {0} because it does not provide a valid hash validator", hash.name());
                    plugin.logErr("Failed to register hash method {0}. It does not provide a valid hash validator", hash.name());

                    return;
                } else {
                    plugin.logInfo("Hash {0} passed check #3", hash.name());
                }

                hashes.add(hash);
                plugin.logInfo("Registered hash method {0}", hash.name());
                plugin.info("Registered hash method {0}", hash.name());
            } catch (Throwable ex) {
                ex.printStackTrace();
                plugin.log(ex, "Failed to register hashing method");
                plugin.logInfo("An exception was raised while trying to register hash method {0}", hash.name());
                plugin.err("An error occurred while registering hashing method {0}", hash.name());
            }
        } else {
            plugin.err("Failed to register hash {0} because it is already registered", hash.name());
            plugin.logErr("Failed to register hash method {0}. It is already registered", hash.name());
        }
    }

    /**
     * Tries to unregister the hash method
     *
     * @param name the hash method name to unregister
     * @return if the method could be removed
     */
    @Override
    public boolean unregisterMethod(final String name) {
        return hashes.removeIf((stored) -> stored.name().equals(name));
    }

    /**
     * Get the hashing method
     *
     * @param name the hash method
     * @return the hashing method
     */
    @Override
    public PluginHash getMethod(final String name) {
        Optional<PluginHash> result = hashes.stream().filter((stored) -> stored.name().equals(name)).findFirst();
        return result.orElse(null);
    }

    /**
     * Get all the compatible hashing method
     *
     * @return the hashing methods
     */
    @Override
    public String[] getMethods() {
        List<String> methods = new ArrayList<>();
        for (PluginHash hash : hashes) {
            methods.add(hash.name());
        }

        return methods.toArray(new String[0]);
    }

    /**
     * Get the plugin virtual ID
     *
     * @return the plugin virtual ID
     */
    @Override
    public VirtualID virtualID() throws SecurityException {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.getRuntime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

        return virtualID;
    }
}
