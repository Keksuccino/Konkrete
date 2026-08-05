package de.keksuccino.persephone.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class MathUtils {

	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();
		if (!Double.isFinite(value)) return value;
		BigDecimal bd = BigDecimal.valueOf(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	/**
	 * Formats finite whole-number doubles as plain integer text without narrowing them to the range of a {@code long}.
	 * Non-finite and fractional values retain Java's canonical double representation so callers do not silently change their semantics.
	 */
	public static String formatWholeNumber(double value) {
		if (!Double.isFinite(value) || value != Math.rint(value)) return Double.toString(value);
		// The exact double constructor is intentional; valueOf would round large integers through Double.toString before formatting them.
		return new BigDecimal(value).toBigIntegerExact().toString();
	}

	public static boolean isIntegerOrDouble(String value) {
    	try {
    		if (value.contains(".")) {
    			Double.parseDouble(value);
    		} else {
    			Integer.parseInt(value);
    		}
    		return true;
    	} catch (Exception ignored) {}
    	return false;
    }
	
	public static boolean isInteger(String value) {
		try {
			Integer.parseInt(value);
    		return true;
    	} catch (Exception ignored) {}
    	return false;
	}
	
	public static boolean isDouble(String value) {
		try {
			Double.parseDouble(value);
    		return true;
    	} catch (Exception ignored) {}
    	return false;
	}
	
	public static boolean isLong(String value) {
		try {
			Long.parseLong(value);
    		return true;
    	} catch (Exception ignored) {}
    	return false;
	}
	
	public static boolean isFloat(String value) {
		try {
			Float.parseFloat(value);
    		return true;
    	} catch (Exception ignored) {}
    	return false;
	}
	
	public static int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			return min;
		}
		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}

}
