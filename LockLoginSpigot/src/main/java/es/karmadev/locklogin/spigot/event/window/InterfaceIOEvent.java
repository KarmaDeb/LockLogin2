package es.karmadev.locklogin.spigot.event.window;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.window.PinButton;
import es.karmadev.locklogin.spigot.util.window.PinInventory;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

public class InterfaceIOEvent implements Listener {

    private final LockLoginSpigot spigot;

    public InterfaceIOEvent(final LockLoginSpigot spigot) {
        this.spigot = spigot;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(final InventoryOpenEvent e) {
        HumanEntity entity = e.getPlayer();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            int networkId = UserDataHandler.getNetworkId(player);

            if (networkId > 0) {
                NetworkClient client = spigot.network().getPlayer(networkId);
                if (client == null) {
                    /*
                    The client has been defined, but not his connection, prevent
                    him from opening anything
                     */
                    e.setCancelled(true);
                    return;
                }

                PinInventory pin = PinInventory.getInstance(client);

                if (pin != null) {
                    Inventory toOpen = e.getInventory();
                    Inventory pinInventory = pin.getInventory();

                    if (pinInventory == null || toOpen.equals(pin.getInventory())) return; //Allow

                    e.setCancelled(true);
                    Bukkit.getServer().getScheduler()
                            .runTaskLater(spigot.plugin(), () -> player.openInventory(pinInventory), 5);
                }
            } else {
                if (UserDataHandler.isReady(player)) e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(final InventoryCloseEvent e) {
        HumanEntity entity = e.getPlayer();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            int networkId = UserDataHandler.getNetworkId(player);

            if (networkId > 0) {
                NetworkClient client = spigot.network().getPlayer(networkId);
                if (client == null) return;

                PinInventory pin = PinInventory.getInstance(client);

                if (pin != null) {
                    Inventory pinInventory = pin.getInventory();
                    if (pinInventory == null) return; //Allow

                    Bukkit.getServer().getScheduler()
                            .runTaskLater(spigot.plugin(), () -> player.openInventory(pinInventory), 5);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        HumanEntity entity = e.getWhoClicked();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            int networkId = UserDataHandler.getNetworkId(player);

            if (networkId > 0) {
                NetworkClient client = spigot.network().getPlayer(networkId);
                if (client == null) return;

                PinInventory pin = PinInventory.getInstance(client);
                if (pin == null) return;

                Inventory pinInventory = pin.getInventory();
                if (pinInventory == null) return;

                Inventory clicked = e.getClickedInventory();
                if (clicked == null) return;

                if (!clicked.equals(pinInventory)) {
                    e.setCancelled(true);
                    return;
                }
                /*
                We have a valid pin inventory, and we didn't click a pin inventory,
                then we cancel the event
                 */

                int slot = e.getSlot();
                PinButton button = PinButton.fromSlot(slot);

                if (button == null) {
                    e.setCancelled(true);
                    return;
                }

                switch (button) {
                    case DELETE:
                        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.BLOCKS, 1f, 2f);
                        break;
                    case CONFIRM:
                        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1f, 0.1f);
                        break;
                    default:
                        int number = button.toNumber();
                        double min = 0.1;
                        double max = 2.0;
                        float pitch = (float) (min + (max - min) * (number / 9.0));

                        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS, 1f, pitch);
                }

                pin.click(button);
                e.setCancelled(true);
            }
        }
    }
}
