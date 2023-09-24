package es.karmadev.locklogin.api.security.hash;

/**
 * Legacy plugin hash
 */
public interface LegacyPluginHash {

    /**
     * Validates the legacy hash
     *
     * @param input the input
     * @param token the legacy hash token
     * @return if the input matches the token
     */
    boolean auth(final String input, final String token);
}
