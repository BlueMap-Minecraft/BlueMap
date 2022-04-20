package de.bluecolored.bluemap.common.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ConfigTemplate {

    private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\$\\{([\\w\\-.]+)}"); // ${variable}
    private static final Pattern TEMPLATE_CONDITIONAL = Pattern.compile("\\$\\{([\\w\\-.]+)<<([\\s\\S]*?)>>}"); // ${conditionid<< ... >>}

    private final String template;

    private final Set<String> enabledConditionals;
    private final Map<String, String> variables;

    public ConfigTemplate(String template) {
        this.template = template;
        this.enabledConditionals = new HashSet<>();
        this.variables = new HashMap<>();
    }

    public ConfigTemplate setConditional(String conditional, boolean enabled) {
        if (enabled) enabledConditionals.add(conditional);
        else enabledConditionals.remove(conditional);
        return this;
    }

    public ConfigTemplate setVariable(String variable, String value) {
        if (value == null) variables.remove(variable);
        else variables.put(variable, replacerEscape(value));

        return this;
    }

    public String build() {
        return build(this.template, enabledConditionals::contains, s -> variables.getOrDefault(s, "?"));
    }

    private String build(String template, Predicate<? super String> conditionalResolver, Function<? super String, String> variableResolver) {
        String result = TEMPLATE_CONDITIONAL.matcher(template).replaceAll(match ->
                conditionalResolver.test(match.group(1)) ? replacerEscape(build(match.group(2), conditionalResolver, variableResolver)) : ""
        );
        return TEMPLATE_VARIABLE.matcher(result).replaceAll(match -> variableResolver.apply(match.group(1)));
    }

    private String replacerEscape(String raw) {
        return raw
                .replace("\\", "\\\\")
                .replace("$", "\\$");
    }

}
