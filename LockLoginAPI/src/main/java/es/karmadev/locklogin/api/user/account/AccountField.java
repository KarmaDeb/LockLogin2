package es.karmadev.locklogin.api.user.account;

/**
 * Valid fields
 */
public enum AccountField {
    /**
     * A field containing the user name
     */
    USERNAME,
    /**
     * A field containing the user unique id
     */
    UNIQUEID,
    /**
     * A field containing the user password
     */
    PASSWORD,
    /**
     * A field containing the user pin
     */
    PIN,
    /**
     * A field containing the user 2fa token
     */
    TOKEN_2FA,
    /**
     * A field containing the user panic token
     */
    PANIC,
    /**
     * A field containing the user 2fa status
     */
    STATUS_2FA,
    /**
     * A field containing the user session persistence configuration
     */
    SESSION_PERSISTENCE
}
