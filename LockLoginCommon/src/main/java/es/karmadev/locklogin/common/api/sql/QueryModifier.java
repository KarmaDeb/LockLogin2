package es.karmadev.locklogin.common.api.sql;

import lombok.Getter;
import lombok.Value;

/**
 * Query modifier
 */
@Value(staticConstructor = "of")
public class QueryModifier {

    @Getter
    String raw;
}
