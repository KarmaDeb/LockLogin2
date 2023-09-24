package es.karmadev.locklogin.common.api.dependency;

import es.karmadev.api.shaded.google.gson.JsonArray;
import es.karmadev.api.shaded.google.gson.JsonElement;
import es.karmadev.api.shaded.google.gson.JsonObject;
import es.karmadev.api.shaded.google.gson.JsonPrimitive;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyType;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyVersion;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import lombok.RequiredArgsConstructor;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class JsonDependency implements LockLoginDependency {

    private final JsonObject object;
    private final Checksum checksum = new Checksum(this);
    private final Checksum generated_checksum = new Checksum(this);

    private final static Set<String> ignored_hosts = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Get the dependency type
     *
     * @return the type
     */
    @Override
    public DependencyType type() {
        return DependencyType.valueOf(object.get("type").getAsString().toUpperCase());
    }

    /**
     * Get the dependency id
     *
     * @return the dependency id
     */
    @Override
    public String id() {
        return object.get("id").getAsString();
    }

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
     * Get the class to test with if
     * the dependency exists
     *
     * @return the dependency test class
     */
    @Override
    public String testClass() {
        return object.get("test").getAsString();
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
        if (!type().equals(DependencyType.SINGLE)) return null;
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
        if (type().equals(DependencyType.PLUGIN)) return null;

        JsonArray urls = object.get("download").getAsJsonArray();
        if (urls.isEmpty()) return null; //No need to iterate over nothing

        for (JsonElement element : urls) {
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isString()) {
                    String raw_url = primitive.getAsString() + object.get("id").getAsString() + ".jar";
                    try {
                        return new URL(raw_url);
                    } catch (MalformedURLException ignored) {}
                }
            }
        }

        return null;
    }

    /**
     * Get if the dependency is a plugin
     *
     * @return if the dependency is a plugin
     * @deprecated See {@link LockLoginDependency#type()}
     */
    @Override
    @Deprecated
    public boolean isPlugin() {
        return type().equals(DependencyType.PLUGIN);
    }
}
