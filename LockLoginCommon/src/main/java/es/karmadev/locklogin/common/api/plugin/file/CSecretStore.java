package es.karmadev.locklogin.common.api.plugin.file;

import es.karmadev.locklogin.api.plugin.file.section.SecretStore;

/**
 * Secret store
 */
public class CSecretStore implements SecretStore {

    private final byte[] token;
    private final byte[] iv;

    public CSecretStore(final byte[] t, final byte[] i) {
        token = t;
        iv = i;
    }

    /**
     * The stored token
     *
     * @return the token
     */
    @Override
    public byte[] token() {
        return token;
    }

    /**
     * The stored IV
     *
     * @return the IV
     */
    @Override
    public byte[] iv() {
        return iv;
    }

    public static CSecretStore of(final byte[] token, final byte[] iv) {
        return new CSecretStore(token, iv);
    }
}
