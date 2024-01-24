package es.karmadev.locklogin.common.api.dependency;

import es.karmadev.api.kson.JsonArray;
import es.karmadev.api.kson.JsonInstance;
import es.karmadev.api.kson.JsonNative;
import es.karmadev.api.kson.JsonObject;
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
        return DependencyType.valueOf(object.getChild("type").asNative().getString().toUpperCase());
    }

    /**
     * Get the dependency id
     *
     * @return the dependency id
     */
    @Override
    public String id() {
        return object.getChild("id").asNative().getString();
    }

    /**
     * Get the dependency name
     *
     * @return the dependency name
     */
    @Override
    public String name() {
        return object.getChild("name").asNative().getString();
    }

    /**
     * Get the class to test with if
     * the dependency exists
     *
     * @return the dependency test class
     */
    @Override
    public String testClass() {
        String test = object.getChild("test").asNative().getString();
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
        String project = object.getChild("version")
                .asObject().getChild("project").asNative().getString();
        String required = object.getChild("version")
                .asObject().getChild("required").asNative().getString();

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
        return CurrentPlugin.getPlugin().workingDirectory().resolve("dependencies")
                .resolve(object.getChild("file").asNative().getString());
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
                !object.hasChild("relocations") ||
                !object.getChild("relocations").isArrayType()) return CRelocationSet.empty();

        JsonArray relocations = object.getChild("relocations").asArray();
        if (relocations.isEmpty()) return CRelocationSet.empty();

        List<CRelocation> relocationList = new ArrayList<>();
        for (JsonInstance element : relocations) {
            if (element.isObjectType()) {
                JsonObject sub = element.asObject();
                if (sub.hasChild("from") && sub.hasChild("to")) {
                    String from = sub.getChild("from").asNative().getString();
                    String to = sub.getChild("to").asNative().getString();

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
        if (object.hasChild("depends") && object.getChild("depends").isArrayType()) {
            JsonArray array = object.getChild("depends").asArray();
            for (JsonInstance element : array) {
                dependencies.add(element.asNative().getString());
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

        JsonArray urls = object.getChild("download").asArray();
        if (urls.isEmpty()) return null; //No need to iterate over nothing

        for (JsonInstance element : urls) {
            if (element.isNativeType()) {
                JsonNative primitive = element.asNative();
                if (primitive.isString()) {
                    String raw_url = primitive.getAsString() + object.getChild("id")
                            .asNative().getString() + ".jar";
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
