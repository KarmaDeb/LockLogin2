package es.karmadev.locklogin.spigot.process;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.task.FutureTask;
import es.karmadev.locklogin.api.user.auth.process.AuthType;
import es.karmadev.locklogin.api.user.auth.process.ProcessPriority;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;
import es.karmadev.locklogin.common.api.user.auth.CAuthProcess;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.entity.Player;

/**
 * Spigot TOTP process
 */
public class SpigotTotpProcess implements UserAuthProcess {

    private final static SpigotTotpProcess DUMMY = new SpigotTotpProcess(null);

    private final LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final NetworkClient client;
    private static boolean enabled;

    public SpigotTotpProcess(final NetworkClient client) {
        if (client == null && DUMMY != null) throw new IllegalStateException("Cannot create a TOTP process with a null client");
        this.client = client;
    }

    public static String getName() {
        return "totp";
    }

    public static int getPriority() {
        return ProcessPriority.RUN_FIRST + 3; //Run the third
    }

    public static void setStatus(final boolean status) {
        SpigotTotpProcess.enabled = status;
    }

    public static SpigotTotpProcess createFor(final NetworkClient client) {
        return new SpigotTotpProcess(client);
    }

    public static SpigotTotpProcess createDummy() {
        return SpigotTotpProcess.DUMMY;
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
        return AuthType.LOGIN;
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
    public void setEnabled(boolean status) {
        SpigotTotpProcess.enabled = status;
    }

    /**
     * Process the auth process
     *
     * @param previous the previous auth process
     * @return the auth process
     */
    @Override
    public FutureTask<AuthProcess> process(final AuthProcess previous) {
        if (client == null) throw new IllegalStateException("Cannot process auth process for a dummy instance");

        FutureTask<AuthProcess> task = new FutureTask<>();
        Player player = UserDataHandler.getPlayer(client);

        if (player == null) {
            task.complete(CAuthProcess.forResult(false, this));
            return task;
        }

        if (!client.account().hasTotp() || !client.account().totpSet() || client.session().fetch("totp_logged", false)) {
            if (!client.account().hasPin()) {
                client.session().append(CSessionField.newField(Boolean.class, "pin_logged", true));
            }

            task.complete(CAuthProcess.forResult(true, this));
            return task;
        }

        spigot.getTotpHandler().addHandler(client, (success, handler) -> {
            if (success) {
                if (!client.account().hasPin()) {
                    client.session().append(CSessionField.newField(Boolean.class, "pin_logged", true));
                }
                client.session().append(CSessionField.newField(Boolean.class, "totp_logged", true));

                task.complete(CAuthProcess.forResult(true, this));
                handler.destroy();
            }
        });

        return task;
    }
}
