package de.keksuccino.konkrete.util.terminal;

/**
 * Captures a completed command's process status and standard output.
 *
 * @param exitCode process exit status
 * @param output captured standard output
 */
public record CommandResult(int exitCode, String output) {

}
