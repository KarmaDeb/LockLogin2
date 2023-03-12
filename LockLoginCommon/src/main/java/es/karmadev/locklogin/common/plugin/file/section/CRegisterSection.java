package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.RegisterConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CRegisterSection implements RegisterConfiguration {

    boolean bossBar;
    boolean blindEffect;
    int timeout;
    int maxAccounts;

}
