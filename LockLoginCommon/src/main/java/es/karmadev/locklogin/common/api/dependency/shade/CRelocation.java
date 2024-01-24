package es.karmadev.locklogin.common.api.dependency.shade;

import es.karmadev.locklogin.api.plugin.runtime.dependency.shade.Relocation;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;

@Value(staticConstructor = "of")
@Getter
@Accessors(fluent = true)
public class CRelocation implements Relocation {

    String from;
    String to;
}
