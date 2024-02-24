package de.keksuccino.konkrete.properties;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("all")
public class PropertiesSection {
	
	private String sectionType;
	private Map<String, String> entries = new LinkedHashMap<>();
	
	public PropertiesSection(String sectionType) {
		this.sectionType = sectionType;
	}
	
	public void addEntry(String name, String value) {
		this.entries.putIfAbsent(name, value);
	}
	
	public Map<String, String> getEntries() {
		return this.entries;
	}
	
	public String getEntryValue(String name) {
		return this.entries.get(name);
	}
	
	public void removeEntry(String name) {
		this.entries.remove(name);
	}
	
	public boolean hasEntry(String name) {
		return this.entries.containsKey(name);
	}
	
	public String getSectionType() {
		return this.sectionType;
	}

}
