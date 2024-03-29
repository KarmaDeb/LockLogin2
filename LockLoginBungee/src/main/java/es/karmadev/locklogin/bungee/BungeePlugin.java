package es.karmadev.locklogin.bungee;

import es.karmadev.api.bungee.core.KarmaPlugin;
import es.karmadev.api.core.source.exception.AlreadyRegisteredException;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.strings.StringOptions;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;
import es.karmadev.locklogin.bungee.command.TestCommand;
import es.karmadev.locklogin.bungee.listener.JoinHandler;
import es.karmadev.locklogin.common.api.packet.COutPacket;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class BungeePlugin extends KarmaPlugin implements Listener {

    LockLoginBungee bungee;

    public BungeePlugin() throws NoSuchFieldException, IllegalAccessException, AlreadyRegisteredException, NoSuchAlgorithmException {
        super(false);
        bungee = new LockLoginBungee(this);
    }

    @Override
    public void enable() {
        if (bungee.boot) {
            bungee.installDriver();

            getProxy().registerChannel("login:inject");

            getProxy().getPluginManager().registerListener(this, this);
            getProxy().getPluginManager().registerListener(this, new JoinHandler(bungee));
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
    public void onMessageReceive(final PluginMessageEvent e) throws InvalidPacketDataException { //I feel bad for having a single class just for this...
        Connection connection = e.getSender();
        String tag = e.getTag();

        if (connection instanceof Server) {
            Server server = (Server) connection;
            ServerInfo info = server.getInfo();
            String channel = info.getName();

            if (tag.equalsIgnoreCase("login:inject")) {
                OutgoingPacket packet = new COutPacket(DataType.HELLO);
                bungee.getProtocol().write(channel, String.format("%s_%s",
                        StringUtils.generateString(4, StringOptions.LOWERCASE),
                        StringUtils.generateString(6, StringOptions.LOWERCASE)), packet);

                return;
            }

            byte[] raw = e.getData();
            String rawData = new String(raw, StandardCharsets.UTF_8);

            Object object = StringUtils.load(rawData).orElse(null);

            if (object instanceof PacketFrame) {
                PacketFrame frame = (PacketFrame) object;
                bungee.getProtocol().receive(channel, tag, frame);
            }
        }
    }
}
