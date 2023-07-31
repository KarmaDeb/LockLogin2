package es.karmadev.locklogin.api.security.check;

import es.karmadev.locklogin.api.plugin.service.PluginService;

/**
 * LockLogin password validator
 */
public interface PasswordValidator extends PluginService {

    /**
     * Get if the password is safe
     *
     * @return the password check result
     */
    CheckResult validate();
}
