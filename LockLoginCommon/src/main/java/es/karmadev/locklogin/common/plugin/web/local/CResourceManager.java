package es.karmadev.locklogin.common.plugin.web.local;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.plugin.marketplace.storage.ResourceManager;
import es.karmadev.locklogin.api.plugin.marketplace.storage.StoredResource;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CResourceManager extends ResourceManager {

    private final Set<CStoredResource> resourceSet = ConcurrentHashMap.newKeySet();

    /**
     * Get the amount of resources
     *
     * @return the amount of resources
     */
    @Override
    public int getResourceCount() {
        return resourceSet.size();
    }

    /**
     * Get all the installed resources
     *
     * @return the installed resource
     */
    @Override
    public Collection<? extends StoredResource> getResources() {
        return Collections.unmodifiableSet(resourceSet);
    }

    /**
     * Uninstall the resource with the
     * specified ID
     *
     * @param id the resource ID
     */
    @Override
    public void uninstall(final int id) {
        CStoredResource resource = resourceSet.stream().filter((rs) -> rs.getId() == id).findAny().orElse(null);
        if (resource == null) return;

        resourceSet.remove(resource);
        resource.unload();

        LockLogin plugin = CurrentPlugin.getPlugin();
        if (resource.getCategory().equals(Category.TRANSLATION)) {
            Path translationDirectory = plugin.workingDirectory().resolve("marketplace").resolve("resources")
                    .resolve(String.valueOf(id));
            PathUtilities.destroy(translationDirectory);

            resource.getManifest().getFiles().forEach((manifestFile -> {
                Path languageDirectory = plugin.workingDirectory().resolve("marketplace")
                        .resolve("translations").resolve(manifestFile.getDirectoryName());
                PathUtilities.destroy(languageDirectory);
            }));
        }
    }
}
