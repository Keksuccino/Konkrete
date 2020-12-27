package de.keksuccino.konkrete.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionHelper {
	
	public static Field findField(Class<?> c, String... names) throws NoSuchFieldException {
		Field f = null;
		
		for (String s : names) {
			try {
				f = c.getDeclaredField(s);
				f.setAccessible(true);
				break;
			} catch (Exception e) {}
		}
		
		if (f == null) {
			throw new NoSuchFieldException("No field found matching one of the given names: " + names);
		}
		
		return f;
	}
	
	public static Method findMethod(Class<?> c, String deobName, String obName, Class<?>... args) throws NoSuchMethodException {
		Method m = null;
		
		try {
			m = c.getDeclaredMethod(deobName, args);
			m.setAccessible(true);
		} catch (Exception e) {}
		try {
			m = c.getDeclaredMethod(obName, args);
			m.setAccessible(true);
		} catch (Exception e) {}
		
		if (m == null) {
			throw new NoSuchMethodException("No method found matching one of the given names or arguments: " + deobName + ", " + obName);
		}
		
		return m;
	}

}
