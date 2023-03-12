package es.karmadev.locklogin.api.user.account.migration;

import es.karmadev.locklogin.api.security.hash.HashResult;

import java.io.Serializable;
import java.util.UUID;

/**
 * LockLogin transictionable account
 */
public interface Transictionable extends Serializable {

    /**
     * Get the transictionable account player
     *
     * @return the transictionable
     * player
     */
    String player();

    /**
     * Get teh transictionable account
     * unique id
     *
     * @return the transictionable uuid
     */
    UUID uniqueId();

    /**
     * Get the transictionable account
     * password
     *
     * @return the account password
     */
    HashResult password();

    /**
     * Get the transictionable account
     * pin
     *
     * @return the account pin
     */
    HashResult pin();

    /**
     * Get the transictionable account
     * 2fa token
     *
     * @return the account 2fa token
     */
    String _2fa();

    /**
     * Get the transictionable account
     * panic token
     *
     * @return the account panic token
     */
    HashResult panic();

    /**
     * Get if the transictionable account
     * has a password
     *
     * @return if the account has password
     */
    boolean hasPassword();

    /**
     * Get if the transictionable account
     * has a pin
     *
     * @return if the account has passwpinord
     */
    boolean hasPin();

    /**
     * Get if the transictionable account
     * has a 2fa
     *
     * @return if the account has 2fa enabled
     */
    boolean has2fa();

    /**
     * Get if the transictionable account
     * has a 2fa token
     *
     * @return if the account has 2fa token
     */
    boolean isTokenSet();

    /**
     * Get if the transictionable account
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
