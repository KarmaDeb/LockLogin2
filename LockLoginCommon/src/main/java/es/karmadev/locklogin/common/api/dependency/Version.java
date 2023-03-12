package es.karmadev.locklogin.common.api.dependency;

import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyVersion;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
class Version implements DependencyVersion {

    String project;
    String plugin;
}
