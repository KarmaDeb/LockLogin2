package es.karmadev.locklogin.common.api.dependency;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyType;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyVersion;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.plugin.runtime.dependency.shade.Relocation;
import es.karmadev.locklogin.api.plugin.runtime.dependency.shade.RelocationSet;
import es.karmadev.locklogin.common.api.dependency.shade.CRelocation;
import es.karmadev.locklogin.common.api.dependency.shade.CRelocationSet;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class JsonDependency implements LockLoginDependency {

    private final JsonObject object;
    private final Checksum checksum = new Checksum(this);
    private final Checksum generated_checksum = new Checksum(this);

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
        String test = object.get("test").getAsString();
        RelocationSet set = getRelocations();
        if (set.hasRelocation()) {
            Relocation relocation;
            while ((relocation = set.next()) != null) {
                if (relocation.from().startsWith(test)) {
                    test = test.replace(relocation.from(), relocation.to());
                    break;
                }
            }
        }

        return test;
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
    public DependencyChecksum checksum() {
        return checksum;
    }

    /**
     * Generate a checksum for the current dependency file
     *
     * @return the dependency checksum
     */
    @Override
    public DependencyChecksum generateChecksum() {
        return generated_checksum;
    }

    /**
     * Get the dependency relocations
     *
     * @return the dependency relocations
     */
    @Override
    public @NotNull RelocationSet getRelocations() {
        if (type().equals(DependencyType.PLUGIN) ||
                type().equals(DependencyType.PACKAGE) ||
                !object.has("relocations") ||
                !object.get("relocations").isJsonArray()) return CRelocationSet.empty();

        JsonArray relocations = object.get("relocations").getAsJsonArray();
        if (relocations.isEmpty()) return CRelocationSet.empty();

        List<CRelocation> relocationList = new ArrayList<>();
        for (JsonElement element : relocations) {
            if (element.isJsonObject()) {
                JsonObject sub = element.getAsJsonObject();
                if (sub.has("from") && sub.has("to")) {
                    String from = sub.get("from").getAsString();
                    String to = sub.get("to").getAsString();

                    CRelocation relocation = CRelocation.of(from, to);
                    relocationList.add(relocation);
                }
            }
        }

        return new CRelocationSet(Collections.unmodifiableList(relocationList));
    }

    /**
     * Get the dependency dependencies
     *
     * @return the dependencies
     */
    @Override
    public @NotNull List<String> getDependencies() {
        List<String> dependencies = new ArrayList<>();
        if (object.has("depends") && object.get("depends").isJsonArray()) {
            JsonArray array = object.getAsJsonArray("depends");
            for (JsonElement element : array) {
                dependencies.add(element.getAsString());
            }
        }

        return Collections.unmodifiableList(dependencies);
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
