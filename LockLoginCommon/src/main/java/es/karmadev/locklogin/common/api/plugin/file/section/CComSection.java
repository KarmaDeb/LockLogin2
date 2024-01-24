package es.karmadev.locklogin.common.api.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.CommunicationConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CComSection implements CommunicationConfiguration {

    String host;
    int port;
    boolean useSSL;
}
