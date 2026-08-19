package de.keksuccino.konkrete.util.file.type.groups;

import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.file.type.types.FileTypes;
import de.keksuccino.konkrete.util.resource.Resource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

/** Describes file type group metadata. */
@SuppressWarnings("unused")
public class FileTypeGroup<T extends FileType<?>> {

    /** Supplies current group members on demand. */
    @NotNull
    protected Supplier<List<T>> typeSupplier;
    /** Optional localized group label. */
    @Nullable
    protected Component displayName;

    /** Creates a group containing every supported file type. */
    @SuppressWarnings("all")
    @NotNull
    public static FileTypeGroup<FileType<Resource>> allSupported() {
        List<FileType<Resource>> types = new ArrayList<>();
        FileTypes.getAllImageFileTypes().forEach(imageFileType -> types.add((FileType<Resource>)((FileType<?>)imageFileType)));
        FileTypes.getAllAudioFileTypes().forEach(imageFileType -> types.add((FileType<Resource>)((FileType<?>)imageFileType)));
        FileTypes.getAllVideoFileTypes().forEach(imageFileType -> types.add((FileType<Resource>)((FileType<?>)imageFileType)));
        FileTypes.getAllTextFileTypes().forEach(imageFileType -> types.add((FileType<Resource>)((FileType<?>)imageFileType)));
        FileTypeGroup<FileType<Resource>> group = new FileTypeGroup<>(() -> types, null);
        group.setDisplayName(Component.translatable("konkrete.file_types.groups.all_supported"));
        return group;
    }

    /** Creates an instance from the supplied values. */
    @SafeVarargs
    @NotNull
    public static <T extends FileType<?>> FileTypeGroup<T> of(@NotNull T... types) {
        FileTypeGroup<T> group = new FileTypeGroup<>(() -> Arrays.asList(types));
        if (types.length == 1) group.setDisplayName(types[0].getDisplayName());
        return group;
    }

    /** Creates an unlabeled dynamic group. */
    public FileTypeGroup(@NotNull Supplier<List<T>> typeSupplier) {
        this(typeSupplier, null);
    }

    /** Creates a dynamic group with an optional display name. */
    public FileTypeGroup(@NotNull Supplier<List<T>> typeSupplier, @Nullable Component displayName) {
        this.typeSupplier = typeSupplier;
        this.displayName = displayName;
    }

    /** Returns the supplier's current list, or an empty mutable list when it returns {@code null}. */
    @NotNull
    public List<T> getFileTypes() {
        return Objects.requireNonNullElse(this.typeSupplier.get(), new ArrayList<>());
    }

    /** Returns the supplier used to resolve current group members. */
    @NotNull
    public Supplier<List<T>> getTypeSupplier() {
        return this.typeSupplier;
    }

    /** Replaces the group-membership supplier. */
    public void setTypeSupplier(@NotNull Supplier<List<T>> typeSupplier) {
        this.typeSupplier = typeSupplier;
    }

    /** Returns the optional localized group label. */
    @Nullable
    public Component getDisplayName() {
        return this.displayName;
    }

    /** Sets or clears the localized group label. */
    public void setDisplayName(@Nullable Component displayName) {
        this.displayName = displayName;
    }

}
