package es.karmadev.locklogin.spigot.util.window;

import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.spigot.core.scheduler.SpigotTask;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import lombok.Getter;
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

    @Getter
    private final Inventory inventory;

    private final ItemStack inputDisplay;

    private NetworkClient player;
    private Runnable onSuccess;

    public PinInventory(final NetworkClient player) {
        this.player = player;
        PinInventory stored = inventories.getOrDefault(player.uniqueId(), null);

        if (stored == null) {
            Messages messages = plugin.messages();
            String title = ColorComponent.parse(messages.pinTitle());

            if (title.length() > 32) title = title.substring(0, 32);

            inventory = Bukkit.getServer().createInventory(null, 45, title);
            inputDisplay = new ItemStack(Material.PAPER);
            ItemMeta meta = inputDisplay.getItemMeta();
            assert meta != null;

            meta.setDisplayName(ColorComponent.parse("&7____"));
            meta.addItemFlags(ItemFlag.values());

            inputDisplay.setItemMeta(meta);
            inventories.put(player.uniqueId(), this);
        } else {
            if (!stored.player.equals(player)) {
                stored.player = player;
                inventories.put(player.uniqueId(), stored);
            }

            inventory = stored.inventory;
            inputDisplay = stored.inputDisplay;
        }
    }

    /**
     * Open the inventory to client
     * @param onSuccess the action to execute when the
     *                  client types in the pin correctly
     */
    public void open(final Runnable onSuccess) {
        this.onSuccess = onSuccess;
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
        inputs.remove(player.uniqueId());
        List<HumanEntity> views = new ArrayList<>(inventory.getViewers());
        views.forEach(HumanEntity::closeInventory);
        onSuccess = null;
    }

    /**
     * Trigger a button click
     *
     * @param button the button that has been clicked
     */
    public void click(final PinButton button) {
        if (button.equals(PinButton.CONFIRM)) {
            confirm();
            return;
        }

        if (button.equals(PinButton.DELETE)) {
            String[] inputs = PinInventory.inputs.computeIfAbsent(player.uniqueId(), (d) -> new String[4]);
            int index = -1;
            for (int i = 0; i < inputs.length; i++) {
                String val = inputs[i];
                if (!ObjectUtils.isNullOrEmpty(val)) {
                    index = i;
                }
            }

            if (index >= 0) {
                inputs[index] = null;
                PinInventory.inputs.put(player.uniqueId(), inputs);
                rebuildInput();
                return;
            }

            return;
        }

        String[] inputs = PinInventory.inputs.computeIfAbsent(player.uniqueId(), (d) -> new String[4]);
        int index = -1;
        for (int i = 0; i < inputs.length; i++) {
            String val = inputs[i];
            if (ObjectUtils.isNullOrEmpty(val)) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            inputs[index] = String.valueOf(button.toNumber());
            rebuildInput();
        }
    }

    private void rebuildInput() {
        String[] inputs = PinInventory.inputs.computeIfAbsent(player.uniqueId(), (d) -> new String[4]);
        StringBuilder inputBuilder = new StringBuilder();
        for (String value : inputs) {
            if (!ObjectUtils.isNullOrEmpty(value)) {
                inputBuilder.append(value);
            } else {
                inputBuilder.append("_");
            }
        }

        ItemMeta displayMeta = inputDisplay.getItemMeta();
        assert displayMeta != null;

        displayMeta.setDisplayName(ColorComponent.parse("&7{0}", inputBuilder));
        inputDisplay.setItemMeta(displayMeta);
        inventory.setItem(25, inputDisplay);
    }

    /**
     * Confirm the pin
     */
    private void confirm() {
        String[] input = inputs.computeIfAbsent(player.uniqueId(), (data) -> new String[4]);
        if (invalidInput(input)) {
            Messages messages = plugin.messages();
            player.sendMessage(messages.prefix() + messages.pinLength());
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
                //TODO: Send pin auth status
            }

            player.session().append(CSessionField.newField(Boolean.class, "pin_logged", true));
            if (onSuccess != null)
                onSuccess.run();

            close();
        }
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
        inventory.setItem(25, inputDisplay);

        Material emptySlot = Material.DIRT;
        try {
            emptySlot = Material.matchMaterial("STAINED_GLASS_PANE", true);
        } catch (Throwable ex) {
            try {
                emptySlot = Material.matchMaterial("STAINED_GLASS_PANE");
            } catch (IllegalArgumentException ignored) {}
        }
        try {
            if (emptySlot == null) emptySlot = Material.LEGACY_STAINED_GLASS_PANE;
            /*
            Even though we don't really provide official support
            for legacy versions, let the plugin kinda work on them
             */
        } catch (Throwable ignored) {}

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

    /**
     * Get the pin inventory of the client
     *
     * @param client the client
     * @return the client pin inventory
     */
    public static PinInventory getInstance(final NetworkClient client) {
        return inventories.getOrDefault(client.uniqueId(), null);
    }
}
