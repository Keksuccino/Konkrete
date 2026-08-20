package de.keksuccino.konkrete.util.input;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Builds predicates for accepted input characters. */
@SuppressWarnings("unused")
public class CharacterFilter {

    /** Creates a signed-decimal filter; syntactically incomplete intermediate input remains allowed. */
    @NotNull
    public static CharacterFilter buildDecimalFiler() {
        CharacterFilter f = buildIntegerFilter();
        f.addAllowedCharacters(".");
        return f;
    }

    /** Creates a filter accepting digits and sign characters. */
    @NotNull
    public static CharacterFilter buildIntegerFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "-", "+");
        return f;
    }

    /** Creates a lowercase file/resource-name filter that permits dots, underscores, and hyphens. */
    @NotNull
    public static CharacterFilter buildResourceNameFilter() {
        //Support for dots (".") is needed for file extensions
        return buildOnlyLowercaseFileNameFilter();
    }

    /** Creates a lowercase ASCII filename filter. */
    @NotNull
    public static CharacterFilter buildOnlyLowercaseFileNameFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "_", "-"
        );
        return f;
    }

    /** Creates an ASCII filename filter accepting both letter cases. */
    @NotNull
    public static CharacterFilter buildLowercaseAndUppercaseFileNameFilter() {
        CharacterFilter f = buildOnlyLowercaseFileNameFilter();
        f.addAllowedCharacters(
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
        );
        return f;
    }

    /** Creates a permissive ASCII URL-character filter. */
    @NotNull
    public static CharacterFilter buildUrlFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters(
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h",
                "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
                "S", "T", "U", "V", "W", "X", "Y", "Z", ".", "-", "_", "~", "+", "#", ",", "%", "&", "=",
                "*", ";", ":", "@", "?", "/", "\\"
        );
        return f;
    }

    private final List<Character> allowed = new ArrayList<>();
    private final List<Character> forbidden = new ArrayList<>();

    /** Returns whether filtering would leave {@code text} unchanged. */
    public boolean isAllowedText(@NotNull String text) {
        return this.filterForAllowedChars(text).equals(text);
    }

    /** Tests one character against the allow-list, or the deny-list when no allow-list exists. */
    public boolean isAllowedChar(char c) {
        if (!this.allowed.isEmpty()) {
            return this.allowed.contains(c);
        } else {
            return !this.forbidden.contains(c);
        }
    }

    /** Tests the first character, treating an empty string as allowed. */
    public boolean isAllowedChar(@NotNull String charAsString) {
        return (charAsString.length() < 1) || this.isAllowedChar(charAsString.charAt(0));
    }

    /** Removes characters rejected by this filter while preserving input order. */
    @NotNull
    public String filterForAllowedChars(@NotNull String text) {
        String s = "";
        for (int i = 0; i < text.length(); ++i) {
            if (this.isAllowedChar(text.charAt(i))) {
                s = s + text.charAt(i);
            }
        }
        return s;
    }

    /** Adds unique characters to the allow-list. */
    public void addAllowedCharacters(char... chars) {
        for (char c : chars) {
            if (!this.allowed.contains(c)) {
                this.allowed.add(c);
            }
        }
    }

    /** Adds the first character of each non-empty string to the allow-list. */
    public void addAllowedCharacters(String... chars) {
        for (String s : chars) {
            if (s != null && s.length() >= 1 && !this.allowed.contains(s.charAt(0))) {
                this.allowed.add(s.charAt(0));
            }
        }
    }

    /** Adds unique characters to the deny-list used when the allow-list is empty. */
    public void addForbiddenCharacters(char... chars) {
        for (char c : chars) {
            if (!this.forbidden.contains(c)) {
                this.forbidden.add(c);
            }
        }
    }

    /** Adds the first character of each non-empty string to the deny-list. */
    public void addForbiddenCharacters(String... chars) {
        for (String s : chars) {
            if (s != null && s.length() >= 1 && !this.forbidden.contains(s.charAt(0))) {
                this.forbidden.add(s.charAt(0));
            }
        }
    }

}
