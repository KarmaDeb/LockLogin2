package es.karmadev.locklogin.common.api.protection.totp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import es.karmadev.api.database.DatabaseManager;
import es.karmadev.api.database.model.JsonDatabase;
import es.karmadev.api.database.model.json.JsonConnection;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.security.totp.TotpService;
import es.karmadev.locklogin.common.api.protection.type.SHA512Hash;
import es.karmadev.totp.TOTP;
import es.karmadev.totp.TOTPKey;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Totp service
 */
public class CTotpService implements TotpService {

    private final JsonDatabase scratchCodeDatabase = (JsonDatabase) DatabaseManager.getEngine("json").orElse(new JsonDatabase());
    private final TOTP totp = new TOTP();

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "TOTP";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return false;
    }

    /**
     * Generate a TOTP code for the
     * client
     *
     * @param client the client to generate for
     * @return the client TOTP code
     */
    @Override
    public @Nullable URL generateQR(final LocalNetworkClient client) {
        if (client.account().totpSet()) return null; //We already have totp code, do nothing

        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot generate TOTP while plugin is not enabled");

        Configuration configuration = plugin.configuration();
        try {
            String server = URLEncoder.encode(configuration.server(), StandardCharsets.UTF_8.name());
            String name = URLEncoder.encode(client.name(), StandardCharsets.UTF_8.name());

            TOTPKey key = totp.createCredentials();

            String token = key.getKey();
            URL url = URLUtilities.fromString(String.format("https://karmadev.es/locklogin/qr/?issuer=%s&token=%s&target=%s",
                    server,
                    token,
                    name));

            if (url == null) return null;
            String response = URLUtilities.get(url);

            Gson gson = new GsonBuilder().create();
            try {
                JsonElement element = gson.fromJson(response, JsonElement.class);
                if (element.isJsonObject()) return null; //We failed to generate QR code if we have a valid json
            } catch (JsonSyntaxException ignored) {}

            client.account().setTotp(token);
            return url;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the client scratch codes
     *
     * @param client the client to get codes for
     * @return the client scratch codes
     * @throws SecurityException if the scratch codes have
     *                           been already hashed (meaning they've been already requested)
     */
    @Override
    public String[] scratchCodes(final LocalNetworkClient client) throws SecurityException {
        if (!client.account().totpSet() || !client.account().hasTotp()) {
            return new String[0];
        }

        JsonConnection connection = scratchCodeDatabase.grabConnection("locklogin" + File.pathSeparator + "scratch_codes" + File.pathSeparator + client.id());
        if (connection.isSet("scratch")) {
            List<String> codes = connection.getStringList("scratch");
            if (!codes.isEmpty()) return new String[0];
            /*
            In the case we have already set the scratch codes, then we
            will simply do nothing, and as the scratch codes should have
            been taken on the first setup (as we apply a hash on them to store
            them) then we won't send the client the scratch codes, as they don't
            "exist" anymore
             */
        }

        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();
        List<String> rawCodes = new ArrayList<>();

        SHA512Hash hash = new SHA512Hash();
        for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[8];
            random.nextBytes(bytes);

            String code = toHexString(bytes).toUpperCase();
            codes.add(hash.hashInput(code));
            rawCodes.add(code);
        }
        connection.setStringList("scratch", codes);

        if (connection.save()) {
            return rawCodes.toArray(new String[0]);
        }

        return new String[0];
    }

    /**
     * Validate the TOTP code for the
     * client
     *
     * @param code   the code to validate
     * @param client the client that is typing the
     *               code
     * @return if the code is valid
     */
    @Override
    public boolean validateTotp(final String code, final LocalNetworkClient client) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot validate TOTP while plugin is not enabled");

        if (client.account().totpSet()) {
            String secret = client.account().totp();
            int otp = totp.getTotpPassword(secret);
            if (code.equals(String.valueOf(otp))) {
                return true;
            }

            if (client.account().hasTotp()) {
                //Allow the use of scratch codes only if the user has totp enabled
                JsonConnection connection = scratchCodeDatabase.grabConnection("locklogin" + File.pathSeparator + "scratch_codes" + File.pathSeparator + client.id());
                if (connection.isSet("scratch")) {
                    List<String> codes = connection.getStringList("scratch");

                    boolean result = false;
                    Iterator<String> scratchCodes = codes.listIterator();
                    SHA512Hash hash = new SHA512Hash();
                    while (scratchCodes.hasNext()) {
                        String scratch = scratchCodes.next();
                        if (hash.auth(code, scratch)) {
                            result = true;
                            scratchCodes.remove();
                            break;
                        }
                    }

                    if (result) {
                        //We must update data
                        connection.setStringList("scratch", codes);
                        connection.save();
                        /*
                        Even though is a security risk, as a scratch code could be
                        used twice, we don't really care if the updated scratch code list
                        has been saved, we simply want the user to know he's scratch code
                        was used
                         */
                    }

                    if (codes.isEmpty()) {
                        String[] newCodes = scratchCodes(client);

                        if (newCodes.length > 0) {
                            Messages messages = plugin.messages();
                            if (client.online()) {
                                NetworkClient online = client.client();
                                assert online != null;

                                online.sendMessage(messages.prefix() + messages.gAuthScratchCodes(Arrays.asList(newCodes)));
                            }
                        }
                    }

                    return result;
                }
            }

            return false;
        }

        return false;
    }

    private static String toHexString(final byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (byte b : data) {
            int unsigned = Byte.toUnsignedInt(b);
            builder.append(Integer.toHexString(unsigned));
        }

        return builder.toString();
    }
}
