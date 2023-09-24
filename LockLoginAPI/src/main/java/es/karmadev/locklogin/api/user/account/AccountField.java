package es.karmadev.locklogin.api.user.account;

import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.security.hash.HashResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Valid fields
 */
@AllArgsConstructor
public enum AccountField {
    /**
     * A field containing the username
     */
    USERNAME(Table.USER, Row.NAME, String.class),
    /**
     * A field containing the user email address
     */
    EMAIL(Table.USER, Row.EMAIL, String.class),
    /**
     * A field containing the user unique id
     */
    UNIQUEID(Table.USER, Row.UUID, String.class),
    /**
     * A field containing the user password
     */
    PASSWORD(Table.ACCOUNT, Row.PASSWORD, HashResult.class),
    /**
     * A field containing the user pin
     */
    PIN(Table.ACCOUNT, Row.PIN, HashResult.class),
    /**
     * A field containing the user 2fa token
     */
    TOKEN_2FA(Table.ACCOUNT, Row.TOKEN_2FA, String.class),
    /**
     * A field containing the user panic token
     */
    PANIC(Table.ACCOUNT, Row.PANIC, HashResult.class),
    /**
     * A field containing the user 2fa status
     */
    STATUS_2FA(Table.ACCOUNT, Row.STATUS_2FA, Boolean.class),
    /**
     * A field containing the user session persistence configuration
     */
    SESSION_PERSISTENCE(Table.SESSION, Row.PERSISTENT, Boolean.class);

    @Getter
    private final Table table;
    @Getter
    private final Row row;
    private final Class<?> type;

    /**
     * Get if the type matches
     *
     * @param type the type
     * @return if the type matches
     */
    public boolean isType(final Class<?> type) {
        return this.type.equals(type) || this.type.isAssignableFrom(type);
    }
}
