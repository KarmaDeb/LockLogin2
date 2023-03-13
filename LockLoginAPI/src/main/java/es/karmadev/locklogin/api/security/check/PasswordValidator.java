package es.karmadev.locklogin.api.security.check;

/**
 * LockLogin password validator
 */
public interface PasswordValidator {

    /**
     * Get if the password is safe
     *
     * @param input the password
     * @return the password check result
     */
    CheckResult isSafe(final String input);
}
