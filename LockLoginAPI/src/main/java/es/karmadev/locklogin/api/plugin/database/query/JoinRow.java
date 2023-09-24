package es.karmadev.locklogin.api.plugin.database.query;

import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * A join-able row
 */
@Value(staticConstructor = "at")
@AllArgsConstructor(staticName = "at")
@Getter
public class JoinRow {

    Row row;
    Table table;

    /**
     * Create a join row using the default table
     *
     * @param row the row
     * @return the join row
     */
    public static JoinRow at(final Row row) {
        return JoinRow.at(row, null);
    }
}
