package es.karmadev.locklogin.common.api.web.license.data;

import es.karmadev.locklogin.api.plugin.license.data.LicenseOwner;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CLicenseOwner implements LicenseOwner {

    String name;
    String contact;
}
