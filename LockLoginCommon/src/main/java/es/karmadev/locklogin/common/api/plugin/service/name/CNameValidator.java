package es.karmadev.locklogin.common.api.plugin.service.name;

import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.service.name.NameValidator;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CNameValidator implements NameValidator {

    boolean grantedThroughService = false;

    private final String name;
    private boolean valid = true;
    private char[] invalid;

    CNameValidator(final String name) {
        this.name = name;
    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "Name Validator";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return true;
    }

    /**
     * Validate the name
     */
    @Override
    public void validate() {
        if (!grantedThroughService) return;

        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();

        Pattern pattern = configuration.namePattern();
        Matcher matcher = pattern.matcher(name);

        if (matcher.matches()) {
            valid = true;
            return;
        }

        String unMatched = matcher.replaceAll("");
        Set<Character> invalid = new LinkedHashSet<>();
        for (char character : unMatched.toCharArray())
            invalid.add(character);

        char[] chars = new char[invalid.size()];
        int index = 0;
        for (Character character : invalid) chars[index++] = character;

        this.invalid = chars;
    }

    /**
     * Get if the name is valid
     *
     * @return if the name is valid
     */
    @Override
    public boolean isValid() {
        return grantedThroughService && valid;
    }

    /**
     * Get the name invalid characters
     *
     * @return the invalid characters
     */
    @Override
    public String invalidCharacters() {
        if (!grantedThroughService || invalid == null) return "";
        StringBuilder builder = new StringBuilder();

        if (name.length() > 16) {
            int overSize = name.length() - 16;
            builder.append("&cName too long (").append(overSize).append(")&7, ");
        }
        if (name.length() < 3) {
            int needed = 3 - name.length();
            builder.append("&cName too short (").append(needed).append(")&7, ");
        }

        for (Character character : invalid) {
            String value = String.valueOf(character);

            if (Character.isSpaceChar(character)) {
                value = "spaces";
            }
            if (character == ',') {
                value = "commas";
            }

            builder.append("&c").append(value).append("&7, ");
        }

        return StringUtils.replaceLast(builder.toString(), "&7, ", "");
    }
}
