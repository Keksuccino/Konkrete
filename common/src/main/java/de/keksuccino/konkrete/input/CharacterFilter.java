package de.keksuccino.konkrete.input;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CharacterFilter {

    @NotNull
    public static CharacterFilter buildDecimalFiler() {
        CharacterFilter f = buildIntegerFilter();
        f.addAllowedCharacters(".");
        return f;
    }

    @NotNull
    public static CharacterFilter buildIntegerFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "-", "+");
        return f;
    }

    @NotNull
    public static CharacterFilter buildResourceNameFilter() {
        //Support for dots (".") is needed for file extensions
        return buildOnlyLowercaseFileNameFilter();
    }

    @NotNull
    public static CharacterFilter buildOnlyLowercaseFileNameFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "_", "-"
        );
        return f;
    }

    @NotNull
    public static CharacterFilter buildLowercaseAndUppercaseFileNameFilter() {
        CharacterFilter f = buildOnlyLowercaseFileNameFilter();
        f.addAllowedCharacters(
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
        );
        return f;
    }

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

    public boolean isAllowedText(@NotNull String text) {
        return this.filterForAllowedChars(text).equals(text);
    }

    public boolean isAllowedChar(char c) {
        if (!this.allowed.isEmpty()) {
            return this.allowed.contains(c);
        } else {
            return !this.forbidden.contains(c);
        }
    }

    public boolean isAllowedChar(@NotNull String charAsString) {
        return (charAsString.isEmpty()) || this.isAllowedChar(charAsString.charAt(0));
    }

    @NotNull
    public String filterForAllowedChars(@NotNull String text) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            if (this.isAllowedChar(text.charAt(i))) {
                s.append(text.charAt(i));
            }
        }
        return s.toString();
    }

    public void addAllowedCharacters(char... chars) {
        for (char c : chars) {
            if (!this.allowed.contains(c)) {
                this.allowed.add(c);
            }
        }
    }

    public void addAllowedCharacters(String... chars) {
        for (String s : chars) {
            if (s != null && !s.isEmpty() && !this.allowed.contains(s.charAt(0))) {
                this.allowed.add(s.charAt(0));
            }
        }
    }

    public void addForbiddenCharacters(char... chars) {
        for (char c : chars) {
            if (!this.forbidden.contains(c)) {
                this.forbidden.add(c);
            }
        }
    }

    public void addForbiddenCharacters(String... chars) {
        for (String s : chars) {
            if (s != null && !s.isEmpty() && !this.forbidden.contains(s.charAt(0))) {
                this.forbidden.add(s.charAt(0));
            }
        }
    }

}
