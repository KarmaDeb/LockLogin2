package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.PasswordConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CPasswordSection implements PasswordConfiguration {

    boolean printSuccess;
    boolean blockUnsafe;
    boolean warningUnsafe;
    boolean ignoreCommon;
    int minLength;
    int characters;
    int numbers;
    int upperLetters;
    int lowerLetters;
}
