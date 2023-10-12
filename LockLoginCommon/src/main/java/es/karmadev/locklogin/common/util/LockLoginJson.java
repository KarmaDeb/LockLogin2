package es.karmadev.locklogin.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.communication.data.Channel;
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
    public static String versionId;
    @Getter
    public static BuildType channel;
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
                    Gson gson = new GsonBuilder().create();
                    JsonObject json = gson.fromJson(isr, JsonObject.class);

                    versionId = json.get("id").getAsString();
                    version = json.getAsJsonObject("version").get("name").getAsString();
                    channel = BuildType.valueOf(json.get("update").getAsString().toUpperCase()).map(versionId, version);

                    updateName = json.getAsJsonObject("version").get("type").getAsString();
                    marketVersion = json.getAsJsonObject("version").get("marketplace").getAsInt();

                    List<URI> uriList = new ArrayList<>();
                    for (JsonElement element : json.getAsJsonObject("version").getAsJsonArray("check")) {
                        String url = element.getAsString();
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
