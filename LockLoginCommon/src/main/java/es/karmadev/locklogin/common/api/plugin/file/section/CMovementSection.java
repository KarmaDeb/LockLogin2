package es.karmadev.locklogin.common.api.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.MovementConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CMovementSection implements MovementConfiguration {

    boolean allow;
    MovementMethod method;
    int distance;

}
