package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.PremiumConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CPremiumSection implements PremiumConfiguration {

    boolean enable;
    boolean forceOfflineId;
}
