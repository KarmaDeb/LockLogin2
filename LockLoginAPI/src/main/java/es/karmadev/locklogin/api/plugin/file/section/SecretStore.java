package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * Secret store
 */
public interface SecretStore extends Serializable {

    /**
     * The stored token
     *
     * @return the token
     */
    byte[] token();

    /**
     * The stored IV
     *
     * @return the IV
     */
    byte[] iv();
}
