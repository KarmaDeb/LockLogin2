package es.karmadev.locklogin.api.plugin.database.query;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.database.driver.Driver;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.RowType;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.database.Database;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query builder
 */
@SuppressWarnings("unused")
public final class QueryBuilder {

    /**
     * Raw query
     */
    private final StringBuilder rawQuery = new StringBuilder();
    /**
     * The SQL driver
     */
    private final Driver driver;

    /**
     * First row status
     */
    private boolean firstRow = true;
    /**
     * And or status
     */
    private boolean andOr = false;
    /**
     * Join status
     */
    private boolean join = false;
    /**
     * If the query can use joins
     */
    private boolean joinSuitable = false;
    /**
     * The query type
     */
    private String queryType = "";

    /**
     * The query table
     */
    private Table table;

    /**
     * Table and row translator
     */
    private final Database databaseSettings;

    /**
     * Initialize the query builder
     * @param driver the driver to use
     */
    QueryBuilder(final Driver driver) {
        this.driver = driver;
        LockLogin plugin = CurrentPlugin.getPlugin();

        databaseSettings = plugin.configuration().database();
    }

    /**
     * Create a new query
     *
     * @return the query
     */
    public static QueryBuilder createQuery() {
        Configuration configuration = CurrentPlugin.getPlugin().configuration();
        return createQuery(configuration.database().driver());
    }

    /**
     * Create a new query
     *
     * @param driver the driver to use
     * @return the query
     */
    public static QueryBuilder createQuery(final Driver driver) {
        return new QueryBuilder(driver);
    }

    /**
     * Create a new query
     *
     * @return the new query
     */
    public QueryBuilder newQuery() {
        return new QueryBuilder(driver);
    }

    public QueryBuilder delete(final Table table) {
        if (!ObjectUtils.isNullOrEmpty(queryType)) return this;
        queryType = "drop-column";

        this.table = table;
        String tableName = databaseSettings.tableName(table);

        rawQuery.append("DELETE FROM `").append(tableName).append("` ");
        return this;
    }

    /**
     * Create a new table
     *
     * @param if_not_exists create if not exists
     * @param table the table to create
     * @return the query
     */
    public QueryBuilder createTable(final boolean if_not_exists, final Table table) {
        if (!ObjectUtils.isNullOrEmpty(queryType)) return this;
        queryType = "create";

        this.table = table;
        String tableName = databaseSettings.tableName(table);

        rawQuery.append("CREATE TABLE").append((if_not_exists ? " IF NOT EXISTS `" : " `")).append(tableName).append("` (");

        return this;
    }

    /**
     * Insert data into an existing table
     *
     * @param table the table
     * @param rows the data rows to insert
     * @return the query
     */
    public QueryBuilder insert(final Table table, final Row... rows) {
        if (!ObjectUtils.isNullOrEmpty(queryType)) return this;
        queryType = "insert";

        this.table = table;
        String tableName = databaseSettings.tableName(table);

        rawQuery.append("INSERT INTO `").append(tableName).append("` (");
        List<Row> usableRows = Arrays.stream(rows).filter(table::hasRow).collect(Collectors.toList());

        StringBuilder rowBuilder = new StringBuilder();
        for (Row row : usableRows) {
            String rowName = databaseSettings.columnName(table, row);
            rowBuilder.append("`").append(rowName).append("`").append(", ");
        }
        String finalRow = StringUtils.replaceLast(rowBuilder.toString(), ", ", "");
        rawQuery.append(finalRow).append(") VALUES (");

        return this;
    }

    /**
     * Update a table values
     *
     * @param table the tables
     * @return the query
     */
    public QueryBuilder update(final Table table) {
        if (!ObjectUtils.isNullOrEmpty(queryType)) return this;
        queryType = "update";

        this.table = table;
        String tableName = databaseSettings.tableName(table);

        rawQuery.append("UPDATE `").append(tableName).append("` ");
        return this;
    }

