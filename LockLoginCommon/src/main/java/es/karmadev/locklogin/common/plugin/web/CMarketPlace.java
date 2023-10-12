package es.karmadev.locklogin.common.plugin.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.CacheAble;
import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.plugin.marketplace.MarketPlace;
import es.karmadev.locklogin.api.plugin.marketplace.resource.MarketResource;
import es.karmadev.locklogin.api.plugin.marketplace.storage.ResourceManager;

import java.net.URL;
import java.util.Collection;

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
    public int getPages(final Category category) {
        return 0;
    }

    /**
     * Get the amount of resources on
     * that category
     *
     * @param category the category
     * @return the resources under the category
     */
    @Override
    public int getResources(final Category category) {
        return 0;
    }

    /**
     * Get all the resources for
     * the category on the specified
     * page
     *
     * @param category the category
     * @param page
     * @return the resources on the page
     */
    @Override
    public Collection<? extends MarketResource> getResources(final Category category, final int page) {
        return null;
    }

    /**
     * Get a resource
     *
     * @param id the resource id
     * @return the resource
     */
    @Override
    public MarketResource getResource(final int id) {
        return null;
    }

    /**
     * Get the resource manager
     *
     * @return the resource manager
     */
    @Override
    public ResourceManager getManager() {
        return null;
    }
}
