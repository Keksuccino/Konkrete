package de.keksuccino.konkrete.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
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
			LOGGER.error("[KONKRETE] Failed to read text lines of file: " + file.getAbsolutePath(), ex);
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
	
	public static List<String> getFilenames(@NotNull String path, boolean includeExtension) {
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
	
	public static String generateAvailableFilename(@NotNull String dir, @NotNull String baseName, @NotNull String extension) {
		File f = new File(dir);
		if (!f.exists() && f.isDirectory()) {
			f.mkdirs();
		}
		File f2 = new File(f.getPath() + "/" + baseName + "." + extension.replace(".", ""));
		int i = 1;
		while (f2.exists()) {
			f2 = new File(f.getPath() + "/" + baseName + "_" + i + "." + extension.replace(".", ""));
			i++;
		}
		return f2.getName();
	}

	/**
	 * @deprecated Use {@link Files#copy(File, File)} instead.
	 */
	@Deprecated
	public static boolean copyFile(@NotNull File from, @NotNull File to) {
		if (!from.getAbsolutePath().replace("\\", "/").equals(to.getAbsolutePath().replace("\\", "/"))) {
			if (from.exists() && from.isFile()) {
				File toParent = to.getParentFile();
				if ((toParent != null) && !toParent.exists()) {
					toParent.mkdirs();
				}
				InputStream in = null;
				OutputStream out = null;
				try {
					in = new BufferedInputStream(new FileInputStream(from));
					out = new BufferedOutputStream(new FileOutputStream(to));
					byte[] buffer = new byte[1024];
			        int lengthRead;
			        while ((lengthRead = in.read(buffer)) > 0) {
			            out.write(buffer, 0, lengthRead);
			            out.flush();
			        }
				} catch (Exception e) {
					e.printStackTrace();
				}
				IOUtils.closeQuietly(in);
		        IOUtils.closeQuietly(out);
		        //Should never be triggered
		        try {
		        	int i = 0;
					while (!to.exists() && (i < 10*20)) {
						Thread.sleep(50);
						i++;
					}
		        } catch (Exception e) {
		        	e.printStackTrace();
		        }
		        return to.exists();
			}
		}
		return false;
	}

	/**
	 * @deprecated Use {@link Files#move(File, File)} instead.
	 */
	@Deprecated
	public static boolean moveFile(@NotNull File from, @NotNull File to) throws InterruptedException {
		if (!from.getAbsolutePath().replace("\\", "/").equals(to.getAbsolutePath().replace("\\", "/"))) {
			if (from.exists() && from.isFile()) {
				if (from.renameTo(to)) {
					int i = 0;
					//Should never be triggered
					while (!to.exists() && (i < 10*20)) {
						Thread.sleep(50);
						i++;
					}
					return true;
				} else if (copyFile(from, to)) {
					if (from.delete()) {
						return true;
					} else {
						if (from.exists() && to.exists()) {
							to.delete();
							return false;
						}
					}
				}
			}
		}
		return false;
	}
	
	public static void compressToZip(@NotNull String pathToCompare, @NotNull String zipFile) {
        byte[] buffer = new byte[1024];
        String source = new File(pathToCompare).getName();
		FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);
            for (String file: getFiles(pathToCompare)) {
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);
				FileInputStream in = null;
                try {
                	in = new FileInputStream(file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } catch (Exception ex) {
					LOGGER.error("[KONKRETE] Error while trying to compress ZIP: " + zipFile, ex);
                }
				IOUtils.closeQuietly(in);
            }
            try {
				zos.closeEntry();
			} catch (Exception ignore) {}
        } catch (Exception ex) {
			LOGGER.error("[KONKRETE] Error while trying to compress ZIP: " + zipFile, ex);
        }
		IOUtils.closeQuietly(fos);
		IOUtils.closeQuietly(zos);
    }
	
	public static void compressToZip(@NotNull List<String> filePathsToCompare, @NotNull String zipFile) {
        byte[] buffer = new byte[1024];
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);
            for (String file: filePathsToCompare) {
                ZipEntry ze = new ZipEntry(Files.getNameWithoutExtension(zipFile) + "/" + file);
                zos.putNextEntry(ze);
				FileInputStream in = null;
                try {
                	in = new FileInputStream(file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } catch (Exception ex) {
					LOGGER.error("[KONKRETE] Error while trying to compress ZIP: " + zipFile, ex);
                }
				IOUtils.closeQuietly(in);
            }
            try {
				zos.closeEntry();
			} catch (Exception ignore) {}
        } catch (IOException ex) {
            LOGGER.error("[KONKRETE] Error while trying to compress ZIP: " + zipFile, ex);
        }
		IOUtils.closeQuietly(fos);
		IOUtils.closeQuietly(zos);
    }

	public static void unpackZip(@NotNull String zipPath, @NotNull String outputDir) throws IOException {
		ZipFile zipFile = new ZipFile(zipPath);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			File entryDestination = new File(outputDir,  entry.getName());
			if (entry.isDirectory()) {
				entryDestination.mkdirs();
			} else {
				entryDestination.getParentFile().mkdirs();
				InputStream in = zipFile.getInputStream(entry);
				OutputStream out = new FileOutputStream(entryDestination);
				IOUtils.copy(in, out);
			}
		}
		IOUtils.closeQuietly(zipFile);
	}
	
}
