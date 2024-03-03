package de.keksuccino.konkrete.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import de.keksuccino.konkrete.config.ConfigEntry.EntryType;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.math.MathUtils;

/**
 * Simple config system to store values.<br>
 * Can store Strings, Booleans and all common types of numbers.
 */
@SuppressWarnings("all")
public class Config {

	private static final Logger LOGGER = LogManager.getLogger();
	
	private String path;
	private File config;
	private Map<String, ConfigEntry> values = new HashMap<>();
	private List<String> registeredValues = new ArrayList<>();
	@Nullable
	private String name = null;
	private List<String> categorys = new ArrayList<>();
	
	public Config(@NotNull String path) {
		this.path = Objects.requireNonNull(path);
		this.config = new File(path);
		if (this.config.isFile()) {
			File f = this.config.getParentFile();
			if ((f != null) && !f.exists()) {
				f.mkdirs();
			}
		}
		this.init();
	}
	
	private void init() {
		List<String> l = this.getTextFileData();
		if (l.isEmpty()) {
			return;
		}
		String category = null;
		String desc = null;
		String valueName = null;
		EntryType type = null;
		StringBuilder value = new StringBuilder();
		boolean b = false;
		for (String s : l) {
			if (b) {
				if (new StringBuilder(s).reverse().toString().replace(" ", "").startsWith(";'")) {
					value.append("\n").append(new StringBuilder(new StringBuilder(s).reverse().toString().split(";", 2)[1].substring(1)).reverse().toString());
					if ((category != null) && (valueName != null) && (type != null) && !this.valueExists(valueName)) {
						this.values.put(valueName, new ConfigEntry(valueName, value.toString(), type, category, desc));
						if (!this.categorys.contains(category)) {
							this.categorys.add(category);
						}
					}
					desc = null;
					valueName = null;
					type = null;
					value = new StringBuilder();
					b = false;
				} else {
					value.append("\n").append(s);
				}
			}
			if (s.startsWith("##[")) {
				if (!s.contains("]")) {
					continue;
				}
				category = new StringBuilder(new StringBuilder(s.split("\\[", 2)[1]).reverse().toString().split("]")[1]).reverse().toString();
				continue;
			}
			if (s.startsWith("[")) {
				if (!s.contains("]")) {
					continue;
				}
				desc = new StringBuilder(new StringBuilder(s.split("\\[", 2)[1]).reverse().toString().split("]")[1]).reverse().toString();
				continue;
			}
			if (s.contains("=") && s.contains("'") && s.substring(1).startsWith(":")) {
				valueName = s.split(":", 2)[1].replace(" ", "").split("=")[0];
				if (s.startsWith("I:")) {
					type = EntryType.INTEGER;
				}
				if (s.startsWith("S:")) {
					type = EntryType.STRING;
				}
				if (s.startsWith("B:")) {
					type = EntryType.BOOLEAN;
				}
				if (s.startsWith("L:")) {
					type = EntryType.LONG;
				}
				if (s.startsWith("D:")) {
					type = EntryType.DOUBLE;
				}
				if (s.startsWith("F:")) {
					type = EntryType.FLOAT;
				}
				if (new StringBuilder(s).reverse().toString().replace(" ", "").startsWith(";'")) {
					value = new StringBuilder(new StringBuilder(new StringBuilder(s.split("'", 2)[1]).reverse().toString().split(";", 2)[1].substring(1)).reverse().toString());
					if ((category != null) && (valueName != null) && (type != null) && !this.valueExists(valueName)) {
						this.values.put(valueName, new ConfigEntry(valueName, value.toString(), type, category, desc));
						if (!this.categorys.contains(category)) {
							this.categorys.add(category);
						}
					}
					desc = null;
					valueName = null;
					type = null;
					value = new StringBuilder();
				} else {
					value = new StringBuilder(s.split("'", 2)[1]);
					b = true;
				}
			}
		}
	}
	
	/**
	 * @return Unmodifiable list of all registered categories of this config.
	 */
	@NotNull
	public List<String> getCategories() {
        return new ArrayList<>(categorys);
	}

	/**
	 * @deprecated Use {@link Config#getCategories()} instead.
	 */
	@Deprecated
	public List<String> getCategorys() {
		return this.getCategories();
	}

	@Nullable
	public String getConfigName() {
		return this.name;
	}
	
	/**
	 * The name of the config displayed in the header of the config file.<br>
	 * It is needed to call {@code Config.syncConfig()} to write the name to the config file.
	 */
	public void setConfigName(@Nullable String name) {
		this.name = Objects.requireNonNull(name);
	}
	