    /**
     * Get data from a table
     *
     * @param table the table
     * @param rows the rows to get
     * @return the query
     */
    public QueryBuilder select(final Table table, final Row... rows) {
        if (!ObjectUtils.isNullOrEmpty(queryType)) return this;
        queryType = "fetch";

        this.table = table;
        String tableName = databaseSettings.tableName(table);
        List<Row> usableRows = Arrays.stream(rows).filter(table::hasRow).collect(Collectors.toList());

        StringBuilder rowBuilder = new StringBuilder();
        for (Row row : usableRows) {
            String rowName = databaseSettings.columnName(table, row);

            rowBuilder.append("`").append(rowName).append("`").append(", ");
        }
        String finalRow = StringUtils.replaceLast(rowBuilder.toString(), ", ", "");

        rawQuery.append("SELECT ").append(finalRow).append(" ").append("FROM `").append(tableName).append("` ");
        return this;
    }

    /**
     * Get data from a table
     *
     * @param table the table
     * @param rows the rows to get
     * @return the query
     */
    public QueryBuilder select(final Table table, final AsRow... rows) {
        if (!ObjectUtils.isNullOrEmpty(queryType)) return this;
        queryType = "fetch";

        this.table = table;
        String tableName = databaseSettings.tableName(table);
        List<AsRow> usableRows = Arrays.stream(rows).filter((as) -> table.hasRow(as.getRow())).collect(Collectors.toList());

        StringBuilder rowBuilder = new StringBuilder();
        for (AsRow asRow : usableRows) {
            Row row = asRow.getRow();
            String rowName = databaseSettings.columnName(table, row);

            rowBuilder.append("`").append(rowName).append("`").append(" AS `").append(asRow.getName()).append("`").append(", ");
        }

        String finalRow = StringUtils.replaceLast(rowBuilder.toString(), ", ", "");

        rawQuery.append("SELECT ").append(finalRow).append(" ").append(" FROM `").append(tableName).append("` ");
        return this;
    }

    /**
     * Get data from a table
     *
     * @param table the table
     * @param rows the rows to get
     * @return the query
     */
    public QueryBuilder select(final Table table, final JoinRow... rows) {
        if (!ObjectUtils.isNullOrEmpty(queryType)) return this;
        queryType = "fetch";
        joinSuitable = true;

        this.table = table;
        String tableName = databaseSettings.tableName(table);
        List<JoinRow> usableRows = Arrays.stream(rows).filter((join) -> table.hasRow(join.getRow())).collect(Collectors.toList());

        StringBuilder rowBuilder = new StringBuilder();
        for (JoinRow jr : rows) {
            Row row = jr.getRow();
            Table joinTable = jr.getTable();
            if (joinTable == null) joinTable = table;

            String joinTableName = databaseSettings.tableName(joinTable);
            String rowName = databaseSettings.columnName(joinTable, row);

            rowBuilder.append("`").append(joinTableName).append("`").append(".").append("`").append(rowName).append("`").append(", ");
        }
        String finalRow = StringUtils.replaceLast(rowBuilder.toString(), ", ", "");

        rawQuery.append("SELECT ").append(finalRow).append(" ").append("FROM `").append(tableName).append("` ");
        return this;
    }

    /**
     * Alter a table
     *
     * @param table the table
     * @return the query
     */
    public QueryBuilder alter(final Table table) {
        if (!ObjectUtils.isNullOrEmpty(queryType)) return this;
        queryType = "alter";

        this.table = table;
        String tableName = databaseSettings.tableName(table);

        rawQuery.append("ALTER TABLE `").append(tableName).append("` ");
        return this;
    }

