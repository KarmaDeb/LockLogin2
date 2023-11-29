package es.karmadev.locklogin.spigot.util.window.pin;

import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.api.spigot.reflection.skull.SkullBuilder;
import es.karmadev.locklogin.api.plugin.CacheContainer;
import es.karmadev.locklogin.common.api.plugin.CacheElement;
import es.karmadev.locklogin.spigot.util.window.ConstantUtils;
import lombok.Getter;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * Pin number
 */
public enum PinButton {
    /**
     * Zero
     */
    ZERO(0, 40),
    /**
     * One
     */
    ONE(1, 30),
    /**
     * Two
     */
    TWO(2, 31),
    /**
     * Three
     */
    THREE(3, 32),
    /**
     * Four
     */
    FOUR(4, 21),
    /**
     * Five
     */
    FIVE(5, 22),
    /**
     * Six
     */
    SIX(6, 23),
    /**
     * Seven
     */
    SEVEN(7, 12),
    /**
     * Eight
     */
    EIGHT(8, 13),
    /**
     * Nine
     */
    NINE(9, 14),
    /**
     * Delete
     */
    DELETE(Integer.MIN_VALUE, 36),
    /**
     * Confirm
     */
    CONFIRM(Integer.MAX_VALUE, 44);

    private final int number;
    @Getter
    private final int slot;
    @Getter
    private final String value;
    @Getter
    private final String signature;
    private final CacheContainer<ItemStack> itemCache = new CacheElement<>();

    /**
     * Initialize the pin button
     *
     * @param number the pin button
     *               number
     * @param slot the button slot in
     *             inventory
     */
    PinButton(final int number, final int slot) {
        this.number = number;
        this.slot = slot;
        this.value = ConstantUtils.getValue(number);
        this.signature = ConstantUtils.getSignature(number);
    }

    /**
     * Get the button from the slot
     * number
     *
     * @param slot the slot
     * @return the number for the slot
     */
    public static PinButton fromSlot(final int slot) {
        for (PinButton button : PinButton.values()) {
            if (button.slot == slot) return button;
        }

        return null;
    }

    /**
     * Get the button for the number
     *
     * @param number the number
     * @return the number for the raw number
     */
    public static PinButton forNumber(final int number) {
        for (PinButton button : PinButton.values()) {
            if (button.number == number) return button;
        }

        return null;
    }

    /**
     * Get the button number
     * value
     *
     * @return the number
     */
    public int toNumber() {
        return number;
    }

    /**
     * Build the button item stack
     *
     * @return the item stack
     */
    public ItemStack toItemStack() {
        return itemCache.getOrElse(() -> SkullBuilder.createSkull(value, signature, (meta) -> {
            if (meta == null) return null;
            if (number == Integer.MIN_VALUE) {
                meta.setDisplayName(Colorize.colorize("&cRemove"));
            } else {
                if (number == Integer.MAX_VALUE) {
                    meta.setDisplayName(Colorize.colorize("&aConfirm"));
                } else {
                    meta.setDisplayName(Colorize.colorize("&e" + number));
                }
            }

            meta.addItemFlags(ItemFlag.values());
            return meta;
        }));
    }
}
