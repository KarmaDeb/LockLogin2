package es.karmadev.locklogin.common.api.user.auth;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.auth.ProcessFactory;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CProcessFactory implements ProcessFactory {

    private final UserAuthProcessContainer container = new UserAuthProcessContainer();
    private final Map<UUID, UserAuthProcess> userProcess  = new ConcurrentHashMap<>();

    /**
     * Get the next process for the client
     *
     * @param client the client
     * @return the client next process
     */
    @Override
    public Optional<? extends UserAuthProcess> getNextProcess(final NetworkClient client) {
        UserAuthProcess current = userProcess.get(client.uniqueId());
        Class<? extends UserAuthProcess> processClass = container.getNext(current);

        if (processClass == null) return Optional.empty();
        try {
            Method createFor = processClass.getDeclaredMethod("createFor", NetworkClient.class);
            UserAuthProcess process = (UserAuthProcess) createFor.invoke(processClass, client);

            if (process != null) {
                userProcess.put(client.uniqueId(), process);
            }

            return Optional.ofNullable(process);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}
        return Optional.empty();
    }

    /**
     * Register an auth process
     *
     * @param process the process
     * @throws IllegalStateException if the {@link UserAuthProcess instance} doesn't have a
     *                               static createFor({@link NetworkClient}) method
     */
    @Override
    public void registerAuthProcess(final Class<? extends UserAuthProcess> process) throws IllegalStateException {
        if (container.insert(process)) {
            CurrentPlugin.getPlugin().info("Successfully registered user auth process: {0}", container.getNameFor(process));
        } else {
            CurrentPlugin.getPlugin().err("Couldn't register auth process {0}. Make sure it has a public static int getPriority(), public static String getName() and public static UserAuthProcess createFor(NetworkClient)", process);
        }
    }

    /**
     * Remove the progress
     *
     * @param client the client to remove progress of
     */
    public void removeProgress(final NetworkClient client) {
        userProcess.remove(client.uniqueId());
    }
}
