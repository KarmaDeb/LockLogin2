package es.karmadev.locklogin.api.user.account;

import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.security.hash.HashResult;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.time.Instant;
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
     * Get the account owner id
     *
     * @return the account owner id
     */
    int ownerId();

    /**
     * Removes an account
     *
     * @param issuer the issuer of the account removal
     * @return if the account was able to be removed
     * @throws SecurityException if the caller of this method
     * is not a module or plugin
     */
    boolean destroy(final NetworkEntity issuer) throws SecurityException;

    /**
     * Get the account name
     *
     * @return the account name
     */
    String name();

    /**
     * Update the account name
     *
     * @param name the account name
     */
    void updateName(final String name);

    /**
     * Get the account unique ID
     *
     * @return the account unique ID
     */
    UUID uniqueId();

    /**
     * Update the account unique id
     *
     * @param uuid the account unique id
     */
    void updateUniqueId(final UUID uuid);

    /**
     * Get the account password
     *
     * @return the account password
     */
    HashResult password();

    /**
     * Set the client password
     *
     * @param password the password
     */
    void setPassword(final String password);

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    HashResult pin();

    /**
     * Set the client pin
     *
     * @param pin the pin
     */
    void setPin(final String pin);

    /**
     * Get the account 2fa token
     *
     * @return the account 2fa token
     */
    String _2FA();

    /**
     * Set the client 2fa token
     *
     * @param token the token
     */
    void set2FA(final String token);

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    HashResult panic();

    /**
     * Set the client panic token
     *
     * @param token the token
     */
    void setPanic(final String token);

    /**
     * Set the client 2fa status
     *
     * @param status the client 2fa status
     */
    void set2FA(final boolean status);

    /**
     * Get if the client is registered
     *
     * @return if the client is registered
     */
    default boolean isRegistered() {
        HashResult value = password();
        return !StringUtils.isNullOrEmpty(value);
    }

    /**
     * Get if the account has a pin
     *
     * @return if the account has a pin
     */
    default boolean hasPin() {
        HashResult value = pin();
        return !StringUtils.isNullOrEmpty(value);
    }

    /**
     * Get if the account has 2fa enabled
     *
     * @return if the account has 2fa
     */
    boolean has2FA();

    /**
     * Get if the account has a valid 2fa token
     * set
     *
     * @return if the account has a valid 2fa token set
     */
    default boolean _2faSet() {
        String token = _2FA();
        return !StringUtils.isNullOrEmpty(token);
    }

    /**
     * Get if the account is protected
     *
     * @return if the account is protected
     */
    default boolean isProtected() {
        HashResult value = panic();
        return !StringUtils.isNullOrEmpty(value);
    }

    /**
     * Get when the account was created
     *
     * @return the account creation date
     */
    Instant creation();
}
