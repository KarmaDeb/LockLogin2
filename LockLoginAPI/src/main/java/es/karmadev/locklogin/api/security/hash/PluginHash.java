package es.karmadev.locklogin.api.security.hash;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * LockLogin hashing method
 */
public abstract class PluginHash implements Serializable {

    /**
     * Get the hashing name
     *
     * @return the hashing name
     */
    public abstract String name();

    /**
     * Hash the input
     *
     * @param input the input to hash
     * @return the hashed input
     */
    public abstract HashResult hash(final String input);

    /**
     * Verify a hash
     *
     * @param input the input to verify with
     * @param result the hashed input
     * @return if the input is correct
     */
    public abstract boolean verify(final String input, final HashResult result);

    /**
     * Converses a base64 string into a raw string, or vice-versa
     *
     * @param input the input to encode
     * @return the result
     */
    protected final String base64(final String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get if the input is a base64 string
     *
     * @param input the input to check
     * @return if the input is a base64 string
     */
    protected final boolean isBase64(final String input) {
        String regex = "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";

        Pattern patron = Pattern.compile(regex);
        return patron.matcher(input).matches();
    }
}
