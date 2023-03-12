package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.LoginConfiguration;
import es.karmadev.locklogin.api.plugin.file.section.RegisterConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CLoginSection implements LoginConfiguration {

    boolean bossBar;
    boolean blindEffect;
    int timeout;

}
