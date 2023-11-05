package es.karmadev.locklogin.common.plugin.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.api.file.yaml.handler.YamlHandler;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.web.request.RequestData;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.CacheAble;
import lombok.Getter;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@CacheAble(name = "LockLogin optional license")
public class License {

    @Getter
    private static boolean licensed;
    @Getter
    private static String buyer;

    public static void preCache() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path configLocation = plugin.workingDirectory().resolve("config.yml");

        if (!Files.exists(configLocation)) return;

        try {
            YamlFileHandler config = YamlHandler.load(configLocation);
            String license = config.getString("License");

            if (ObjectUtils.isNullOrEmpty(license)) return;
            String host = "https://api.karmadev.es/validate-purcharse";
            URL url = URLUtilities.fromString(host);

            if (url == null) return;

            try {
                InetAddress internetProtocol = InetAddress.getLocalHost();
                NetworkInterface netInterface = NetworkInterface.getByInetAddress(internetProtocol);

                byte[] mac = netInterface.getHardwareAddress();

                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(mac);
                byte[] hwBytes = md.digest();

                StringBuilder certBuilder = new StringBuilder();
                for (byte b : hwBytes) {
                    int unsigned = Byte.toUnsignedInt(b);
                    certBuilder.append(Integer.toHexString(unsigned));
                }

                String certificate = certBuilder.toString();
                String response = URLUtilities.post(url,
                        RequestData.newRequest()
                                .add("key", license)
                                .add("certificate", certificate)
                                .contentType(RequestData.ContentType.JSON));

                Gson gson = new GsonBuilder().create();
                JsonElement element = gson.fromJson(response, JsonElement.class);

                if (element == null || !element.isJsonObject()) return;
                JsonObject object = element.getAsJsonObject();

                if (object.has("success")) {
                    licensed = object.get("success").getAsBoolean();
                }
                if (object.has("username")) {
                    buyer = object.get("username").getAsString();
                }
            } catch (UnknownHostException | SocketException | NoSuchAlgorithmException ignored) {}

        } catch (IOException ignored) {}
    }
}
