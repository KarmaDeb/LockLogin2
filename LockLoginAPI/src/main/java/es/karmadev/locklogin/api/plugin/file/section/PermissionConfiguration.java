package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin permissions configuration
 */
public interface PermissionConfiguration extends Serializable {

    /**
     * Get if the plugin blocks operators
     * by removing OP from them
     *
     * @return if the plugin protects OP
     */
    boolean blockOperator();

    /**
     * Get if the plugin removes every single
     * permission of any non logged user
     *
     * @return if the plugin clears
     * permissions
     */
    boolean removeEveryPermission();

    /**
     * Get if the plugin allows the use of
     * '*' permission, which is like an
     * OP with more elevated permissions
     *
     * @return if the plugin allows the use
     * of '*'
     */
    boolean allowWildcard();
}
