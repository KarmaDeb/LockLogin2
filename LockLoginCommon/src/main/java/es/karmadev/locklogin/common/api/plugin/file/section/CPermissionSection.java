package es.karmadev.locklogin.common.api.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.PermissionConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CPermissionSection implements PermissionConfiguration {

    boolean blockOperator;
    boolean removeEveryPermission;
    boolean allowWildcard;

    String[] unLoggedGrants;
    String[] loggedGrants;
}
