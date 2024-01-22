package es.karmadev.locklogin.spigot.util.window.settings;

import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.api.spigot.reflection.skull.SkullBuilder;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.function.Consumer;

public enum SettingsButton {
    TOGGLE_SESSION(
            "ewogICJ0aW1lc3RhbXAiIDogMTY5Njk1ODk5MjM5OCwKICAicHJvZmlsZUlkIiA6ICI1YzY1MGIwMWIyYWE0MTY2ODIyMDQyNzA3YzMyMmU5YSIsCiAgInByb2ZpbGVOYW1lIiA6ICJQdXJwTEFCIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2JlZDY2NDNjYWI4YmJmNTk1NDM0OGVkZDdjZGU2Yjk4MDYyMzFlZDFjMDcyMjdlNWRlNjI4OTVlMGZhY2YxNWEiCiAgICB9CiAgfQp9",
            "BaEWuZkn4r/ChfDs1ac1AS6czXolorwEKQqnz1mP7rPG0SxXL0GMh73rIjOAbGm++DPUIMi/kd6SY2CYmw6h7DAYYBETXR3rJ1P8XO6BMed1DE7ckOX26hV8lrbXjX2OsEgLhXnPJ4+GxprSKrJY6U3gFqc73mgmOZOiJKBrOcZ0OMxZtWWfSwCVNnUnYVDLR/4McDQdYo+YB3UfubGKht9ig9kbF6tpbXFsnunmmzSMAGjhLzqznMT6cZbup+yZrgBqS39QC3kIx4ZFC8VUNo4TndqlBzF62pbu2GwOgqwLeE0Gir35lRuyuspcg09i2VI92t0nHLVTbRH4gzyida85Vfq7vRDrTb5yB3qdvB5Wv4jSm+pzByJMGPGWgimrqcbmAjMoKy9ENxKCfGCHNFeadKwIrvcBzJr4r0Mh0zkFJNkk0Zff5AaL6X8p2GUVvgOlt3uB4BK7Qj6rMwZ5lQnUwYjl6vC3Ucx1MHJffqgsFg/+rxDWnjN8QwGqxOYqk/qFRT74gTuL8iTK9ZMWrlDOvBRfQo62CEjlL15yqrjCZLeVFEnX3VCZhYrX0NP/bGieWVj3zYm7xMlCCpZ1V9K3J5trebA5tQY/2IFphxZMoRT6P4JqulMT+XLk3jQ5QOYa7NSxXPLLRRKtxyj6lCj4Py3RqaKKFPx0tSTMaio="
    ),
    TOGGLE_PREMIUM(
            "ewogICJ0aW1lc3RhbXAiIDogMTY5NTkzMzAxNTk5MSwKICAicHJvZmlsZUlkIiA6ICI0NDAzZGM1NDc1YmM0YjE1YTU0OGNmZGE2YjBlYjdkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTa3lSZWFsbVNwaWVsZXIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTBjMzliM2Q4NmU2NzFlNGEwYjZhZGU2NDg3ZmFhYzBhY2NhOGNlMDNhMzY1M2QxYjc2MjcwYmE4MzRkZjQzOSIKICAgIH0KICB9Cn0=",
            "dZgG33PmPL4GDT0JYe7n6j7VPGZBW237TEgkkaGdhx8zptGNjO4fCC7eq7rltI9uYu+xS+/DpnFRgVdMESqdIm9ASuyFTxaWQQAlzR0zGO7czMfXsTrHySOv3gQF6edzE5URsL2kmYgKXHu0KlfP90uo3tjOPaFiybfWkW583t2GK8IfilnGcuQey72cLJw2+uWETeGdqRcftSNvmNNFlYNNnigt3R82cnRGRnHZms4dooJ8nVbzOhJ7GZHjvbHB2OGHe6zgNJ8J3w8VcbFbHfoBsj53tl8t3R68u/Gh9qEtOzcDhovipA+xm2UCS4vw8gOax3Zq+g6F+769xNiIqsVx5NsWQeC3R8a68KE5F8g6KsZMJybh1ij8z8Hz5J+Cd79mGwNhcayqur/wYN4jvYrjJM4FC4EuUEXMUlWQe13xtns75nKMRlG9X3He2LCx0brxBJ26wxS417c5E4P/LTXxJUhDuGfU5BPlXSkZCPxfACRmU7YLaAgKkSj5sbbjglhXM2iiGCtB4nRD4Z0wWZErqbsi1wYWTL5Agh51GmcPWJGCQK1hJIpBSjz/mwmF1XrM7lwkppsrPlH4eO9qgeU/1gYl8CtNd0nPtq+TGmAbYEVKe5TEf46/tzk8B+YLdZAU/gFWClwQuaCBhZAn4KjARu+hJgkCet4fWkUIeRA="
    ),
    LOGOUT(
            "ewogICJ0aW1lc3RhbXAiIDogMTcwMTI1NDUzMDYwOSwKICAicHJvZmlsZUlkIiA6ICJiYmQyOTNiYzJmM2U0ZDVlYmQ1YWU2ZTM3NWUxYTczNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUcmFuc3BhcmVudF8wXyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZjZiZjhjYjg2NzFiYjY2YTNiMmUzNThmNDcyNzg0MzdjNGNmYzEyNDdjMzM2ZTgxY2E5NGI5Mjc1MTlkNDY2IgogICAgfQogIH0KfQ==",
            "e68jnnJfR7vYqoygMA/3FvlTVffx2+xU6MZfbCrIYI3UkUc7i+CLNuJjXS6jTtZM1zHfZmXwz7J1wjp7M29htndv8b2HgC/YZGmNTCZj4CDrh2lbm1fWFrEEIWuk+46VUPjWaUi9up6YVPHqxkOQ0UisflGYDjI1Zz1jow05m8Hc2MoN2bSMXtCgV5Dgv1qLkDHOb/8MUTCbhDHalakUBBH6Yh+yp/aSy6lXc5PZbuPVy+lcBLULsHP36Em9xnI5QRKjwxlH0+NqtNR7+TdgII9gI224FzJUOH9aInwqsh8DxeQ5hZTmsc8DmJSqLyNp1x6c4jREA0yT0yp4HJFYjRQrEuvqrJfCL8Db8+JU4xoaH+w0LiJzHDuxtYAH9y1/HgzoWM/gduLmngKDPW2kF/MWalsDQ4CjCGgSs/2d0OTg8Mv47swdVbdUmF0yTwtkmOQdNJYw32QrsoWQjEmwvLdVgLJ0Mehb0ocYFT+CxTVyXUFoy2iBn+RNwRXdDhFWu5vXnStYKiBhKgFt+S8lbxsCkyXZdCEJEJ3R/fXiPsfeK87H+TqqXxiM2ofmlOp0tfkofA67Wdsykk9l8XOdj2y9fs9x6ARcz0+ZrogEagH+pPKlXiFIex7OkVeeXt/KWARSa6NxIfv0pzPK778YAuo0xv42KWYWk1S7kWp2C0I="
    );

    private final String value;
    private final String signature;

    /**
     * Create a settings button
     *
     * @param value the button head value
     * @param signature the button head signature
     */
    SettingsButton(final String value, final String signature) {
        this.value = value;
        this.signature = signature;
    }

    public ItemStack toItemStack() {
        ItemStack item = SkullBuilder.createSkull(value, signature);

        if (item == null) {
            item = new ItemStack(Material.STONE);

            ItemMeta meta = item.getItemMeta();
            assert meta != null;

            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }

        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        meta.addItemFlags(ItemFlag.values());
        LockLogin plugin = CurrentPlugin.getPlugin();
        Messages messages = plugin.messages();

        //TODO: Set item name
        String prettyName = this.name().substring(0, 1).toUpperCase() + this.name()
                .substring(1).toLowerCase().replace('_', ' ');
        meta.setDisplayName(Colorize.colorize("&e{0}", prettyName));
        item.setItemMeta(meta);

        return item;
    }
}
