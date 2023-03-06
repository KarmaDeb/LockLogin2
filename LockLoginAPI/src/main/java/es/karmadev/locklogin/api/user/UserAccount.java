package es.karmadev.locklogin.api.user;

import ml.karmaconfigs.api.common.string.StringUtils;

import java.util.UUID;

/**
 * Client account
 */
public interface UserAccount {

    /**
     * Get the account id
     *
     * @return the account id
     */
    int id();

    /**
     * Get the account name
     *
     * @return the account name
     */
    String name();

    /**
     * Get the account unique ID
     *
     * @return the account unique ID
     */
    UUID uniqueId();

    /**
     * Get the account password
     *
     * @return the account password
     */
    String password();

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    String pin();

    /**
     * Get the account 2fa token
     *
     * @return the account 2fa token
     */
    String _2FA();

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    String panic();

    default boolean isRegistered() {
        String value = password();
        return !StringUtils.isNullOrEmpty(value);
    }

    /**
     * Get if the account has a pin
     *
     * @return if the account has a pin
     */
    default boolean hasPin() {
        String value = pin();
        return !StringUtils.isNullOrEmpty(value);
    }

    /**
     * Get if the account has 2fa enabled
     *
     * @return if the account has 2fa
     */
    boolean has2FA();

    /**
     * Get if the account is protected
     *
     * @return if the account is protected
     */
    default boolean isProtected() {
        String value = panic();
        return !StringUtils.isNullOrEmpty(value);
    }
}
