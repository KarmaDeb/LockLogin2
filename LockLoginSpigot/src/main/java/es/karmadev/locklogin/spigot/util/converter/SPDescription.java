package es.karmadev.locklogin.spigot.util.converter;

import es.karmadev.api.strings.ListSpacer;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.extension.module.ModuleDescription;
import es.karmadev.locklogin.api.extension.module.exception.InvalidDescriptionException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;

public class SPDescription extends ModuleDescription {

    private SPDescription(final InputStream stream) throws YAMLException, InvalidDescriptionException {
        super(stream);
    }

    SPDescription(final SpigotModule module) {
        super();
        this.module = module;
        this.name = module.getPlugin().getName();
        this.version = module.getPlugin().getDescription().getVersion();
        this.description = module.getPlugin().getDescription().getDescription();
        this.author = StringUtils.listToString(module.getPlugin().getDescription().getAuthors(), ListSpacer.COMMA);
        this.main = module.getPlugin().getDescription().getMain();
    }
}
