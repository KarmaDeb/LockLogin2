package es.karmadev.locklogin.common.api.plugin.service.password;

import es.karmadev.locklogin.api.plugin.file.section.PasswordConfiguration;
import es.karmadev.locklogin.api.security.check.CheckResult;
import es.karmadev.locklogin.api.security.check.CheckType;
import es.karmadev.locklogin.common.plugin.secure.KnownPasswords;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "from")
public class CPasswordResult implements CheckResult {

    PasswordConfiguration configuration;
    String rawText;

    int length;
    int specialCharacters;
    int numbers;
    int upperLetters;
    int lowerLetters;
    boolean valid;

    private PasswordConfiguration configuration() {
        return null;
    }

    private String rawText() {
        return null;
    }

    /**
     * Get the checktype status
     *
     * @param type the type
     * @return the status for the
     * specified check type
     */
    @Override
    public boolean getStatus(CheckType type) {
        switch (type) {
            case LOWER:
                return configuration.lowerLetters() > lowerLetters;
            case UPPER:
                return configuration.upperLetters() > upperLetters;
            case LENGTH:
                return configuration.minLength() > length;
            case NUMBER:
                return configuration.numbers() > numbers;
            case UNIQUE:
                return KnownPasswords.isKnown(rawText);
            case SPECIAL:
                return configuration.characters() > specialCharacters;
            default:
                return false;
        }
    }
}
