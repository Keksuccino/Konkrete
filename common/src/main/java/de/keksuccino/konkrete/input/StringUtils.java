package de.keksuccino.konkrete.input;

import java.util.Arrays;
import java.util.List;

public class StringUtils {
	
	public static String replaceAllExceptOf(String in, String replaceWith, String... keepChars) {
		StringBuilder s = new StringBuilder();
		List<String> l = Arrays.asList(keepChars);
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if (l.contains(String.valueOf(c))) {
				s.append(c);
			} else {
				s.append(replaceWith);
			}
		}
		return s.toString();
	}
	
	public static String[] splitLines(String in, String separator) {
		if (!in.contains(separator)) {
			return new String[] {in};
		} else {
			return in.split(separator);
		}
	}

}