	/**
	 * Synchronizes all changes with the config file.<br>
	 * Needs to be called every time a {@link ConfigEntry} was manually manipulated or new values were registered to the config.<br>
	 * This is <b>NOT</b> needed after setting a value by calling the {@code Config.setValue()} method!
	 */
	public void syncConfig() {
		StringBuilder data = new StringBuilder();
		boolean b = false;
		if (this.name != null) {
			data.append("//").append(this.name).append("\n");
			b = true;
		}
		//Use getCategories() to avoid co-modifications
		for (String s : this.getCategories()) {
			List<ConfigEntry> l = this.getEntriesForCategory(s);
			if (l.isEmpty()) {
				this.categorys.remove(s);
				continue;
			}
			if (!b) {
				b = true;
			} else {
				data.append("\n\n\n");
			}
			data.append("##[").append(s).append("]\n");
			for (ConfigEntry e : l) {
				String value = e.getValue();
				String valueName = e.getName();
				String desc = e.getDescription();
				EntryType type = e.getType();
				if ((value != null) && (valueName != null) && (type != null)) {
					if (desc != null) {
						data.append("\n[").append(desc).append("]");
					}
					if (type == EntryType.STRING) {
						data.append("\nS:").append(valueName).append(" = '");
					}
					if (type == EntryType.INTEGER) {
						data.append("\nI:").append(valueName).append(" = '");
					}
					if (type == EntryType.BOOLEAN) {
						data.append("\nB:").append(valueName).append(" = '");
					}
					if (type == EntryType.LONG) {
						data.append("\nL:").append(valueName).append(" = '");
					}
					if (type == EntryType.DOUBLE) {
						data.append("\nD:").append(valueName).append(" = '");
					}
					if (type == EntryType.FLOAT) {
						data.append("\nF:").append(valueName).append(" = '");
					}
					data.append(value).append("';");
				}
			}
		}
		File oldConfig = this.backupConfig();
		if (oldConfig == null) {
			LOGGER.error("[KONKRETE] Config backup failed: " + this.path, new NullPointerException("Backup config was NULL!"));
		}
		if (!this.config.exists()) {
			try {
				this.config.createNewFile();
			} catch (IOException ex) {
				LOGGER.error("[KONKRETE] Error while trying to create config file!", ex);
			}
		}
		BufferedWriter writer = null;
		try {
        	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.config), StandardCharsets.UTF_8));
            writer.write(data.toString());
            writer.flush();
            oldConfig.delete();
        } catch (Exception ex) {
			LOGGER.error("[KONKRETE] Error while trying to write to config file or deleting backup config file!", ex);
        }
		IOUtils.closeQuietly(writer);
	}

	@Nullable
	private File backupConfig() {
		File back = new File(this.config.getAbsolutePath() + ".backup");
		List<String> data = this.getTextFileData();
		StringBuilder data2 = new StringBuilder();
		if (!back.exists()) {
			try {
				back.createNewFile();
			} catch (IOException ex) {
				LOGGER.error("[KONKRETE] Error while trying to create backup config file!", ex);
			}
		}
		boolean b = false;
		for (String s : data) {
			if (!b) {
				b = true;
			} else {
				data2.append("\n");
			}
			data2.append(s);
		}
		BufferedWriter writer = null;
		try {
        	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(back), StandardCharsets.UTF_8));
            writer.write(data2.toString());
            writer.flush();
        } catch (Exception ex) {
			LOGGER.error("[KONKRETE] Error while trying to write to backup config file!", ex);
        }
		IOUtils.closeQuietly(writer);
		if (back.exists()) return back;
		return null;
	}

	@NotNull
	public List<ConfigEntry> getEntriesForCategory(@NotNull String category) {
		List<ConfigEntry> l = new ArrayList<>();
		for (Map.Entry<String, ConfigEntry> m : this.values.entrySet()) {
			if (m.getValue().getCategory().equals(Objects.requireNonNull(category))) {
				l.add(m.getValue());
			}
		}
		return l;
	}

	/**
	 * @deprecated Use {@link Config#getEntriesForCategory(String)} instead.
	 */
	@Deprecated
	public List<ConfigEntry> getEntrysForCategory(String category) {
		return this.getEntriesForCategory(category);
	}

	@NotNull
	private List<String> getTextFileData() {
		List<String> l = new ArrayList<>();
		if (this.config.exists()) {
			BufferedReader reader = null;
			InputStreamReader inputStreamReader = null;
			FileInputStream inputStream = null;
			try {
				inputStream = new FileInputStream(this.config);
				inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				reader = new BufferedReader(inputStreamReader);
	            String line;
	            while ((line = reader.readLine()) != null) {
	            	l.add(line);
	            }
			} catch (Exception ex) {
				LOGGER.error("[KONKRETE] Error while trying to read data from config file!", ex);
			}
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(inputStreamReader);
			IOUtils.closeQuietly(inputStream);
		}
		return l;
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Integer defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.INTEGER, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Double defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.DOUBLE, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Long defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.LONG, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Float defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.FLOAT, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Boolean defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.BOOLEAN, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, String defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, defaultValue, category, EntryType.STRING, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Integer defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.INTEGER, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Double defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.DOUBLE, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Float defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.FLOAT, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Long defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.LONG, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, Boolean defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.BOOLEAN, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 */
	public void registerValue(String uniqueName, String defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, defaultValue, category, EntryType.STRING, description);
	}

	private void registerRawValue(String uniqueName, String defaultValue, String category, EntryType type, @Nullable String description) throws InvalidValueException {
		if (uniqueName == null) {
			throw new InvalidValueException("Value name cannot be null!");
		}
		if (defaultValue == null) {
			throw new InvalidValueException("Default value cannot be null!");
		}
		if (category == null) {
			throw new InvalidValueException("Category cannot be null!");
		}
		if (type == null) {
			throw new InvalidValueException("Type cannot be null!");
		}
		
		if (!this.categorys.contains(category)) {
			this.categorys.add(category);
		}
		
		if (type == EntryType.BOOLEAN) {
			if (!defaultValue.equalsIgnoreCase("true") && !defaultValue.equalsIgnoreCase("false")) {
				throw new InvalidValueException("This value is not a valid BOOLEAN! (" + defaultValue + ")");
			}
		}
		if (type == EntryType.INTEGER) {
			if (!MathUtils.isInteger(defaultValue)) {
				throw new InvalidValueException("This value is not a valid INTEGER! (" + defaultValue + ")");
			}
		}
		if (type == EntryType.DOUBLE) {
			if (!MathUtils.isDouble(defaultValue)) {
				throw new InvalidValueException("This value is not a valid DOUBLE! (" + defaultValue + ")");
			}
		}
		if (type == EntryType.FLOAT) {
			if (!MathUtils.isFloat(defaultValue)) {
				throw new InvalidValueException("This value is not a valid FLOAT! (" + defaultValue + ")");
			}
		}
		if (type == EntryType.LONG) {
			if (!MathUtils.isLong(defaultValue)) {
				throw new InvalidValueException("This value is not a valid LONG! (" + defaultValue + ")");
			}
		}
		
		if (!this.valueExists(uniqueName)) {
			this.values.put(uniqueName, new ConfigEntry(uniqueName, defaultValue, type, category, description));
		}
		this.registeredValues.add(uniqueName);
	}
	
	/**
	 * All changes to a {@link ConfigEntry} must be manually synchronized with the config file by calling {@link Config#syncConfig()}!
	 * @return An editable {@link ConfigEntry}.
	 */
	public ConfigEntry getAsEntry(String name) {
		if (this.valueExists(name)) {
			return this.values.get(name);
		}
		return null;
	}
	
	/**
	 * All changes to a {@link ConfigEntry} must be manually synchronized with the config file by calling {@link Config#syncConfig()}!
	 * @return A list with all values of this config as editable {@link ConfigEntry}s.
	 */
	public List<ConfigEntry> getAllAsEntry() {
        return new ArrayList<>(values.values());
	}

	public void setValue(@NotNull String name, @NotNull String value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.STRING) {
			e.setValue(value);
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a STRING value to it!");
		}
	}

	/**
	 * @deprecated Use {@link Config#setValue(String, int)} instead.
	 */
	@Deprecated(forRemoval = true)
	public void setValue(@NotNull String name, @NotNull Integer value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.INTEGER) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set an INTEGER value to it!");
		}
	}

	public void setValue(@NotNull String name, int value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.INTEGER) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set an INTEGER value to it!");
		}
	}

	/**
	 * @deprecated Use {@link Config#setValue(String, boolean)} instead.
	 */
	@Deprecated(forRemoval = true)
	public void setValue(@NotNull String name, @NotNull Boolean value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.BOOLEAN) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a BOOLEAN value to it!");
		}
	}

	public void setValue(@NotNull String name, boolean value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.BOOLEAN) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a BOOLEAN value to it!");
		}
	}

	/**
	 * @deprecated Use {@link Config#setValue(String, float)} instead.
	 */
	@Deprecated(forRemoval = true)
	public void setValue(@NotNull String name, @NotNull Float value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.FLOAT) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a FLOAT value to it!");
		}
	}

	public void setValue(@NotNull String name, float value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.FLOAT) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a FLOAT value to it!");
		}
	}

	/**
	 * @deprecated Use {@link Config#setValue(String, double)} instead.
	 */
	@Deprecated(forRemoval = true)
	public void setValue(@NotNull String name, @NotNull Double value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.DOUBLE) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a DOUBLE value to it!");
		}
	}

	public void setValue(@NotNull String name, double value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.DOUBLE) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a DOUBLE value to it!");
		}
	}

	/**
	 * @deprecated Use {@link Config#setValue(String, long)} instead.
	 */
	@Deprecated(forRemoval = true)
	public void setValue(@NotNull String name, @NotNull Long value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.LONG) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a LONG value to it!");
		}
	}

	public void setValue(@NotNull String name, long value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.LONG) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a LONG value to it!");
		}
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to remove the value from the config file.
	 */
	public void unregisterValue(@NotNull String name) {
		if (this.valueExists(name)) {
			this.values.remove(name);
            this.registeredValues.remove(name);
		}
	}

	@NotNull
	public Boolean getBoolean(@NotNull String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.BOOLEAN) {
			if (e.getValue().equalsIgnoreCase("true")) {
				return true;
			} else if (e.getValue().equalsIgnoreCase("false")) {
				return false;
			} else {
				throw new InvalidValueException("This value is not a valid BOOLEAN value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not BOOLEAN!");
		}
	}

	@NotNull
	public String getString(@NotNull String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.STRING) {
			return e.getValue();
		} else {
			throw new InvalidValueException("This value's type is not STRING!");
		}
	}

	@NotNull
	public Integer getInteger(@NotNull String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.INTEGER) {
			if (MathUtils.isInteger(e.getValue())) {
				return Integer.parseInt(e.getValue());
			} else {
				throw new InvalidValueException("This value is not a valid INTEGER value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not INTEGER!");
		}
	}

	@NotNull
	public Double getDouble(@NotNull String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.DOUBLE) {
			if (MathUtils.isDouble(e.getValue())) {
				return Double.parseDouble(e.getValue());
			} else {
				throw new InvalidValueException("This value is not a valid DOUBLE value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not DOUBLE!");
		}
	}

	@NotNull
	public Long getLong(@NotNull String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.LONG) {
			if (MathUtils.isLong(e.getValue())) {
				return Long.parseLong(e.getValue());
			} else {
				throw new InvalidValueException("This value is not a valid LONG value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not LONG!");
		}
	}

	@NotNull
	public Float getFloat(@NotNull String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.FLOAT) {
			if (MathUtils.isFloat(e.getValue())) {
				return Float.parseFloat(e.getValue());
			} else {
				throw new InvalidValueException("This value is not a valid LONG value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not LONG!");
		}
	}
	
	public void setCategory(@NotNull String valueName, @NotNull String category) throws InvalidValueException {
		if (this.valueExists(valueName)) {
			ConfigEntry e = this.getAsEntry(valueName);
			e.setCategory(Objects.requireNonNull(category));
			if (!this.categorys.contains(category)) {
				this.categorys.add(category);
			}
			this.syncConfig();
		} else {
			throw new InvalidValueException("This values does not exist! (" + valueName + ")");
		}
	}
	
	public void setDescription(@NotNull String valueName, @Nullable String description) throws InvalidValueException {
		if (this.valueExists(valueName)) {
			ConfigEntry e = this.getAsEntry(valueName);
			e.setDescription(description);
			this.syncConfig();
		} else {
			throw new InvalidValueException("This values does not exist! (" + valueName + ")");
		}
	}
	
	public boolean valueExists(@NotNull String name) {
		return this.values.containsKey(Objects.requireNonNull(name));
	}
	
	/**
	 * Will remove all (unused) values from the config file which were not registered via {@code Config.registerValue()}.<br><br>
	 * 
	 * <b>DON'T CALL THIS BEFORE YOU'VE REGISTERED ALL VALUES!</b>
	 */
	public void clearUnusedValues() {
		List<String> l = new ArrayList<>();
		for (Map.Entry<String, ConfigEntry> m : this.values.entrySet()) {
			if (!this.registeredValues.contains(m.getKey())) {
				l.add(m.getKey());
			}
		}
		for (String s : l) {
			this.unregisterValue(s);
		}
		this.syncConfig();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getOrDefault(String valueName, T defaultValue) {
		try {
			if (defaultValue instanceof Integer) {
				return (T) this.getInteger(valueName);
			}
			if (defaultValue instanceof Boolean) {
				return (T) this.getBoolean(valueName);
			}
			if (defaultValue instanceof String) {
				return (T) this.getString(valueName);
			}
			if (defaultValue instanceof Long) {
				return (T) this.getLong(valueName);
			}
			if (defaultValue instanceof Double) {
				return (T) this.getDouble(valueName);
			}
			if (defaultValue instanceof Float) {
				return (T) this.getFloat(valueName);
			}
		} catch (Exception ignore) {}
		return defaultValue;
	}

}
