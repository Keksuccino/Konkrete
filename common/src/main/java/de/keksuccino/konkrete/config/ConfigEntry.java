package de.keksuccino.konkrete.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigEntry {

	private String name;
	private String value;
	private String category;
	private String desc;
	private EntryType type;

	/**
	 * All changes to a {@link ConfigEntry} must be manually synchronized with the config file by calling {@link Config#syncConfig()}!
	 */
	public ConfigEntry(@NotNull String name, @NotNull String value, @NotNull EntryType type, @NotNull String category, @Nullable String description) {
		this.name = name;
		this.value = value;
		this.type = type;
		this.category = category;
		this.desc = description;
	}

	@NotNull
	public String getName() {
		return this.name;
	}

	@NotNull
	public String getValue() {
		return this.value;
	}

	@NotNull
	public EntryType getType() {
		return this.type;
	}

	@NotNull
	public String getCategory() {
		return this.category;
	}
	
	public void setValue(@NotNull String value) {
		this.value = value;
	}
	
	public void setCategory(@NotNull String category) {
		this.category = category;
	}
	
	public void setDescription(@Nullable String description) {
		this.desc = description;
	}
	
	/**
	 * @return The value description <b>or NULL</b> if the value has no description.
	 */
	@Nullable
	public String getDescription() {
		return this.desc;
	}
	
	public enum EntryType {
		INTEGER,
		STRING,
		DOUBLE,
		LONG,
		FLOAT,
		BOOLEAN;
	}
	
}
