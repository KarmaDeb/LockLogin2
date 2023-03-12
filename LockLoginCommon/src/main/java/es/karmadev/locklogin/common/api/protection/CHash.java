package es.karmadev.locklogin.common.api.protection;

import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CHash implements HashResult {

    PluginHash hasher;
    VirtualizedInput product;
}
