package de.keksuccino.konkrete.util.file.type.groups;

import de.keksuccino.konkrete.util.file.type.types.*;
import net.minecraft.network.chat.Component;

/** Describes file type groups metadata. */
public class FileTypeGroups {

    /** Creates the localized component for this value. */
    public static final Component IMAGE_GROUP_COMPONENT = Component.translatable("konkrete.file_types.groups.image");
    /** Creates the localized component for this value. */
    public static final Component AUDIO_GROUP_COMPONENT = Component.translatable("konkrete.file_types.groups.audio");
    /** Creates the localized component for this value. */
    public static final Component VIDEO_GROUP_COMPONENT = Component.translatable("konkrete.file_types.groups.video");
    /** Creates the localized component for this value. */
    public static final Component TEXT_GROUP_COMPONENT = Component.translatable("konkrete.file_types.groups.text");

    /** All registered image types, evaluated when requested. */
    public static final FileTypeGroup<ImageFileType> IMAGE_TYPES = new FileTypeGroup<>(FileTypes::getAllImageFileTypes, IMAGE_GROUP_COMPONENT);
    /** All registered audio types, evaluated when requested. */
    public static final FileTypeGroup<AudioFileType> AUDIO_TYPES = new FileTypeGroup<>(FileTypes::getAllAudioFileTypes, AUDIO_GROUP_COMPONENT);
    /** All registered video types, evaluated when requested. */
    public static final FileTypeGroup<VideoFileType> VIDEO_TYPES = new FileTypeGroup<>(FileTypes::getAllVideoFileTypes, VIDEO_GROUP_COMPONENT);
    /** All registered text types, evaluated when requested. */
    public static final FileTypeGroup<TextFileType> TEXT_TYPES = new FileTypeGroup<>(FileTypes::getAllTextFileTypes, TEXT_GROUP_COMPONENT);

}
