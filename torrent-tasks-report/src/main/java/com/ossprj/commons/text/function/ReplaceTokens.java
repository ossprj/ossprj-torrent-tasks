package com.ossprj.commons.text.function;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplaceTokens implements BiFunction<String, Map<String, String>, String> {

    public static final Pattern pattern = Pattern.compile("\\{(.+?)\\}");

    @Override
    public String apply(final String template, final Map<String, String> tokenValues) {

        final Matcher matcher = pattern.matcher(template);
        final StringBuffer output = new StringBuffer();

        while (matcher.find()) {
            final String replacement = tokenValues.get(matcher.group(1));
            if (replacement != null) {
                // matcher.appendReplacement(buffer, replacement);
                // see comment
                matcher.appendReplacement(output, "");
                output.append(replacement);
            }
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
