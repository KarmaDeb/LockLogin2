package es.karmadev.locklogin.api.plugin.database.schema;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * Simple row types
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class RowType<T> {

    /**
     * Varchar
     */
    public final static RowType<String> VARCHAR = new RowType<>(String.class, "varchar", RowEnum.VARCHAR);
    /**
     * Tiny integer
     */
    public final static RowType<Byte> TINY_INTEGER = new RowType<>(byte.class, "tinyint", RowEnum.TINY_INTEGER);
    /**
     * Number
     */
    public final static RowType<Short> INTEGER = new RowType<>(short.class, "integer", RowEnum.INTEGER);

    /**
     * Big integer
     */
    public final static RowType<Integer> BIG_INTEGER = new RowType<>(int.class, "bigint", RowEnum.BIG_INTEGER);

    /**
     * Double value
     */
    public final static RowType<Double> DOUBLE = new RowType<>(double.class, "double", RowEnum.DOUBLE);

    /**
     * Long value
     */
    public final static RowType<Long> LONG = new RowType<>(long.class, "long", RowEnum.LONG);

    /**
     * MySQL timestamp
     */
    public final static RowType<Instant> TIMESTAMP = new RowType<>(Instant.class, "timestamp", RowEnum.TIMESTAMP);

    /**
     * Float value
     */
    public final static RowType<Float> FLOAT = new RowType<>(float.class, "float", RowEnum.FLOAT);

    /**
     * Any number
     */
    public final static RowType<Number> NUMERIC = new RowType<>(Number.class, "numeric", RowEnum.NUMERIC);

    /**
     * Text
     */
    public final static RowType<CharSequence> TEXT = new RowType<>(CharSequence.class, "text", RowEnum.TEXT);

    /**
     * A long text
     */
    public final static RowType<CharSequence> LONGTEXT = new RowType<>(CharSequence.class, "longtext", RowEnum.LONGTEXT);

    /**
     * Blob
     */
    public final static RowType<CharSequence> BLOB = new RowType<>(CharSequence.class, "blob", RowEnum.BLOB);

    /**
     * Boolean
     */
    public final static RowType<Boolean> BOOLEAN = new RowType<>(boolean.class, "boolean", RowEnum.BOOLEAN);

    /**
     * Create a new row type
     *
     * @param type the type
     * @param name the type name
     * @return the type
     * @param <T> the row type
     */
    public static <T> RowType<T> custom(final Class<T> type, final String name) {
        return new RowType<>(type, name, RowEnum.CUSTOM);
    }

    public final static RowType<?>[] values = new RowType[]{
            VARCHAR,
            TINY_INTEGER,
            INTEGER,
            BIG_INTEGER,
            DOUBLE,
            LONG,
            TIMESTAMP,
            FLOAT,
            NUMERIC,
            TEXT,
            LONGTEXT,
            BLOB,
            BOOLEAN
    };

    private final Class<T> type;
    private final String name;
    private final RowEnum rowEnum;

    /**
     * Get the type name
     *
     * @return the type name
     */
    public String name() {
        return name;
    }

    /**
     * Get the row type as an enumeration
     *
     * @return the row type enumeration
     */
    public RowEnum toEnumType() {
        return rowEnum;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     * @apiNote In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * The string output is not necessarily stable over time or across
     * JVM invocations.
     * @implSpec The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Get the row type from its name
     *
     * @param name the row name
     * @return the row type
     */
    public static RowType<?> valueOf(final String name) {
        for (RowType<?> type : values) {
            if (type.name.equalsIgnoreCase(name)) return type;

            try {
                RowEnum e = RowEnum.valueOf(name.toUpperCase());
                if (type.rowEnum.equals(e)) return type;
            } catch (IllegalArgumentException ignored) {}
        }

        throw new IllegalArgumentException("No row type constant known as " + name);
    }

    public enum RowEnum {
        VARCHAR,
        /**
         * Tiny integer
         */
        TINY_INTEGER,
        /**
         * Number
         */
        INTEGER,
        /**
         * Big integer
         */
        BIG_INTEGER,
        /**
         * Double value
         */
        DOUBLE,
        /**
         * Long value
         */
        LONG,
        /**
         * MySQL timestamp
         */
        TIMESTAMP,
        /**
         * Float value
         */
        FLOAT,
        /**
         * Any number
         */
        NUMERIC,
        /**
         * Text
         */
        TEXT,

        /**
         * A long text
         */
        LONGTEXT,
        /**
         * Blob
         */
        BLOB,
        /**
         * Boolean
         */
        BOOLEAN,
        /**
         * Custom row
         */
        CUSTOM
    }
}
