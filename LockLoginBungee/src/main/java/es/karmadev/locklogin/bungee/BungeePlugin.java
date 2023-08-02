package es.karmadev.locklogin.bungee;

import es.karmadev.locklogin.bungee.command.TestCommand;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeePlugin extends Plugin {

    @Override
    public void onEnable() {
        //getProxy().registerChannel("test:test");
        getProxy().getPluginManager().registerCommand(this, new TestCommand());
    }
}
