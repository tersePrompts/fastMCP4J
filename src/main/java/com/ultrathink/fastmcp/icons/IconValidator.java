package com.ultrathink.fastmcp.icons;

import com.ultrathink.fastmcp.exception.FastMcpException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates Icon objects according to MCP specification security requirements.
 */
public class IconValidator {

    // Required MIME types that clients MUST support
    private static final Set<String> REQUIRED_MIME_TYPES = new HashSet<>(Arrays.asList(
        "image/png",
        "image/jpeg",
        "image/jpg"
    ));

    // Recommended MIME types that clients SHOULD support
    private static final Set<String> RECOMMENDED_MIME_TYPES = new HashSet<>(Arrays.asList(
        "image/svg+xml",
        "image/webp"
    ));

    // All allowed MIME types
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>() {{
        addAll(REQUIRED_MIME_TYPES);
        addAll(RECOMMENDED_MIME_TYPES);
    }};

    // Allowed URI schemes
    private static final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList(
        "https",
        "data"
    ));

    // Forbidden schemes
    private static final Set<String> FORBIDDEN_SCHEMES = new HashSet<>(Arrays.asList(
        "javascript",
        "file",
        "ftp",
        "ws",
        "wss"
    ));

    // Theme validation
    private static final Set<String> VALID_THEMES = new HashSet<>(Arrays.asList(
        "light",
        "dark"
    ));

    // Size format pattern: either "any" or "WxH" where W and H are positive integers
    private static final Pattern SIZE_PATTERN = Pattern.compile("^(any|\\d+x\\d+)$");

    /**
     * Validates an Icon object.
     *
     * @param icon the icon to validate
     * @throws FastMcpException if validation fails
     */
    public static void validate(Icon icon) throws FastMcpException {
        if (icon == null) {
            throw new FastMcpException("Icon cannot be null");
        }

        if (icon.getSrc() == null || icon.getSrc().trim().isEmpty()) {
            throw new FastMcpException("Icon src cannot be null or empty");
        }

        validateSrc(icon.getSrc());
        validateMimeType(icon.getMimeType());
        validateSizes(icon.getSizes());
        validateTheme(icon.getTheme());
    }

    /**
     * Validates the icon URI according to security requirements.
     */
    private static void validateSrc(String src) throws FastMcpException {
        try {
            URI uri = new URI(src);
            String scheme = uri.getScheme();

            if (scheme == null) {
                throw new FastMcpException("Icon URI must have a scheme");
            }

            String schemeLower = scheme.toLowerCase();

            // Check for forbidden schemes
            if (FORBIDDEN_SCHEMES.contains(schemeLower)) {
                throw new FastMcpException("Icon URI uses forbidden scheme: " + schemeLower);
            }

            // Only allow https and data schemes
            if (!ALLOWED_SCHEMES.contains(schemeLower)) {
                throw new FastMcpException("Icon URI must use https or data scheme, got: " + schemeLower);
            }

            // For data URIs, do additional validation
            if (schemeLower.equals("data")) {
                validateDataUri(src);
            }

        } catch (URISyntaxException e) {
            throw new FastMcpException("Invalid icon URI: " + src, e);
        }
    }

    /**
     * Validates data URI format.
     */
    private static void validateDataUri(String src) throws FastMcpException {
        // Data URIs should follow: data:[<mediatype>][;base64],<data>
        if (!src.startsWith("data:")) {
            throw new FastMcpException("Invalid data URI format");
        }

        int commaIndex = src.indexOf(',');
        if (commaIndex == -1 || commaIndex == 5) {
            throw new FastMcpException("Invalid data URI: missing data part");
        }

        // Extract and validate media type if present
        String metaPart = src.substring(5, commaIndex);
        if (!metaPart.isEmpty() && !metaPart.startsWith("image/")) {
            throw new FastMcpException("Data URI media type must be an image type");
        }
    }

    /**
     * Validates the MIME type.
     */
    private static void validateMimeType(String mimeType) throws FastMcpException {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            // MIME type is optional
            return;
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new FastMcpException("Unsupported MIME type: " + mimeType + 
                ". Supported types: " + ALLOWED_MIME_TYPES);
        }
    }

    /**
     * Validates the sizes array.
     */
    private static void validateSizes(java.util.List<String> sizes) throws FastMcpException {
        if (sizes == null || sizes.isEmpty()) {
            return; // sizes is optional
        }

        for (String size : sizes) {
            if (size == null || !SIZE_PATTERN.matcher(size).matches()) {
                throw new FastMcpException("Invalid size format: " + size + 
                    ". Expected 'any' or 'WxH' format (e.g., '48x48')");
            }
        }
    }

    /**
     * Validates the theme value.
     */
    private static void validateTheme(String theme) throws FastMcpException {
        if (theme == null || theme.trim().isEmpty()) {
            return; // theme is optional
        }

        if (!VALID_THEMES.contains(theme.toLowerCase())) {
            throw new FastMcpException("Invalid theme: " + theme + 
                ". Valid values: " + VALID_THEMES);
        }
    }
}
