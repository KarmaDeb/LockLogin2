package es.karmadev.locklogin.common.api.plugin.file.spawn;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.api.file.yaml.handler.YamlHandler;
import es.karmadev.api.file.yaml.handler.YamlReader;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.spawn.CancelPolicy;
import es.karmadev.locklogin.api.plugin.file.spawn.SpawnConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CSpawnConfiguration implements SpawnConfiguration {

    private final YamlFileHandler yaml;

    public CSpawnConfiguration(final LockLogin plugin) {
        Path file = plugin.workingDirectory().resolve("spawn.yml");
        if (!Files.exists(file)) {
            PathUtilities.copy(plugin, "plugin/yaml/configuration/spawn/config.yml", file);
        }

        try {
            YamlReader reader = new YamlReader(plugin.loadResource("plugin/yaml/configuration/spawn/config.yml"));
            yaml = YamlHandler.load(file, reader);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        yaml.validate();
    }

    /**
     * Reload the configuration
     *
     * @return if the configuration
     * was able to be reloaded
     */
    @Override
    public boolean reload() {
        return yaml.reload();
    }

    /**
     * Get if the spawn is enabled
     *
     * @return if the spawn is enabled
     */
    @Override
    public boolean enabled() {
        return yaml.getBoolean("Enabled", false);
    }

    /**
     * Get if the spawn teleport command
     * is enabled
     *
     * @return if the /spawn command with
     * no argument is enabled
     */
    @Override
    public boolean teleport() {
        return yaml.getBoolean("Teleport.Enabled", false);
    }

    /**
     * Get the delay before sending a client
     * to the spawn location
     *
     * @return the spawn teleport delay
     */
    @Override
    public int teleportDelay() {
        return yaml.getInteger("Teleport.Delay", 10);
    }

    /**
     * Get if the policy is one of the
     * teleport-denying ones
     *
     * @param policy the policy
     * @return if the policy cancels teleport
     */
    @Override
    public boolean cancelWithPolicy(final CancelPolicy policy) {
        List<String> policies = yaml.getList("Teleport.Cancel");
        if (policies.isEmpty()) return false;

        for (String policyName : policies) {
            if (policyName.equalsIgnoreCase(policy.name())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get if the plugin takes back the
     * client after a successful login
     *
     * @return if the client gets
     * teleported back
     */
    @Override
    public boolean takeBack() {
        return yaml.getBoolean("TakeBack.Enabled", false);
    }

    /**
     * Get the minimum radius the client must
     * be away from spawn in order to store
     * his last location
     *
     * @return the last location store radius
     */
    @Override
    public int spawnRadius() {
        return yaml.getInteger("TakeBack.Distance", 30);
    }
}
