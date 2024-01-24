package es.karmadev.locklogin.common.api.protection;

import es.karmadev.api.kson.JsonInstance;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.kson.io.JsonReader;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualInput;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CHash extends HashResult {

    PluginHash hasher;
    VirtualizedInput product;

    public static CHash fromString(final String data, final int userId) {
        try {
            byte[] rawJson = Base64.getDecoder().decode(data);
            String string = new String(rawJson, StandardCharsets.UTF_8);

            JsonInstance element = JsonReader.read(string);
            if (element.isObjectType()) {
                JsonObject main = element.asObject();
                JsonObject result = main.getChild("product").asObject();
                JsonObject references = result.getChild("references").asObject();

                String hashMethodName = main.getChild("method").asNative().getString();

                LockLogin plugin = CurrentPlugin.getPlugin();
                LockLoginHasher hasher = plugin.hasher();
                PluginHash hash = hasher.getMethod(hashMethodName);
                if (hash == null) {
                    plugin.err("Cannot fetch hash value of user with id {0} because he used a no-longer existing hash method", userId);
                    return null;
                }

                byte[] value = Base64.getDecoder().decode(result.getChild("value").asNative().getString());
                boolean valid = result.getChild("virtualized").asNative().getBoolean();
                int refSize = references.getChild("size").asNative().getInteger();
                VirtualizedInput input = CVirtualInput.raw(value);
                if (refSize > 0) {
                    int[] refs = new int[refSize];
                    for (JsonInstance child : references.getChild("values").asArray()) {
                        if (!child.isObjectType()) continue;
                        JsonObject childObject = child.asObject();
                        int index = childObject.getChild("source").asNative().getInteger();
                        int refValue = childObject.getChild("target").asNative().getInteger();

                        refs[index] = refValue;
                    }

                    input = CVirtualInput.of(refs, valid, value);
                }

                return CHash.of(hash, input);
            }
        } catch (IllegalArgumentException ignored) {}

        return null;
    }
}
