package es.karmadev.locklogin.bungee;

import es.karmadev.api.bungee.core.KarmaPlugin;
import es.karmadev.api.core.source.exception.AlreadyRegisteredException;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.locklogin.bungee.command.TestCommand;
import es.karmadev.locklogin.bungee.packet.PacketDataHandler;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BungeePlugin extends KarmaPlugin implements Listener {

    LockLoginBungee bungee;

    private final Map<String, BiConsumer<Server, byte[]>> channelListeners = new ConcurrentHashMap<>();

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

    /**
     * Add a channel listener
     *
     * @param channel the channel name to listen at
     * @param dataHandler the data handler
     */
    public void addChannelListener(final String channel, final BiConsumer<Server, byte[]> dataHandler) {
        this.channelListeners.put(channel, dataHandler);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMessageReceive(final PluginMessageEvent e) { //I feel bad for having a single class just for this...
        String tag = e.getTag();
        if (PacketDataHandler.tagExists(tag)) {
            Server server = (Server) e.getSender();

            BiConsumer<Server, byte[]> handler = channelListeners.get(e.getTag());
            if (handler == null) {
                logger().send(LogLevel.WARNING, "Unhandled plugin message at {0}", tag);
                return;
            }

            handler.accept(server, e.getData());
        }

        //CustomPacket.handle(e);
    }
}
