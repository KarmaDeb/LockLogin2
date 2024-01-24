package es.karmadev.locklogin.api.extension.module.resource;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.resource.exception.ClosedHandleException;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Represents a module resource
 * handle
 */
public interface ResourceHandle extends AutoCloseable {

    /**
     * Get the module that owns this resource
     *
     * @return the resource module
     */
    Module getModule();

    /**
     * Get the resource name
     *
     * @return the resource name
     */
    @NotNull
    String getName();

    /**
     * Get the directory path (including name)
     *
     * @return the resource path
     */
    @NotNull
    String getPath();

    /**
     * Creates a stream for the resource
     * handle. This stream won't be bind to
     * this resource handle and for so,
     * any new call to {@code #createStream()} and then
     * {@link InputStream#close()} won't affect the
     * other stream instances
     *
     * @return the resource input stream
     * @throws ClosedHandleException if the original handle is closed
     */
    @NotNull
    InputStream createStream() throws ClosedHandleException;

    /**
     * Tries to export the resource into the
     * specified path
     *
     * @param destination the path
     * @param overwrite if the path already exists, do nothing and return false
     * @throws ClosedHandleException if the handle is closed
     */
    boolean export(final Path destination, final boolean overwrite) throws ClosedHandleException;

    /**
     * Export a file safely, using a generated hash. The default
     * implementations generate that hash based on bytes, so even if
     * the file gets moved, it won't export unless the contents of the
     * file are changed
     *
     * @param destination the path
     * @return if the hash didn't match
     */
    boolean exportSafely(final Path destination);

    /**
     * Get if the resource handle is open
     *
     * @return if the handle is open
     */
    boolean isOpen();

    /**
     * Tries to close the stream, otherwise
     * will remain open
     */
    @Override
    void close();
}
