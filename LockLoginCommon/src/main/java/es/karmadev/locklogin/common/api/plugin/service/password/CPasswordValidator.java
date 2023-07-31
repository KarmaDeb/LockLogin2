package es.karmadev.locklogin.common.api.plugin.service.password;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.PasswordConfiguration;
import es.karmadev.locklogin.api.security.check.CheckResult;
import es.karmadev.locklogin.api.security.check.PasswordValidator;
import es.karmadev.locklogin.common.plugin.secure.KnownPasswords;

public class CPasswordValidator implements PasswordValidator {

    boolean grantedThroughServiceProvider = false;

    private final static LockLogin plugin = CurrentPlugin.getPlugin();
    private final String password;

    /**
     * Initialize the password validator
     *
     * @param password the password
     */
    CPasswordValidator(final String password) {
        this.password = password;
    }

    /**
     * Get if the password is safe
     *
     * @return the password check result
     */
    @Override
    public CheckResult validate() {
        if (!grantedThroughServiceProvider) return null;

        Configuration configuration = plugin.configuration();
        PasswordConfiguration passwordConfiguration = configuration.password();

        int length = password.length();
        int numbers = 0;
        int special = 0;
        int upper = 0;
        int lower = 0;
        for (int i = 0; i < length; i++) {
            char character = password.charAt(i);
            if (Character.isDigit(character)) {
                numbers++;
                continue;
            }
            if (Character.isUpperCase(character)) {
                upper++;
                continue;
            }
            if (Character.isLowerCase(lower)) {
                lower++;
                continue;
            }

            special++;
        }

        boolean valid = (length >= passwordConfiguration.minLength() && numbers >= passwordConfiguration.numbers()
                && special >= passwordConfiguration.characters() && upper >= passwordConfiguration.upperLetters()
                && lower >= passwordConfiguration.lowerLetters() && !KnownPasswords.isKnown(password));

        return CPasswordResult.from(
                passwordConfiguration,
                password,
                length,
                special,
                numbers,
                upper,
                lower,
                valid
        );
    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "Password Validator";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return true;
    }
}
