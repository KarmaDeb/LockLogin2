package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * Password configuration section
 */
public interface PasswordConfiguration extends Serializable {

    /**
     * Get if the plugin prints success
     * password checks when a password is not
     * safe
     *
     * @return if the plugin shows passed checks
     */
    boolean printSuccess();

    /**
     * Get if the plugin blocks automatically
     * the use of unsafe passwords
     *
     * @return if the plugin blocks unsafe
     * passwords
     */
    boolean blockUnsafe();

    /**
     * Get if the plugin should tell about
     * staff members when a client is using
     * an unsafe password
     *
     * @return if staff members should have
     * knowledge of unsafe passwords usage
     */
    boolean warningUnsafe();

    /**
     * Get if the plugin ignores common
     * passwords on the password security
     * check
     *
     * @return if the plugin ignores most
     * known passwords
     */
    boolean ignoreCommon();

    /**
     * Get the minimum length of a password
     * for it to be safe
     *
     * @return the minimum password length
     */
    int minLength();

    /**
     * Get the minimum amount of special
     * characters of a password for it
     * to be safe
     *
     * @return the minimum password special characters
     */
    int characters();

    /**
     * Get the minimum amount of numbers
     * of a password for it to be safe
     *
     * @return the minimum password numbers
     */
    int numbers();

    /**
     * Get the minimum amount of upper letters
     * of a password for it to be safe
     *
     * @return the minimum password upper letters
     */
    int upperLetters();

    /**
     * Get the minimum amount of lower letters
     * of a password for it to be safe
     *
     * @return the minimum password lower letters
     */
    int lowerLetters();
}
