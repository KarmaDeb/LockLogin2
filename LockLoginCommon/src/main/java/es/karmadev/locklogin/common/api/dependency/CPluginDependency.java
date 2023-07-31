package es.karmadev.locklogin.common.api.dependency;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.shaded.google.gson.*;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

public class CPluginDependency {

    private final static Map<String, LockLoginDependency> dependencies = new LinkedHashMap<>();

    /**
     * Load all the dependencies
     */
    public static void load() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.info("Loading dependencies, please wait");

        try (InputStream locklogin = plugin.load("internal/locklogin.json")) {
            if (locklogin != null) {
                Path dataFile = plugin.workingDirectory().resolve("cache").resolve("tables.json");
                JsonObject object = null;

                try (InputStreamReader isr = new InputStreamReader(locklogin)) {
                    Gson gson = new GsonBuilder().create();
                    JsonObject json = gson.fromJson(isr, JsonObject.class);

                    JsonArray checksum_urls = json.getAsJsonArray("checksum");
                    JsonArray dependencies = json.getAsJsonArray("dependencies");

                    String[] urls = new String[checksum_urls.size()];
                    for (int i = 0; i < checksum_urls.size(); i++) {
                        urls[i] = checksum_urls.get(i).getAsString();
                    }

                    URL checksum_url = URLUtilities.getOptional(urls).orElse(null);
                    if (checksum_url != null) {
                        HttpURLConnection connection = (HttpURLConnection) checksum_url.openConnection();
                        connection.setRequestMethod("GET");

                        int response = connection.getResponseCode();
                        if (response == HttpURLConnection.HTTP_OK) {
                            plugin.info("&aFetching checksum results to keep dependencies safe ( {0} )", checksum_url);

                            InputStream inputStream = connection.getInputStream();
                            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                            BufferedReader bf = new BufferedReader(reader);

                            PathUtilities.createPath(dataFile);

                            BufferedWriter writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8);
                            String line;
                            while ((line = bf.readLine()) != null) {
                                writer.write(line + "\n");
                            }

                            writer.flush();
                            writer.close();

                            bf.close();
                            reader.close();
                            bf.close();

                            object = gson.fromJson(PathUtilities.read(dataFile), JsonObject.class);
                        }
                    }

                    List<JsonDependency> ignore = new ArrayList<>();
                    List<JsonDependency> install = new ArrayList<>();
                    for (JsonElement element : dependencies) {
                        JsonObject dependencyJson = element.getAsJsonObject();
                        JsonDependency dependency = new JsonDependency(dependencyJson);

                        if (dependency.isPlugin()) continue; //Ignore plugin dependencies

                        String id = dependencyJson.get("id").getAsString();
                        long adler = 0;
                        long crc = 0;
                        String hash = "";
                        if (object != null) {
                            JsonObject dependencyTable = object.get("dependency").getAsJsonObject();
                            if (!dependencyTable.has(dependency.id())) continue; //Ignore unknown dependencies

                            JsonObject dependencyData = dependencyTable.get(dependency.id()).getAsJsonObject();

                            adler = dependencyData.get("adler").getAsLong();
                            crc = dependencyData.get("crc").getAsLong();
                            hash = dependencyData.get("hash").getAsString();
                        }

                        String testClass = dependency.testClass();
                        if (ObjectUtils.isNullOrEmpty(testClass)) continue;
                        try {
                            Class.forName(dependency.testClass());
                            ignore.add(dependency);
                        } catch (Throwable ex) {
                            install.add(dependency);
                        }

                        if (Files.exists(dependency.file())) {
                            byte[] rBytes = Files.readAllBytes(dependency.file());

                            Adler32 rAdler = new Adler32();
                            rAdler.update(ByteBuffer.wrap(rBytes));

                            CRC32 rCrc = new CRC32();
                            rCrc.update(ByteBuffer.wrap(rBytes));

                            dependency.generateChecksum().define("adler", rAdler.getValue());
                            dependency.generateChecksum().define("crc", rCrc.getValue());
                        } else {
                            dependency.generateChecksum().define("adler", 0);
                            dependency.generateChecksum().define("crc", 0);
                        }

                        dependency.checksum().define("adler", adler);
                        dependency.checksum().define("crc", crc);

                        Checksum checksum = dependency.checksum();
                        if (checksum.matches(dependency.generateChecksum()) && !checksum.verify(hash)) {
                            if (!ignore.contains(dependency))
                                ignore.add(dependency);

                            install.remove(dependency);
                        }

                        CPluginDependency.dependencies.put(id, dependency);
                    }

                    for (JsonDependency dependency : ignore) {
                        plugin.logInfo("Dependency {0} will be loaded from memory", dependency.name());
                    }
                    for (JsonDependency dependency : install) {
                        plugin.warn("Dependency {0} will be downloaded", dependency.name());
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
