package es.karmadev.locklogin.api.security.check;

/**
 * Password check result
 */
public interface CheckResult {

    /**
     * Get the password length
     *
     * @return the password length
     */
    int length();

    /**
     * Get the password special characters
     *
     * @return the password special
     * characters
     */
    int specialCharacters();

    /**
     * Get the password numbers
     *
     * @return the passowrd numbers
     */
    int numbers();

    /**
     * Get the password upper case letters
     *
     * @return the password upper case
     */
    int upperLetters();

    /**
     * Get the password lower case letters
     *
     * @return the password lower case
     */
    int lowerLetters();

    /**
     * Get the checktype status
     *
     * @param type the type
     * @return the status for the
     * specified check type
     */
    boolean getStatus(final CheckType type);

    /**
     * Get if the password is valid
     *
     * @return if the password is valid
     */
    boolean valid();
}
