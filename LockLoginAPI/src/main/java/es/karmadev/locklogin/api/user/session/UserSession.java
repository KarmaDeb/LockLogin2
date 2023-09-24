package es.karmadev.locklogin.api.user.session;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

import java.time.Instant;

/**
 * User session
 */
public interface UserSession {

    /**
     * Get the session id
     *
     * @return the session id
     */
    int id();

    /**
     * Get the client this session
     * pertains to
     *
     * @return the session owner
     */
    LocalNetworkClient client();

    /**
     * Validate this session
     */
    void validate();

    /**
     * Invalidate this session
     */
    void invalidate();

    /**
     * Get if the session is valid
     *
     * @return if the session is valid
     */
    boolean isValid();

    /**
     * Get if the session is captcha logged
     *
     * @return if the session is captcha logged
     */
    boolean isCaptchaLogged();

    /**
     * Perform captcha login for this session
     *
     * @param status the login status
     */
    void captchaLogin(final boolean status);

    /**
     * Get if the session is logged
     *
     * @return if the session is logged
     */
    boolean isLogged();

    /**
     * Perform login for this session
     *
     * @param status the login status
     */
    void login(final boolean status);

    /**
     * Get if the session is pin logged
     *
     * @return if the session is pin logged
     */
    boolean isPinLogged();

    /**
     * Perform pin login for this session
     *
     * @param status the login status
     */
    void pinLogin(final boolean status);

    /**
     * Get if the session is 2fa logged
     *
     * @return if the session is 2fa login
     */
    boolean is2FALogged();

    /**
     * Perform 2fa login for this session
     *
     * @param status the login status
     */
    void _2faLogin(final boolean status);

    /**
     * Set the captcha code
     *
     * @param captcha the captcha code
     */
    void setCaptcha(final String captcha);

    /**
     * Get the session captcha
     *
     * @return the session captcha
     */
    String captcha();

    /**
     * Get if the session is persistent
     *
     * @return if the session is persistent
     */
    boolean isPersistent();

    /**
     * Set the session persistence
     *
     * @param status persistence status
     */
    void persistent(final boolean status);

    /**
     * Append a field to this session
     *
     * @param value the session field to append
     */
    void append(final SessionField<?> value);

    /**
     * Get a field from the session
     *
     * @param key the field key
     * @return the field or null if none
     * @param <T> the field type
     */
    <T> SessionField<T> fetch(final String key);

    /**
     * Get a field from the session
     *
     * @param key the field key
     * @param defaultValue the default value if null/none
     * @return the field
     * @param <T> the filed type
     */
    <T> T fetch(final String key, final T defaultValue);

    /**
     * Get when the session was created
     *
     * @return the session creation date
     */
    Instant creation();
}
