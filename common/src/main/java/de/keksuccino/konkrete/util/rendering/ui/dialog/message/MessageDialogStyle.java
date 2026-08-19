package de.keksuccino.konkrete.util.rendering.ui.dialog.message;

import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcons;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Identifies one supported message dialog option. */
public enum MessageDialogStyle {

    /** Selects generic behavior. */
    GENERIC("generic", Component.translatable("konkrete.ui.dialog.title.message"), null),
    /** Selects info behavior. */
    INFO("info", Component.translatable("konkrete.ui.dialog.title.info"), MaterialIcons.INFO),
    /** Selects warning behavior. */
    WARNING("warning", Component.translatable("konkrete.ui.dialog.title.warning"), MaterialIcons.WARNING),
    /** Selects error behavior. */
    ERROR("error", Component.translatable("konkrete.ui.dialog.title.error"), MaterialIcons.ERROR);

    @NotNull
    private final String name;
    @NotNull
    private final Component title;
    @Nullable
    private final MaterialIcon icon;

    MessageDialogStyle(@NotNull String name, @NotNull Component title, @Nullable MaterialIcon icon) {
        this.name = name;
        this.title = title;
        this.icon = icon;
    }

    /** Returns the stable serialized name. */
    public @NotNull String getName() {
        return name;
    }

    /** Returns the title displayed by this screen, window, or style. */
    public @NotNull Component getTitle() {
        return title;
    }

    /** Returns the optional Material icon associated with this message style. */
    public @Nullable MaterialIcon getIcon() {
        return icon;
    }

}
