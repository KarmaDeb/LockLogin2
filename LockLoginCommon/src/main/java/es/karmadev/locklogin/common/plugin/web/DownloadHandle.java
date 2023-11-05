package es.karmadev.locklogin.common.plugin.web;

import com.google.gson.*;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.util.StreamUtils;
import es.karmadev.api.web.WebDownloader;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.marketplace.resource.ResourceDownload;
import es.karmadev.locklogin.api.plugin.marketplace.storage.StoredResource;
import es.karmadev.locklogin.api.task.FutureTask;
import es.karmadev.locklogin.common.plugin.web.local.CStoredResource;
import es.karmadev.locklogin.common.plugin.web.manifest.ManifestFile;
import es.karmadev.locklogin.common.plugin.web.manifest.ResourceManifest;
import es.karmadev.locklogin.common.util.LockLoginJson;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Value(staticConstructor = "of")
@Getter
public class DownloadHandle implements ResourceDownload {

    private final static DownloadHandle EMPTY_HANDLE = new DownloadHandle("", 0, null, null);

    String file;
    long size;

    @FieldNameConstants.Exclude
    URL downloadURL;
    @FieldNameConstants.Exclude
    CMarketResource resource;

    /**
     * Download the file
     *
     * @return if the file was able to be downloaded
     */
    @Override
    public FutureTask<Boolean> download() {
        FutureTask<Boolean> executor = new FutureTask<>();
        executor.completeAsynchronously(() -> {
            LockLogin plugin = CurrentPlugin.getPlugin();
            if (plugin == null) throw new IllegalStateException("Cannot download LockLogin marketplace resource because plugin is not valid");

            CMarketPlace market = (CMarketPlace) plugin.getMarketPlace();
            StoredResource stored = market.getManager().getResources().stream().filter((rs) ->
                    rs.getId() == resource.getId() && rs.getVersion().equals(resource.getVersion())).findAny().orElse(null);
            if (stored != null) {
                plugin.warn("Resource {0} is already installed and up-to-date, skipping download", resource.getName());
                if (!stored.isLoaded()) {
                    stored.load();
                }

                return true;
            }

            stored = market.getManager().getResources().stream().filter((rs) ->
                    rs.getId() == resource.getId()).findAny().orElse(null);
            if (stored != null) {
                plugin.warn("Preparing to update resource {0}", resource.getName());
                market.getManager().uninstall(stored.getId());
            }

            if (downloadURL == null) {
                return false;
            }

            String response = URLUtilities.get(downloadURL);
            Gson gson = new GsonBuilder().create();

            JsonElement element = gson.fromJson(response, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                return false;
            }

            String finalDownloadURL = element.getAsJsonObject().get("url").getAsString();
            URL url = URLUtilities.fromString(finalDownloadURL);
            if (url == null) {
                return false;
            }

            WebDownloader downloader = new WebDownloader(url);
            Path destination = plugin.workingDirectory().resolve("marketplace").resolve("tmp")
                    .resolve(file);

            try {
                if (downloader.download(destination)) {
                    Path resourcesDirectory = plugin.workingDirectory().resolve("marketplace").resolve("resources")
                            .resolve(String.valueOf(resource.getId()));
                    String extension = PathUtilities.getExtension(destination);

                    ResourceManifest manifest = new ResourceManifest();
                    switch (extension.toLowerCase()) {
                        case "zip":
                            try (ZipFile zip = new ZipFile(destination.toFile())) {
                                ZipEntry entry = zip.getEntry("manifest.json");
                                if (entry == null || entry.isDirectory()) {
                                    plugin.warn("Resource file is not a valid zip file (missing manifest.json)");
                                    return false;
                                }

                                try (InputStream stream = zip.getInputStream(entry)) {
                                    JsonElement mf = gson.fromJson(new String(StreamUtils.read(stream)), JsonElement.class);
                                    manifest = new ResourceManifest();
                                    if (!manifest.read(plugin, mf)) {
                                        plugin.warn("Resource file is not a valid zip file (invalid manifest.json)");
                                        return false;
                                    }
                                }

                                Enumeration<? extends ZipEntry> entries = zip.entries();
                                while (entries.hasMoreElements()) {
                                    ZipEntry tmpEntry = entries.nextElement();
                                    if (tmpEntry.isDirectory()) continue;

                                    String name = tmpEntry.getName();
                                    Path entryPath = resourcesDirectory.resolve(name);
                                    if (name.contains("/")) {
                                        entryPath = resourcesDirectory;
                                        for (String p : name.split("/")) {
                                            entryPath = entryPath.resolve(p);
                                        }
                                    }

                                    try (InputStream str = zip.getInputStream(tmpEntry)) {
                                        PathUtilities.write(entryPath, StreamUtils.read(str));
                                    }
                                }
                            } finally {
                                PathUtilities.destroy(destination);
                            }
                            break;
                        case "jar":
                            Path virtualManifest = resourcesDirectory.resolve("manifest.json");

                            try (JarFile jar = new JarFile(destination.toFile())) {
                                ZipEntry entry = jar.getEntry("manifest.json");
                                if (entry != null && !entry.isDirectory()) {
                                    try (InputStream stream = jar.getInputStream(entry)) {
                                        String raw = new String(StreamUtils.read(stream));
                                        JsonElement mf = gson.fromJson(raw, JsonElement.class);
                                        manifest = new ResourceManifest();

                                        if (manifest.read(plugin, mf)) {
                                            PathUtilities.write(virtualManifest, raw);

                                            for (ManifestFile file : manifest.getFiles()) {
                                                JarEntry jarEntry = jar.getJarEntry(file.getFile());
                                                if (jarEntry == null || jarEntry.isDirectory()) continue;

                                                String name = jarEntry.getName();
                                                Path entryPath = resourcesDirectory.resolve(name);
                                                if (name.contains("/")) {
                                                    entryPath = resourcesDirectory;
                                                    for (String p : name.split("/")) {
                                                        entryPath = entryPath.resolve(p);
                                                    }
                                                }

                                                try (InputStream str = jar.getInputStream(jarEntry)) {
                                                    PathUtilities.write(entryPath, StreamUtils.read(str));
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!Files.exists(virtualManifest)) {
                                JsonObject object = new JsonObject();
                                object.addProperty("title", resource.getName());
                                object.addProperty("category", resource.getCategory().getId());
                                object.addProperty("langVersion", LockLoginJson.getLangVersion());
                                JsonArray files = new JsonArray();
                                JsonObject singleFile = new JsonObject();
                                singleFile.addProperty("name", "module.jar");
                                singleFile.addProperty("directory", PathUtilities.getName(destination, true));
                                singleFile.add("locales", new JsonArray());
                                files.add(singleFile);
                                object.add("files", files);
                                object.addProperty("_comment", "Automatically generated by LockLogin");
                                String raw = gson.toJson(object);

                                manifest.read(plugin, object);
                                PathUtilities.write(virtualManifest, raw);
                            }

                            Path virtualModule = resourcesDirectory.resolve("module.jar");
                            try {
                                Files.copy(destination, virtualModule, StandardCopyOption.REPLACE_EXISTING);
                            } catch (Exception ex) {
                                plugin.log(ex, "Failed to prepare resource {0}", resource.getName());
                                plugin.warn("Failed to prepare resource {0}", resource.getName());
                                return false;
                            } finally {
                                PathUtilities.destroy(destination);
                            }
                            break;
                        default:
                            plugin.err("Invalid resource downloaded. Expected it to be a .zip or .jar, but got .{0}", extension);
                            PathUtilities.destroy(destination);
                            return false;
                    }

                    Path resourceMeta = resourcesDirectory.resolve("resource.meta");
                    List<String> lines = new ArrayList<>();
                    lines.add("id=" + resource.getId());
                    lines.add("category=" + resource.getCategory().name());
                    lines.add("name=" + resource.getName());
                    lines.add("description=" + resource.getDescription());
                    lines.add("publisher=" + resource.getPublisher());
                    lines.add("version=" + resource.getVersion());
                    lines.add("download=" + Instant.now().toEpochMilli());
                    PathUtilities.write(resourceMeta, lines);

                    CStoredResource storedResource = CStoredResource.of(
                            resource.getId(),
                            false,
                            resource.getCategory(),
                            resource.getName(),
                            resource.getDescription(),
                            resource.getPublisher(),
                            resource.getVersion(),
                            Instant.now(),
                            manifest
                    );

                    market.getManager().getResourceSet().add(storedResource);
                    storedResource.load();

                    if (Files.exists(destination)) {
                        PathUtilities.destroy(destination);
                        /*
                        We want to ensure the downloaded module
                        is not keep in system
                         */
                    }

                    return storedResource.isLoaded();
                }

                PathUtilities.destroy(destination);
                return false;
            } catch (IOException | NoSuchAlgorithmException | KeyManagementException ex) {
                ex.printStackTrace();
                return false;
            }
        });

        return executor;
    }

    /**
     * Get an empty download handle
     *
     * @return the download handle
     */
    public static DownloadHandle empty() {
        return EMPTY_HANDLE;
    }
}
