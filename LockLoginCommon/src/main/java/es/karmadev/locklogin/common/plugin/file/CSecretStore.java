package es.karmadev.locklogin.common.plugin.file;

import es.karmadev.locklogin.api.plugin.file.Configuration;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Secret store
 */
@Accessors(fluent = true)
@Value(staticConstructor = "of")
public final class CSecretStore implements Configuration.SecretStore {

    byte[] token;
    byte[] iv;
}
