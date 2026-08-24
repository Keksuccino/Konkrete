package de.keksuccino.konkrete.file;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void compressToZip(@NotNull String pathToCompare, @NotNull String zipFile) {
        byte[] buffer = new byte[1024];
        String source = new File(pathToCompare).getName();
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);
            for (String file: FileUtils.getFiles(pathToCompare)) {
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
                    LOGGER.error("Error while trying to compress ZIP: " + zipFile, ex);
                }
                IOUtils.closeQuietly(in);
            }
            try {
                zos.closeEntry();
            } catch (Exception ignore) {}
        } catch (Exception ex) {
            LOGGER.error("Error while trying to compress ZIP: " + zipFile, ex);
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
                    LOGGER.error("Error while trying to compress ZIP: " + zipFile, ex);
                }
                IOUtils.closeQuietly(in);
            }
            try {
                zos.closeEntry();
            } catch (Exception ignore) {}
        } catch (IOException ex) {
            LOGGER.error("Error while trying to compress ZIP: " + zipFile, ex);
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
