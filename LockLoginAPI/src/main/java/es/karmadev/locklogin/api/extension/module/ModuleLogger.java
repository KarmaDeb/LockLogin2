package es.karmadev.locklogin.api.extension.module;

import es.karmadev.api.logger.log.console.ConsoleColor;

import java.util.MissingResourceException;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ModuleLogger extends Logger {

    /**
     * Protected method to construct a logger for a named subsystem.
     * <p>
     * The logger will be initially configured with a null Level
     * and with useParentHandlers set to true.
     *
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *                                  no corresponding resource can be found.
     */
    protected ModuleLogger(final AbstractModule module) {
        super(module.getName(), null);
    }

    /**
     * Log a LogRecord.
     * <p>
     * All the other logging methods in this class call through
     * this method to actually perform any logging.  Subclasses can
     * override this single method to capture all log activity.
     *
     * @param record the LogRecord to be published
     */
    @Override
    public void log(final LogRecord record) {
        record.setMessage(ConsoleColor.parse(record.getMessage()));
        super.log(record);
    }
}
