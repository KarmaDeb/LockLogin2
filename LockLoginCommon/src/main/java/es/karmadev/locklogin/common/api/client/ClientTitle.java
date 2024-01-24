package es.karmadev.locklogin.common.api.client;

import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Client title wrapper
 */
@Accessors(fluent = true)
@Value(staticConstructor = "on")
public class ClientTitle {

    String title;
    String subtitle;

    int fadeIn;
    int show;
    int fadeOut;
}
