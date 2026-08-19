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

/** Implements the interactive window body for choose directory. */
public class ChooseDirectoryWindowBody extends AbstractFileBrowserWindowBody {

    /** Creates a directory chooser rooted at the supplied folder. */
    @NotNull
    public static ChooseDirectoryWindowBody build(@NotNull File rootDirectory, @NotNull Consumer<File> callback) {
        return new ChooseDirectoryWindowBody(rootDirectory, rootDirectory, callback);
    }

    /** Opens a directory chooser at the supplied location and installs its selection callback. */
    public ChooseDirectoryWindowBody(@Nullable File rootDirectory, @NotNull File startDirectory, @NotNull Consumer<File> callback) {
        super(Component.translatable("konkrete.ui.filechooser.choose.directory"), rootDirectory, startDirectory, callback);
        this.setWindowAlwaysOnTop(false);
        this.setWindowBlocksMinecraftScreenInputs(false);
        this.setWindowForceFocus(false);
    }

    /** Builds the button that commits the displayed directory. */
    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("konkrete.ui.filechooser.choose.directory.confirm"), button -> {
            File selectedDirectory = this.getSelectedDirectory();
            if (selectedDirectory != null) {
                this.callback.accept(new File(selectedDirectory.getPath().replace("\\", "/")));
                this.closeWindow();
            }
        }) {
            /** Adds this component's content draw state to the active GUI extraction pass. */
            @Override
            protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
                this.active = ChooseDirectoryWindowBody.this.getSelectedDirectory() != null;
                super.extractContents(graphics, mouseX, mouseY, partial);
            }
        };
    }

    /** Omits the file-preview apply control for directory selection. */
    @Override
    protected @Nullable ExtendedButton buildApplyButton() {
        return null;
    }

    /** Builds navigable entries for directories and non-selectable entries for files. */
    @Override
    protected AbstractFileScrollAreaEntry buildFileEntry(@NotNull File file) {
        return new DirectoryScrollAreaEntry(this.fileListScrollArea, file);
    }

    /** Handles cancel for this choose directory window body. */
    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    /** Returns selected directory. */
    @Nullable
    protected File getSelectedDirectory() {
        AbstractFileScrollAreaEntry selected = this.getSelectedEntry();
        if ((selected != null) && selected.file.isDirectory() && !selected.resourceUnfriendlyFileName) {
            return selected.file;
        }
        if ((this.currentDir != null) && this.currentDir.isDirectory()) {
            return this.currentDir;
        }
        return null;
    }

    /** Represents one renderable, focusable directory scroll area entry. */
    protected class DirectoryScrollAreaEntry extends AbstractFileScrollAreaEntry {

        /** Represents a child directory inside its owning browser scroll area. */
        public DirectoryScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {
            super(parent, file);
        }

        /** Handles click for this directory scroll area entry. */
        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyFileName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (this.file.isDirectory()) {
                    ChooseDirectoryWindowBody.this.setDirectory(this.file, true);
                }
            }
            ChooseDirectoryWindowBody.this.updatePreview(this.file);
            this.lastClick = now;
        }

    }

}
