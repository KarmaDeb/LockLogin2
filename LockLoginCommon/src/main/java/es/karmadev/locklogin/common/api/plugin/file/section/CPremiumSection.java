package es.karmadev.locklogin.common.api.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.PremiumConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CPremiumSection implements PremiumConfiguration {

    boolean auto;
    boolean enable;
    boolean forceOfflineId;
}
