package es.karmadev.locklogin.api.plugin.database.schema;

import java.util.Arrays;

/**
 * LockLogin valid tables
 */
public enum Table {
    /**
     * Accounts table
     */
    ACCOUNT("Accounts", Row.PASSWORD, Row.PIN, Row.PIN, Row.TOKEN_2FA, Row.PANIC, Row.STATUS_2FA, Row.CREATED_AT),
    /**
     * Sessions table
     */
    SESSION("Sessions", Row.LOGIN_CAPTCHA, Row.LOGIN_PASSWORD, Row.LOGIN_PIN, Row.LOGIN_2FA, Row.PERSISTENT, Row.CAPTCHA, Row.CREATED_AT),
    /**
     * Servers table
     */
    SERVER("Servers", Row.NAME, Row.ADDRESS, Row.PORT, Row.CREATED_AT),
    /**
     * Users table
     */
    USER("Users", Row.NAME, Row.EMAIL, Row.UUID, Row.PREMIUM_UUID, Row.ACCOUNT_ID, Row.SESSION_ID, Row.CONNECTION_TYPE, Row.LAST_SERVER, Row.PREV_SERVER, Row.STATUS, Row.CREATED_AT),
    /**
     * Brute force table
     */
    BRUTE_FORCE("BruteForce", Row.ADDRESS, Row.TRIES, Row.BLOCKED, Row.REMAINING, Row.CREATED_AT);

    /**
     * The table path
     */
    public final String name;
    private final Row[] rows;

    /**
     * Initialize the table
     *
     * @param name the table configuration name
     */
    Table(final String name, final Row... rows) {
        this.name = name;
        Row[] vRows = rows;
        if (!Arrays.asList(rows).contains(Row.ID)) {
            vRows = new Row[rows.length + 1];
            vRows[0] = Row.ID;
            System.arraycopy(rows, 0, vRows, 1, vRows.length - 1);
        }

        this.rows = vRows;
    }

    /**
     * Get if the table has the specified
     * row
     *
     * @param row the row
     * @return if the table has row
     */
    public boolean hasRow(final Row row) {
        if (row.equals(Row.ID)) return true; //All tables have the ID row
        return Arrays.asList(rows).contains(row);
    }

    /**
     * Get the table usable rows
     *
     * @return the table rows
     */
    public Row[] getUsableRows() {
        return rows.clone();
    }
}
