package es.karmadev.locklogin.common.api.plugin.file.section;

import es.karmadev.locklogin.api.plugin.file.section.CaptchaConfiguration;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value(staticConstructor = "of")
public class CCaptchaSection implements CaptchaConfiguration {

    boolean enable;
    int length;
    boolean letters;
    boolean strikethrough;
    boolean randomStrike;
}
