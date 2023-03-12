package es.karmadev.locklogin.common.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.EncryptionConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CEncryptionSection implements EncryptionConfiguration {

    String algorithm;
    boolean applyBase64;
    boolean virtualID;
    int memory;
    int parallelism;
    int iterations;
}