    /**
     * Add a row
     *
     * @param row the row
     * @param type the row type
     * @param modifiers the row modifiers
     * @return the query
     */
    public QueryBuilder add(final Row row, final RowType<?> type, final QueryModifier... modifiers) {
        if (table == null || !queryType.equals("alter") || !table.hasRow(row)) return this;

        StringBuilder customModifiers = new StringBuilder();
        for (QueryModifier modifier : modifiers) {
            String modifiedRaw = modifier.getRaw();
            if (driver.equals(Driver.SQLite)) {
                if (modifiedRaw.equals("AUTO_INCREMENT")) {
                    modifiedRaw = "AUTOINCREMENT";
                }
            }

            if (modifiedRaw.matches("\\([0-9]*\\)") && driver.equals(Driver.SQLite)) {
                continue;
            }

            customModifiers.append(" ").append(modifiedRaw);
        }
        String rawCustom = customModifiers.toString();
        if (firstRow) {
            rawQuery.append("ADD COLUMN ");
            firstRow = false;
        } else {
            return this;
        }

        String rowName = databaseSettings.columnName(table, row);
        RowType<?> modifiedType = type;

        if (driver.equals(Driver.SQLite)) {
            switch (type.toEnumType()) {
                case VARCHAR:
                case LONGTEXT:
                    modifiedType = RowType.TEXT;
                    break;
                case TINY_INTEGER:
                case BIG_INTEGER:
                    modifiedType = RowType.INTEGER;
                    break;
                case LONG:
                case DOUBLE:
                case FLOAT:
                case TIMESTAMP:
                    modifiedType = RowType.NUMERIC;
                    break;
            }
        } else {
            if (type.equals(RowType.NUMERIC)) {
                modifiedType = RowType.DOUBLE;
            }
        }

        rawQuery.append("`").append(rowName).append("` ").append(modifiedType).append(rawCustom);

        return this;
    }

    /**
     * Remove a row
     *
     * @param row the row
     * @return the query
     */
    public QueryBuilder remove(final Row row) {
        if (table == null || !queryType.equals("alter") || !table.hasRow(row)) return this;

        if (firstRow) {
            rawQuery.append("DROP COLUMN ");
            firstRow = false;
        } else {
            return this;
        }

        String rowName = databaseSettings.columnName(table, row);
        rawQuery.append("`").append(rowName).append("`");

        return this;
    }

    /**
     * Rename a column
     *
     * @param row the row to rename
     * @param oldName the old name (which should be the current incorrect)
     * @return the query
     */
    public QueryBuilder rename(final Row row, final String oldName) {
        if (table == null || !queryType.equals("alter") || !table.hasRow(row)) return this;

        if (firstRow) {
            rawQuery.append("RENAME COLUMN `").append(oldName).append("`");
        } else {
            return this;
        }

        String rowName = databaseSettings.columnName(table, row);
        rawQuery.append(" TO `").append(rowName).append("`");

        return this;
    }

    /**
     * Rename the table
     *
     * @param oldName the old name (which should be the current incorrect)
     * @return the query
     */
    public QueryBuilder rename(final String oldName) {
        if (table == null || !queryType.equals("alter") || !firstRow) return this;
        queryType = "alter-rename-table";
        firstRow = false;

        String tableName = databaseSettings.tableName(table);

        rawQuery.append("RENAME TO `").append(tableName).append("`");
        return this;
    }

    /**
     * Set a value in the update
     *
     * @param row the row to modify
     * @param value the new value
     * @return the query
     */
    public QueryBuilder set(final Row row, final Object value) {
        if (table == null || !queryType.equals("update") || !table.hasRow(row)) return this;

        String rowName = databaseSettings.columnName(table, row);
        String rawValue;
        if (value instanceof Boolean || value instanceof Number) {
            rawValue = String.valueOf(value);
        } else {
            if (value == null) {
                rawValue = "NULL";
            } else {
                if (value instanceof QueryModifier) {
                    QueryModifier modifier = (QueryModifier) value;
                    String modifiedRaw = modifier.getRaw();
                    if (driver.equals(Driver.SQLite)) {
                        if (modifiedRaw.equals("AUTO_INCREMENT")) {
                            modifiedRaw = "AUTOINCREMENT";
                        }
                    }

                    rawValue = modifiedRaw;
                } else {
                    rawValue = "'" + value + "'";
                }
            }
        }

        if (firstRow) {
            firstRow = false;
            rawQuery.append("SET `").append(rowName).append("` = ").append(rawValue).append(" ");
        } else {
            rawQuery.append(", SET `").append(rowName).append("` = ").append(rawValue).append(" ");
        }

        return this;
    }

