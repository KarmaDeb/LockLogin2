package es.karmadev.locklogin.bungee;

import es.karmadev.api.bungee.core.KarmaPlugin;
import es.karmadev.api.core.source.exception.AlreadyRegisteredException;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.locklogin.bungee.command.TestCommand;
import es.karmadev.locklogin.bungee.listener.JoinHandler;
import es.karmadev.locklogin.bungee.packet.CustomPacket;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.concurrent.TimeUnit;

public class BungeePlugin extends KarmaPlugin implements Listener {

    LockLoginBungee bungee;

    public BungeePlugin() throws NoSuchFieldException, IllegalAccessException, AlreadyRegisteredException {
        super(false);
        bungee = new LockLoginBungee(this);
    }

    @Override
    public void enable() {
        if (bungee.boot) {
            getProxy().getPluginManager().registerListener(this, this);
            getProxy().getPluginManager().registerCommand(this, new TestCommand());

            long end = System.currentTimeMillis();
            long diff = end - bungee.getStartup().toEpochMilli();
            logger().send(LogLevel.INFO, "LockLogin initialized in {0}ms ({1} seconds)", diff, TimeUnit.MILLISECONDS.toSeconds(diff));
        } else {
            logger().send(LogLevel.WARNING, "LockLogin won't initialize due an internal error. Please report this to discord {0}", "https://discord.gg/77p8KZNfqE");
        }
    }

    @Override
    public void disable() {

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMessageReceive(final PluginMessageEvent e) { //I feel bad for having a single class just for this...
        CustomPacket.handle(e);
    }
}
