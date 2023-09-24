package es.karmadev.locklogin.spigot.util.ui;

import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.api.spigot.core.scheduler.SpigotTask;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin pin inventory
 */
public class PinInventory {

    private final static LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final static Map<UUID, String[]> inputs = new ConcurrentHashMap<>();
    private final static Map<UUID, PinInventory> inventories = new ConcurrentHashMap<>();
    private final Inventory inventory;
    private NetworkClient player;

    public PinInventory(final NetworkClient player) {
        this.player = player;
        PinInventory stored = inventories.getOrDefault(player.uniqueId(), null);

        if (stored == null) {
            Messages messages = plugin.messages();
            String title = ColorComponent.parse(messages.pinTitle());

            if (title.length() > 32) title = title.substring(0, 32);

            inventory = Bukkit.getServer().createInventory(null, 45, title);
            inventories.put(player.uniqueId(), this);
        } else {
            if (!stored.player.equals(player)) {
                stored.player = player;
                inventories.put(player.uniqueId(), stored);
            }

            inventory = stored.inventory;
        }
    }

    /**
     * Open the inventory to client
     */
    public void open() {
        SpigotTask task = plugin.plugin().scheduler("sync")
                .schedule(() -> {
                    Player client = UserDataHandler.getPlayer(player);
                    if (client == null) return;

                    InventoryView view = client.getOpenInventory();
                    Inventory top = view.getTopInventory();

                    if (!top.equals(inventory)) {
                        makeInventory();
                        client.openInventory(inventory);
                    }
                });
        task.markSynchronous();
    }

    /**
     * Close the inventory to client
     */
    public void close() {
        inventories.remove(player.uniqueId());
        List<HumanEntity> views = new ArrayList<>(inventory.getViewers());
        views.forEach(HumanEntity::closeInventory);
    }

    /**
     * Confirm the pin
     */
    public boolean confirm() {
        String[] input = inputs.computeIfAbsent(player.uniqueId(), (data) -> new String[4]);
        if (invalidInput(input)) {
            Messages messages = plugin.messages();
            player.sendMessage(messages.prefix() + messages.pinLength());

            return false;
        }

        StringBuilder pinBuilder = new StringBuilder();
        for (String str : input) pinBuilder.append(str);

        UserAccount account = player.account();

        String pin = pinBuilder.toString();
        HashResult rs = account.pin();
        if (rs != null && rs.verify(pin)) {
            if (rs.hasher().isLegacy()) {
                plugin.info("Migrated pin from legacy client {0}", player.name());
                account.setPin(pin);
            }

            if (plugin.bungeeMode()) {

            }

            return true;
        }

        return false;
    }

    private boolean invalidInput(final String[] data) {
        int index = 0;
        for (String str : data) {
            if (str == null) return true;
            try {
                Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                data[index] = null;
                inputs.put(player.uniqueId(), data);
                return true;
            }

            index++;
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    void makeInventory() {
        for (PinButton button : PinButton.values()) {
            inventory.setItem(button.getSlot(), button.toItemStack());
        }

        Material emptySlot;
        try {
            emptySlot = Material.matchMaterial("STAINED_GLASS_PANE", true);
        } catch (Throwable ex) {
            emptySlot = Material.matchMaterial("STAINED_GLASS_PANE");
        }

        if (emptySlot == null) emptySlot = Material.LEGACY_STAINED_GLASS_PANE;
        ItemStack empty = new ItemStack(emptySlot);
        ItemMeta meta = empty.getItemMeta();
        assert meta != null;
        meta.setDisplayName("");
        meta.addItemFlags(ItemFlag.values());

        empty.setItemMeta(meta);

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir()) {
                inventory.setItem(i, empty);
            }
        }
    }
}
