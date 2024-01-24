package es.karmadev.locklogin.common.plugin.web.manifest;

import es.karmadev.api.kson.JsonArray;
import es.karmadev.api.kson.JsonInstance;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.marketplace.Category;

import java.util.ArrayList;
import java.util.List;

public class ResourceManifest {

    private JsonObject object;

    public boolean read(final LockLogin plugin, final JsonInstance element) {
        if (element == null || !element.isObjectType()) {
            plugin.err("Cannot load resource manifest.json because it's not a valid json");
            return false;
        }

        object = element.asObject();
        return true;
    }

    public String getTitle() {
        return object.getChild("title").asNative().getString();
    }

    public Category getCategory() {
        return Category.byId(object.getChild("category").asNative().getInteger());
    }

    public int getVersion() {
        if (!object.hasChild("langVersion")) return -1;
        return object.getChild("langVersion").asNative().getInteger();
    }

    public List<ManifestFile> getFiles() {
        List<ManifestFile> files = new ArrayList<>();

        JsonArray array = object.getChild("files").asArray();
        for (JsonInstance element : array) {
            if (element == null || !element.isObjectType()) continue;
            JsonObject elementObject = element.asObject();

            String name = elementObject.getChild("name").asNative().getString();
            String directory = elementObject.getChild("directory").asNative().getString();
            JsonArray locales = elementObject.getChild("locales").asArray();

            List<String> rawLocales = new ArrayList<>();
            for (JsonInstance localeElement : locales) {
                if (localeElement == null || !localeElement.isNativeType() || !localeElement.asNative().isString()) continue;
                rawLocales.add(localeElement.asNative().getString());
            }

            files.add(ManifestFile.of(name, directory, rawLocales));
        }

        return files;
    }
}
