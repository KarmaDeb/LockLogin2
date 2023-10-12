package es.karmadev.locklogin.common.api.protection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

            Gson gson = new GsonBuilder().create();

            JsonElement element = gson.fromJson(string, JsonElement.class);
            if (element != null && element.isJsonObject()) {
                JsonObject main = element.getAsJsonObject();
                JsonObject result = main.getAsJsonObject("product");
                JsonObject references = result.getAsJsonObject("references");

                String hashMethodName = main.get("method").getAsString();

                LockLogin plugin = CurrentPlugin.getPlugin();
                LockLoginHasher hasher = plugin.hasher();
                PluginHash hash = hasher.getMethod(hashMethodName);
                if (hash == null) {
                    plugin.err("Cannot fetch hash value of user with id {0} because he used a no-longer existing hash method", userId);
                    return null;
                }

                byte[] value = Base64.getDecoder().decode(result.get("value").getAsString());
                boolean valid = result.get("virtualized").getAsBoolean();
                int refSize = references.get("size").getAsInt();
                VirtualizedInput input = CVirtualInput.raw(value);
                if (refSize > 0) {
                    int[] refs = new int[refSize];
                    for (JsonElement child : references.getAsJsonArray("values")) {
                        if (!child.isJsonObject()) continue;
                        JsonObject childObject = child.getAsJsonObject();
                        int index = childObject.get("source").getAsInt();
                        int refValue = childObject.get("target").getAsInt();

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
