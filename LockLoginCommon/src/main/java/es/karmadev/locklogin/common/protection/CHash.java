package es.karmadev.locklogin.common.protection;

import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import lombok.Value;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CHash implements HashResult {

    PluginHash hasher;
    VirtualizedInput product;
}