    /**
     * Where clause
     *
     * @param row the row
     * @param operation the operation to use (ex: NOT IN())
     * @return the value
     */
    public QueryBuilder where(final Row row, final String operation) {
        if (table == null || !table.hasRow(row)) return this;
        if (queryType.equals("update") && firstRow) return this;
        if (!queryType.equals("update") && !queryType.equals("fetch") && !queryType.equals("drop-column")) return this;

        firstRow = false;

        String rowName = databaseSettings.columnName(table, row);
        String rawValue;

        rawQuery.append((andOr ? "" : (join ? " " : "") + "WHERE ")).append("`").append(rowName).append("` ").append(operation);
        return this;
    }

    /**
     * Where clause
     *
     * @param row the row
     * @param operation the operation to use (ex: =)
     * @param value the operation value
     * @return the value
     */
    public QueryBuilder where(final Row row, final String operation, final Object value) {
        if (table == null || !table.hasRow(row)) return this;
        if (queryType.equals("update") && firstRow) return this;
        if (!queryType.equals("update") && !queryType.equals("fetch")) return this;

        firstRow = false;

        String rowName = databaseSettings.columnName(table, row);
        String rawValue;
        if (value instanceof Boolean || value instanceof Number) {
            rawValue = String.valueOf(value);
        } else {
            if (value == null) {
                rawValue = "NULL";
            } else {
                if (value instanceof QueryModifier) {
                    QueryModifier modifier = (QueryModifier) value;
                    String modifiedRaw = modifier.getRaw();
                    if (driver.equals(Driver.SQLite) && modifiedRaw.equals("AUTO_INCREMENT")) {
                        modifiedRaw = "AUTOINCREMENT";
                    }

                    rawValue = modifiedRaw;
                } else {
                    String valueString = String.valueOf(value);
                    if (valueString.startsWith("(")) {
                        rawValue = valueString;
                    } else {
                        rawValue = "'" + value + "'";
                    }
                }
            }
        }

        rawQuery.append((andOr ? "" : (join ? " " : "") + "WHERE ")).append("`").append(rowName).append("` ").append(operation).append(" ").append(rawValue);
        return this;
    }

    /**
     * Where clause
     *
     * @param joinRow the row
     * @param operation the operation to use (ex: =)
     * @param value the operation value
     * @return the value
     */
    public QueryBuilder where(final JoinRow joinRow, final String operation, final Object value) {
        if (table == null || !table.hasRow(joinRow.getRow())) return this;
        if (queryType.equals("update") && firstRow) return this;
        if (!queryType.equals("update") && !queryType.equals("fetch")) return this;

        firstRow = false;
        Row row = joinRow.getRow();
        Table tmpTable = joinRow.getTable();
        if (tmpTable == null) tmpTable = table;

        String tableName = databaseSettings.tableName(tmpTable);
        String rowName = databaseSettings.columnName(tmpTable, row);
        String rawValue;
        if (value instanceof Boolean || value instanceof Number) {
            rawValue = String.valueOf(value);
        } else {
            if (value == null) {
                rawValue = "NULL";
            } else {
                if (value instanceof QueryModifier) {
                    QueryModifier modifier = (QueryModifier) value;
                    String modifiedRaw = modifier.getRaw();
                    if (driver.equals(Driver.SQLite) && modifiedRaw.equals("AUTO_INCREMENT")) {
                        modifiedRaw = "AUTOINCREMENT";
                    }

                    rawValue = modifiedRaw;
                } else {
                    rawValue = "'" + value + "'";
                }
            }
        }

        rawQuery.append((andOr ? "" : (join ? " " : "") + "WHERE ")).append("`").append(tableName).append("`.").append("`").append(rowName).append("` ").append(operation).append(" ").append(rawValue);
        return this;
    }

