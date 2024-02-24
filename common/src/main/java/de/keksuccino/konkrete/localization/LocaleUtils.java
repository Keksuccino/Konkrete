package de.keksuccino.konkrete.localization;

import java.util.Map;
import de.keksuccino.konkrete.mixin.mixins.client.IMixinClientLanguage;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;

@Deprecated(forRemoval = true)
public class LocaleUtils {
	
	/**
	 * Returns the key for the given string or null if no key with the given value was found.
	 */
	public static String getKeyForString(String s) {
		try {
			Language l = Language.getInstance();
			if (!(l instanceof ClientLanguage)) {
				return null;
			}
			Map<String, String> storage = ((IMixinClientLanguage)l).getStorageKonkrete();
			for (Map.Entry<String, String> m : storage.entrySet()) {
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
