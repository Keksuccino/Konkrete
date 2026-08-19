package de.keksuccino.konkrete.util.rendering;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinGuiGraphicsExtractor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Contains stateless helpers for GUI scissor. */
public final class GuiScissorUtil {

    private GuiScissorUtil() {
    }

    /** Returns the current scissor rectangle, or {@code null} when clipping is disabled. */
    @Nullable
    public static ScreenRectangle getActiveScissor(@NotNull GuiGraphicsExtractor graphics) {
        return ((AccessorMixinGuiGraphicsExtractor)graphics).get_scissorStack_Konkrete().peek();
    }

}
