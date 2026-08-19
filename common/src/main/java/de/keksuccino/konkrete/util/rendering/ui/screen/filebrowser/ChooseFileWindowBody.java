package de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser;

import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.function.Consumer;

/** Implements the interactive window body for choose file. */
@SuppressWarnings("all")
public class ChooseFileWindowBody extends AbstractFileBrowserWindowBody {

    @Nullable
    private Consumer<File> previewApplyCallback;
    @Nullable
    private Runnable previewCancelCallback;

    /** Creates a file chooser rooted at the supplied directory. */
    @NotNull
    public static ChooseFileWindowBody build(@NotNull File rootDirectory, @NotNull Consumer<File> callback) {
        return new ChooseFileWindowBody(rootDirectory, rootDirectory, callback);
    }

    /** Opens a file chooser at the supplied location and installs its selection callback. */
    public ChooseFileWindowBody(@Nullable File rootDirectory, @NotNull File startDirectory, @NotNull Consumer<File> callback) {
        super(Component.translatable("konkrete.ui.filechooser.choose.file"), rootDirectory, startDirectory, callback);
        this.setWindowAlwaysOnTop(false);
        this.setWindowBlocksMinecraftScreenInputs(false);
        this.setWindowForceFocus(false);
    }

    /** Builds the button that commits the selected file. */
    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("konkrete.common_components.ok"), (button) -> {
            AbstractFileScrollAreaEntry selected = this.getSelectedEntry();
            if ((selected != null) && !selected.resourceUnfriendlyFileName) {
                this.callback.accept(new File(selected.file.getPath().replace("\\", "/")));
                this.closeWindow();
            }
        }) {
            /** Adds this component's content draw state to the active GUI extraction pass. */
            @Override
            protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
                AbstractFileScrollAreaEntry e = ChooseFileWindowBody.this.getSelectedEntry();
                this.active = (e != null) && !e.resourceUnfriendlyFileName && (e.file.isFile());
                super.extractContents(graphics, mouseX, mouseY, partial);
            }
        };
    }

    /** Builds the optional preview/apply button when an apply callback is configured. */
    @Override
    protected @Nullable ExtendedButton buildApplyButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("konkrete.common_components.apply"), (button) -> {
            Consumer<File> callback = this.previewApplyCallback;
            if (callback == null) return;
            File selected = this.getSelectedFile();
            if ((selected != null) && selected.isFile()) {
                callback.accept(selected);
            }
        }) {
            /** Adds this component's content draw state to the active GUI extraction pass. */
            @Override
            protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
                AbstractFileScrollAreaEntry selected = ChooseFileWindowBody.this.getSelectedEntry();
                this.active = (ChooseFileWindowBody.this.previewApplyCallback != null)
                        && (selected != null)
                        && !selected.resourceUnfriendlyFileName
                        && selected.file.isFile();
                super.extractContents(graphics, mouseX, mouseY, partial);
            }
        };
    }

    /** Handles cancel for this choose file window body. */
    @Override
    protected void onCancel() {
        if (this.previewCancelCallback != null) {
            this.previewCancelCallback.run();
        }
        super.onCancel();
    }

    /** Builds a selectable file or navigable directory entry. */
    @Override
    protected AbstractFileScrollAreaEntry buildFileEntry(@NotNull File f) {
        return new FileScrollAreaEntry(this.fileListScrollArea, f);
    }

    /** Sets preview apply callback for this choose file window body. */
    public ChooseFileWindowBody setPreviewApplyCallback(@Nullable Consumer<File> previewApplyCallback) {
        this.previewApplyCallback = previewApplyCallback;
        return this;
    }

    /** Sets preview cancel callback for this choose file window body. */
    public ChooseFileWindowBody setPreviewCancelCallback(@Nullable Runnable previewCancelCallback) {
        this.previewCancelCallback = previewCancelCallback;
        return this;
    }

    /** Represents one renderable, focusable file scroll area entry. */
    public class FileScrollAreaEntry extends AbstractFileScrollAreaEntry {

        /** Represents a selectable file inside its owning browser scroll area. */
        public FileScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {
            super(parent, file);
        }

        /** Handles click for this file scroll area entry. */
        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyFileName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (this.file.isFile()) {
                    ChooseFileWindowBody.this.callback.accept(new File(this.file.getPath().replace("\\", "/")));
                    ChooseFileWindowBody.this.closeWindow();
                } else if (this.file.isDirectory()) {
                    ChooseFileWindowBody.this.setDirectory(this.file, true);
                }
            }
            ChooseFileWindowBody.this.updatePreview(this.file);
            this.lastClick = now;
        }

    }

}
