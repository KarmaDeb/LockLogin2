package es.karmadev.locklogin.api.plugin.file.section;

/**
 * LockLogin communications section
 */
public interface CommunicationSection {

    /**
     * Get the communication server host
     *
     * @return the communication
     * server host
     */
    String host();

    /**
     * Get the communication server port
     *
     * @return the communication
     * server port
     */
    int port();

    /**
     * Get the communication server
     * ssl configuration
     *
     * @return the connection is performed
     * under SSL
     */
    boolean useSSL();
}
