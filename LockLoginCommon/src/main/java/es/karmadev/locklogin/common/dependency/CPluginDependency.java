package es.karmadev.locklogin.common.dependency;

import com.google.gson.*;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CPluginDependency {

    private final static Map<String, LockLoginDependency> dependencies = new LinkedHashMap<>();

    /**
     * Load all the dependencies
     */
    public static void load() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        try (InputStream locklogin = plugin.load("internal/locklogin.json")) {
            if (locklogin != null) {
                try (InputStreamReader isr = new InputStreamReader(locklogin)) {
                    Gson gson = new GsonBuilder().create();
                    JsonObject json = gson.fromJson(isr, JsonObject.class);

                    JsonArray dependencies = json.getAsJsonArray("dependencies");
                    for (JsonElement element : dependencies) {
                        JsonObject dependencyJson = element.getAsJsonObject();
                        JsonDependency dependency = new JsonDependency(dependencyJson);

                        String id = dependencyJson.get("id").getAsString();

                        String testClass = dependency.testClass();
                        if (StringUtils.isNullOrEmpty(testClass)) continue;
                        try {
                            Class.forName(dependency.testClass());
                            plugin.info("Ignoring dependency {0} because it seems to be already injected", dependency.name());
                            dependency.assertInstalled();
                        } catch (Throwable notFound) {}

                        CPluginDependency.dependencies.put(id, dependency);
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get all the dependencies
     *
     * @return the dependencies
     */
    public static Set<LockLoginDependency> getAll() {
        return new LinkedHashSet<>(dependencies.values());
    }

    /**
     * Get a dependency
     *
     * @param id the dependency id
     * @return the dependency
     */
    public static LockLoginDependency get(final String id) {
        return dependencies.getOrDefault(id, null);
    }
}
