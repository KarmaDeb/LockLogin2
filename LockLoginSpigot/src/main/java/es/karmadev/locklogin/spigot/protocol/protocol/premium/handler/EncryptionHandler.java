/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package es.karmadev.locklogin.spigot.protocol.protocol.premium.handler;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedProfilePublicKey;
import com.github.games647.craftapi.model.auth.Verification;
import com.github.games647.craftapi.model.skin.SkinProperty;
import com.github.games647.craftapi.resolver.MojangResolver;
import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.minecraft.uuid.UUIDFetcher;
import es.karmadev.api.minecraft.uuid.UUIDType;
import es.karmadev.api.spigot.server.SpigotServer;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.spigot.protocol.protocol.premium.LoginSession;
import es.karmadev.locklogin.spigot.protocol.protocol.premium.mojang.MojangEncryption;
import es.karmadev.locklogin.spigot.protocol.protocol.premium.mojang.client.ClientKey;
import org.bukkit.entity.Player;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Optional;
import java.util.UUID;

import static com.comphenix.protocol.PacketType.Login.Client.START;
import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;

/**
 * Part of the code of this class is from:
 * <a href="https://github.com/games647/FastLogin/blob/main/bukkit/src/main/java/com/github/games647/fastlogin/bukkit/listener/protocollib/VerifyResponseTask.java">FastLogin</a>
 */
public final class EncryptionHandler implements Runnable {

    private final static Messages messages = CurrentPlugin.getPlugin().messages();

    private final static Class<?> ENCRYPTION = MinecraftReflection.getMinecraftClass("util.MinecraftEncryption", "MinecraftEncryption");
    private final static MojangResolver resolver = new MojangResolver();

    private final PacketEvent packet;
    private final KeyPair pair;
    private final Player player;
    private final LoginSession session;
    private final byte[] secret;
    private static Method encrypt;
    private static Method cipher;

    public EncryptionHandler(final PacketEvent event, final Player player , final LoginSession session, final byte[] secret, final KeyPair pair) {
        packet = event;
        this.player = player;
        this.session = session;
        this.secret = secret;
        this.pair = pair;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            PrivateKey privateKey = pair.getPrivate();
            SecretKey login = MojangEncryption.getShared(privateKey, secret);
            if (login == null) {
                kick(messages.premiumFailSession());
                return;
            }

            if (encrypt == null) {
                Class<?> network_manager = MinecraftReflection.getNetworkManagerClass();
                try {
                    encrypt = FuzzyReflection.fromClass(network_manager)
                            .getMethodByParameters("a", SecretKey.class);
                } catch (IllegalArgumentException ex) {
                    encrypt = FuzzyReflection.fromClass(network_manager)
                            .getMethodByParameters("a", Cipher.class, Cipher.class);

                    // Get the needed Cipher helper method (used to generate ciphers from login key)
                    cipher = FuzzyReflection.fromClass(ENCRYPTION)
                            .getMethodByParameters("a", int.class, Key.class);
                }
            }

            Object networkManager = manager();
            try {
                if (cipher == null) {
                    encrypt.invoke(networkManager, login);
                } else {
                    Object decrypt_cipher = cipher.invoke(null, Cipher.DECRYPT_MODE, login);
                    Object encrypt_cipher = cipher.invoke(null, Cipher.ENCRYPT_MODE, login);

                    encrypt.invoke(networkManager, decrypt_cipher, encrypt_cipher);
                }
            } catch (Exception ex) {
                CurrentPlugin.getPlugin().log(ex, "Failed to encrypt premium connection");
                kick(messages.premiumFailInternal());
                return;
            }

            String server = MojangEncryption.getServerId("", login, pair.getPublic());

            String name = session.getUsername();
            InetSocketAddress ip = player.getAddress();
            if (ip != null) {
                try {
                    InetAddress address = ip.getAddress();
                    Optional<Verification> response = resolver.hasJoined(name, server, address);
                    if (response.isPresent()) {
                        Verification verification = response.get();

                        SkinProperty[] properties = verification.getProperties();
                        if (properties.length > 0) {
                            session.setSkin(properties[0]);
                        }

                        session.setVerified(true);

                        String username = session.getUsername();
                        PremiumDataStore premium = CurrentPlugin.getPlugin().premiumStore();
                        UUID offline_id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getUsername()).getBytes());
                        UUID online_uid = premium.onlineId(username);
                        if (online_uid == null) {
                            online_uid = UUIDFetcher.fetchUUID(username, UUIDType.ONLINE);
                            if (online_uid != null) {
                                premium.saveId(username, online_uid);
                            }
                        }

                        LocalNetworkClient offline = CurrentPlugin.getPlugin().network().getOfflinePlayer(offline_id);

                        Configuration config = CurrentPlugin.getPlugin().configuration();
                        if ((!config.premium().forceOfflineId() || CurrentPlugin.getPlugin().onlineMode()) && online_uid != null && offline.connection().equals(ConnectionType.ONLINE)) {
                            session.setId(online_uid);
                        } else {
                            session.setId(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()));
                        }

                        if (networkManager != null) {
                            if (!config.premium().forceOfflineId() || CurrentPlugin.getPlugin().onlineMode()) {
                                Class<?> managerClazz = networkManager.getClass();
                                FieldAccessor accessor = Accessors.getFieldAccessorOrNull(managerClazz, "spoofedUUID", UUID.class);
                                accessor.set(networkManager, verification.getId());
                            } else {
                                Class<?> managerClazz = networkManager.getClass();
                                FieldAccessor accessor = Accessors.getFieldAccessorOrNull(managerClazz, "spoofedUUID", UUID.class);
                                accessor.set(networkManager, UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()));
                            }
                        }

