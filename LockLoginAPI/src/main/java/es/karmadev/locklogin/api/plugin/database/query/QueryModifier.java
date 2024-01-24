package es.karmadev.locklogin.api.plugin.database.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * Query modifier
 */
@Value(staticConstructor = "of")
@AllArgsConstructor(staticName = "of")
public class QueryModifier {

    /**
     * Get the raw value
     */
    @Getter
    String raw;
}
