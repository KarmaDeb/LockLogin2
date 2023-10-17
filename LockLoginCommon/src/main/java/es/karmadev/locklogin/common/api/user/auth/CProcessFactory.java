package es.karmadev.locklogin.common.api.user.auth;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.auth.ProcessFactory;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CProcessFactory implements ProcessFactory {

    private final UserAuthProcessContainer container = new UserAuthProcessContainer();
    private final Map<UUID, UserAuthProcess> userProcess  = new HashMap<>();

    /**
     * Get the next process for the client
     *
     * @param client the client
     * @return the client next process
     */
    @Override
    public Optional<UserAuthProcess> nextProcess(final NetworkClient client) {
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
     * Get the previous process for the client
     *
     * @param client the client
     * @return the previous process
     */
    @Override
    public Optional<UserAuthProcess> previousProcess(final NetworkClient client) {
        UserAuthProcess current = userProcess.get(client.uniqueId());
        Class<? extends UserAuthProcess> processClass = container.getPrevious(current);

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
    public void register(final Class<? extends UserAuthProcess> process) throws IllegalStateException {
        if (container.insert(process)) {
            CurrentPlugin.getPlugin().logInfo("Successfully registered user auth process: {0}", container.getNameFor(process));
        } else {
            CurrentPlugin.getPlugin().logErr("Couldn't register auth process {0}. Make sure it has a public static AuthProcess createDummy() public static int getPriority(), public static String getName() and public static UserAuthProcess createFor(NetworkClient)", process);
        }
    }

    /**
     * Get if the process is enabled
     *
     * @param processClass if the process is enabled
     * @return the process status
     */
    @Override
    public boolean isEnabled(final Class<? extends UserAuthProcess> processClass) {
        try {
            Method createFor = processClass.getDeclaredMethod("createDummy");
            UserAuthProcess process = (UserAuthProcess) createFor.invoke(processClass);

            if (process != null) {
                return process.isEnabled();
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}

        return false;
    }

    /**
     * Get the process name
     *
     * @param processClass the process
     * @return the process name
     */
    @Override
    public String getName(final Class<? extends UserAuthProcess> processClass) {
        try {
            Method createFor = processClass.getDeclaredMethod("createDummy");
            UserAuthProcess process = (UserAuthProcess) createFor.invoke(processClass);

            if (process != null) {
                return process.name();
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}

        return null;
    }

    /**
     * Get the process priority
     *
     * @param processClass the process
     * @return the process priority
     */
    @Override
    public int getPriority(final Class<? extends UserAuthProcess> processClass) {
        try {
            Method createFor = processClass.getDeclaredMethod("createDummy");
            UserAuthProcess process = (UserAuthProcess) createFor.invoke(processClass);

            if (process != null) {
                return process.priority();
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}

        return 0;
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
