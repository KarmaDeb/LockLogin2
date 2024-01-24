package es.karmadev.locklogin.common.api.protection.legacy;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.LegacyPluginHash;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.api.protection.CHash;
import es.karmadev.locklogin.common.api.protection.legacy.virtual.LegacyID;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualInput;

import java.nio.charset.StandardCharsets;

public class LegacyHash extends PluginHash {

    /**
     * Initialize the plugin hash
     */
    public LegacyHash() {
        super(true);
    }

    /**
     * Get the hashing name
     *
     * @return the hashing name
     */
    @Override
    public String name() {
        return "legacy";
    }

    /**
     * Hash the input
     *
     * @param input the input to hash
     * @return the hashed input
     */
    @Override
    public HashResult hash(final String input) {
        return CHash.of(this, CVirtualInput.raw(input.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Get if the provided hash result needs a rehash
     *
     * @param result the hash result
     * @return if the hash needs a rehash
     */
    @Override
    public boolean needsRehash(final HashResult result) {
        return false;
    }

    /**
     * Verify a hash
     *
     * @param input  the input to verify with
     * @param result the hashed input
     * @return if the input is correct
     */
    @Override
    public boolean verify(final String input, final HashResult result) {
        PluginHash hash = result.hasher();
        if (!hash.name().equals("legacy")) return false; //We will only support legacy hashed

        VirtualizedInput product = result.product();
        String token = new String(product.product(), StandardCharsets.UTF_8);

        if (isBase64(token)) {
            token = base64(token);
        }

        LockLogin plugin = CurrentPlugin.getPlugin();
        for (String method : plugin.hasher().getMethods()) {
            if (method.equals("legacy")) continue; //Ignore ourselves
            PluginHash h = plugin.hasher().getMethod(method);
            LegacyPluginHash legacy = h.legacyHasher();

            if (legacy != null) {
                /*
                Basically meaning the hash method supports legacy, implementations might implement this for example
                to add support back to migrate from AuthMe SHA, which was supported and a usable hash method in
                LockLogin legacy v2
                 */
                if (!legacy.auth(input, token)) {
                    LegacyID id = new LegacyID();
                    String virtualized = new String(id.virtualize(input).product(), StandardCharsets.UTF_8);

                    if (legacy.auth(virtualized, token)) {
                        return true;
                    }
                }

                return true;
            }
        }

        /*SHA512Hash sha = new SHA512Hash();
        String token = new String(product.product(), StandardCharsets.UTF_8);

        if (isBase64(token)) {
            token = base64(token);
        }
        if (!sha.auth(input, token)) {
            LegacyID id = new LegacyID();
            String virtualized = new String(id.virtualize(input).product(), StandardCharsets.UTF_8);

            return sha.auth(virtualized, token);
        }*/

        return false;
    }
}
