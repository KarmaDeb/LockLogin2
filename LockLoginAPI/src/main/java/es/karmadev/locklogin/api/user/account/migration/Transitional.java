package es.karmadev.locklogin.api.user.account.migration;

import es.karmadev.locklogin.api.security.hash.HashResult;

import java.io.Serializable;
import java.util.UUID;

/**
 * LockLogin transitional account
 */
public interface Transitional extends Serializable {

    /**
     * Get the transitional account player
     *
     * @return the transitional
     * player
     */
    String player();

    /**
     * Get teh transitional account
     * unique id
     *
     * @return the transitional uuid
     */
    UUID uniqueId();

    /**
     * Get the transitional account
     * password
     *
     * @return the account password
     */
    HashResult password();

    /**
     * Get the transitional account
     * pin
     *
     * @return the account pin
     */
    HashResult pin();

    /**
     * Get the transitional account
     * totp token
     *
     * @return the account totp token
     */
    String totp();

    /**
     * Get the transitional account
     * panic token
     *
     * @return the account panic token
     */
    HashResult panic();

    /**
     * Get if the transitional account
     * has a password
     *
     * @return if the account has password
     */
    boolean hasPassword();

    /**
     * Get if the transitional account
     * has a pin
     *
     * @return if the account has pin
     */
    boolean hasPin();

    /**
     * Get if the transitional account
     * has a totp
     *
     * @return if the account has totp enabled
     */
    boolean hasTotp();

    /**
     * Get if the transitional account
     * has a totp token
     *
     * @return if the account has totp token
     */
    boolean isTotpSet();

    /**
     * Get if the transitional account
     * has a panic token
     *
     * @return if the account has panic token
     */
    boolean hasPanic();

    /**
     * Get if the user had persistent
     * sessions
     *
     * @return if the user had persistent
     * sessions
     */
    boolean sessionPersistent();
}
