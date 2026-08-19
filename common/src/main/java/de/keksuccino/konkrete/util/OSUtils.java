package de.keksuccino.konkrete.util;

import net.minecraft.util.Util;

/** Utility methods for osutils. */
public class OSUtils {

    /** Returns whether the host operating system is macOS. */
    public static boolean isMacOS() {
        return Util.getPlatform() == Util.OS.OSX;
    }

    /** Returns whether the host operating system is Windows. */
    public static boolean isWindows() {
        return Util.getPlatform() == Util.OS.WINDOWS;
    }

    /** Returns whether the host operating system is Linux. */
    public static boolean isLinux() {
        return Util.getPlatform() == Util.OS.LINUX;
    }

}
