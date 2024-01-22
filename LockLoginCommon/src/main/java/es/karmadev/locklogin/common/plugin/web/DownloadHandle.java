package es.karmadev.locklogin.common.plugin.web;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.util.StreamUtils;
import es.karmadev.api.kson.JsonArray;
import es.karmadev.api.kson.JsonInstance;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.kson.io.JsonReader;
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
                plugin.logErr("An error occurred while downloading resource {0} (Invalid resource URL)");
                return false;
            }

            String response = URLUtilities.get(downloadURL);

            JsonInstance element = JsonReader.read(response);
            if (!element.isObjectType()) {
                plugin.logErr("An error occurred while downloading resource {0} [{1}]", resource.getId(), response);
                return false;
            }

            String finalDownloadURL = element.asObject().getChild("url").asNative().getString();
            URL url = URLUtilities.fromString(finalDownloadURL);

            if (url == null) {
                plugin.logErr("An error occurred while downloading resource {0} (Invalid download URL)");
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
                                    JsonInstance mf = JsonReader.read(stream);

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
                                        JsonInstance mf = JsonReader.read(raw);

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
                                JsonObject object = JsonObject.newObject("", "");
                                object.put("title", resource.getName());
                                object.put("category", resource.getCategory().getId());
                                object.put("langVersion", LockLoginJson.getLangVersion());
                                JsonArray files = JsonArray.newArray("", "files");
                                JsonObject singleFile = JsonObject.newObject("", "");
                                singleFile.put("name", "module.jar");
                                singleFile.put("directory", PathUtilities.getName(destination, true));
                                singleFile.put("locales", JsonArray.newArray("", "locales"));
                                files.add(singleFile);
                                object.put("files", files);
                                object.put("_comment", "Automatically generated by LockLogin");
                                String raw = object.toString(false);

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
                    }

                    return storedResource.isLoaded();
                }

                plugin.logErr("An error occurred while downloading resource {0}", resource.getId());
                PathUtilities.destroy(destination);
                return false;
            } catch (IOException | NoSuchAlgorithmException | KeyManagementException ex) {
                plugin.log(ex, "An error occurred while downloading resource {0}", resource.getId());
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
