package es.karmadev.locklogin.spigot.process;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.auth.process.AuthType;
import es.karmadev.locklogin.api.user.auth.process.ProcessPriority;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;
import es.karmadev.locklogin.common.api.user.auth.CAuthProcess;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Spigot login process
 */
public class SpigotPinProcess implements UserAuthProcess {

    private final NetworkClient client;
    private static boolean enabled;

    public SpigotPinProcess(final NetworkClient client) {
        this.client = client;
    }

    public static String getName() {
        return "pin";
    }

    public static int getPriority() {
        return ProcessPriority.RUN_FIRST + 1; //Run the second
    }

    public static void setStatus(final boolean status) {
        SpigotPinProcess.enabled = status;
    }

    public static SpigotPinProcess createFor(final NetworkClient client) {
        return new SpigotPinProcess(client);
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
        SpigotPinProcess.enabled = status;
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

        InventoryHolder holder = new InventoryHolder() {
            @NotNull
            @Override
            public Inventory getInventory() {
                return null;
            }
        };

        Inventory inventory = Bukkit.createInventory(holder, 9);
        player.openInventory(inventory);

        LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
        Bukkit.getServer().getPluginManager().registerEvent(InventoryCloseEvent.class, new Listener() {}, EventPriority.HIGHEST, ((listener, event) -> {
            assert event instanceof InventoryCloseEvent;
            InventoryCloseEvent e = (InventoryCloseEvent) event;
            Inventory eInventory = e.getInventory();
            InventoryHolder eHolder = eInventory.getHolder();
            if (eHolder != null) {
                if (eHolder.equals(holder)){
                    task.complete(CAuthProcess.forResult(true, this));
                } else{
                    task.complete(CAuthProcess.forResult(false, this));
                }
            }
        }), plugin.plugin());

        return task;
    }
}
