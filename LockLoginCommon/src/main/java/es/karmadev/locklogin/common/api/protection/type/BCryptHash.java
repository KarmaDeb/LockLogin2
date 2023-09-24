package es.karmadev.locklogin.common.api.protection.type;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.EncryptionConfiguration;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.LegacyPluginHash;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.api.protection.CHash;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualInput;

import java.nio.charset.StandardCharsets;

public final class BCryptHash extends PluginHash implements LegacyPluginHash {

    public BCryptHash() {
        super("version");
        permanent_properties.put("version", "VERSION_2A");
    }

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
            virtualized = CVirtualInput.raw(input.getBytes(StandardCharsets.UTF_8));
        }

        BCrypt.Version version = getMainVersion();

        String pwd = new String(virtualized.product(), StandardCharsets.UTF_8);
        String finalProduct = BCrypt.with(version, LongPasswordStrategies.hashSha512(version)).hashToString(Math.min(Math.max(3, encryption.parallelism()), 12), pwd.toCharArray());

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
     * @return if the hash needs a rehash
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

        BCrypt.Version checkVersion = getMainVersion();

        boolean valid = BCrypt.verifyer(checkVersion, LongPasswordStrategies.hashSha512(checkVersion)).verify(password.toCharArray(), token).verified;
        if (valid) return true;

        for (BCrypt.Version version : BCrypt.Version.SUPPORTED_VERSIONS) {
            if (BCrypt.verifyer(version, LongPasswordStrategies.hashSha512(version)).verify(password.toCharArray(), token).verified) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the principal version to use
     *
     * @return the bcrypt version to use
     */
    private BCrypt.Version getMainVersion() {
        String version = getProperty("version");
        BCrypt.Version bCryptVersion = BCrypt.Version.VERSION_2A;
        switch (version.toUpperCase()) {
            case "VERSION_2Y":
            case "2Y":
            case "PHP":
                bCryptVersion = BCrypt.Version.VERSION_2Y;
                break;
            case "VERSION_2Y_LESS":
            case "2YL":
                bCryptVersion = BCrypt.Version.VERSION_2Y_NO_NULL_TERMINATOR;
                break;
            case "VERSION_2B":
            case "2B":
                bCryptVersion = BCrypt.Version.VERSION_2B;
                break;
            case "VERSION_2C":
            case "2C":
                bCryptVersion = BCrypt.Version.VERSION_BC;
                break;
            case "VERSION_2X":
            case "2X":
                bCryptVersion = BCrypt.Version.VERSION_2X;
                break;
            case "VERSION_2A":
            case "2A":
            default:
                break;
        }

        return bCryptVersion;
    }

    /**
     * Validates the legacy hash
     *
     * @param input the input
     * @param token the legacy hash token
     * @return if the input matches the token
     */
    @Override
    public boolean auth(final String input, final String token) {
        BCrypt.Version checkVersion = getMainVersion();

        boolean valid = BCrypt.verifyer(checkVersion, LongPasswordStrategies.hashSha512(checkVersion)).verify(input.toCharArray(), token).verified;
        if (valid) return true;

        for (BCrypt.Version version : BCrypt.Version.SUPPORTED_VERSIONS) {
            if (BCrypt.verifyer(version, LongPasswordStrategies.hashSha512(version)).verify(input.toCharArray(), token).verified) {
                return true;
            }
        }

        return false;
    }
}