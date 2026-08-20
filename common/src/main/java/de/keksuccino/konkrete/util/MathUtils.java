package de.keksuccino.konkrete.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/** Provides reusable numeric parsing, calculation, rounding, and formatting helpers. */
public class MathUtils {

    /** Returns whether the value is a valid integer or decimal number. */
    public static boolean isIntegerOrDouble(String value) {
        return isInteger(value) || isDouble(value);
    }

    /** Returns whether the value is a valid base-ten integer. */
    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Returns whether the value is a valid double. */
    public static boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Returns whether the value is a valid long. */
    public static boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Returns whether the value is a valid float. */
    public static boolean isFloat(String value) {
        try {
            Float.parseFloat(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Returns an inclusive random integer, or {@code min} when the range is empty or inverted. */
    public static int getRandomNumberInRange(int min, int max) {
        if (min >= max) return min;
        if (max == Integer.MAX_VALUE) {
            long result = ThreadLocalRandom.current().nextLong((long) min, (long) max + 1L);
            return (int) result;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** Evaluates a basic arithmetic expression, returning zero when it is invalid. */
    public static double calculateFromString(String input) {
        if (isDouble(input)) return Double.parseDouble(input);
        try {
            return new ExpressionParser(input).parse();
        } catch (RuntimeException ignored) {
            return 0.0D;
        }
    }

    /** Returns whether the value can be evaluated by {@link #calculateFromString(String)}. */
    public static boolean isCalculateableString(String input) {
        if (isDouble(input)) return true;
        try {
            new ExpressionParser(input).parse();
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Rounds a finite value to the requested number of decimal places using half-up rounding. */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException("places must not be negative");
        if (!Double.isFinite(value)) return value;
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Formats finite whole-number doubles as plain integer text without narrowing them to the range of a {@code long}.
     * Non-finite and fractional values retain Java's canonical double representation.
     */
    public static String formatWholeNumber(double value) {
        if (!Double.isFinite(value) || value != Math.rint(value)) return Double.toString(value);
        // The exact double constructor is intentional; valueOf rounds large integers through Double.toString first.
        return new BigDecimal(value).toBigIntegerExact().toString();
    }

    private static final class ExpressionParser {

        private final String input;
        private int position = -1;
        private int currentCharacter;

        private ExpressionParser(String input) {
            if (input == null) throw new IllegalArgumentException("input must not be null");
            this.input = input;
        }

        private double parse() {
            nextCharacter();
            double value = parseExpression();
            skipWhitespace();
            if (this.position < this.input.length()) throw new IllegalArgumentException("Unexpected character: " + (char) this.currentCharacter);
            return value;
        }

        private void nextCharacter() {
            this.currentCharacter = ++this.position < this.input.length() ? this.input.charAt(this.position) : -1;
        }

        private void skipWhitespace() {
            while (Character.isWhitespace(this.currentCharacter)) nextCharacter();
        }

        private boolean consume(int character) {
            skipWhitespace();
            if (this.currentCharacter != character) return false;
            nextCharacter();
            return true;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                if (consume('+')) value += parseTerm();
                else if (consume('-')) value -= parseTerm();
                else return value;
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                if (consume('*')) value *= parseFactor();
                else if (consume('/')) value /= parseFactor();
                else return value;
            }
        }

        private double parseFactor() {
            if (consume('+')) return parseFactor();
            if (consume('-')) return -parseFactor();

            skipWhitespace();
            int startPosition = this.position;
            double value;
            if (consume('(')) {
                value = parseExpression();
                if (!consume(')')) throw new IllegalArgumentException("Missing closing parenthesis");
            } else if (Character.isDigit(this.currentCharacter) || this.currentCharacter == '.') {
                while (Character.isDigit(this.currentCharacter) || this.currentCharacter == '.') nextCharacter();
                value = Double.parseDouble(this.input.substring(startPosition, this.position));
            } else if (Character.isLetter(this.currentCharacter)) {
                while (Character.isLetter(this.currentCharacter)) nextCharacter();
                String function = this.input.substring(startPosition, this.position);
                double argument = parseFactor();
                value = switch (function) {
                    case "sqrt" -> Math.sqrt(argument);
                    case "sin" -> Math.sin(Math.toRadians(argument));
                    case "cos" -> Math.cos(Math.toRadians(argument));
                    case "tan" -> Math.tan(Math.toRadians(argument));
                    default -> throw new IllegalArgumentException("Unknown function: " + function);
                };
            } else {
                throw new IllegalArgumentException("Unexpected character: " + (char) this.currentCharacter);
            }
            if (consume('^')) value = Math.pow(value, parseFactor());
            return value;
        }

    }

}
