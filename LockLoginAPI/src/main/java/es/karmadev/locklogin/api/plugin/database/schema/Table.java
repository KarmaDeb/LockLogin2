package es.karmadev.locklogin.api.plugin.database.schema;

/**
 * LockLogin valid tables
 */
public enum Table {
    /**
     * Accounts table
     */
    ACCOUNT("Accounts"),
    /**
     * Sessions table
     */
    SESSION("Sessions"),
    /**
     * Servers table
     */
    SERVER("Servers"),
    /**
     * Users table
     */
    USER("Users"),
    /**
     * Brute force table
     */
    BRUTE_FORCE("BruteForce");

    /**
     * The table path
     */
    public final String name;

    /**
     * Initialize the table
     *
     * @param name the table configuration name
     */
    Table(final String name) {
        this.name = name;
    }
}
