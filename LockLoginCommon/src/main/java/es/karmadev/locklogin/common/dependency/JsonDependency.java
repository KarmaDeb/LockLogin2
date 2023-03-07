package es.karmadev.locklogin.common.dependency;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyVersion;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import lombok.AllArgsConstructor;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.url.URLUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class JsonDependency implements LockLoginDependency {

    private final JsonObject object;
    private final Checksum checksum = new Checksum();
    private final Checksum generated_checksum = new Checksum();

    private final static Set<String> ignored_hosts = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Get the dependency name
     *
     * @return the dependency name
     */
    @Override
    public String name() {
        return object.get("name").getAsString();
    }

    /**
     * Get the dependency version
     *
     * @return the dependency version
     */
    @Override
    public DependencyVersion version() {
        String project = object.get("version").getAsJsonObject().get("project").getAsString();
        String required = object.get("version").getAsJsonObject().get("required").getAsString();

        return Version.of(project, required);
    }

    /**
     * Get the dependency file
     *
     * @return the dependency file
     */
    @Override
    public Path file() {
        boolean plugin = object.get("plugin").getAsBoolean();
        if (plugin) return null; //A plugin file name is not always the same

        return CurrentPlugin.getPlugin().workingDirectory().resolve("dependencies").resolve(object.get("file").getAsString());
    }

    /**
     * Get the dependency checksum
     *
     * @return the dependency checksum
     */
    @Override
    public Checksum checksum() {
        return checksum;
    }

    /**
     * Generate a checksum for the current dependency file
     *
     * @return the dependency checksum
     */
    @Override
    public Checksum generateChecksum() {
        return generated_checksum;
    }

    /**
     * Get the dependency download URL
     *
     * @return the dependency download URL
     */
    @Override
    public URL downloadURL() {
        boolean plugin = object.get("plugin").getAsBoolean();
        if (plugin) return null; //A plugin cannot be downloaded and injected, it must be loaded by the server itself

        JsonArray urls = object.get("download").getAsJsonArray();
        if (urls.isEmpty()) return null; //No need to iterate over nothing

        for (JsonElement element : urls) {
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isString()) {
                    String raw_url = primitive.getAsString();
                    if (!ignored_hosts.contains(raw_url)) {
                        String domain = URLUtils.getDomainName(raw_url);

                        if (domain != null && URLUtils.exists(domain)) {
                            try {
                                return new URL(raw_url + object.get("file").getAsString());
                            } catch (MalformedURLException ignored) {}
                        } else {
                            CurrentPlugin.getPlugin().info("Ignoring dependency host {0} because checks failed", Level.INFO, (domain != null ? domain : raw_url));
                            ignored_hosts.add(raw_url);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get if the dependency is a plugin
     *
     * @return if the dependenc is a plugin
     */
    @Override
    public boolean isPlugin() {
        return object.get("plugin").getAsBoolean();
    }
}
