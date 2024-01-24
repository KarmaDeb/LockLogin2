package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * Authentication settings
 */
public interface AuthenticationConfiguration extends Serializable {


    boolean register();

    boolean login();

    boolean pin();

    boolean totp();
}
