package es.karmadev.locklogin.common.api.extension.loader;

import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.locklogin.api.extension.module.Module;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class finder
 */
public class CModData {

    private final Set<Class<?>> classes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Getter
    private final Path file;
    @Getter @Setter
    private Module module;

    @Getter
    private final YamlFileHandler moduleYML;

    public CModData(final Path file, final Module module, final YamlFileHandler yaml) {
        this.file = file;
        this.module = module;
        this.moduleYML = yaml;
    }

    public void assign(final Class<?>... classes) {
        if (classes == null) return;
        this.classes.addAll(Arrays.asList(classes));
    }

    public boolean ownsClass(final Class<?> clazz) {
        return classes.contains(clazz);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hash tables such as those provided by
     * {@link HashMap}.
     * <p>
     * The general contract of {@code hashCode} is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     *     an execution of a Java application, the {@code hashCode} method
     *     must consistently return the same integer, provided no information
     *     used in {@code equals} comparisons on the object is modified.
     *     This integer need not remain consistent from one execution of an
     *     application to another execution of the same application.
     * <li>If two objects are equal according to the {@code equals(Object)}
     *     method, then calling the {@code hashCode} method on each of
     *     the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     *     according to the {@link Object#equals(Object)}
     *     method, then calling the {@code hashCode} method on each of the
     *     two objects must produce distinct integer results.  However, the
     *     programmer should be aware that producing distinct integer results
     *     for unequal objects may improve the performance of hash tables.
     * </ul>
     * <p>
     * As much as is reasonably practical, the hashCode method defined by
     * class {@code Object} does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java&trade; programming language.)
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see System#identityHashCode
     */
    @Override
    public int hashCode() {
        return file.getFileName().toString().hashCode();
    }
}
