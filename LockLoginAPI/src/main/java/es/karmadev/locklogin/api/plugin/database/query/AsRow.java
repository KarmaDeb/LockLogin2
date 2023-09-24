package es.karmadev.locklogin.api.plugin.database.query;

import es.karmadev.locklogin.api.plugin.database.schema.Row;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

/**
 * A select as row
 */
@Value(staticConstructor = "as")
@AllArgsConstructor(staticName = "as")
public class AsRow {

    Row row;
    String name;
}
