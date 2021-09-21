package de.keksuccino.konkrete.localization;

import net.minecraft.locale.Language;

import java.util.Map;

public class LocaleUtils {
	
	/**
	 * Returns the key for the given string or null if no key with the given value was found.
	 */
	public static String getKeyForString(String s) {
		try {
			Language l = Language.getInstance();
			Map<String, String> properties = l.getLanguageData();
			for (Map.Entry<String, String> m : properties.entrySet()) {
				if (m.getValue().equals(s)) {
					return m.getKey();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
