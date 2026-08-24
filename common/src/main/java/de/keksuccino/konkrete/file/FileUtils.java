package de.keksuccino.konkrete.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import com.google.common.io.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FileUtils {

	private static final Logger LOGGER = LogManager.getLogger();

	public static void writeTextToFile(@NotNull File file, boolean append, String... text) throws IOException {
		FileOutputStream fo = new FileOutputStream(file, append);
		OutputStreamWriter os = new OutputStreamWriter(fo, StandardCharsets.UTF_8);
		BufferedWriter writer = new BufferedWriter(os);
        if (text.length == 1) {
        	writer.write(text[0]);
        } else {
        	for (String s : text) {
        		writer.write(s + "\n");
        	}
        }
        writer.flush();
		IOUtils.closeQuietly(fo);
		IOUtils.closeQuietly(os);
		IOUtils.closeQuietly(writer);
	}
	
	public static List<String> getFileLines(@NotNull File file) {
		List<String> list = new ArrayList<>();
		BufferedReader in = null;
		FileInputStream fileIn = null;
		InputStreamReader inReader = null;
		try {
			fileIn = new FileInputStream(file);
			inReader = new InputStreamReader(fileIn, StandardCharsets.UTF_8);
			in = new BufferedReader(inReader);
			String line = in.readLine();
			while (line != null) {
				list.add(line);
				line = in.readLine();
			}
		} catch (Exception ex) {
			LOGGER.error("Failed to read text lines of file: " + file.getAbsolutePath(), ex);
		}
		IOUtils.closeQuietly(in);
		IOUtils.closeQuietly(fileIn);
		IOUtils.closeQuietly(inReader);
		return list;
	}
	
	public static List<String> getFiles(@NotNull String path) {
		List<String> list = new ArrayList<>();
		File f = new File(path);
		if (f.exists()) {
			File[] files = f.listFiles();
			if (files != null) {
				for (File file : files) {
					list.add(file.getAbsolutePath());
				}
			}
		}
		return list;
	}
	
	public static List<String> getFileNames(@NotNull String path, boolean includeExtension) {
		List<String> list = new ArrayList<>();
		File f = new File(path);
		if (f.exists()) {
			File[] files = f.listFiles();
			if (files != null) {
				for (File file : files) {
					if (includeExtension) {
						list.add(file.getName());
					} else {
						list.add(Files.getNameWithoutExtension(file.getName()));
					}
				}
			}
		}
		return list;
	}

	@NotNull
	public static File generateUniqueFileName(@NotNull File fileOrFolder, boolean isDirectory) {
		if (isDirectory && !fileOrFolder.isDirectory()) return fileOrFolder;
		if (!isDirectory && !fileOrFolder.isFile()) return fileOrFolder;
		File f = new File(fileOrFolder.getPath());
		int count = 1;
		while ((isDirectory && f.isDirectory()) || (!isDirectory && f.isFile())) {
			f = new File(fileOrFolder.getPath() + "_" + count);
			count++;
		}
		return f;
	}

	/**
	 * Reads every UTF-8 text line from the given stream. The caller retains ownership of the stream and must close it.
	 *
	 * @throws IOException if the complete stream cannot be read; partially read lines are never returned
	 */
	@NotNull
	public static List<String> readTextLinesFrom(@NotNull InputStream in) throws IOException {
		Objects.requireNonNull(in);
		List<String> lines = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			lines.add(line);
		}
		return lines;
	}

	/**
	 * Opens the given file, reads every UTF-8 text line and closes the internally owned stream.
	 *
	 * @throws IOException if the file cannot be opened, completely read or closed; partially read lines are never returned
	 */
	@NotNull
	public static List<String> readTextLinesFrom(@NotNull File file) throws IOException {
		return readTextLinesFrom(file, source -> java.nio.file.Files.newInputStream(source.toPath()));
	}

	/**
	 * Keeps the complete owned-stream lifecycle deterministic and testable without depending on platform file-lock behavior.
	 */
	@NotNull
	static List<String> readTextLinesFrom(@NotNull File file, @NotNull OwnedInputStreamOpener inputStreamOpener) throws IOException {
		Objects.requireNonNull(file);
		Objects.requireNonNull(inputStreamOpener);
		try (InputStream in = inputStreamOpener.open(file)) {
			return readTextLinesFrom(in);
		}
	}

	/**
	 * Creates the given directory and returns it.
	 */
	@NotNull
	public static File createDirectory(@NotNull File directory) {
		try {
			if (!directory.isDirectory()) {
				directory.mkdirs();
			}
		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] Failed to create directory: " + directory.getAbsolutePath(), ex);
		}
		if (directory.getName().startsWith(".")) {
			try {
				java.nio.file.Files.setAttribute(directory.toPath(), "dos:hidden", true);
			} catch (Exception ignore) {}
		}
		return directory;
	}

	public static void openFile(@NotNull File file) {
		try {
			String url = file.toURI().toURL().toString();
			String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			URL u = new URL(url);
			if (Util.getPlatform() != Util.OS.OSX) {
				if (s.contains("win")) {
					Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
				} else {
					if (u.getProtocol().equals("file")) {
						url = url.replace("file:", "file://");
					}
					Runtime.getRuntime().exec(new String[]{"xdg-open", url});
				}
			} else {
				Runtime.getRuntime().exec(new String[]{"open", url});
			}
		} catch (Exception e) {
			LOGGER.error("[FANCYMENU] Failed to open file: " + file.getAbsolutePath(), e);
		}
	}

	@FunctionalInterface
	interface OwnedInputStreamOpener {

		@NotNull
		InputStream open(@NotNull File file) throws IOException;

	}
	
}
