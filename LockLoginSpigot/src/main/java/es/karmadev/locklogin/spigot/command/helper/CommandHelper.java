package es.karmadev.locklogin.spigot.command.helper;

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

                    try {
                        Constructor<? extends Command> commandConstructor = executor.getConstructor(String.class);
                        commandConstructor.setAccessible(true);

                        Map<String, Command> knownCommands = getMap();
                        if (!knownCommands.containsKey(command.command())) {
                            toUnregister.add(command.command());
                        }
                        toUnregister.add("locklogin:" + command.command());

                        Command unknownCommand = commandConstructor.newInstance(command.command());
                        boolean success = map.register(command.command(), "locklogin", unknownCommand);

                        if (success) {
                            spigot.logInfo("Registered command {0}", command.command());
                        } else {
                            spigot.logInfo("Failed to register command {0}", command.command());
                        }
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                        spigot.log(ex, "An error occurred while registering command {0}", command.command());
                        spigot.err("Something went wrong while registering command {0}", command.command());
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
}
