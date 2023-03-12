package es.karmadev.locklogin.api.security.check;

/**
 * Password check type
 */
public enum CheckType {
    /**
     * If password is unique
     */
    UNIQUE("Unique", "&e- &aYour password is mostly unique", "&e- &cYour password is similar to known ones"),
    /**
     * If password has the minimum length
     */
    LENGTH("Length", "&e- &aYour password has at least {min_length} characters", "&e- &cYour password has {length} of {min_length} characters"),
    /**
     * If password has the minimum special characters
     */
    SPECIAL("Special", "&e- &aYour password has at least {min_special} special characters", "&e- &cYour password has {special} of {min_special} characters"),
    /**
     * If password has the minimum amount of numbers
     */
    NUMBER("Numbers", "&e- &aYour password has at least {min_number} numbers", "&e- &cYour password has {number} of {min_number} numbers"),
    /**
     * If password has the minimum amount of lowercase letters
     */
    LOWER("Uppers", "&e- &aYour password has at least {min_lower} lowercase characters", "&e- &cYour password has {lower} of {min_lower} lowercase characters"),
    /**
     * If password has the minimum amount of uppercase letters
     */
    UPPER("Lowers", "&e- &aYour password has at least {min_upper} uppercase characters", "&e- &cYour password has {upper} of {min_upper} uppercase characters");

    public final String name;
    public final String successMessage;
    public final String failMessage;

    CheckType(final String n, final String sm, final String fm) {
        name = n;
        successMessage = sm;
        failMessage = fm;
    }
}