    /**
     * And operator
     *
     * @return the query
     */
    public QueryBuilder and() {
        if (table == null || firstRow) return this;
        if (!queryType.equals("update") && !queryType.equals("fetch")) return this;

        andOr = true;

        rawQuery.append(" AND ");
        return this;
    }

    /**
     * Or operator
     *
     * @return the query
     */
    public QueryBuilder or() {
        if (table == null || firstRow) return this;
        if (!queryType.equals("update") && !queryType.equals("fetch")) return this;

        andOr = true;

        rawQuery.append(" OR ");
        return this;
    }

    /**
     * Join a table
     *
     * @param table the table to join
     * @return the query
     */
    public QueryBuilder join(final Table table) {
        if (!joinSuitable) return this;
        if (table == null || !queryType.equals("fetch")) return this;
        String tableName = databaseSettings.tableName(table);

        join = true;
        firstRow = false;

        rawQuery.append("INNER JOIN `").append(tableName).append("` ON ");
        return this;
    }

    public QueryBuilder on(final JoinRow joinRow, final String operation, final Object value) {
        if (table == null || !queryType.equals("fetch") || !join || !table.hasRow(joinRow.getRow())) return this;

        Row row = joinRow.getRow();
        Table tmpTable = joinRow.getTable();
        if (tmpTable == null) tmpTable = table;

        String tableName = databaseSettings.tableName(tmpTable);
        String rowName = databaseSettings.columnName(tmpTable, row);

        String rawValue;
        if (value instanceof Boolean || value instanceof Number) {
            rawValue = String.valueOf(value);
        } else {
            if (value == null) {
                rawValue = "NULL";
            } else {
                if (value instanceof QueryModifier) {
                    QueryModifier modifier = (QueryModifier) value;
                    String modifiedRaw = modifier.getRaw();
                    if (driver.equals(Driver.SQLite) && modifiedRaw.equals("AUTO_INCREMENT")) {
                        modifiedRaw = "AUTOINCREMENT";
                    }

                    rawValue = modifiedRaw;
                } else {
                    rawValue = "'" + value + "'";
                }
            }
        }

        rawQuery.append("`").append(tableName).append("`.").append("`").append(rowName).append("` ").append(operation).append(" ").append(rawValue);
        return this;
    }

    /**
     * Set the insert values
     *
     * @param values the values to insert
     * @return the query
     */
    public QueryBuilder values(final Object... values) {
        if (table == null || !queryType.equals("insert")) return this;

        StringBuilder valuesBuilder = new StringBuilder();
        for (Object value : values) {
            if (value instanceof Boolean || value instanceof Number) {
                valuesBuilder.append(value).append(", ");
            } else {
                if (value == null) {
                    valuesBuilder.append("NULL").append(", ");
                } else {
                    if (value instanceof QueryModifier) {
                        QueryModifier modifier = (QueryModifier) value;
                        String modifiedRaw = modifier.getRaw();
                        if (driver.equals(Driver.SQLite) && modifiedRaw.equals("AUTO_INCREMENT")) {
                            modifiedRaw = "AUTOINCREMENT";
                        }

                        valuesBuilder.append(" ").append(modifiedRaw);
                    } else {
                        valuesBuilder.append("'").append(value).append("'").append(", ");
                    }
                }
            }
        }
        String finalValues = StringUtils.replaceLast(valuesBuilder.toString(), ", ", "");
        rawQuery.append(finalValues);

        return this;
    }

