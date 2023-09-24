package es.karmadev.locklogin.spigot.process;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.MovementConfiguration;
import es.karmadev.locklogin.api.user.auth.process.AuthType;
import es.karmadev.locklogin.api.user.auth.process.ProcessPriority;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;
import es.karmadev.locklogin.api.user.session.check.SessionChecker;
import es.karmadev.locklogin.common.api.user.auth.CAuthProcess;
import es.karmadev.locklogin.common.api.user.session.CSessionChecker;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.concurrent.CompletableFuture;

/**
 * Spigot login process
 */
public class SpigotAccountProcess implements UserAuthProcess {

    private final NetworkClient client;
    private static boolean enabled = true;


    public SpigotAccountProcess(final NetworkClient client) {
        this.client = client;
    }

    public static String getName() {
        return "account";
    }

    public static int getPriority() {
        return ProcessPriority.RUN_FIRST;
    }

    public static void setStatus(final boolean status) {
        SpigotAccountProcess.enabled = status;
    }

    public static SpigotAccountProcess createFor(final NetworkClient client) {
        return new SpigotAccountProcess(client);
    }

    /**
     * Get the process name
     *
     * @return the process name
     */
    @Override
    public String name() {
        return getName();
    }

    /**
     * Get the process priority
     *
     * @return the process priority
     */
    @Override
    public int priority() {
        return getPriority();
    }

    /**
     * Get the process auth type
     *
     * @return the process auth type
     */
    @Override
    public AuthType getAuthType() {
        return AuthType.ANY;
    }

    /**
     * Get if the process is enabled
     *
     * @return the process status
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the process status
     *
     * @param status the process status
     */
    @Override
    public void setEnabled(final boolean status) {
        SpigotAccountProcess.enabled = status;
    }

    /**
     * Process the auth process
     *
     * @param previous the previous auth process
     * @return the auth process
     */
    @Override
    public CompletableFuture<AuthProcess> process(final AuthProcess previous) {
        CompletableFuture<AuthProcess> task = new CompletableFuture<>();
        Player player = UserDataHandler.getPlayer(client);

        if (player == null) {
            task.complete(CAuthProcess.forResult(false, this));
            return task;
        }

        LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        MovementConfiguration movement = configuration.movement();

        if (!movement.allow() && movement.method().equals(MovementConfiguration.MovementMethod.SPEED)) {
            float walkSpeed = player.getWalkSpeed();
            float flySpeed = player.getFlySpeed();
            if (walkSpeed <= 0) walkSpeed = 0.2f;
            if (flySpeed <= 0) flySpeed = 0.1f;

            player.setMetadata("walkSpeed", new FixedMetadataValue(plugin.plugin(), walkSpeed));
            player.setMetadata("flySpeed", new FixedMetadataValue(plugin.plugin(), flySpeed));

            player.setWalkSpeed(0f);
            player.setFlySpeed(0f);
        }

        SessionChecker checker = client.getSessionChecker();
        checker.onEnd((status) -> task.complete(CAuthProcess.forResult(status, this)));

        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin.plugin(), checker);
        return task;
    }
}
