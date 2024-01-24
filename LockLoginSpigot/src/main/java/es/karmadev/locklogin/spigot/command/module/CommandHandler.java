package es.karmadev.locklogin.spigot.command.module;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * LockLogin command handler
 */
@Getter
@Builder
public class CommandHandler extends Command {

    @NonNull
    private final String name;
    @NonNull
    private final String description;
    @NonNull
    private final CommandExecutor executor;

    protected CommandHandler(final @NotNull String name, final @NotNull String description, final @NotNull CommandExecutor executor, final String... aliases) {
        super(name, description, "", Arrays.asList(aliases));
        this.name = name;
        this.description = description;
        this.executor = executor;
    }

    /**
     * Executes the command, returning its success
     *
     * @param sender       Source object which is executing this command
     * @param commandLabel The alias of the command used
     * @param args         All arguments passed to the command, split via ' '
     * @return true if the command was successful, otherwise false
     */
    @Override
    public boolean execute(final @NotNull CommandSender sender, final @NotNull String commandLabel, final @NotNull String[] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }

    public static class CommandHandlerBuilder {

        private String[] aliases = new String[]{};

        public CommandHandlerBuilder aliases(final String[] aliases) {
            this.aliases = aliases;
            return this;
        }

        public CommandHandler build() {
            return new CommandHandler(name, description, executor, aliases);
        }
    }
}
