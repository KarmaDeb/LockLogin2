package es.karmadev.locklogin.api.extension.command.worker;

import es.karmadev.locklogin.api.network.NetworkEntity;

import java.util.Collections;
import java.util.List;

/**
 * LockLogin command tab completor
 */
public interface CommandCompletor {

    /**
     * Get the auto complete values
     *
     * @param entity the command executor
     * @param command the command name
     * @param args the command arguments
     * @return the auto complete values
     */
    default List<String> autoComplete(final NetworkEntity entity, final String command, final String[] args) {
        return Collections.emptyList();
    }
}
