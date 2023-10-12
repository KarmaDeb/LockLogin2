package es.karmadev.locklogin.common.api.user.storage.account.transiction;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.AccountField;
import es.karmadev.locklogin.api.user.account.migration.AccountMigrator;
import es.karmadev.locklogin.api.user.account.migration.Transitional;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.user.storage.account.CAccount;
import es.karmadev.locklogin.common.api.user.storage.account.CAccountFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CMigrator implements AccountMigrator<CAccount> {

    private final CAccountFactory factory;

    public CMigrator(final CAccountFactory factory) {
        this.factory = factory;
    }

    /**
     * Migrate an account
     *
     * @param owner           the account owner
     * @param transictionable the transictionable account
     * @param ignore          the fields to ignore
     * @return the migrated account
     */
    @Override
    public CAccount migrate(final LocalNetworkClient owner, final Transitional transictionable, final AccountField... ignore) {
        CAccount account = (CAccount) owner.account();
        UserSession session = owner.session();

        List<AccountField> fields = Arrays.stream(AccountField.values()).filter((field) ->
                !Arrays.asList(ignore).contains(field)).collect(Collectors.toList());

        if (account == null || account.id() <= 0) {
            account = factory.create(owner);
        }

        if (account != null) {
            for (AccountField userField : fields) {
                switch (userField) {
                    case USERNAME:
                        if (!ObjectUtils.isNullOrEmpty(transictionable.player())) account.writeStringValue(AccountField.USERNAME, transictionable.player());
                        break;
                    case UNIQUEID:
                        if (transictionable.uniqueId() != null) account.writeStringValue(AccountField.UNIQUEID, transictionable.uniqueId().toString());
                        break;
                    case PASSWORD:
                        if (transictionable.hasPassword()) account.writeHashField(AccountField.PASSWORD, transictionable.password());
                        break;
                    case PIN:
                        if (transictionable.hasPin()) account.writeHashField(AccountField.PASSWORD, transictionable.pin());
                        break;
                    case TOKEN_TOTP:
                        if (transictionable.isTotpSet()) account.writeStringValue(AccountField.TOKEN_TOTP, transictionable.totp());
                        break;
                    case PANIC:
                        if (transictionable.hasPanic()) account.writeHashField(AccountField.PASSWORD, transictionable.panic());
                        break;
                    case STATUS_TOTP:
                        account.writeBooleanValue(AccountField.STATUS_TOTP, transictionable.hasTotp());
                        break;
                    case SESSION_PERSISTENCE:
                        if (session != null) session.persistent(transictionable.sessionPersistent());
                        break;
                }
            }
        }

        return account;
    }

    /**
     * Export an account into a transictionable
     * account format
     *
     * @param account the account to export
     * @return the transictionable account
     */
    @Override
    public Transitional export(final CAccount account) {
        int owner_id = account.ownerId();
        LockLogin plugin = CurrentPlugin.getPlugin();
        PluginNetwork network = plugin.network();

        LocalNetworkClient client = network.getEntity(owner_id);
        if (client != null) {
            return CTransitional.from(client);
        }

        return null;
    }
}
