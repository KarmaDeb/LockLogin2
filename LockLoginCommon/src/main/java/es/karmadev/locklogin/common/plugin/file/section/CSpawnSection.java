package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.SpawnSection;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CSpawnSection implements SpawnSection {

    boolean enable;
    boolean takeBack;
    int minDistance;
}
