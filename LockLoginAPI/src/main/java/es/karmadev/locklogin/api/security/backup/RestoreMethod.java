package es.karmadev.locklogin.api.security.backup;

import es.karmadev.locklogin.api.user.account.AccountField;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backup restore method
 */
@SuppressWarnings("unused")
public enum RestoreMethod {
    /**
     * Restore only whitelisted fields
     */
    FIELD_WHITELIST,
    /**
     * Restore only non blacklisted fields
     */
    FIELD_BLACKLIST,
    /**
     * Restore everything
     */
    EVERYTHING(AccountField.values()),
    /**
     * Restore the non set values (default)
     */
    NON_SET;

    private final Set<AccountField> fields = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Initialize the restore method
     *
     * @param d the method fields
     */
    RestoreMethod(final AccountField... d) {
        fields.addAll(Arrays.asList(d));
    }

    /**
     * Set the fields of the method
     *
     * @param fields the fields
     * @return this instance
     */
    public RestoreMethod addFields(final AccountField... fields) {
        if (this.equals(NON_SET) || this.equals(EVERYTHING)) return this; //This should be handled by the BackupService

        this.fields.addAll(Arrays.asList(fields));
        return this;
    }

    /**
     * Get if the method contains the field
     *
     * @param field the field
     * @return if the method has the field
     */
    public boolean hasField(final AccountField field) {
        return fields.contains(field);
    }

    /**
     * Get all the fields
     *
     * @return the fields
     */
    public AccountField[] getFields() {
        return fields.toArray(new AccountField[0]);
    }

    /**
     * Get all the fields not in
     * the fields
     *
     * @return all the fields not in the fields
     */
    public AccountField[] getFieldsReverse() {
        AccountField[] f = new AccountField[AccountField.values().length - fields.size()];
        int index = 0;
        for (AccountField field : AccountField.values()) {
            if (!fields.contains(field)) {
                f[index++] = field;
            }
        }

        return f;
    }
}
