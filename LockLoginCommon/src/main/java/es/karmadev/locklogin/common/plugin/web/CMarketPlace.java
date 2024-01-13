package es.karmadev.locklogin.common.plugin.web;

import es.karmadev.api.kson.JsonArray;
import es.karmadev.api.kson.JsonInstance;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.kson.io.JsonReader;
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
        JsonInstance element = JsonReader.read(response);

        if (!element.isObjectType()) {
            plugin.err("Connected to LockLogin Marketplace server but service didn't give any valid response");
            return;
        }

        cachedVersion = element.asObject().getChild("version").asNative().getInteger();
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

            return element.asObject()
                    .getChild("pagination").asObject()
                    .getChild("max").asNative()
                    .getInteger();
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

            return element.asObject()
                    .getChild("resources").asArray().size();
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
            if (element == null || !element.isObjectType()) {
                return resources;
            }

            JsonArray resourceArray = element.getChild("resources").asArray();
            for (JsonInstance jsonResource : resourceArray) {
                if (!jsonResource.isObjectType()) continue;
                JsonObject object = jsonResource.asObject();

                int resourceId = object.getChild("id").asNative().getInteger();
                Category resourceCategory = Category.byId(object.getChild("category").asObject()
                        .getChild("id").asNative().getInteger());
                String name = object.getChild("name").asNative().getString();
                String description = object.getChild("description").asNative().getString();
                String version = object.getChild("version").asNative().getString();
                String publisher = object.getChild("publisher").asObject()
                        .getChild("name").asNative().getString();
                int downloadCount = object.getChild("downloads").asNative().getInteger();

                JsonObject downloadInfo = object.getChild("download").asObject();
                String downloadName = downloadInfo.getChild("name").asNative().getString();
                long downloadSize = downloadInfo.getChild("size").asNative().getLong();
                String downloadURL = downloadInfo.getChild("url").asNative().getString();

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

            JsonObject object = element.getChild("resource").asObject();
            Category resourceCategory = Category.byId(object.getChild("category").asObject()
                    .getChild("id").asNative().getInteger());
            String name = object.getChild("name").asNative().getString();
            String description = object.getChild("description").asNative().getString();
            String version = object.getChild("version").asNative().getString();
            String publisher = object.getChild("publisher").asObject()
                    .getChild("name").asNative().getString();
            int downloadCount = object.getChild("downloads").asNative().getInteger();

            JsonObject downloadInfo = object.getChild("download").asObject();
            String downloadName = downloadInfo.getChild("name").asNative().getString();
            long downloadSize = downloadInfo.getChild("size").asNative().getLong();
            String downloadURL = downloadInfo.getChild("url").asNative().getString();

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

        String response = URLUtilities.get(url).replace("\\u003d", "=");
        /*
        For some reason, in some cases, sometimes, it encodes the = as the UTF code. Don't know
        why, don't know when, don't know how, but it just happens
        */
        JsonInstance element = JsonReader.read(response);
        if (!element.isObjectType()) {
            plugin.err("Connected to LockLogin Marketplace server but service didn't give any valid response");
            return null;
        }

        return element.asObject();
    }

}
