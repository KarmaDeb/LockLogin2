package es.karmadev.locklogin.common.dependency;

import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyVersion;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
class Version implements DependencyVersion {

    String project;
    String plugin;
}
