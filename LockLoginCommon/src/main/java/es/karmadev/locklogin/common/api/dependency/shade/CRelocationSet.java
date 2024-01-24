package es.karmadev.locklogin.common.api.dependency.shade;

import es.karmadev.locklogin.api.plugin.runtime.dependency.shade.Relocation;
import es.karmadev.locklogin.api.plugin.runtime.dependency.shade.RelocationSet;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class CRelocationSet implements RelocationSet {

    private final List<CRelocation> relocationList;
    private int index = 0;

    private final static RelocationSet EMPTY = new CRelocationSet(Collections.emptyList());

    /**
     * Get if the dependency has
     * relocations
     *
     * @return if the dependency has relocations
     */
    @Override
    public boolean hasRelocation() {
        return !relocationList.isEmpty();
    }

    /**
     * Iterate to the next relocation
     *
     * @return the next relocation
     */
    @Override
    public @Nullable Relocation next() {
        return (index < relocationList.size() ? relocationList.get(index++) : null);
    }

    public static RelocationSet empty() {
        return EMPTY;
    }
}
