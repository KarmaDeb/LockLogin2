package es.karmadev.locklogin.common.protection;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.common.protection.virtual.CVirtualId;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
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
        if (hash == null) throw new UnnamedHashException();

        if (hashes.stream().noneMatch((stored) -> stored.name().equals(hash.name()))) {
            String name = hash.name();
            if (StringUtils.isNullOrEmpty(name)) throw new UnnamedHashException(hash);

            hashes.add(hash);
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
     * Get the plugin virtual ID
     *
     * @return the plugin virtual ID
     */
    @Override
    public VirtualID virtualID() throws SecurityException {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

        return virtualID;
    }

    public final static String SHA_512 = "sha512";
    public final static String SHA_256 = "sha256";
    public final static String ARGON_2I = "argon2i";
    public final static String ARGON_2ID = "argon2id";
    public final static String BCRYPT = "bcrypt";
    public final static String BCRYPT_PHP = "bcrypt_php";
}