                        PacketContainer start;
                        if (SpigotServer.isOver(SpigotServer.v1_19_X)) {
                            start = new PacketContainer(START);
                            start.getStrings().write(0, name);

                            ClientKey cl_key = session.getKey();
                            EquivalentConverter<WrappedProfilePublicKey.WrappedProfileKeyData> converter = BukkitConverters.getWrappedPublicKeyDataConverter();
                            Optional<WrappedProfilePublicKey.WrappedProfileKeyData> wrapped = Optional.ofNullable(cl_key).map(key ->
                                    new WrappedProfilePublicKey.WrappedProfileKeyData(cl_key.expiration(), cl_key.key(), cl_key.sign())
                            );

                            start.getOptionals(converter).write(0, wrapped);
                        } else {
                            WrappedGameProfile fake = new WrappedGameProfile(UUID.randomUUID(), name);

                            Class<?> profileHandler = fake.getHandleType();
                            Class<?> packetHandler = PacketRegistry.getPacketClassFromType(START);
                            ConstructorAccessor startConstructor = Accessors.getConstructorAccessorOrNull(packetHandler, profileHandler);
                            start = new PacketContainer(START, startConstructor.invoke(fake));
                        }

                        ProtocolLibrary.getProtocolManager().receiveClientPacket(player, start, false);
                    } else {
                        kick(ConsoleColor.parse(messages.premiumFailAuth()));
                    }
                } catch (IOException e) {
                    kick(ConsoleColor.parse(messages.premiumFailConnection()));
                }
            } else {
                kick(ConsoleColor.parse(messages.premiumFailAddress()));
            }
        } finally {
            synchronized (packet.getAsyncMarker().getProcessingLock()) {
                packet.setCancelled(true);
            }

            ProtocolLibrary.getProtocolManager().getAsynchronousManager().signalPacketTransmission(packet);
        }
    }

    private void kick(final String reason) {
        PacketContainer kick_packet = new PacketContainer(DISCONNECT);
        kick_packet.getChatComponents().write(0, WrappedChatComponent.fromText(ConsoleColor.parse(reason)));

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, kick_packet);
        player.kickPlayer(ConsoleColor.parse(reason));
    }

    private Object manager() {
        try {
            Object injectorContainer= TemporaryPlayerFactory.getInjectorFromPlayer(player);

            Class<?> injectorClazz = Class.forName("com.comphenix.protocol.injector.netty.Injector");
            Object rawInjector = FuzzyReflection.getFieldValue(injectorContainer, injectorClazz, true);

            Class<?> rawInjectorClazz = rawInjector.getClass();
            FieldAccessor accessor = Accessors.getFieldAccessorOrNull(rawInjectorClazz, "networkManager", Object.class);
            return accessor.get(rawInjector);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}
