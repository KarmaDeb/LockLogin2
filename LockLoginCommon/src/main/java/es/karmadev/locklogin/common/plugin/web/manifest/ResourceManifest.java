package es.karmadev.locklogin.common.plugin.web.manifest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.marketplace.Category;

import java.util.ArrayList;
import java.util.List;

public class ResourceManifest {

    private JsonObject object;

    public boolean read(final LockLogin plugin, final JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            plugin.err("Cannot load resource manifest.json because it's not a valid json");
            return false;
        }

        object = element.getAsJsonObject();
        return true;
    }

    public String getTitle() {
        return object.get("title").getAsString();
    }

    public Category getCategory() {
        return Category.byId(object.get("category").getAsInt());
    }

    public int getVersion() {
        if (!object.has("langVersion")) return -1;
        return object.get("langVersion").getAsInt();
    }

    public List<ManifestFile> getFiles() {
        List<ManifestFile> files = new ArrayList<>();

        JsonArray array = object.getAsJsonArray("files");
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) continue;
            JsonObject elementObject = element.getAsJsonObject();

            String name = elementObject.get("name").getAsString();
            String directory = elementObject.get("directory").getAsString();
            JsonArray locales = elementObject.getAsJsonArray("locales");

            List<String> rawLocales = new ArrayList<>();
            for (JsonElement localeElement : locales) {
                if (localeElement == null || !localeElement.isJsonPrimitive() || !localeElement.getAsJsonPrimitive().isString()) continue;
                rawLocales.add(localeElement.getAsString());
            }

            files.add(ManifestFile.of(name, directory, rawLocales));
        }

        return files;
    }
}
