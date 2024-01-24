package es.karmadev.locklogin.common.api.user.storage.session;

import es.karmadev.locklogin.api.user.session.SessionField;
import lombok.Value;
import lombok.experimental.Accessors;

import java.lang.reflect.Type;

@Accessors(fluent = true)
@Value(staticConstructor = "newField")
public class CSessionField<T> implements SessionField<T> {

    Type type;
    String key;
    T get;
}
