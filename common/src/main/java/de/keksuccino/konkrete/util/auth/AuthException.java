package de.keksuccino.konkrete.util.auth;

/**
 * Signals an authentication failure while preserving an optional underlying cause.
 */
public class AuthException extends Exception {

    /**
     * Creates an authentication exception with a cause.
     *
     * @param message human-readable failure description
     * @param cause underlying failure
     */
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an authentication exception without a cause.
     *
     * @param message human-readable failure description
     */
    public AuthException(String message) {
        super(message);
    }

}
