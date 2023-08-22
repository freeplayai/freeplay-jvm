package ai.freeplay.client.internal;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateUtils {

    private static final Pattern pattern = Pattern.compile("\\{\\{(\\w+)}}");

    public static String format(String template, Map<String, Object> variables) {
        Matcher matcher = pattern.matcher(template);
        String formatted = matcher.replaceAll((MatchResult result) -> {
            String name = result.group(1);
            Object value = variables.get(name);

            return value != null ? String.valueOf(value) : "";
        });
        return formatted.replace("\n", "\\\\n").replace("\r", "\\\\r");
    }
}
