package es.karmadev.locklogin.spigot.process;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.MovementConfiguration;
import es.karmadev.locklogin.api.task.FutureTask;
import es.karmadev.locklogin.api.user.auth.process.AuthType;
import es.karmadev.locklogin.api.user.auth.process.ProcessPriority;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;
import es.karmadev.locklogin.api.user.session.check.SessionChecker;
import es.karmadev.locklogin.common.api.user.auth.CAuthProcess;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Spigot register process
 */
@SuppressWarnings("unused")
public class SpigotRegisterProcess implements UserAuthProcess {

    private static final SpigotRegisterProcess DUMMY = new SpigotRegisterProcess(null);

    private final NetworkClient client;
    private static boolean enabled = true;

    public SpigotRegisterProcess(final NetworkClient client) {
        if (client == null && DUMMY != null)
            throw new IllegalStateException("Cannot create a register process with a null client");

        this.client = client;
    }

    public static String getName() {
        return "register";
    }

    public static int getPriority() {
        return ProcessPriority.RUN_FIRST;
    }

    public static void setStatus(final boolean status) {
        enabled = status;
    }

    public static SpigotRegisterProcess createFor(final NetworkClient client) {
        return new SpigotRegisterProcess(client);
    }

    public static SpigotRegisterProcess createDummy() {
        return DUMMY;
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
        return AuthType.REGISTER;
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
        enabled = status;
    }

    /**
     * Process the auth process
     *
     * @param previous the previous auth process
     * @return the auth process
     */
    @Override
    public FutureTask<AuthProcess> process(final AuthProcess previous) {
        if (this.client == null)
            throw new IllegalStateException("Cannot process auth process for a dummy instance");

        FutureTask<AuthProcess> task = new FutureTask<>();

        Player player = UserDataHandler.getPlayer(this.client);
        if (player == null) {
            task.complete(CAuthProcess.forResult(false, this));
            return task;
        }

        if (client.session().fetch("pass_logged", false)) {
            task.complete(CAuthProcess.forResult(true, this));
            return task;
        }

        LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        MovementConfiguration movement = configuration.movement();

        if (!movement.allow() && movement.method().equals(MovementConfiguration.MovementMethod.SPEED)) {
            float walkSpeed = player.getWalkSpeed();
            float flySpeed = player.getFlySpeed();

            if (walkSpeed <= 0.0F)
                walkSpeed = 0.2F;
            if (flySpeed <= 0.0F)
                flySpeed = 0.1F;

            player.setMetadata("walkSpeed", new FixedMetadataValue(plugin.plugin(), walkSpeed));
            player.setMetadata("flySpeed", new FixedMetadataValue(plugin.plugin(), flySpeed));

            player.setWalkSpeed(0.0F);
            player.setFlySpeed(0.0F);
        }

        SessionChecker checker = this.client.getSessionChecker();
        checker.onEnd(status -> {
            if (client.account().isRegistered()) {
                client.session().append(CSessionField.newField(Boolean.class, "pass_logged", true));
            }

            task.complete(CAuthProcess.forResult(client.account().isRegistered(), this));
        });

        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin.plugin(), checker);
        return task;
    }
}
