package com.ultrathink.fastmcp.mcptools.bash;

/**
 * Detects the current operating system for shell command selection.
 */
public final class OsDetector {

    private static final OsType CURRENT_OS = detectOs();
    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase();

    private OsDetector() {
        // Utility class
    }

    /**
     * Detect the current operating system.
     */
    private static OsType detectOs() {
        if (OS_NAME.contains("win")) {
            return OsType.WINDOWS;
        } else if (OS_NAME.contains("mac") || OS_NAME.contains("darwin")) {
            return OsType.MACOS;
        } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix")) {
            return OsType.LINUX;
        }
        return OsType.UNKNOWN;
    }

    /**
     * Get the current operating system type.
     */
    public static OsType getOsType() {
        return CURRENT_OS;
    }

    /**
     * Check if running on Windows.
     */
    public static boolean isWindows() {
        return CURRENT_OS == OsType.WINDOWS;
    }

    /**
     * Check if running on Linux.
     */
    public static boolean isLinux() {
        return CURRENT_OS == OsType.LINUX;
    }

    /**
     * Check if running on macOS.
     */
    public static boolean isMacOS() {
        return CURRENT_OS == OsType.MACOS;
    }

    /**
     * Check if running on a Unix-like system (Linux or macOS).
     */
    public static boolean isUnix() {
        return CURRENT_OS == OsType.LINUX || CURRENT_OS == OsType.MACOS;
    }

    /**
     * Get the default shell command for the current OS.
     * @return shell command array (command + args)
     */
    public static String[] getDefaultShell() {
        return switch (CURRENT_OS) {
            case WINDOWS -> new String[]{"cmd.exe", "/c"};
            case MACOS -> new String[]{"/bin/zsh", "-c"};
            case LINUX -> new String[]{"/bin/bash", "-c"};
            default -> new String[]{"/bin/sh", "-c"};
        };
    }

    /**
     * Get the shell name for display purposes.
     */
    public static String getShellName() {
        return switch (CURRENT_OS) {
            case WINDOWS -> "cmd.exe";
            case MACOS -> "/bin/zsh";
            case LINUX -> "/bin/bash";
            default -> "/bin/sh";
        };
    }
}
