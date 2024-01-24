package es.karmadev.locklogin.common.api.user.auth;

import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "forResult")
public class CAuthProcess implements AuthProcess {

    boolean wasSuccess;
    UserAuthProcess authProcess;
}
