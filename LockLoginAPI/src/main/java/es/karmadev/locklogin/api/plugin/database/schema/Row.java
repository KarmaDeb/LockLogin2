package es.karmadev.locklogin.api.plugin.database.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LockLogin valid rows
 */
@AllArgsConstructor
@Getter
public enum Row {
    /**
     * LockLogin row
     */
    ID("id", RowType.INTEGER),
    /**
     * LockLogin row
     */
    PASSWORD("password", RowType.BLOB),
    /**
     * LockLogin row
     */
    PIN("pin", RowType.BLOB),
    /**
     * LockLogin row
     */
    TOKEN_2FA("2fa_token", RowType.BLOB),
    /**
     * LockLogin row
     */
    PANIC("panic", RowType.BLOB),
    /**
     * LockLogin row
     */
    STATUS_2FA("2fa", RowType.BOOLEAN),
    /**
     * LockLogin row
     */
    LOGIN_CAPTCHA("captcha", RowType.BOOLEAN),
    /**
     * LockLogin row
     */
    LOGIN_PASSWORD("login", RowType.BOOLEAN),
    /**
     * LockLogin row
     */
    LOGIN_PIN("pin", RowType.BOOLEAN),
    /**
     * LockLogin row
     */
    LOGIN_2FA("gAuth", RowType.BOOLEAN),
    /**
     * LockLogin row
     */
    PERSISTENT("persistent", RowType.BOOLEAN),
    /**
     * LockLogin row
     */
    CAPTCHA("code", RowType.VARCHAR),
    /**
     * LockLogin row
     */
    NAME("name", RowType.VARCHAR),
    /**
     * LockLogin row
     */
    EMAIL("email", RowType.VARCHAR),
    /**
     * LockLogin row
     */
    ADDRESS("address", RowType.VARCHAR),
    /**
     * LockLogin row
     */
    PORT("port", RowType.INTEGER),
    /**
     * LockLogin row
     */
    UUID("uuid", RowType.VARCHAR),
    /**
     * LockLogin row
     */
    PREMIUM_UUID("premium", RowType.BOOLEAN),
    /**
     * LockLogin row
     */
    CONNECTION_TYPE("connection", RowType.VARCHAR),
    /**
     * LockLogin row
     */
    ACCOUNT_ID("account", RowType.INTEGER),
    /**
     * LockLogin row
     */
    SESSION_ID("session", RowType.INTEGER),
    /**
     * LockLogin row
     */
    LAST_SERVER("server", RowType.INTEGER),
    /**
     * LockLogin row
     */
    PREV_SERVER("pre_server", RowType.INTEGER),
    /**
     * LockLogin row
     */
    TRIES("tries", RowType.INTEGER),
    /**
     * LockLogin row
     */
    BLOCKED("blocked", RowType.INTEGER),
    /**
     * LockLogin row
     */
    REMAINING("remaining", RowType.LONG),
    /**
     * LockLogin row
     */
    STATUS("panicking", RowType.BOOLEAN),
    /**
     * LockLogin row
     */
    CREATED_AT("creation", RowType.TIMESTAMP);

    /**
     * The row path
     */
    public final String name;

    private final RowType<?> rowType;
}
