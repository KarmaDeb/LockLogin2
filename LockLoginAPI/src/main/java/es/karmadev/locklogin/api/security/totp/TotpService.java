package es.karmadev.locklogin.api.security.totp;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

/**
 * LockLogin totp service
 */
public interface TotpService extends PluginService {

    /**
     * Generate a QR code for the
     * client, so he can scan it and obtain
     * his TOTP code
     *
     * @param client the client to generate for
     * @return the client QR TOTP scan code
     */
    @Nullable URL generateQR(final LocalNetworkClient client);

    /**
     * Get the client scratch codes
     *
     * @param client the client to get codes for
     * @return the client scratch codes
     * @throws SecurityException if the scratch codes have
     * been already hashed (meaning they've been already requested)
     */
    String[] scratchCodes(final LocalNetworkClient client) throws SecurityException;

    /**
     * Validate the TOTP code for the
     * client. Please note, this implementation should
     * automatically handle invalid code to be
     * considered as a scratch code. Basically, each implementation
     * of the totp service should handle code as a valid scratch code
     * instead of only the expected totp code
     *
     * @param code the code to validate
     * @param client the client that is typing the
     *               code
     * @return if the code is valid
     */
    boolean validateTotp(final String code, final LocalNetworkClient client);
}
