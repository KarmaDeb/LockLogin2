package es.karmadev.locklogin.protocol;

import es.karmadev.api.array.ArrayUtils;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.kson.io.JsonReader;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.strings.StringOptions;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.FrameBuilder;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;
import es.karmadev.locklogin.api.protocol.LockLoginProtocol;
import es.karmadev.locklogin.common.api.packet.CInPacket;
import es.karmadev.locklogin.common.api.packet.COutPacket;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin protocol handler. The LockLogin protocol
 * ensures a secure and encrypted communication between
 * plugin instances when using BungeeCord messaging channels
 */
public abstract class ProtocolHandler implements LockLoginProtocol {

    protected final static int BLOCK_SIZE = 1024;

    private final static DataType[] PRE_TYPES = {
            DataType.HELLO,
            DataType.CHANNEL_INIT,
            DataType.CHANNEL_CLOSE,
            DataType.CONNECTION_INIT
    };
    private final static LockLogin plugin = CurrentPlugin.getPlugin();

    private final KeyPair pair;
    private final String pairAlgorithm;
    private final SecretKey secret;
    private final String secretAlgorithm;

    private final ConcurrentHashMap<String, PacketData> packets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PeerKeyData> sharedKeys = new ConcurrentHashMap<>();

    private final Map<String, List<QueuedPacket>> packetQue = new ConcurrentHashMap<>();

    /**
     * Initialize a new protocol handler
     *
     * @param pair the protocol handler
     *             key pair. The key pair is used
     *             when communicating, to encrypt
     *             and decrypt the data
     * @param pairAlgorithm the key pair algorithm
     * @param secret the protocol handler secret key
     * @param secretAlgorithm the secret key algorithm
     * @throws NoSuchAlgorithmException if the secret key generation fails. The
     * secret key is strictly required to encrypt data sent by
     * the protocol
     */
    public ProtocolHandler(final KeyPair pair, final String pairAlgorithm, final SecretKey secret, final String secretAlgorithm) throws NoSuchAlgorithmException {
        this.pair = pair;
        this.pairAlgorithm = pairAlgorithm;
        this.secret = secret;
        this.secretAlgorithm = secretAlgorithm;
    }

    /**
     * Receive data to the protocol handler
     *
     * @param frame the received frame
     * @throws InvalidPacketDataException if the packet full data
     * results on an invalid packet. This might be caused by a MitM
     * attack, or a malformed packet (possibly caused by an outdated
     * plugin)
     */
    @Override
    public final void receive(final String channel, final String tag, final PacketFrame frame) throws InvalidPacketDataException {
        PacketData pData = this.packets.computeIfAbsent(channel, (d) -> new PacketData());
        FrameBuilder builder = pData.getFrameBuilder(tag);
        if (builder == null) return;

        int max = frame.frames();
        int position = frame.position();

        builder.append(frame);
        if (position != max) {
            return;
        }

        byte[] completePacket = builder.build();
        readPacket(completePacket, channel);
    }

    /**
     * Write data into the protocol
     *
     * @param data the data to write
     */
    @Override
    public final void write(final String channel, final String tag, final OutgoingPacket data) {
        DataType type = data.getType();
        if (!ArrayUtils.containsAny(PRE_TYPES, type) && !this.sharedKeys.containsKey(channel)) {
            List<QueuedPacket> que = this.packetQue.computeIfAbsent(channel, (q) -> new ArrayList<>());
            que.add(new QueuedPacket(tag, data));
            return;
        }

        data.addProperty("channel", channel);
        PacketData pData = this.packets.computeIfAbsent(channel, (d) -> new PacketData());
        pData.assign(tag, data);

        PeerKeyData shared = this.sharedKeys.get(channel);
        byte[] dataToWrite = data.build().toString(false).getBytes();

        if (shared != null) {
            SecretKey secret = shared.getKey();
            dataToWrite = doCipherWithKey(dataToWrite, Cipher.ENCRYPT_MODE, secret, shared.getAlgorithm(), true);

            if (dataToWrite == null) return;
        }

        emit(channel, data.id(), tag, dataToWrite);
    }

    /**
     * Get the protocol encoded secret
     * key
     *
     * @return the protocol encoded secret
     */
    @Override
    public final byte[] getEncodedSecret() {
        return getEncodedSecret(this.pair.getPublic(), this.pairAlgorithm);
    }

