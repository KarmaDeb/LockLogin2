package es.karmadev.locklogin.api.user.session;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

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
     * Get if the session is captcha logged
     *
     * @return if the session is captcha logged
     */
    boolean isCaptchaLogged();

    /**
     * Perform captcha login for this session
     */
    void captchaLogin();

    /**
     * Get if the session is logged
     *
     * @return if the session is logged
     */
    boolean isLogged();

    /**
     * Perform login for this session
     */
    void login();

    /**
     * Get if the session is pin logged
     *
     * @return if the session is pin logged
     */
    boolean isPinLogged();

    /**
     * Perform pin login for this session
     */
    void pinLogin();

    /**
     * Get if the session is 2fa logged
     *
     * @return if the session is 2fa login
     */
    boolean is2FALogged();

    /**
     * Perform 2fa login for this session
     */
    void _2faLogin();

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
}
