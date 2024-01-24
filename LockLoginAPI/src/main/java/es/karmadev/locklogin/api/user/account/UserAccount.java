package es.karmadev.locklogin.api.user.account;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.network.Cached;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.security.hash.HashResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Client account
 */
@SuppressWarnings("unused")
public interface UserAccount extends Cached {

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
     * Get the account email address
     *
     * @return the account email address
     */
    String email();

    /**
     * Update the account email address
     *
     * @param email the account email address
     */
    void updateEmail(final String email);

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
     * Get the account totp token
     *
     * @return the account totp token
     */
    String totp();

    /**
     * Set the client totp token
     *
     * @param token the token
     */
    void setTotp(final String token);

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
     * Set the client totp status
     *
     * @param status the client totp status
     */
    void setTotp(final boolean status);

    /**
     * Get if the client is registered
     *
     * @return if the client is registered
     */
    default boolean isRegistered() {
        HashResult value = password();
        return !ObjectUtils.isNullOrEmpty(value);
    }

    /**
     * Get if the account has a pin
     *
     * @return if the account has a pin
     */
    default boolean hasPin() {
        HashResult value = pin();
        return !ObjectUtils.isNullOrEmpty(value);
    }

    /**
     * Get if the account has totp enabled
     *
     * @return if the account has totp
     */
    boolean hasTotp();

    /**
     * Get if the account has a valid totp token
     * set
     *
     * @return if the account has a valid totp token set
     */
    default boolean totpSet() {
        String token = totp();
        return !ObjectUtils.isNullOrEmpty(token);
    }

    /**
     * Get if the account is protected
     *
     * @return if the account is protected
     */
    default boolean isProtected() {
        HashResult value = panic();
        return !ObjectUtils.isNullOrEmpty(value);
    }

    /**
     * Get when the account was created
     *
     * @return the account creation date
     */
    Instant creation();
}
