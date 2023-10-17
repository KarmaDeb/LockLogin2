package es.karmadev.locklogin.api.extension.module;

import es.karmadev.api.security.PermissionManager;
import es.karmadev.locklogin.api.extension.module.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.module.exception.InvalidDescriptionException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a module description
 */
@Getter
@Setter(value = AccessLevel.PACKAGE)
public class ModuleDescription {

    protected Module module;
    protected String name;
    protected String version;
    protected String description;
    protected String author;
    protected String main;
    protected List<String> depends = new ArrayList<>();
    protected List<String> optDepends = new ArrayList<>();
    protected Map<String, Object> yaml;

    protected ModuleDescription() {}

    public ModuleDescription(final InputStream stream) throws YAMLException, InvalidDescriptionException {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(stream);

        if (!data.containsKey("name")) {
            throw new InvalidDescriptionException("Missing name from description");
        }
        if (!data.containsKey("version")) {
            throw new InvalidDescriptionException("Missing version from description");
        }
        if (!data.containsKey("description")) {
            throw new InvalidDescriptionException("Missing description from description");
        }
        if (!data.containsKey("author")) {
            throw new InvalidDescriptionException("Missing author from description");
        }
        if (!data.containsKey("main")) {
            throw new InvalidDescriptionException("Missing main class from description");
        }

        if (data.containsKey("depends")) {
            Object value = data.get("depends");
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> depends = (Map<String, Object>) value;

                if (depends.containsKey("required")) {
                    Object requiredValue = depends.get("required");
                    if (requiredValue instanceof List) {
                        List<?> list = (List<?>) requiredValue;
                        list.forEach((element) -> this.depends.add(String.valueOf(element)));
                    }
                }
                if (depends.containsKey("optional")) {
                    Object optionalValue = depends.get("optional");
                    if (optionalValue instanceof List) {
                        List<?> list = (List<?>) optionalValue;
                        list.forEach((element) -> this.optDepends.add(String.valueOf(element)));
                    }
                }
            }
        }

        name = String.valueOf(data.get("name"));
        version = String.valueOf(data.get("version"));
        description = String.valueOf(data.get("description"));
        author = String.valueOf(data.get("author"));
        main = String.valueOf(data.get("main"));

        this.yaml = data;
    }
}
