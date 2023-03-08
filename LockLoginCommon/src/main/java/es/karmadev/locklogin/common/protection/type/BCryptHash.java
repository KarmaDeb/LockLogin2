package es.karmadev.locklogin.common.protection.type;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.EncryptionConfiguration;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.protection.CHash;
import es.karmadev.locklogin.common.protection.virtual.CVirtualInput;

import java.nio.charset.StandardCharsets;

public final class BCryptHash extends PluginHash {

    /**
     * Get the hashing name
     *
     * @return the hashing name
     */
    @Override
    public String name() {
        return "bcrypt";
    }

    /**
     * Hash the input
     *
     * @param input the input to hash
     * @return the hashed input
     */
    @Override
    public HashResult hash(final String input) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();

        EncryptionConfiguration encryption = plugin.configuration().encryption();
        VirtualizedInput virtualized;

        if (encryption.virtualID()) {
            VirtualID id = hasher.virtualID();
            virtualized = id.virtualize(input);
        } else {
            virtualized = CVirtualInput.of(new int[0], false, input.getBytes(StandardCharsets.UTF_8));
        }

        String pwd = new String(virtualized.product(), StandardCharsets.UTF_8);
        String finalProduct = BCrypt.with(BCrypt.Version.VERSION_2Y, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).hashToString(encryption.parallelism(), pwd.toCharArray());

        if (encryption.applyBase64()) {
            virtualized = CVirtualInput.of(virtualized.refferences(), virtualized.valid(), base64(finalProduct).getBytes(StandardCharsets.UTF_8));
        } else {
            virtualized = CVirtualInput.of(virtualized.refferences(), virtualized.valid(), finalProduct.getBytes(StandardCharsets.UTF_8));
        }

        return CHash.of(this, virtualized);
    }

    /**
     * Get if the provided hash result needs a rehash
     *
     * @param result the hash result
     * @return if the hahs needs a rehash
     */
    @Override
    public boolean needsRehash(final HashResult result) {
        Configuration configuration = CurrentPlugin.getPlugin().configuration();
        EncryptionConfiguration encryption = configuration.encryption();

        return !encryption.algorithm().equalsIgnoreCase("bcrypt") || result.product().valid() != encryption.virtualID();
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
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();

        VirtualizedInput virtualized = result.product();
        EncryptionConfiguration encryption = plugin.configuration().encryption();

        String token = new String(virtualized.product(), StandardCharsets.UTF_8);
        String password = input;
        if (virtualized.valid()) {
            try {
                VirtualizedInput clone = hasher.virtualID().virtualize(input, virtualized.refferences());
                password = new String(clone.product(), StandardCharsets.UTF_8);
            } catch (IllegalStateException i) {
                return false;
            }
        }

        if (isBase64(token)) {
            token = base64(token);
        }

        return BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(password.toCharArray(), token).verified;
    }
}