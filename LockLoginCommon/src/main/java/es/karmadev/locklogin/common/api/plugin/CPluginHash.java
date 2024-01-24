package es.karmadev.locklogin.common.api.plugin;

import es.karmadev.locklogin.api.plugin.ServerHash;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CPluginHash implements ServerHash {

    String value;
    long creation;

    /**
     * Get the hash creation time
     *
     * @return the hash time
     */
    @Override
    public Instant creation() {
        return Instant.ofEpochMilli(creation);
    }
}
