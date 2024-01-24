package es.karmadev.locklogin.api.user.auth.process;

/**
 * Auth process type
 */
public enum AuthType {
    /**
     * For non registered users
     */
    REGISTER,
    /**
     * For registered users
     */
    LOGIN,
    /**
     * Any kind of user
     */
    ANY
}
