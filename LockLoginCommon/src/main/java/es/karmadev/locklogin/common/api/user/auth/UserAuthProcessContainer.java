package es.karmadev.locklogin.common.api.user.auth;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User auth process container
 */
public class UserAuthProcessContainer implements Iterable<Class<? extends UserAuthProcess>> {

    private Class<?>[] que = new Class<?>[0];
    private final Map<Class<?>, String> names = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> priorities = new ConcurrentHashMap<>();

    public String getNameFor(final Class<?> process) {
        return names.get(process);
    }

    public boolean insert(final Class<? extends UserAuthProcess> process) {
        if (process == null) return false;

        try {
            Method priorityMethod = process.getDeclaredMethod("getPriority");
            Method nameMethod = process.getDeclaredMethod("getName");
            Method createForMethod = process.getDeclaredMethod("createFor", NetworkClient.class);

            if (!priorityMethod.getReturnType().equals(int.class)) return false;
            if (!nameMethod.getReturnType().equals(String.class)) return false;
            if (!UserAuthProcess.class.isAssignableFrom(createForMethod.getReturnType())) return false;

            String name = (String) nameMethod.invoke(process);
            if (name == null) return false;
            int priority = (int) priorityMethod.invoke(process);
            if (names.containsValue(name)) return false;

            names.put(process, name);
            priorities.put(process, priority);

            que = Arrays.copyOf(que, que.length + 1);
            que[que.length - 1] = process;

            return true;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            CurrentPlugin.getPlugin().log(ex, "Failed to register UserAuthProcess {0}", process);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Class<? extends UserAuthProcess> getNext(final UserAuthProcess current) {
        List<Class<?>> arrayProcess = new ArrayList<>(Arrays.asList(que.clone()));
        arrayProcess.sort((o1, o2) -> {
            int o1Priority = priorities.get(o1);
            int o2Priority = priorities.get(o2);

            if (o1Priority == o2Priority) return 0;
            return (o1Priority > o2Priority ? 1 : -1);
        });

        int index = 0;
        for (Class<?> process : arrayProcess) {
            if (current == null) return (Class<? extends UserAuthProcess>) process;

            String name = names.get(process);
            if (name.equals(current.name())) {
                if (arrayProcess.size() <= index + 1) return null;
                return (Class<? extends UserAuthProcess>) arrayProcess.get(index + 1);
            }

            index++;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends UserAuthProcess> getPrevious(final UserAuthProcess current) {
        List<Class<?>> arrayProcess = new ArrayList<>(Arrays.asList(que.clone()));
        arrayProcess.sort((o1, o2) -> {
            int o1Priority = priorities.get(o1);
            int o2Priority = priorities.get(o2);

            if (o1Priority == o2Priority) return 0;
            return (o1Priority > o2Priority ? 1 : -1);
        });

        int index = 0;
        for (Class<?> process : arrayProcess) {
            if (current == null) return (Class<? extends UserAuthProcess>) process;

            String name = names.get(process);
            if (name.equals(current.name())) {
                if (index - 1 <= 0) return null;
                return (Class<? extends UserAuthProcess>) arrayProcess.get(index - 1);
            }

            index++;
        }

        return null;
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @NotNull
    @Override @SuppressWarnings("unchecked")
    public Iterator<Class<? extends UserAuthProcess>> iterator() {
        List<Class<?>> arrayProcess = new ArrayList<>(Arrays.asList(que.clone()));
        arrayProcess.sort((o1, o2) -> {
            int o1Priority = priorities.get(o1);
            int o2Priority = priorities.get(o2);

            if (o1Priority == o2Priority) return 0;
            return (o1Priority > o2Priority ? 1 : -1);
        });

        List<Class<? extends UserAuthProcess>> clone = new ArrayList<>();
        for (Class<?> clazz : arrayProcess) clone.add((Class<? extends UserAuthProcess>) clazz);

        return clone.iterator();
    }
}
