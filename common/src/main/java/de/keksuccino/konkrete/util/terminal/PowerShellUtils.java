package de.keksuccino.konkrete.util.terminal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Locates and starts PowerShell processes. */
public class PowerShellUtils {

    /** Locates power shell. */
    public static String locatePowerShell() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            systemRoot = System.getenv("WINDIR");
        }
        if (systemRoot != null && !systemRoot.isBlank()) {
            Path ps = Paths.get(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
            if (Files.isRegularFile(ps)) {
                return ps.toString();
            }
        }
        return "powershell.exe";
    }

}
