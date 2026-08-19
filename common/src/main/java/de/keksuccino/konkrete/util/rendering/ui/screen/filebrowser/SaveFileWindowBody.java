package de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser;

import de.keksuccino.konkrete.util.file.FileFilter;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import de.keksuccino.konkrete.util.input.InputConstants;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.konkrete.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.function.Consumer;

/** Adds validated filename entry and overwrite confirmation to the file browser. */
@SuppressWarnings("all")
public class SaveFileWindowBody extends AbstractFileBrowserWindowBody {

    /** Localized label preceding the file-name field. */
    protected static final Component FILE_NAME_PREFIX_TEXT = Component.translatable("konkrete.file_browser.save_file.file_name");

    /** Optional extension appended to names that do not already contain it. */
    @Nullable
    protected String forcedFileExtension;
    /** Initial filename shown in the save input. */
    protected String defaultFileName;
    /** Whether entered file names are normalized to resource-path rules. */
    protected boolean forceResourceFriendlyFileNames = true;
    /** Edit box containing the proposed file name. */
    protected ExtendedEditBox fileNameEditBox;

    /** Creates a save dialog rooted at the supplied directory and restricted to an optional extension. */
    @NotNull
    public static SaveFileWindowBody build(@NotNull File rootDirectory, @Nullable String fileNamePreset, @Nullable String forcedFileExtension, @NotNull Consumer<File> callback) {
        return new SaveFileWindowBody(rootDirectory, rootDirectory, fileNamePreset, forcedFileExtension, callback);
    }

    /** Opens a confined save dialog with optional filename and extension defaults. */
    public SaveFileWindowBody(@Nullable File rootDirectory, @NotNull File startDirectory, @Nullable String fileNamePreset, @Nullable String forcedFileExtension, @NotNull Consumer<File> callback) {

        super(Component.translatable("konkrete.ui.save_file"), rootDirectory, startDirectory, callback);

        this.forcedFileExtension = forcedFileExtension;
        if (this.forcedFileExtension != null) {
            if (this.forcedFileExtension.startsWith(".")) this.forcedFileExtension = this.forcedFileExtension.substring(1);
            this.fileFilter = file -> file.getName().toLowerCase().endsWith("." + this.forcedFileExtension.toLowerCase());
        }

        this.fileNameEditBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 150, 18, Component.translatable("konkrete.ui.save_file.file_name"));
        if (this.forcedFileExtension != null) {
            this.fileNameEditBox.setInputSuffix("." + this.forcedFileExtension.toLowerCase());
            this.fileNameEditBox.applyInputPrefixSuffixCharacterRenderFormatter();
        }
        this.fileNameEditBox.setMaxLength(10000);
        UIBase.applyDefaultWidgetSkinTo(this.fileNameEditBox, UIBase.shouldBlur());

        String editBoxPresetValue = "new_file";
        if (fileNamePreset != null) {
            if ((this.forcedFileExtension != null) && (fileNamePreset.toLowerCase().endsWith("." + this.forcedFileExtension.toLowerCase()))) {
                fileNamePreset = fileNamePreset.substring(0, Math.max(1, fileNamePreset.length() - (this.forcedFileExtension.length() + 1)));
            }
            editBoxPresetValue = fileNamePreset;
        }
        if (this.forcedFileExtension != null) {
            editBoxPresetValue += "." + this.forcedFileExtension;
        }
        this.fileNameEditBox.setValue(editBoxPresetValue);
        this.fileNameEditBox.setCursorPosition(0);
        this.fileNameEditBox.setHighlightPos(0);
        this.fileNameEditBox.setDisplayPosition(0);
        this.defaultFileName = editBoxPresetValue;

        this.setForceResourceFriendlyFileNames(true);

