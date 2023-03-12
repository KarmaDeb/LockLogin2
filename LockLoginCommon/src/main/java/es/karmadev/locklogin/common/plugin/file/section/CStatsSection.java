package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.StatisticsConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CStatsSection implements StatisticsConfiguration {

    boolean shareBStats;
    boolean publicLockLogin;
}
