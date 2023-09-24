package es.karmadev.locklogin.common.api.protection.virtual;

import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CVirtualInput implements VirtualizedInput {

    int[] references;

    boolean valid;

    byte[] product;

    /**
     * Create an invalid-raw product
     *
     * @param product the product
     * @return the virtualized input
     */
    public static CVirtualInput raw(final byte[] product) {
        return of(new int[0], false, product);
    }
}
