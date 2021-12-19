package de.keksuccino.konkrete.localization;

import java.lang.reflect.Field;
import java.util.Map;

import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.util.Language;

public class LocaleUtils {
	
	/**
	 * Returns the key for the given string or null if no key with the given value was found.
	 */
	public static String getKeyForString(String s) {
		try {
			Language l = Language.getInstance();
			if (!(l instanceof TranslationStorage)) {
				return null;
			}
			Field f = ReflectionHelper.findField(TranslationStorage.class, "translations", "field_5330");
			Map<String, String> properties = (Map<String, String>) f.get((TranslationStorage)l);
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
