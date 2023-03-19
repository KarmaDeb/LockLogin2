package es.karmadev.locklogin.common.api.protection.type;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.EncryptionConfiguration;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.api.protection.CHash;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualInput;

import java.nio.charset.StandardCharsets;

public class Argon2I extends PluginHash {

    /**
     * Get the hashing name
     *
     * @return the hashing name
     */
    @Override
    public String name() {
        return "argon2i";
    }

    /**
     * Hash the input
     *
     * @param input the input to hash
     * @return the hashed input
     */
    @Override
    public HashResult hash(final String input) {
        Argon2 argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2i);

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
        String finalProduct = argon.hash(encryption.iterations(), encryption.memory(), encryption.parallelism(), pwd);

        if (encryption.applyBase64()) {
            virtualized = CVirtualInput.of(virtualized.references(), virtualized.valid(), base64(finalProduct).getBytes(StandardCharsets.UTF_8));
        } else {
            virtualized = CVirtualInput.of(virtualized.references(), virtualized.valid(), finalProduct.getBytes(StandardCharsets.UTF_8));
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
        Argon2 argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2i);

        String product = new String(result.product().product());
        if (isBase64(product)) {
            product = base64(product);
        }

        Configuration configuration = CurrentPlugin.getPlugin().configuration();
        EncryptionConfiguration encryption = configuration.encryption();

        return argon.needsRehash(product, encryption.iterations(), encryption.memory(), encryption.parallelism())
                || !encryption.algorithm().equalsIgnoreCase("argon2i") || result.product().valid() != encryption.virtualID();
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
        Argon2 argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2i);

        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();

        VirtualizedInput virtualized = result.product();
        EncryptionConfiguration encryption = plugin.configuration().encryption();

        String token = new String(virtualized.product(), StandardCharsets.UTF_8);
        String password = input;
        if (virtualized.valid()) {
            try {
                VirtualizedInput clone = hasher.virtualID().virtualize(input, virtualized.references());
                password = new String(clone.product(), StandardCharsets.UTF_8);
            } catch (IllegalStateException i) {
                return false;
            }
        }

        if (isBase64(token)) {
            token = base64(token);
        }

        return argon.verify(token, password.toCharArray());
    }
}
