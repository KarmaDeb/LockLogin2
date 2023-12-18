package es.karmadev.locklogin.common.util;

import es.karmadev.api.kson.JsonInstance;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.kson.io.JsonReader;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.CacheAble;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * LockLogin json file
 */
@CacheAble(name = "locklogin.json")
public class LockLoginJson {

    @Getter
    private static String versionId;
    @Getter
    private static BuildType channel;
    @Getter
    private static int langVersion;
    @Getter
    private static String version;
    @Getter
    private static String updateName;
    @Getter
    private static int marketVersion;
    @Getter
    private static URI[] updateURIs;

    public static void preCache() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot precache locklogin.json because the plugin is not valid");

        try (InputStream locklogin = plugin.load("internal/locklogin.json")) {
            if (locklogin != null) {
                try (InputStreamReader isr = new InputStreamReader(locklogin)) {
                    JsonObject json = JsonReader.read(isr).asObject();

                    versionId = json.getChild("id").asNative().getString();
                    version = json.getChild("version.name").asNative().getString();
                    channel = BuildType.valueOf(json.getChild("update")
                            .asNative().getAsString().toUpperCase()).map(versionId, version);
                    langVersion = json.getChild("lang").asNative().getInteger();

                    updateName = json.getChild("version.type").asNative().getString();
                    marketVersion = json.getChild("version.marketplace").asNative().getInteger();

                    List<URI> uriList = new ArrayList<>();
                    for (JsonInstance element : json.getChild("version.check").asArray()) {
                        String url = element.asNative().getString();
                        URL netURL = URLUtilities.fromString(url);

                        if (netURL == null) continue;
                        try {
                            uriList.add(netURL.toURI());
                        } catch (URISyntaxException ignored) {}
                    }

                    updateURIs = uriList.toArray(new URI[0]).clone();
                }
            }
        } catch (IOException ex) {
            plugin.log(ex, "Failed to precache locklogin.json");
        }
    }
}
