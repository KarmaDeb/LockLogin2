package es.karmadev.locklogin.common.api.protection.type;

import com.google.common.hash.Hashing;
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
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.string.text.TextContent;
import ml.karmaconfigs.api.common.string.text.TextType;

import java.nio.charset.StandardCharsets;

public class SHA256Hash extends PluginHash {

    /**
     * Get the hashing name
     *
     * @return the hashing name
     */
    @Override
    public String name() {
        return "sha256";
    }

    private String hashInput(final String password) {
        String random_salt = new RandomString(RandomString.createBuilder().
                withSize(64)
                .withContent(TextContent.ONLY_LETTERS)
                .withType(TextType.ALL_UPPER)).create();

        return "$SHA256$" + random_salt + "$" + Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();
    }

    private boolean auth(final String password, final String token) {
        try {
            String[] data = token.split("\\$");
            String salt = data[2];

            String generated = hashInput(password);
            String generated_salt = generated.split("\\$")[2];
            generated = generated.replaceFirst(generated_salt, salt);

            return generated.equals(token);
        } catch (Throwable ex) {
            //Add compatibility with old SHA256 generation

            String old_token = token;
            if (token.contains("\\$")) {
                String[] data = token.split("\\$");

                int max = data.length - 1;

                StringBuilder removeFrom = new StringBuilder();
                for (int i = 0; i < max; i++) {
                    removeFrom.append("$").append(data[i]);
                }

                old_token = token.replace(removeFrom.toString(), "");
            }

            String hashed = hashInput(password);
            String salt = hashed.split("\\$")[2];

            hashed = hashed.replaceFirst("\\$SHA256\\$" + salt + "\\$", "");

            return old_token.equals(hashed);
        }
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
        String finalProduct = hashInput(pwd);

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
    public boolean needsRehash(HashResult result) {
        Configuration configuration = CurrentPlugin.getPlugin().configuration();
        EncryptionConfiguration encryption = configuration.encryption();

        return !encryption.algorithm().equalsIgnoreCase("sha256") || result.product().valid() != encryption.virtualID();
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

        return auth(password, token);
    }
}