    /**
     * Add a row to table creation
     *
     * @param row the row to create
     * @param type the row type
     * @param modifiers the row modifiers
     * @return the query
     */
    public QueryBuilder withRow(final Row row, final RowType<?> type, final QueryModifier... modifiers) {
        if (table == null || !queryType.equals("create") || !table.hasRow(row)) return this;

        StringBuilder customModifiers = new StringBuilder();
        for (QueryModifier modifier : modifiers) {
            String modifiedRaw = modifier.getRaw();
            if (driver.equals(Driver.SQLite)) {
                if (modifiedRaw.equals("AUTO_INCREMENT")) {
                    modifiedRaw = "AUTOINCREMENT";
                }
            }

            if (modifiedRaw.matches("\\([0-9]*\\)") && driver.equals(Driver.SQLite)) {
                continue;
            }

            customModifiers.append(" ").append(modifiedRaw);
        }
        String rawCustom = customModifiers.toString();
        if (firstRow) {
            firstRow = false;
        } else {
            rawQuery.append(", ");
        }

        String rowName = databaseSettings.columnName(table, row);
        RowType<?> modifiedType = type;

        if (driver.equals(Driver.SQLite)) {
            switch (type.toEnumType()) {
                case VARCHAR:
                case LONGTEXT:
                    modifiedType = RowType.TEXT;
                    break;
                case TINY_INTEGER:
                case BIG_INTEGER:
                    modifiedType = RowType.INTEGER;
                    break;
                case LONG:
                case DOUBLE:
                case FLOAT:
                case TIMESTAMP:
                    modifiedType = RowType.NUMERIC;
                    break;
            }
        } else {
            if (type.equals(RowType.NUMERIC)) {
                modifiedType = RowType.DOUBLE;
            }
        }

        rawQuery.append("`").append(rowName).append("` ").append(modifiedType).append(rawCustom);

        return this;
    }

    /**
     * Add a foreign key to the create query
     *
     * @param row the row to append the key to
     * @param target the target row
     * @param modifiers the modifiers of the foreign key
     * @return the query
     */
    public QueryBuilder withForeign(final Row row, final JoinRow target, final QueryModifier... modifiers) {
        if (table == null || !queryType.equals("create") || !table.hasRow(row)) return this;

        StringBuilder customModifiers = new StringBuilder();
        for (QueryModifier modifier : modifiers) {
            String modifiedRaw = modifier.getRaw();
            if (modifiedRaw.contains("ON UPDATE") || modifiedRaw.contains("ON DELETE")) {
                customModifiers.append(modifiedRaw);
            }
        }

        Row targetRow = target.getRow();
        Table targetTable = target.getTable();

        if (targetTable == null) return this;
        String targetTableName = databaseSettings.tableName(targetTable);
        String targetRowName = databaseSettings.columnName(targetTable, targetRow);

        if (firstRow) {
            firstRow = false;
        } else {
            rawQuery.append(", ");
        }

        String rowName = databaseSettings.columnName(table, row);
        rawQuery.append("FOREIGN KEY(").append("`").append(rowName).append("`) ").append("REFERENCES ").append("`").append(targetTableName).append("`").append("(`").append(targetRowName).append("`)").append(customModifiers);

        return this;
    }

    /**
     * Append a primary key definition
     *
     * @param row the row to make primary key
     * @param auto_increment if the row value increments automatically
     * @return the query
     */
    public QueryBuilder withPrimaryKey(final Row row, final boolean auto_increment) {
        if (table == null || !queryType.equals("create") || !table.hasRow(row)) return this;

        if (firstRow) {
            firstRow = false;
        } else {
            rawQuery.append(", ");
        }

        String extra = "";
        if (auto_increment) {
            if (driver.equals(Driver.SQLite)) {
                extra = " AUTOINCREMENT";
            } else {
                extra = " AUTO_INCREMENT";
            }
        }


        String rowName = databaseSettings.columnName(table, row);
        rawQuery.append("PRIMARY KEY(").append("`").append(rowName).append("`").append(extra).append(")");

        return this;
    }