    /**
     * Handle a packet
     *
     * @param channel the channel
     * @param packet the packet
     */
    protected abstract void handle(final String channel, final IncomingPacket packet);

    /**
     * Write a packet. The packet parsed in this method should be
     * already encoded.
     * Implementation must split the packet into frames, otherwise
     * the other part won't take the packet. The reason for why we
     * don't parse an already built in packet frame array is because
     * we want to let implementations choose how and how big the frames
     * are.
     *
     * @param channel the channel
     * @param packetId the packet id that is being write
     * @param tag the tag
     * @param data the packet to write
     */
    protected abstract void emit(final String channel, final long packetId, final String tag, final byte[] data);

    /**
     * Forget all the data from a channel. This does the
     * same effect as if the other part sent us a {@link DataType#CHANNEL_CLOSE}
     *
     * @param channel the channel to forget
     */
    @Override
    public void forget(final String channel) {
        this.sharedKeys.remove(channel);
    }

    /**
     * Get the encoded secret using the
     * specified public key and algorithm
     *
     * @return the encoded secret
     */
    protected final byte[] getEncodedSecret(final PublicKey encoder, final String algorithm) {
        return doCipherWithKey(secret.getEncoded(), Cipher.ENCRYPT_MODE, encoder, algorithm, true);
    }

    /**
     * Perform a cipher using the provided
     * key and algorithm
     *
     * @param data the data to process
     * @param mode the cipher mode
     * @param key the key to use to process
     * @param algo the key algorithm
     * @param log if log should be made if something fails
     * @return the processed data
     */
    private byte[] doCipherWithKey(final byte[] data, final int mode, final Key key, final String algo, final boolean log) {
        try {
            Cipher cipher = Cipher.getInstance(algo);
            cipher.init(mode, key);

            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException ex) {
            if (log) plugin.log(ex, "Failed to encode for communications");
        }

        return null;
    }

    /**
     * Read a packet
     *
     * @param completePacket the complete packet bytes
     * @param channel the channel in where data was sent
     * @throws InvalidPacketDataException if the packet full data
     * results on an invalid packet. This might be caused by a MitM
     * attack, or a malformed packet (possibly caused by an outdated
     * plugin)
     */
    private void readPacket(final byte[] completePacket, final String channel) throws InvalidPacketDataException {
        byte[] resolved = doCipherWithKey(completePacket, Cipher.DECRYPT_MODE, this.secret, this.secretAlgorithm, true);
        boolean notSecure = false;

        if (resolved == null) {
            resolved = doCipherWithKey(completePacket, Cipher.DECRYPT_MODE, this.pair.getPrivate(), this.pairAlgorithm, false);
            if (resolved == null) {
                resolved = completePacket;
                notSecure = true;
            }
        }

        IncomingPacket incoming = validatePacket(resolved, notSecure, channel);
        DataType type = incoming.getType();

        String responseTag = String.format("%s_%s",
                StringUtils.generateString(4, StringOptions.LOWERCASE),
                StringUtils.generateString(6, StringOptions.LOWERCASE));

        switch (type) {
            case HELLO: {
                OutgoingPacket response = new COutPacket(DataType.CHANNEL_INIT);
                response.addProperty("key", Base64.getEncoder()
                        .encodeToString(pair.getPublic().getEncoded()));
                response.addProperty("algorithm", this.pairAlgorithm);

                write(channel, responseTag, response);
            }
                return;
            case CHANNEL_INIT: {
                String rawPeerPKey = incoming.getSequence("key");
                String rawPeerPKeyAlgorithm = incoming.getSequence("algorithm");
                if (ObjectUtils.areNullOrEmpty(false, rawPeerPKey, rawPeerPKeyAlgorithm)) throw new IllegalStateException("Received invalid key or algorithm from peer" + channel);

                byte[] rawPeerPKeyBytes = Base64.getDecoder().decode(rawPeerPKey);
                PublicKey key = loadPublic(rawPeerPKeyBytes, rawPeerPKeyAlgorithm);
                if (key == null) throw new IllegalStateException("Invalid key received from peer " + pairAlgorithm);

                OutgoingPacket response = new COutPacket(DataType.CONNECTION_INIT);
                response.addProperty("secret", Base64.getEncoder().encodeToString(getEncodedSecret(key, rawPeerPKeyAlgorithm)));
                response.addProperty("algorithm", this.secretAlgorithm);

                write(channel, responseTag, response);
            }
                return;
            case CHANNEL_CLOSE:
                sharedKeys.remove(channel);
                return;
            case CONNECTION_INIT:
                String rawPeerSKey = incoming.getSequence("secret");
                String rawPeerSKeyAlgorithm = incoming.getSequence("algorithm");
                if (ObjectUtils.areNullOrEmpty(false, rawPeerSKey, rawPeerSKeyAlgorithm)) throw new IllegalStateException("Received invalid key or algorithm from peer" + channel);

                byte[] rawPeerSKeyBytes = Base64.getDecoder().decode(rawPeerSKey);
                rawPeerSKeyBytes = doCipherWithKey(rawPeerSKeyBytes, Cipher.DECRYPT_MODE, pair.getPrivate(), this.pairAlgorithm, true);

                SecretKey key = loadSecret(rawPeerSKeyBytes, rawPeerSKeyAlgorithm);

                PeerKeyData data = new PeerKeyData(key, rawPeerSKeyAlgorithm);
                this.sharedKeys.put(channel, data);

                List<QueuedPacket> queue = this.packetQue.get(channel);
                if (queue != null && !queue.isEmpty()) {
                    Iterator<QueuedPacket> iterator = queue.iterator();
                    while (iterator.hasNext()) {
                        QueuedPacket packet = iterator.next();
                        this.write(channel, packet.getTag(), packet.getPacket());
                        iterator.remove();
                    }
                }

                JsonObject object = JsonReader.parse(incoming.getData()).asObject();
                object.removeChild("secret");
                object.removeChild("algorithm");

                JsonObject obj = new JsonObject("", "", '.');
                obj.put("id", incoming.id());
                obj.put("packetType", incoming.getType().name());
                obj.put("stamp", incoming.timestamp().toEpochMilli());

                incoming = new CInPacket(obj.toString());
        }

        handle(channel, incoming);
    }

