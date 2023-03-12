package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.plugin.file.section.UpdaterSection;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CUpdaterSection implements UpdaterSection {

    BuildType type;
    boolean check;
    int interval;
}
