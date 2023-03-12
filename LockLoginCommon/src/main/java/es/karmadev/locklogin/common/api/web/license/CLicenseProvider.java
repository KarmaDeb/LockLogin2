package es.karmadev.locklogin.common.api.web.license;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.license.License;
import es.karmadev.locklogin.api.plugin.license.LicenseProvider;
import es.karmadev.locklogin.common.api.web.license.data.CLicense;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.routing.RoutingSupport;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

/**
 * LockLogin license provider
 */
public class CLicenseProvider implements LicenseProvider {

    private final URI update_uri = URI.create("https://reddo.es/licenser/locklogin/update");
    private final URI fetch_uri = URI.create("https://reddo.es/licenser/locklogin/fetch");
    private final URI request_uri = URI.create("https://reddo.es/licenser/locklogin/request");

    /**
     * Update the license
     *
     * @param license the license to update
     * @return if the license was able to be updated
     */
    @Override
    public License update(final Path license) {
        LockLogin plugin = CurrentPlugin.getPlugin();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            FileBody fileBody = new FileBody(license.toFile());
            try (HttpEntity entity = MultipartEntityBuilder.create().addPart("file", fileBody).build()) {
                HttpPost request = new HttpPost(update_uri);
                request.setEntity(entity);

                HttpHost host = RoutingSupport.determineHost(request);
                try (ClassicHttpResponse classic = client.executeOpen(host, request, null)) {
                    try (HttpEntity ent = classic.getEntity()) {
                        Header[] contentType = classic.getHeaders("Content-type");
                        boolean json = false;

                        for (Header header : contentType) {
                            if (header.getValue().contains("application/json")) {
                                json = true;
                                break;
                            }
                        }
                        try (InputStream stream = ent.getContent(); InputStreamReader isr = new InputStreamReader(stream); BufferedReader br = new BufferedReader(isr)) {
                            String raw_response = br.readLine();
                            if (json) {
                                Gson gson = new GsonBuilder().create();
                                JsonElement element = gson.fromJson(raw_response, JsonElement.class);

                                if (element.isJsonObject()) {
                                    JsonObject information = element.getAsJsonObject();
                                    JsonObject owner = information.getAsJsonObject("owner");
                                    JsonObject expiration_data = information.getAsJsonObject("expiration");

                                    String bValue = information.get("base").getAsString();
                                    String version = information.get("version").getAsString();
                                    if (information.has("project")) {
                                        String project = information.get("project").getAsString();
                                        if (!project.equalsIgnoreCase("locklogin")) {
                                            plugin.err("Cannot load license of {0} for project: LockLogin", project.toLowerCase());
                                            return null;
                                        }
                                    } else {
                                        plugin.err("Cannot load version {0} license for this version of LockLogin. Run the command /locklogin license update to fix this", version);
                                        return null;
                                    }

                                    String sync = information.get("sync").getAsString();
                                    String key = information.get("key").getAsString();
                                    String name = owner.get("name").getAsString();
                                    String contact = owner.get("contact").getAsString();
                                    long stamp_granted = expiration_data.get("granted").getAsLong();
                                    long stamp_expires = expiration_data.get("expires").getAsLong();
                                    int servers = information.get("servers").getAsInt();
                                    long capacity = information.get("storage").getAsLong();
                                    boolean free = information.get("free").getAsBoolean();

                                    return CLicense.builder()
                                            .license_file(license)
                                            .base64(bValue)
                                            .version(version)
                                            .sync(sync)
                                            .com(key)
                                            .name(name)
                                            .contact(contact)
                                            .created(stamp_granted)
                                            .expiration(stamp_expires)
                                            .proxies(servers)
                                            .storage(capacity)
                                            .free(free)
                                            .build();
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | HttpException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Update the license
     *
     * @param license the license to update
     * @return the update fields
     */
    @Override
    public String[] update(final License license) {
        Path file = license.location();
        LockLogin plugin = CurrentPlugin.getPlugin();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            FileBody fileBody = new FileBody(file.toFile());
            try (HttpEntity entity = MultipartEntityBuilder.create().addPart("file", fileBody).build()) {
                HttpPost request = new HttpPost(update_uri);
                request.setEntity(entity);

                HttpHost host = RoutingSupport.determineHost(request);
                try (ClassicHttpResponse classic = client.executeOpen(host, request, null)) {
                    try (HttpEntity ent = classic.getEntity()) {
                        Header[] contentType = classic.getHeaders("Content-type");
                        boolean json = false;

                        for (Header header : contentType) {
                            if (header.getValue().contains("application/json")) {
                                json = true;
                                break;
                            }
                        }
                        try (InputStream stream = ent.getContent(); InputStreamReader isr = new InputStreamReader(stream); BufferedReader br = new BufferedReader(isr)) {
                            String raw_response = br.readLine();
                            if (json) {
                                Gson gson = new GsonBuilder().create();
                                JsonElement element = gson.fromJson(raw_response, JsonElement.class);

                                if (element.isJsonObject()) {
                                    JsonObject information = element.getAsJsonObject();
                                    JsonObject owner = information.getAsJsonObject("owner");
                                    JsonObject expiration_data = information.getAsJsonObject("expiration");

                                    String bValue = information.get("base").getAsString();
                                    String version = information.get("version").getAsString();
                                    if (information.has("project")) {
                                        String project = information.get("project").getAsString();
                                        if (!project.equalsIgnoreCase("locklogin")) {
                                            plugin.err("Cannot load license of {0} for project: LockLogin", project.toLowerCase());
                                            return new String[]{"error"};
                                        }
                                    } else {
                                        plugin.err("Cannot load version {0} license for this version of LockLogin. Run the command /locklogin license update to fix this", version);
                                        return new String[]{"error"};
                                    }

                                    String sync = information.get("sync").getAsString();
                                    String key = information.get("key").getAsString();
                                    String name = owner.get("name").getAsString();
                                    String contact = owner.get("contact").getAsString();
                                    long stamp_granted = expiration_data.get("granted").getAsLong();
                                    long stamp_expires = expiration_data.get("expires").getAsLong();
                                    int servers = information.get("servers").getAsInt();
                                    long capacity = information.get("storage").getAsLong();
                                    boolean free = information.get("free").getAsBoolean();

                                    License updated = CLicense.builder()
                                            .license_file(file)
                                            .base64(bValue)
                                            .version(version)
                                            .sync(sync)
                                            .com(key)
                                            .name(name)
                                            .contact(contact)
                                            .created(stamp_granted)
                                            .expiration(stamp_expires)
                                            .proxies(servers)
                                            .storage(capacity)
                                            .free(free)
                                            .build();

                                    return license.merge(updated);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | HttpException ex) {
            ex.printStackTrace();
        }

        return new String[]{"error"};
    }

    /**
     * Fetch a license from a file
     *
     * @param file the license file
     * @return the license
     * @throws SecurityException if the license load source is not the plugin
     */
    @Override
    public License load(final Path file) throws SecurityException {
        LockLogin plugin = CurrentPlugin.getPlugin();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            FileBody fileBody = new FileBody(file.toFile());
            try (HttpEntity entity = MultipartEntityBuilder.create().addPart("file", fileBody).build()) {
                HttpPost request = new HttpPost(fetch_uri);
                request.setEntity(entity);

                HttpHost host = RoutingSupport.determineHost(request);
                try (ClassicHttpResponse classic = client.executeOpen(host, request, null)) {
                    try (HttpEntity ent = classic.getEntity()) {
                        Header[] contentType = classic.getHeaders("Content-type");
                        boolean json = false;

                        for (Header header : contentType) {
                            if (header.getValue().contains("application/json")) {
                                json = true;
                                break;
                            }
                        }
                        try (InputStream stream = ent.getContent(); InputStreamReader isr = new InputStreamReader(stream); BufferedReader br = new BufferedReader(isr)) {
                            String raw_response = br.readLine();
                            if (json) {
                                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                JsonElement element = gson.fromJson(raw_response, JsonElement.class);

                                if (element.isJsonObject()) {
                                    JsonObject information = element.getAsJsonObject();
                                    JsonObject owner = information.getAsJsonObject("owner");
                                    JsonObject expiration_data = information.getAsJsonObject("expiration");

                                    String bValue = information.get("base").getAsString();
                                    String version = information.get("version").getAsString();
                                    int versionType = Integer.parseInt(version.replaceAll("[^0-9]", ""));

                                    if (information.has("project") && versionType >= CurrentPlugin.licenseVersion()) {
                                        String project = information.get("project").getAsString();

                                        if (!project.equalsIgnoreCase("locklogin")) {
                                            plugin.err("Cannot load license of {0} for project: LockLogin", project.toLowerCase());
                                            return null;
                                        }
                                    } else {
                                        plugin.err("Cannot load version {0} license for this version of LockLogin (Required at least: {1}). Run the command /locklogin license update to fix this", version, "v" + CurrentPlugin.licenseVersion());
                                        return null;
                                    }

                                    String sync = information.get("sync").getAsString();
                                    String key = information.get("key").getAsString();
                                    String name = owner.get("name").getAsString();
                                    String contact = owner.get("contact").getAsString();
                                    long stamp_granted = expiration_data.get("granted").getAsLong();
                                    long stamp_expires = expiration_data.get("expires").getAsLong();
                                    int servers = information.get("servers").getAsInt();
                                    long capacity = information.get("storage").getAsLong();
                                    boolean free = information.get("free").getAsBoolean();

                                    return CLicense.builder()
                                            .license_file(file)
                                            .base64(bValue)
                                            .version(version)
                                            .sync(sync)
                                            .com(key)
                                            .name(name)
                                            .contact(contact)
                                            .created(stamp_granted)
                                            .expiration(stamp_expires)
                                            .proxies(servers)
                                            .storage(capacity)
                                            .free(free)
                                            .build();
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | HttpException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Synchronize with a license
     *
     * @param key the license synchronization key
     * @return the synchronized license
     */
    @Override
    public License synchronize(final String key) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path default_license = plugin.workingDirectory().resolve("data").resolve("license.dat");
        if (Files.exists(default_license)) {
            plugin.updateLicense(null);
            plugin.warn("Uninstalled current license");

            PathUtilities.destroy(default_license);
        }

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest post = new HttpPost(request_uri);
            JsonObject msg = new JsonObject();
            msg.addProperty("sync", key);

            Gson gson = new GsonBuilder().create();
            try (StringEntity post_data = new StringEntity(gson.toJson(msg))) {
                post.setEntity(post_data);
                post.addHeader("Content-Type", "application/json");

                HttpHost host = RoutingSupport.determineHost(post);
                try (ClassicHttpResponse response = client.executeOpen(host, post, null)) {
                    String license_base = EntityUtils.toString(response.getEntity());

                    if (!license_base.equalsIgnoreCase("error")) {
                        byte[] data = Base64.getDecoder().decode(license_base);

                        Path license_file = Files.createTempFile("locklogin_", "_license");
                        PathUtilities.create(license_file);
                        Files.write(license_file, data);

                        License license = load(license_file); //If it returns null, it basically means that the license failed to register
                        if (license != null) {
                            license.setLocation(default_license.getParent());
                            return license;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Synchronize with a license
     *
     * @param key      the license key
     * @param username the license owner
     * @param password the license password
     * @return the synchronized license
     * @throws SecurityException if the license request source is not the plugin
     */
    @Override
    public License synchronize(final String key, final String username, final String password) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path default_license = plugin.workingDirectory().resolve("data").resolve("license.dat");
        if (Files.exists(default_license)) {
            plugin.updateLicense(null);
            plugin.warn("Uninstalled current license");

            PathUtilities.destroy(default_license);
        }

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest post = new HttpPost(request_uri);
            JsonObject msg = new JsonObject();
            msg.addProperty("sync", key);
            msg.addProperty("username", username);
            msg.addProperty("password", password);

            Gson gson = new GsonBuilder().create();
            try (StringEntity post_data = new StringEntity(gson.toJson(msg))) {
                post.setEntity(post_data);
                post.addHeader("Content-Type", "application/json");

                HttpHost host = RoutingSupport.determineHost(post);
                try (ClassicHttpResponse response = client.executeOpen(host, post, null)) {
                    String license_base = EntityUtils.toString(response.getEntity());

                    if (!license_base.equalsIgnoreCase("error")) {
                        byte[] data = Base64.getDecoder().decode(license_base);

                        Path license_file = Files.createTempFile("locklogin_", "_license");
                        PathUtilities.create(license_file);
                        Files.write(license_file, data);

                        License license = load(license_file); //If it returns null, it basically means that the license failed to register
                        if (license != null) {
                            license.setLocation(default_license.getParent());
                            return license;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Request a new license
     *
     * @return a new free license
     */
    @Override
    public License request() throws SecurityException {
        LockLogin plugin = CurrentPlugin.getPlugin();
        License installed = plugin.license();
        if (installed != null) return installed;

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest post = new HttpPost(request_uri);
            HttpHost host = RoutingSupport.determineHost(post);
            try (ClassicHttpResponse response = client.executeOpen(host, post, null)) {
                String license_base = EntityUtils.toString(response.getEntity());

                if (!license_base.equalsIgnoreCase("error")) {
                    byte[] data = Base64.getDecoder().decode(license_base);

                    Path license_file = Files.createTempFile("locklogin_", "_license");
                    PathUtilities.create(license_file);
                    Files.write(license_file, data);

                    License license = load(license_file); //If it returns null, it basically means that the license failed to register
                    if (license != null) {
                        license.setLocation(plugin.workingDirectory().resolve("data"));
                        return license;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Request a new license
     *
     * @param id       the license id, for example: 3c105031-bd93-45f0-945a-217e6e887e7b
     * @param username the license owner, for example: easter_egg
     * @param password the license password, for example: web_service_2023
     * @return the license
     * @throws SecurityException if the license request source is not the plugin
     */
    @Override
    public License request(final UUID id, final String username, final String password) throws SecurityException {
        LockLogin plugin = CurrentPlugin.getPlugin();
        License installed = plugin.license();
        if (installed != null) return installed;

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest post = new HttpPost(request_uri);
            JsonObject msg = new JsonObject();
            msg.addProperty("license", id.toString());
            msg.addProperty("username", username);
            msg.addProperty("password", password);

            Gson gson = new GsonBuilder().create();
            try (StringEntity post_data = new StringEntity(gson.toJson(msg))) {
                post.setEntity(post_data);
                post.addHeader("Content-Type", "application/json");

                HttpHost host = RoutingSupport.determineHost(post);
                try (ClassicHttpResponse response = client.executeOpen(host, post, null)) {
                    String license_base = EntityUtils.toString(response.getEntity());

                    if (!license_base.equalsIgnoreCase("error")) {
                        byte[] data = Base64.getDecoder().decode(license_base);

                        Path license_file = Files.createTempFile("locklogin_", "_license");
                        PathUtilities.create(license_file);
                        Files.write(license_file, data);

                        License license = load(license_file); //If it returns null, it basically means that the license failed to register
                        if (license != null) {
                            license.setLocation(plugin.workingDirectory().resolve("data"));
                            return license;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
