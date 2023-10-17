package es.karmadev.locklogin.bungee.command;

import es.karmadev.locklogin.api.extension.module.command.ModuleCommand;

import java.util.function.Consumer;
import java.util.function.Function;

public class BungeeCommandManager implements Function<ModuleCommand, Boolean>, Consumer<ModuleCommand> {

    /**
     * Performs this operation on the given argument.
     *
     * @param moduleCommand the input argument
     */
    @Override
    public void accept(ModuleCommand moduleCommand) {

    }

    /**
     * Applies this function to the given argument.
     *
     * @param moduleCommand the function argument
     * @return the function result
     */
    @Override
    public Boolean apply(ModuleCommand moduleCommand) {
        return null;
    }
}
