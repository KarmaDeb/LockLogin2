package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.MessageIntervalSection;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CMessageSection implements MessageIntervalSection {

    int registration;
    int logging;
}
