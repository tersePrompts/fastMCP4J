package io.github.terseprompts.fastmcp.adapter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ResourceTemplateMatcher}.
 */
class ResourceTemplateMatcherTest {

    @Test
    void testIsTemplate() {
        assertTrue(ResourceTemplateMatcher.isTemplate("user://profile/{name}"));
        assertTrue(ResourceTemplateMatcher.isTemplate("{scheme}://{host}/path"));
        assertFalse(ResourceTemplateMatcher.isTemplate("file:///static/file.txt"));
        assertFalse(ResourceTemplateMatcher.isTemplate(null));
    }

    @Test
    void testTemplateParams() {
        assertEquals(List.of("name"), ResourceTemplateMatcher.templateParams("user://profile/{name}"));
        assertEquals(List.of("scheme", "host", "y"),
                ResourceTemplateMatcher.templateParams("{scheme}://{host}/x/{y}"));
        assertEquals(List.of(), ResourceTemplateMatcher.templateParams("file:///static/file.txt"));
        assertEquals(List.of(), ResourceTemplateMatcher.templateParams(null));
    }

    @Test
    void testSimpleTemplate() {
        Map<String, String> params = ResourceTemplateMatcher.match("user://profile/{name}", "user://profile/ford");
        assertEquals(Map.of("name", "ford"), params);
    }

    @Test
    void testMultipleParams() {
        Map<String, String> params = ResourceTemplateMatcher.match(
                "docs://{tenant}/reports/{year}/{month}",
                "docs://acme/reports/2026/09");
        assertEquals(Map.of("tenant", "acme", "year", "2026", "month", "09"), params);
    }

    @Test
    void testNoParamsMatchesOnlyExactUri() {
        assertEquals(Map.of(), ResourceTemplateMatcher.match("file:///static/file.txt", "file:///static/file.txt"));
        assertEquals(Map.of(), ResourceTemplateMatcher.match("file:///static/file.txt", "file:///static/other.txt"),
            "Template without params must only match the exact URI");
    }

    @Test
    void testTrailingContent() {
        Map<String, String> params = ResourceTemplateMatcher.match("logs://app/{app}/tail", "logs://app/web/tail");
        assertEquals(Map.of("app", "web"), params);
    }

    @Test
    void testNonMatchingUriReturnsEmpty() {
        assertEquals(Map.of(), ResourceTemplateMatcher.match("user://profile/{name}", "user://posts/123"));
        assertEquals(Map.of(), ResourceTemplateMatcher.match("user://profile/{name}", "file:///etc/passwd"));
        assertEquals(Map.of(), ResourceTemplateMatcher.match("user://profile/{name}", null));
        assertEquals(Map.of(), ResourceTemplateMatcher.match(null, "user://profile/ford"));
    }

    @Test
    void testSpecialCharactersAreLiteral() {
        // Dots, colons and slashes in the literal parts must not be interpreted as regex.
        Map<String, String> params = ResourceTemplateMatcher.match("a.b://c/{v}.txt", "a.b://c/data.txt");
        assertEquals(Map.of("v", "data"), params);
        assertEquals(Map.of(), ResourceTemplateMatcher.match("a.b://c/{v}.txt", "axb://c/data.txt"));
    }
}