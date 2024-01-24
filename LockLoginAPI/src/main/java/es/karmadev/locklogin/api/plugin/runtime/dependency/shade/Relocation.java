package es.karmadev.locklogin.api.plugin.runtime.dependency.shade;

/**
 * Represents a relocation
 */
public interface Relocation {

    /**
     * The source relocation
     *
     * @return the source relocation
     */
    String from();

    /**
     * The target relocation
     *
     * @return the target relocation
     */
    String to();
}
