package es.karmadev.locklogin.common.api.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.AuthenticationConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Value(staticConstructor = "of")
@Accessors(fluent = true)
public class CAuthSection implements AuthenticationConfiguration {

    boolean register;
    boolean login;
    boolean pin;
    boolean totp;
}
