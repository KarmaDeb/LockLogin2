package es.karmadev.locklogin.common.plugin.web;

import com.google.gson.*;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.CacheAble;
import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.plugin.marketplace.MarketPlace;
import es.karmadev.locklogin.api.plugin.marketplace.resource.MarketResource;
import es.karmadev.locklogin.api.task.FutureTask;
import es.karmadev.locklogin.common.plugin.web.local.CResourceManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@CacheAble(name = "LockLogin Marketplace")
public class CMarketPlace implements MarketPlace {

    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private static int cachedVersion = 0;

    /**
     * Pre cache the object
     */
    @SuppressWarnings("unused")
    public static void preCache() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot precache LockLogin marketplace because plugin is not valid");

        final String apiUrl = "https://api.karmadev.es/";

        URL url = URLUtilities.fromString(apiUrl);
        if (url == null) {
            plugin.err("Couldn't connect to LockLogin Marketplace, are we connected to the internet?");
            return;
        }

        String response = URLUtilities.get(url);
        Gson gson = new GsonBuilder().create();

        JsonElement element = gson.fromJson(response, JsonElement.class);
        if (element == null || !element.isJsonObject()) {
            plugin.err("Connected to LockLogin Marketplace server but service didn't give any valid response");
            return;
        }

        cachedVersion = element.getAsJsonObject().get("version").getAsInt();
    }

    private final CResourceManager resourceManager = new CResourceManager();

    /**
     * Get the marketplace version
     *
     * @return the marketplace version
     */
    @Override
    public int getVersion() {
        return cachedVersion;
    }

    /**
     * Get the amount of pages on the
     * specified resource category
     *
     * @param category the category
     * @return the amount of pages for the category
     */
    @Override
    public FutureTask<Integer> getPages(final Category category) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot precache LockLogin marketplace because plugin is not valid");

        if (category.equals(Category.EMAIL_TEMPLATE)) {
            throw new IllegalStateException("Cannot get pages for category " + category);
        }

        FutureTask<Integer> task = new FutureTask<>();
        task.completeAsynchronously(() -> {
            int categoryId = category.getId();
            final String apiUrl = String.format("https://api.karmadev.es/category/%d/resources", categoryId);

            URL url = URLUtilities.fromString(apiUrl);
            JsonObject element = execute(plugin, url);
            if (element == null) {
                return 0;
            }

            return element.getAsJsonObject().getAsJsonObject("pagination").get("max").getAsInt();
        });

        return task;
    }

    /**
     * Get the amount of resources on
     * that category
     *
     * @param category the category
     * @return the resources under the category
     */
    @Override
    public FutureTask<Integer> getResourcesAmount(final Category category) {
        return getResourcesAmount(category, 0);
    }

    /**
     * Get the amount of resources on
     * that category and the specified
     * page
     *
     * @param category the category
     * @param page     the page
     * @return the resources under the category and
     * the page
     */
    @Override
    public FutureTask<Integer> getResourcesAmount(final Category category, final int page) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot precache LockLogin marketplace because plugin is not valid");

        if (category.equals(Category.EMAIL_TEMPLATE)) {
            throw new IllegalStateException("Cannot get pages for category " + category);
        }

        FutureTask<Integer> task = new FutureTask<>();
        task.completeAsynchronously(() -> {
            int categoryId = category.getId();
            final String apiUrl = String.format("https://api.karmadev.es/category/%d/resources/?page=%d", categoryId, page);

            URL url = URLUtilities.fromString(apiUrl);
            JsonObject element = execute(plugin, url);
            if (element == null) {
                return 0;
            }

            return element.getAsJsonObject().getAsJsonArray("resources").size();
        });

        return task;
    }

    /**
     * Get all the resources for
     * the category on the specified
     * page
     *
     * @param category the category
     * @param page the page
     * @return the resources on the page
     */
    @Override
    public FutureTask<Collection<? extends MarketResource>> getResources(final Category category, final int page) {
        FutureTask<Collection<? extends MarketResource>> task = new FutureTask<>();

        List<CMarketResource> resources = new ArrayList<>();

        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot precache LockLogin marketplace because plugin is not valid");

        if (category.equals(Category.EMAIL_TEMPLATE)) {
            throw new IllegalStateException("Cannot get pages for category " + category);
        }

        task.completeAsynchronously(() -> {
            int categoryId = category.getId();
            final String apiUrl = String.format("https://api.karmadev.es/category/%d/resources/?page=%d", categoryId, page);

            URL url = URLUtilities.fromString(apiUrl);
            JsonObject element = execute(plugin, url);
            if (element == null || !element.isJsonObject()) {
                return resources;
            }

            JsonArray resourceArray = element.getAsJsonArray("resources");
            for (JsonElement jsonResource : resourceArray) {
                if (!jsonResource.isJsonObject()) continue;
                JsonObject object = jsonResource.getAsJsonObject();

                int resourceId = object.get("id").getAsInt();
                Category resourceCategory = Category.byId(object.get("category").getAsJsonObject().get("id").getAsInt());
                String name = object.get("name").getAsString();
                String description = object.get("description").getAsString();
                String version = object.get("version").getAsString();
                String publisher = object.get("publisher").getAsJsonObject().get("name").getAsString();
                int downloadCount = object.get("downloads").getAsInt();

                JsonObject downloadInfo = object.getAsJsonObject("download");
                String downloadName = downloadInfo.get("name").getAsString();
                long downloadSize = downloadInfo.get("size").getAsLong();
                String downloadURL = downloadInfo.get("url").getAsString();

                CMarketResource resource = CMarketResource
                        .of(resourceId, resourceCategory,
                                name, description,
                                version, publisher,
                                downloadCount, downloadName,
                                downloadSize, URLUtilities.fromString(downloadURL));

                resources.add(resource);
            }

            return resources;
        });

        return task;
    }

    /**
     * Get a resource
     *
     * @param id the resource id
     * @return the resource
     */
    @Override
    public FutureTask<MarketResource> getResource(final int id) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot precache LockLogin marketplace because plugin is not valid");

        final String apiUrl = String.format("https://api.karmadev.es/resource/%d", id);

        FutureTask<MarketResource> task = new FutureTask<>();
        task.completeAsynchronously(() -> {
            URL url = URLUtilities.fromString(apiUrl);
            JsonObject element = execute(plugin, url);
            if (element == null) {
                return null;
            }

            JsonObject object = element.getAsJsonObject("resource");
            Category resourceCategory = Category.byId(object.get("category").getAsJsonObject().get("id").getAsInt());
            String name = object.get("name").getAsString();
            String description = object.get("description").getAsString();
            String version = object.get("version").getAsString();
            String publisher = object.get("publisher").getAsJsonObject().get("name").getAsString();
            int downloadCount = object.get("downloads").getAsInt();

            JsonObject downloadInfo = object.getAsJsonObject("download");
            String downloadName = downloadInfo.get("name").getAsString();
            long downloadSize = downloadInfo.get("size").getAsLong();
            String downloadURL = downloadInfo.get("url").getAsString();

            return CMarketResource
                    .of(id, resourceCategory,
                            name, description,
                            version, publisher,
                            downloadCount, downloadName,
                            downloadSize, URLUtilities.fromString(downloadURL));
        });

        return task;
    }

    /**
     * Get the resource manager
     *
     * @return the resource manager
     */
    @Override
    public CResourceManager getManager() {
        return resourceManager;
    }

    private JsonObject execute(final LockLogin plugin, final URL url) {
        if (url == null) {
            plugin.err("Couldn't connect to LockLogin Marketplace, are we connected to the internet?");
            return null;
        }

        String response = URLUtilities.get(url);
        Gson gson = new GsonBuilder().create();

        JsonElement element = gson.fromJson(response, JsonElement.class);
        if (element == null || !element.isJsonObject()) {
            plugin.err("Connected to LockLogin Marketplace server but service didn't give any valid response");
            return null;
        }

        return element.getAsJsonObject();
    }
}
