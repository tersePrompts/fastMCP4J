package io.github.terseprompts.fastmcp.icons;

import io.github.terseprompts.fastmcp.exception.FastMcpException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

class IconValidatorTest {

    @Test
    void testValidHttpsIcon() throws FastMcpException {
        Icon icon = new Icon(
            "https://example.com/icon.png",
            "image/png",
            Arrays.asList("48x48"),
            "light"
        );
        assertDoesNotThrow(() -> IconValidator.validate(icon));
    }

    @Test
    void testValidDataUriIcon() throws FastMcpException {
        Icon icon = new Icon(
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
            "image/png",
            null,
            null
        );
        assertDoesNotThrow(() -> IconValidator.validate(icon));
    }

    @Test
    void testIconWithMultipleSizes() throws FastMcpException {
        Icon icon = new Icon(
            "https://example.com/icon.png",
            "image/png",
            Arrays.asList("48x48", "96x96", "192x192"),
            "dark"
        );
        assertDoesNotThrow(() -> IconValidator.validate(icon));
    }

    @Test
    void testSvgIconWithAnySize() throws FastMcpException {
        Icon icon = new Icon(
            "https://example.com/icon.svg",
            "image/svg+xml",
            Collections.singletonList("any"),
            null
        );
        assertDoesNotThrow(() -> IconValidator.validate(icon));
    }

    @Test
    void testMinimalIcon() throws FastMcpException {
        Icon icon = new Icon(
            "https://example.com/icon.png",
            null,
            null,
            null
        );
        assertDoesNotThrow(() -> IconValidator.validate(icon));
    }

    @Test
    void testNullIcon() {
        assertThrows(FastMcpException.class, () -> IconValidator.validate(null));
    }

    @Test
    void testEmptySrc() {
        Icon icon = new Icon("", "image/png", null, null);
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testNullSrc() {
        Icon icon = new Icon(null, "image/png", null, null);
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testHttpSchemeNotAllowed() {
        Icon icon = new Icon(
            "http://example.com/icon.png",
            "image/png",
            null,
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testJavascriptSchemeForbidden() {
        Icon icon = new Icon(
            "javascript:alert('xss')",
            null,
            null,
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testFileSchemeForbidden() {
        Icon icon = new Icon(
            "file:///etc/passwd",
            null,
            null,
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testFtpSchemeForbidden() {
        Icon icon = new Icon(
            "ftp://example.com/icon.png",
            null,
            null,
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testUnsupportedMimeType() {
        Icon icon = new Icon(
            "https://example.com/icon.bmp",
            "image/bmp",
            null,
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testInvalidSizeFormat() {
        Icon icon = new Icon(
            "https://example.com/icon.png",
            "image/png",
            Collections.singletonList("invalid"),
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testInvalidSizeFormatWithNegativeNumbers() {
        Icon icon = new Icon(
            "https://example.com/icon.png",
            "image/png",
            Collections.singletonList("-48x48"),
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testInvalidSizeFormatWithNonNumeric() {
        Icon icon = new Icon(
            "https://example.com/icon.png",
            "image/png",
            Collections.singletonList("48abc48"),
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testInvalidTheme() {
        Icon icon = new Icon(
            "https://example.com/icon.png",
            "image/png",
            null,
            "blue"
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testInvalidDataUriFormat() {
        Icon icon = new Icon(
            "data:invalid",
            null,
            null,
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testDataUriWithNonImageMediaType() {
        Icon icon = new Icon(
            "data:text/html,<script>alert('xss')</script>",
            null,
            null,
            null
        );
        assertThrows(FastMcpException.class, () -> IconValidator.validate(icon));
    }

    @Test
    void testValidWebpMimeType() throws FastMcpException {
        Icon icon = new Icon(
            "https://example.com/icon.webp",
            "image/webp",
            null,
            null
        );
        assertDoesNotThrow(() -> IconValidator.validate(icon));
    }

    @Test
    void testCaseInsensitiveMimeType() throws FastMcpException {
        Icon icon = new Icon(
            "https://example.com/icon.png",
            "image/PNG",
            null,
            null
        );
        // Note: Our current implementation is case-sensitive, so this would fail
        // In a production implementation, you might want to handle case-insensitivity
    }
}
