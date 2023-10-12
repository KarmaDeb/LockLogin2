package es.karmadev.locklogin.common.plugin.secure.totp;

/**
 * Represents a TotpHandler
 */
public interface TotpHandler {

    /**
     * Destroy the handler
     */
    void destroy();

    /**
     * Handle a TOTP action
     *
     * @param wasSuccess if the action was successful
     */
    void handle(final boolean wasSuccess);
}