        this.fileScrollListHeightOffset = -25;
        this.fileTypeScrollListYOffset = 25;

    }

    /** Initializes resources required by this save file window body. */
    @Override
    protected void init() {

        this.addRenderableWidget(this.fileNameEditBox);

        super.init();

    }

    /** Renders body into the active GUI extraction pass. */
    @Override
    public void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if ((this.forcedFileExtension != null) && !this.fileNameEditBox.getValue().toLowerCase().endsWith("." + this.forcedFileExtension.toLowerCase())) {
            this.fileNameEditBox.setValue(this.defaultFileName);
        }

        this.updateFileNameEditBoxBounds();
        super.renderBody(graphics, mouseX, mouseY, partial);

    }

    /** Renders late body into the active GUI extraction pass. */
    @Override
    public void renderLateBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.renderFileNameLabel(graphics);
    }

    /** Refreshes file name edit box bounds from current state. */
    protected void updateFileNameEditBoxBounds() {
        this.fileNameEditBox.setWidth(this.getBelowFileScrollAreaElementWidth() - 2);
        this.fileNameEditBox.setX((int)(this.fileListScrollArea.getXWithBorder() + this.fileListScrollArea.getWidthWithBorder() - this.fileNameEditBox.getWidth() - 1));
        this.fileNameEditBox.setY((int)(this.fileListScrollArea.getYWithBorder() + this.fileListScrollArea.getHeightWithBorder() + 5 + 1));
    }

    /** Renders file name label into the active GUI extraction pass. */
    protected void renderFileNameLabel(GuiGraphicsExtractor graphics) {
        float labelPadding = UIBase.getAreaLabelVerticalPadding();
        float labelWidth = UIBase.getUITextWidthNormal(FILE_NAME_PREFIX_TEXT);
        float labelHeight = UIBase.getUITextHeightNormal();
        float labelX = this.fileNameEditBox.getX() - 1 - labelWidth - labelPadding;
        float labelY = this.fileNameEditBox.getY() - 1 + (this.fileNameEditBox.getHeight() / 2.0F) - (labelHeight / 2.0F);
        UIBase.renderText(graphics, FILE_NAME_PREFIX_TEXT, labelX, labelY, UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt());
    }

    /** Returns below file scroll area element width. */
    @Override
    protected int getBelowFileScrollAreaElementWidth() {
        float labelPadding = UIBase.getAreaLabelVerticalPadding();
        float labelWidth = UIBase.getUITextWidthNormal(FILE_NAME_PREFIX_TEXT);
        int w = (int) (this.fileListScrollArea.getWidthWithBorder() - labelWidth - labelPadding);
        return Math.min(super.getBelowFileScrollAreaElementWidth(), w);
    }

    /** Builds the button that validates and commits the destination filename. */
    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("konkrete.ui.save_file.save"), (button) -> {
            this.trySave();
        }) {
            /** Adds this component's content draw state to the active GUI extraction pass. */
            @Override
            protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
                AbstractFileScrollAreaEntry selected = getSelectedEntry();
                this.active = canSave();
                super.extractContents(graphics, mouseX, mouseY, partial);
            }
        };
    }

    /** Sets file filter for this save file window body. */
    @Override
    public AbstractFileBrowserWindowBody setFileFilter(@Nullable FileFilter fileFilter) {
        if (this.forcedFileExtension == null) {
            return super.setFileFilter(fileFilter);
        } else {
            LOGGER.error("[KONKRETE] Can't set file filter for SaveFileScreen with forced file extension!");
        }
        return this;
    }

    /** Reports whether the pointer currently hovers the associated widget. */
    @Override
    protected boolean isWidgetHovered() {
        if (this.fileNameEditBox.isHoveredOrFocused()) return false;
        return super.isWidgetHovered();
    }

    /** Returns file name character filter. */
    @Nullable
    public CharacterFilter getFileNameCharacterFilter() {
        return this.fileNameEditBox.getCharacterFilter();
    }

    /** Sets file name character filter for this save file window body. */
    public SaveFileWindowBody setFileNameCharacterFilter(@Nullable CharacterFilter characterFilter) {
        if (this.forceResourceFriendlyFileNames) {
            LOGGER.error("[KONKRETE] Unable to set file name character filter for SaveFileScreen while 'forceResourceFriendlyFileNames' is enabled!");
            return this;
        }
        this.fileNameEditBox.setCharacterFilter(characterFilter);
        return this;
    }

    /** Sets file name for this save file window body. */
    public SaveFileWindowBody setFileName(@NotNull String fileName) {
        this.fileNameEditBox.setValue(fileName);
        return this;
    }

    /** Enables automatic normalization of saved resource filenames. */
    public boolean forceResourceFriendlyFileNames() {
        return this.forceResourceFriendlyFileNames;
    }

    /** Sets force resource friendly file names for this save file window body. */
    public SaveFileWindowBody setForceResourceFriendlyFileNames(boolean forceResourceFriendlyFileNames) {
        this.forceResourceFriendlyFileNames = forceResourceFriendlyFileNames;
        if (!this.forceResourceFriendlyFileNames) {
            this.fileNameEditBox.setCharacterFilter(null);
        } else {
            this.fileNameEditBox.setCharacterFilter(CharacterFilter.buildOnlyLowercaseFileNameFilter());
        }
        return this;
    }

    /** Validates the destination and returns it through the save callback when unused. */
    protected void trySave() {
        File f = this.getSaveFile();
        if (f != null) {
            if (!f.isFile()) {
                this.callback.accept(new File(f.getPath().replace("\\", "/")));
                this.closeWindow();
            } else {
                Dialogs.openMessageWithCallback(Component.translatable("konkrete.ui.save_file.save.override_warning"), MessageDialogStyle.WARNING, call -> {
                    if (call) {
                        try {
                            this.callback.accept(new File(f.getPath().replace("\\", "/")));
                            this.closeWindow();
                        } catch (Exception ex) {
                            LOGGER.error("[KONKRETE] Failed to accept the overwrite destination '{}'.", f, ex);
                        }
                    }
                });
            }
        }
    }

    /** Returns whether save. */
    protected boolean canSave() {
        return (this.getSaveFile() != null);
    }

    /** Returns save file. */
    @Nullable
    protected File getSaveFile() {
        AbstractFileScrollAreaEntry e = this.getSelectedEntry();
        if ((e != null) && e.file.isDirectory()) return null;
        if (!this.fileNameEditBox.getValue().replace(" ", "").isEmpty()) {
            File f = new File(this.currentDir, "/" + this.fileNameEditBox.getValue());
            if (!this.shouldShowFile(f)) return null;
            return f;
        }
        return null;
    }

    /** Builds a directory entry suitable for navigation in a save dialog. */
    @Override
    protected AbstractFileScrollAreaEntry buildFileEntry(@NotNull File f) {
        return new SaveFileScrollAreaEntry(this.fileListScrollArea, f);
    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keycode, int scancode, int modifiers) {

        if ((keycode == InputConstants.KEY_ENTER) || (keycode == InputConstants.KEY_NUMPADENTER)) {
            ScrollAreaEntry selectedEntry = this.getSelectedScrollEntry();
            if (selectedEntry instanceof ParentDirScrollAreaEntry) {
                this.goUpDirectory();
                return true;
            }
            if (selectedEntry instanceof AbstractFileScrollAreaEntry fileEntry) {
                if (!fileEntry.resourceUnfriendlyFileName && fileEntry.file.isDirectory()) {
                    this.setDirectory(fileEntry.file, true);
                    return true;
                }
            }
            this.trySave();
            return true;
        }

        return super.keyPressed(keycode, scancode, modifiers);

    }

    /** Represents one renderable, focusable save file scroll area entry. */
    public class SaveFileScrollAreaEntry extends AbstractFileScrollAreaEntry {

        /** Represents a file candidate inside the save dialog's scroll area. */
        public SaveFileScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {
            super(parent, file);
        }

        /** Handles click for this save file scroll area entry. */
        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyFileName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (this.file.isDirectory()) {
                    SaveFileWindowBody.this.setDirectory(this.file, true);
                } else if (this.file.isFile()) {
                    String name = this.file.getName();
                    if ((SaveFileWindowBody.this.forcedFileExtension == null) || (name.toLowerCase().endsWith("." + SaveFileWindowBody.this.forcedFileExtension.toLowerCase()))) {
                        SaveFileWindowBody.this.fileNameEditBox.setValue(name);
                    }
                }
            }
            SaveFileWindowBody.this.updatePreview(this.file);
            this.lastClick = now;
        }

    }

}
