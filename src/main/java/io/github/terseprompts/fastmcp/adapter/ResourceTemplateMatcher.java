package io.github.terseprompts.fastmcp.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches concrete resource URIs against template URIs containing {@code {param}}
 * placeholders, e.g. {@code user://profile/{name}}.
 *
 * <p>Used for dynamic resource templates: the concrete requested URI (e.g.
 * {@code user://profile/ford}) is matched against a template and the extracted
 * parameters are bound to the resource method's parameters.
 */
public final class ResourceTemplateMatcher {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    private ResourceTemplateMatcher() {
    }

    /**
     * @return true when the URI contains at least one {@code {param}} placeholder.
     */
    public static boolean isTemplate(String uri) {
        return uri != null && PLACEHOLDER.matcher(uri).find();
    }

    /**
     * Extract the placeholder names in order of appearance.
     *
     * @param uri a template URI such as {@code user://{domain}/profile/{name}}
     * @return the list of parameter names appearing in the template
     */
    public static List<String> templateParams(String uri) {
        List<String> params = new ArrayList<>();
        if (uri == null) {
            return params;
        }
        Matcher matcher = PLACEHOLDER.matcher(uri);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }
        return params;
    }

    /**
     * Match a concrete URI against a template URI.
     *
     * @param uriTemplate the template, e.g. {@code user://profile/{name}}
     * @param concreteUri the concrete URI, e.g. {@code user://profile/ford}
     * @return a map of placeholder name to extracted value; an empty map when the
     *         concrete URI does not fully match the template
     */
    public static Map<String, String> match(String uriTemplate, String concreteUri) {
        if (uriTemplate == null || concreteUri == null) {
            return Map.of();
        }
        List<String> names = new ArrayList<>();
        StringBuilder regex = new StringBuilder();
        Matcher matcher = PLACEHOLDER.matcher(uriTemplate);
        int last = 0;
        while (matcher.find()) {
            regex.append(Pattern.quote(uriTemplate.substring(last, matcher.start())));
            names.add(matcher.group(1));
            regex.append("([^/]+)");
            last = matcher.end();
        }
        regex.append(Pattern.quote(uriTemplate.substring(last)));

        Matcher uriMatcher = Pattern.compile(regex.toString()).matcher(concreteUri);
        if (!uriMatcher.matches()) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            result.put(names.get(i), uriMatcher.group(i + 1));
        }
        return result;
    }
}