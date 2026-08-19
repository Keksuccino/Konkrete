package de.keksuccino.konkrete.util.file;

import de.keksuccino.konkrete.util.file.type.types.*;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import org.jetbrains.annotations.NotNull;
import java.io.File;

/** Filters files by extension while retaining directories for navigation. */
@FunctionalInterface
public interface FileFilter {

    /** Accepts files whose game-directory-relative name is resource-safe. */
    FileFilter RESOURCE_NAME_FILTER = file -> {
        String name = GameDirectoryUtils.getPathWithoutGameDirectory(file.getAbsolutePath()).replace("/", "").replace("\\", "");
        return CharacterFilter.buildResourceNameFilter().isAllowedText(name);
    };

    /** Accepts registered image files. */
    FileFilter IMAGE_FILE_FILTER = file -> {
        for (ImageFileType type : FileTypes.getAllImageFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };
    /** Accepts registered audio files. */
    FileFilter AUDIO_FILE_FILTER = file -> {
        for (AudioFileType type : FileTypes.getAllAudioFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };
    /** Accepts registered video files. */
    FileFilter VIDEO_FILE_FILTER = file -> {
        for (VideoFileType type : FileTypes.getAllVideoFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };
    /** Accepts registered text files. */
    FileFilter TEXT_FILE_FILTER = file -> {
        for (TextFileType type : FileTypes.getAllTextFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };

    /** Returns whether the supplied file passes this filter. */
    boolean checkFile(@NotNull File file);

}