    /**
     * Validate a packet and returns its
     * incoming packet instance
     *
     * @param resolved the resolved packet bytes
     * @param notSecure if the resolved packet is not secure
     * @param channel the channel from where packet bytes
     *                were sent
     * @return the incoming packet
     * @throws InvalidPacketDataException if the packet full data
     * results on an invalid packet. This might be caused by a MitM
     * attack, or a malformed packet (possibly caused by an outdated
     * plugin)
     */
    private IncomingPacket validatePacket(final byte[] resolved, final boolean notSecure, final String channel) throws InvalidPacketDataException {
        IncomingPacket incoming;

        Object object = StringUtils.load(new String(resolved)).orElse(null);
        if (!(object instanceof OutgoingPacket)) {
            try {
                incoming = new CInPacket(new String(resolved));
            } catch (InvalidPacketDataException ex) {
                String typeName = "null";
                if (object != null) typeName = object.getClass().getSimpleName();

                throw new IllegalStateException("Invalid packet type received. Expected OutgoingPacket, but got " + typeName);
            }
        } else {
            OutgoingPacket peerMessage = (OutgoingPacket) object;
            incoming = new CInPacket(peerMessage.build().toString());
        }

        DataType type = incoming.getType();
        if (notSecure && !ArrayUtils.containsAny(PRE_TYPES, type))
            throw new InvalidPacketDataException("Refusing to process packet of type " + incoming.getType() + " from " + channel + ". [UNPROTECTED]");

        return incoming;
    }

    /**
     * Load a key
     *
     * @param keyBytes the key bytes
     * @param keyAlgorithm the key algorithm
     * @return the loaded key
     */
    private PublicKey loadPublic(final byte[] keyBytes, final String keyAlgorithm) {
        try {
            KeyFactory factory = KeyFactory.getInstance(keyAlgorithm);
            return factory.generatePublic(
                    new X509EncodedKeySpec(keyBytes)
            );
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            plugin.log(ex, "Failed to load public key");
        }

        return null;
    }

    /**
     * Load a secret key
     *
     * @param keyBytes the secret bytes
     * @param keyAlgorithm the secret algorithm
     * @return the secret
     */
    private SecretKey loadSecret(final byte[] keyBytes, final String keyAlgorithm) {
        return new SecretKeySpec(keyBytes, keyAlgorithm);
    }
}