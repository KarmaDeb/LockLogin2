package es.karmadev.locklogin.api.extension.plugin;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import lombok.Getter;

import java.nio.file.Path;

/**
 * A plugin that extends a module
 */
@Getter
public abstract class PluginModule<T> extends Module {

    protected final T plugin;

    /**
     * Initialize the plugin module
     *
     * @param plugin the plugin owning the module
     */
    public PluginModule(final T plugin, final String name, final String version, final String description, final String[] authors) {
        super(name, version, description, authors, new PermissionObject[0]);
        this.plugin = plugin;
    }

    /**
     * Get the module file
     *
     * @return the module file
     */
    public Path getFile() {
        return runtime().getFile();
    }
}
