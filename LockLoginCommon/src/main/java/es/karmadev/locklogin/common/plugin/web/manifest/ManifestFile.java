package es.karmadev.locklogin.common.plugin.web.manifest;

import lombok.Getter;
import lombok.Value;

import java.util.List;

@Value(staticConstructor = "of")
@Getter
public class ManifestFile {

    String file;
    String directoryName;
    List<String> localeNames;
}
