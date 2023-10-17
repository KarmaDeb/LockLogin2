package es.karmadev.locklogin.spigot.command.helper;

import es.karmadev.locklogin.api.user.auth.ProcessFactory;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Spigot command helper
 */
public class CommandHelper {

    private final Set<String> toUnregister = new HashSet<>();
    private final Map<Command, Set<String>> commandToUnregister = new HashMap<>();

    private final SimpleCommandMap map;

    public CommandHelper(final SimpleCommandMap map) {
        this.map = map;
    }

    /**
     * UnMap the command map
     */
    public void unMap() {
        for (String label : toUnregister) {
            Map<String, Command> knownCommands = getMap();

            Command command = knownCommands.remove(label);
            command.unregister(map);
        }

        toUnregister.clear();
    }

    private void unMap(final Command command) {
        for (String label : commandToUnregister.getOrDefault(command, new HashSet<>())) {
            Map<String, Command> knownCommands = getMap();

            Command c = knownCommands.remove(label);
            c.unregister(map);
        }

        commandToUnregister.remove(command);
    }

    public void mapCommand(final LockLoginSpigot spigot) throws IOException, ClassNotFoundException {
        List<Class<?>> result = getClassesAt(spigot.getRuntime().file().toFile());
        for (Class<?> file : result) {
            String clazzName = file.getName().replaceAll(".*/|[.]class.*","");
            Class<?> clazz = Class.forName(clazzName);

            if (Command.class.isAssignableFrom(clazz)) {
                Class<? extends Command> executor = clazz.asSubclass(Command.class);

                if (clazz.isAnnotationPresent(PluginCommand.class)) {
                    PluginCommand command = clazz.getAnnotation(PluginCommand.class);
                    assert command != null; //We know we have them

                    if (spigot.bungeeMode() && !command.useInBungeecord()) {
                        spigot.logInfo("Ignoring command {0} as it was not designed for BungeeCord mode", command.command());
                        continue;
                    }

                    Class<? extends UserAuthProcess> processAttachment = command.processAttachment();
                    if (!processAttachment.equals(UserAuthProcess.class)) {
                        ProcessFactory factory = spigot.getProcessFactory();
                        if (!factory.isEnabled(processAttachment)) {
                            String cmd = command.command();
                            Command registered = map.getCommand("locklogin:" + cmd);
                            if (registered != null) {
                                unMap(registered);
                                spigot.logInfo("Unregistered command {0}", command.command());
                            }

                            continue;
                        }
                    }

                    Command registered = map.getCommand("locklogin:" + command.command());
                    if (registered == null) {
                        try {
                            Constructor<? extends Command> commandConstructor = executor.getConstructor(String.class);
                            commandConstructor.setAccessible(true);

                            Map<String, Command> knownCommands = getMap();
                            Set<String> localUnregister = new HashSet<>();
                            if (!knownCommands.containsKey(command.command())) {
                                toUnregister.add(command.command());
                                localUnregister.add(command.command());
                            }
                            toUnregister.add("locklogin:" + command.command());
                            localUnregister.add("locklogin:" + command.command());

                            Command unknownCommand = commandConstructor.newInstance(command.command());
                            boolean success = map.register(command.command(), "locklogin", unknownCommand);

                            commandToUnregister.put(unknownCommand, localUnregister);

                            if (success) {
                                spigot.logInfo("Registered command {0}", command.command());
                            } else {
                                spigot.logErr("Failed to register command {0}", command.command());
                            }
                        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                                 IllegalAccessException ex) {
                            spigot.log(ex, "An error occurred while registering command {0}", command.command());
                            //spigot.logErr("Something went wrong while registering command {0}", command.command());
                        }
                    } else {
                        if (!registered.isRegistered()) {
                            registered.register(map);
                            spigot.logInfo("Registered command {0}", command.command());
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getMap() {
        try {
            Field map = SimpleCommandMap.class.getDeclaredField("knownCommands");
            map.setAccessible(true);

            return (Map<String, Command>) map.get(this.map);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<Class<?>> getClassesAt(final File file) throws IOException {
        int pkgCount = countCharacter("es.karmadev.locklogin.spigot.command");

        List<Class<?>> classes = new ArrayList<>();
        try (JarFile jar = new JarFile(file)) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    // This ZipEntry represents a class. Now, what class does it represent?
                    String className = entry.getName().replace('/', '.'); // including ".class"
                    className = className.substring(0, className.length() - ".class".length());

                    int clazzCount = countCharacter(className);

                    if (className.startsWith("es.karmadev.locklogin.spigot.command") && (clazzCount - 1) == pkgCount) {
                        try {
                            Class<?> clazz = Class.forName(className);
                            classes.add(clazz);
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

        return classes;
    }

    private int countCharacter(final CharSequence sequence) {
        int count = 0;

        for (int i = 0; i < sequence.length(); i++) {
            char c = sequence.charAt(i);
            if (c == '.') count++;
        }

        return count;
    }

    public Set<String> getCommands() {
        return Collections.unmodifiableSet(toUnregister);
    }
}
