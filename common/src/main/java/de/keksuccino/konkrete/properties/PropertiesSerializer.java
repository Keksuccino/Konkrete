package de.keksuccino.konkrete.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import de.keksuccino.konkrete.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class PropertiesSerializer {

	private static final Logger LOGGER = LogManager.getLogger();
	
	/**
	 * Returns a new {@link PropertiesSet} instance or null if the given file was not a valid properties file.
	 */
	public static PropertiesSet getProperties(@NotNull String filePath) {
		File f = new File(filePath);
		if (f.exists() && f.isFile()) {
			List<String> lines = FileUtils.getFileLines(f);
			List<PropertiesSection> data = new ArrayList<>();
			String propertiesType = null;
			PropertiesSection currentData = null;
			boolean insideData = false;
			for (String s : lines) {
				String comp = s.replace(" ", "");
				//Setting the type of the PropertiesSet
				if (comp.startsWith("type=") && !insideData) {
					propertiesType = comp.split("=", 2)[1];
					continue;
				}
				//Starting a new data section
				if (comp.endsWith("{")) {
					if (!insideData) {
						insideData = true;
					} else {
						LOGGER.warn("[KONKRETE] Invalid properties found in '" + filePath + "'! (Leaking properties section; Missing '}')");
                        data.add(currentData);
                    }
					currentData = new PropertiesSection(comp.split("\\{")[0]);
					continue;
				}
				//Finishing the data section
				if (comp.startsWith("}") && insideData) {
					data.add(currentData);
					insideData = false;
					continue;
				}
				//Collecting all entries inside the data section
				if (insideData && comp.contains("=")) {
					String value = s.split("=", 2)[1];
					if (value.startsWith(" ")) {
						value = value.substring(1);
					}
					currentData.addEntry(comp.split("=", 2)[0], value);
				}
			}
			if (propertiesType != null) {
				PropertiesSet set = new PropertiesSet(propertiesType);
				for (PropertiesSection d : data) {
					set.addProperties(d);
				}
				return set;
			} else {
				LOGGER.warn("[KONKRETE] Invalid properties file found: " + filePath + " (Missing properties type)");
			}
		}
		return null;
	}
	
	public static void writeProperties(@NotNull PropertiesSet propertiesSet, @NotNull String filePath) {
		try {
			List<PropertiesSection> l = propertiesSet.getProperties();
			File f = new File(filePath);
			//check if path is a file
			if (f.getName().contains(".") && !f.getName().startsWith(".")) {
				File parent = f.getParentFile();
				if ((parent != null) && parent.isDirectory() && !parent.exists()) {
					parent.mkdirs();
				}
				f.createNewFile();
				StringBuilder data = new StringBuilder();
				data.append("type = ").append(propertiesSet.getPropertiesType()).append("\n\n");
				for (PropertiesSection ps : l) {
					data.append(ps.getSectionType()).append(" {\n");
					for (Map.Entry<String, String> e : ps.getEntries().entrySet()) {
						data.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
					}
					data.append("}\n\n");
				}
				FileUtils.writeTextToFile(f, false, data.toString());
			} else {
				LOGGER.error("[KONKRETE] Failed to write properties! Given path is not a file: " + filePath, new FileNotFoundException());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
