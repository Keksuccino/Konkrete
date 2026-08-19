package de.keksuccino.konkrete.util.rendering.ui.dialog;

import de.keksuccino.konkrete.util.Pair;
import de.keksuccino.konkrete.util.rendering.ui.dialog.message.MessageDialogBody;
import de.keksuccino.konkrete.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PipableScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/** Opens reusable message and content dialogs as PiP windows. */
public class Dialogs {

    /** Opens generic. */
    public static <D extends Screen & PipableScreen> Pair<D, PiPWindow> openGeneric(@NotNull D dialog, @Nullable Component title, @Nullable Identifier icon, int width, int height) {
        PiPWindow window = new PiPWindow((title != null) ? title : Component.empty())
                .setScreen(dialog)
                .setIcon(icon)
                .setSize(width, height)
                .setMinSize(width, height)
                .setAlwaysOnTop(true)
                .setForceFocus(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceKonkreteUiScale(true);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        return Pair.of(dialog, window);
    }

    /** Opens message. */
    public static MessageDialogBody openMessage(@NotNull Component message, @NotNull MessageDialogStyle style) {
        return openMessageInternal(message, style, null);
    }

    /** Opens warning message. */
    public static MessageDialogBody openWarningMessage(@NotNull Component message) {
        return openMessage(message, MessageDialogStyle.WARNING).setForceOkOnly(true);
    }

    /** Opens expensive FMA warning. */
    public static MessageDialogBody openExpensiveFmaWarning(@NotNull String fmaName) {
        MessageDialogBody body = openWarningMessage(Component.translatable("konkrete.resources.fma.expensive.warning", Objects.requireNonNull(fmaName)));
        PiPWindow window = body.getWindow();
        if (window != null) {
            window.setMinSize(514, 254);
            window.setSize(514, 254);
        }
        return body;
    }

    /** Opens message with callback. */
    public static MessageDialogBody openMessageWithCallback(@NotNull Component message, @NotNull MessageDialogStyle style, @NotNull Consumer<Boolean> callback) {
        MessageDialogBody body = openMessageInternal(message, style, Objects.requireNonNull(callback));
        if ((style == MessageDialogStyle.INFO) || (style == MessageDialogStyle.ERROR)) body.setForceOkOnly(true);
        return body;
    }

    private static MessageDialogBody openMessageInternal(@NotNull Component message, @NotNull MessageDialogStyle style, @Nullable Consumer<Boolean> callback) {

        MessageDialogBody body = new MessageDialogBody(message, style, callback);
        PiPWindow window = new PiPWindow(style.getTitle())
                .setIcon(style.getIcon())
                .setMinSize(408, 180)
                .setSize(408, 180)
                .setScreen(body)
                .setBlockMinecraftScreenInputs(true)
                .setForceKonkreteUiScale(true)
                .setForceFocus(true);

        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        return body;

    }

}
