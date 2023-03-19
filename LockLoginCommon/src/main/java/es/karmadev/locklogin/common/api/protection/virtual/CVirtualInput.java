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
}
