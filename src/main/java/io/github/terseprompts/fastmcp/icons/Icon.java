package io.github.terseprompts.fastmcp.icons;

import lombok.Value;

import java.util.List;

/**
 * Represents an icon as defined by the MCP specification.
 * Icons provide visual identifiers for servers, tools, resources, and prompts.
 */
@Value
public class Icon {
    /**
     * URI pointing to the icon resource (required).
     * Can be:
     * - An HTTP/HTTPS URL pointing to an image file
     * - A data URI with base64-encoded image data
     */
    String src;

    /**
     * Optional MIME type of the icon.
     * Common values: image/png, image/jpeg, image/svg+xml, image/webp
     */
    String mimeType;

    /**
     * Optional array of size specifications.
     * Examples:
     * - ["48x48"] - single fixed size
     * - ["any"] - scalable format like SVG
     * - ["48x48", "96x96"] - multiple sizes
     */
    List<String> sizes;

    /**
     * Optional theme preference for the icon background.
     * Valid values: "light" or "dark"
     */
    String theme;
}
