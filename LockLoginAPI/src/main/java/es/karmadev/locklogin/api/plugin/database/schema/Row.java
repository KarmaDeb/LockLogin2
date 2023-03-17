package es.karmadev.locklogin.api.plugin.database.schema;

/**
 * LockLogin valid rows
 */
public enum Row {
    /**
     * LockLogin row
     */
    ID("id"),
    /**
     * LockLogin row
     */
    PASSWORD("password"),
    /**
     * LockLogin row
     */
    PIN("pin"),
    /**
     * LockLogin row
     */
    TOKEN_2FA("2fa_token"),
    /**
     * LockLogin row
     */
    PANIC("panic"),
    /**
     * LockLogin row
     */
    STATUS_2FA("2fa"),
    /**
     * LockLogin row
     */
    LOGIN_CAPTCHA("captcha"),
    /**
     * LockLogin row
     */
    LOGIN_PASSWORD("login"),
    /**
     * LockLogin row
     */
    LOGIN_PIN("pin"),
    /**
     * LockLogin row
     */
    LOGIN_2FA("gAuth"),
    /**
     * LockLogin row
     */
    PERSISTENT("persistent"),
    /**
     * LockLogin row
     */
    CAPTCHA("code"),
    /**
     * LockLogin row
     */
    NAME("name"),
    /**
     * LockLogin row
     */
    ADDRESS("address"),
    /**
     * LockLogin row
     */
    PORT("port"),
    /**
     * LockLogin row
     */
    UUID("uuid"),
    /**
     * LockLogin row
     */
    PREMIUM_UUID("premium"),
    /**
     * LockLogin row
     */
    CONNECTION_TYPE("connection"),
    /**
     * LockLogin row
     */
    ACCOUNT_ID("account"),
    /**
     * LockLogin row
     */
    SESSION_ID("session"),
    /**
     * LockLogin row
     */
    LAST_SERVER("server"),
    /**
     * LockLogin row
     */
    PREV_SERVER("pre_server"),
    /**
     * LockLogin row
     */
    TRIES("tries"),
    /**
     * LockLogin row
     */
    BLOCKED("blocked"),
    /**
     * LockLogin row
     */
    REMAINING("remaining"),
    /**
     * LockLogin row
     */
    STATUS("panicking"),
    /**
     * LockLogin row
     */
    CREATED_AT("creation");

    /**
     * The row path
     */
    public final String name;

    /**
     * Initialize the row
     *
     * @param name the row name
     */
    Row(final String name) {
        this.name = name;
    }
}