    /**
     * Insert custom query into the query
     *
     * @param raw the raw query to insert
     * @return this builder
     */
    public QueryBuilder withRowCustom(final String raw) {
        if (firstRow) {
            firstRow = false;
        } else {
            rawQuery.append(", ");
        }
        rawQuery.append(raw);

        return this;
    }

    /**
     * Get the query type
     *
     * @return the query type
     */
    public String queryType() {
        return queryType;
    }

    /**
     * Build the query
     *
     * @return the query
     */
    public String build() {
        if (queryType.equals("update") || queryType.equals("fetch") || queryType.equals("alter")) {
            return rawQuery.toString();
        }

        return rawQuery + ")";
    }

    /**
     * Build the query
     *
     * @param terminator the query terminator
     * @return the query
     */
    public String build(final String terminator) {
        if (queryType.equals("update") || queryType.equals("fetch")) {
            return rawQuery.toString();
        }

        return rawQuery + terminator;
    }

    /**
     * Default modifier
     *
     * @param value the default value
     * @return a query modifier
     */
    public static QueryModifier DEFAULT(final QueryModifier value) {
        return QueryModifier.of("DEFAULT " + value.getRaw());
    }

    /**
     * Number modifier
     *
     * @param number the number
     * @return the query number
     */
    public static QueryModifier NUMBER(final long number) {
        return QueryModifier.of(String.valueOf(number));
    }

    /**
     * Current timestamp modifier
     *
     * @param driver the driver to use
     *               current timestamp with. For example
     *               in sqlite is strftime('%s', 'now') * 1000
     * @return the query
     */
    public static QueryModifier CURRENT_TIMESTAMP(final Driver driver) {
        if (driver.equals(Driver.SQLite)) {
            return QueryModifier.of("(strftime('%s', 'now') * 1000)");
        }

        return QueryModifier.of("CURRENT_TIMESTAMP()");
    }

    /**
     * Equals operation
     */
    public final static String EQUALS = "=";
    /**
     * Not equals operation
     */
    public final static String NOT_EQUALS = "<>";
    /**
     * If with true operation
     */
    public final static String IS = "IS";
    /**
     * If with false operation
     */
    public final static String IS_NOT = "IS NOT";

    /**
     * Not in the present values
     *
     * @param values the values
     * @return the operation
     */
    public static String NOT_IN(final Collection<?> values) {
        return NOT_IN(values.toArray());
    }

    /**
     * Not in the present values
     *
     * @param values the values
     * @return the operation
     */
    public static String NOT_IN(final Object... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            builder.append(values[i]);
            if (i != values.length - 1) {
                builder.append(",");
            }
        }

        return "NOT IN(" + builder + ")";
    }

    /**
     * Undefined value
     */
    public final static QueryModifier UNDEFINED = QueryModifier.of("UNDEFINED");
    /**
     * Null value
     */
    public final static QueryModifier NULL = QueryModifier.of("NULL");
    /**
     * Not null value
     */
    public final static QueryModifier NOT_NULL = QueryModifier.of("NOT NULL");
    /**
     * Unique value
     */
    public final static QueryModifier UNIQUE = QueryModifier.of("UNIQUE");
    /**
     * Auto increment key
     */
    public final static QueryModifier AUTO_INCREMENT = QueryModifier.of("AUTO_INCREMENT");
    /**
     * Primary key
     */
    public final static QueryModifier PRIMARY = QueryModifier.of("PRIMARY KEY");
    /**
     * True value
     */
    public final static QueryModifier TRUE = QueryModifier.of("TRUE");
    /**
     * False value
     */
    public final static QueryModifier FALSE = QueryModifier.of("FALSE");
}

