package de.keksuccino.konkrete.util.rendering.ui.screen.resource;

import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.file.FilenameComparator;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.file.type.groups.FileTypeGroup;
import de.keksuccino.konkrete.util.file.type.types.*;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser.AbstractBrowserWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.konkrete.util.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.konkrete.util.resource.ResourceSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Implements the interactive window body for resource picker. */
public class ResourcePickerWindowBody extends AbstractBrowserWindowBody {

    private static final Logger LOGGER = LogManager.getLogger();

    /** File types accepted by the current browser or chooser. */
    @Nullable
    protected FileTypeGroup<?> allowedFileTypes;
    /** Receives the resource accepted by the picker. */
    @NotNull
    protected Consumer<Identifier> callback;
    /** Namespace of the directory currently shown by the picker. */
    @Nullable
    protected String currentNamespace;
    /** Namespace-relative directory path currently shown by the picker. */
    @NotNull
    protected String currentPath = "";
    /** Resource initially selected when the picker opens. */
    @Nullable
    protected Identifier preselectedLocation;
    /** Whether to reject names that violate resource-path rules. */
    protected boolean blockResourceUnfriendlyNames = true;
    /** Whether rejected resource-unfriendly entries remain visible. */
    protected boolean showBlockedResourceUnfriendlyNames = true;
    @Nullable
    private Consumer<Identifier> previewApplyCallback;
    @Nullable
    private Runnable previewCancelCallback;

    /** Opens a pack-resource picker at an optional identifier with type filtering. */
    public ResourcePickerWindowBody(@Nullable Identifier startLocation, @Nullable FileTypeGroup<?> allowedFileTypes, @NotNull Consumer<Identifier> callback) {
        super(Component.translatable("konkrete.ui.resourcepicker.choose.resource"));
        this.setWindowAlwaysOnTop(false);
        this.setWindowBlocksMinecraftScreenInputs(false);
        this.setWindowForceFocus(false);
        this.allowedFileTypes = allowedFileTypes;
        this.callback = Objects.requireNonNull(callback);
        this.applyStartLocation(startLocation);
        this.updatePreviewForKey(null);
        this.updateResourceList();
        this.updateFileTypeScrollArea();
    }

    /** Applies start location to the supplied target. */
    protected void applyStartLocation(@Nullable Identifier startLocation) {
        this.preselectedLocation = startLocation;
        if (startLocation != null) {
            this.currentNamespace = startLocation.getNamespace();
            String path = startLocation.getPath();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > -1) {
                this.currentPath = path.substring(0, lastSlash);
            } else {
                this.currentPath = "";
            }
        }
    }

    /** Builds the button that commits the selected resource. */
    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("konkrete.common_components.ok"), (button) -> {
            ResourceScrollAreaEntry selected = this.getSelectedEntry();
            if ((selected != null) && !selected.resourceUnfriendlyName) {
                this.callback.accept(selected.location);
                this.closeWindow();
            }
        }) {
            /** Adds this component's content draw state to the active GUI extraction pass. */
            @Override
            protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
                ResourceScrollAreaEntry selected = ResourcePickerWindowBody.this.getSelectedEntry();
                this.active = (selected != null) && !selected.resourceUnfriendlyName;
                super.extractContents(graphics, mouseX, mouseY, partial);
            }
        };
    }

    /** Builds the optional button that previews a selection without closing. */
    @Override
    protected @Nullable ExtendedButton buildApplyButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("konkrete.common_components.apply"), (button) -> {
            Consumer<Identifier> callback = this.previewApplyCallback;
            if (callback == null) return;
            ResourceScrollAreaEntry selected = this.getSelectedEntry();
            if ((selected != null) && !selected.resourceUnfriendlyName) {
                callback.accept(selected.location);
            }
        }) {
            /** Adds this component's content draw state to the active GUI extraction pass. */
            @Override
            protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
                ResourceScrollAreaEntry selected = ResourcePickerWindowBody.this.getSelectedEntry();
                this.active = (ResourcePickerWindowBody.this.previewApplyCallback != null)
                        && (selected != null)
                        && !selected.resourceUnfriendlyName;
                super.extractContents(graphics, mouseX, mouseY, partial);
            }
        };
    }

    /** Handles cancel for this resource picker window body. */
    @Override
    protected void onCancel() {
        if (this.previewCancelCallback != null) {
            this.previewCancelCallback.run();
        }
        this.callback.accept(null);
    }

    /** Returns the label displayed for the current browser entries. */
    @Override
    @NotNull
    protected Component getEntriesLabel() {
        return Component.translatable("konkrete.ui.resourcepicker.resources");
    }

    /** Refreshes entry list from current state. */
    @Override
    protected void updateEntryList() {
        this.updateResourceList();
    }

    /** Moves the selection or viewport to up directory. */
    @Override
    protected boolean goUpDirectory() {
        if (!this.currentPath.isEmpty()) {
            int lastSlash = this.currentPath.lastIndexOf('/');
            if (lastSlash > -1) {
                String newPath = this.currentPath.substring(0, lastSlash);
                this.setDirectory(this.currentNamespace, newPath, true);
                return true;
            } else {
                this.setDirectory(this.currentNamespace, "", true);
                return true;
            }
        } else if (this.currentNamespace != null) {
            this.setDirectory(null, "", true);
            return true;
        }
        return false;
    }

    /** Reports whether this entry navigates to the parent location. */
    @Override
    protected boolean isGoUpEntry(@NotNull ScrollAreaEntry entry) {
        return entry instanceof ParentDirScrollAreaEntry;
    }

    /** Opens directory entry. */
    @Override
    protected boolean openDirectoryEntry(@NotNull ScrollAreaEntry entry) {
        if (entry instanceof DirectoryScrollAreaEntry dirEntry) {
            if (dirEntry.resourceUnfriendlyName) return false;
            this.setDirectory(dirEntry.namespace, dirEntry.path, true);
            return true;
        }
        return false;
    }

    /** Returns preview key for entry. */
    @Override
    protected Object getPreviewKeyForEntry(@NotNull ScrollAreaEntry entry) {
        if (entry instanceof ResourceScrollAreaEntry resourceEntry) {
            return resourceEntry.location;
        }
        return null;
    }

    /** Loads preview for key from the supplied source. */
    @Override
    protected void loadPreviewForKey(@NotNull Object previewKey) {
        if (!(previewKey instanceof Identifier location)) return;
        this.setTextPreview(location);
        if (this.isImageLocation(location)) {
            this.previewTextureSupplier = ResourceSupplier.image(location.toString());
            this.setPreviewAudio(null, null);
            this.setPreviewVideo(null, null);
        } else if (this.isAudioLocation(location)) {
            this.previewTextureSupplier = null;
            this.setPreviewVideo(null, null);
            this.setPreviewAudio(ResourceSupplier.audio(location.toString()), location);
        } else if (this.isVideoLocation(location)) {
            this.previewTextureSupplier = null;
            this.setPreviewAudio(null, null);
            this.setPreviewVideo(ResourceSupplier.video(location.toString()), location);
        } else {
            this.previewTextureSupplier = null;
            this.setPreviewAudio(null, null);
            this.setPreviewVideo(null, null);
        }
    }

    /** Returns the currently selected entry, or {@code null}. */
    @Nullable
    protected ResourceScrollAreaEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e instanceof ResourceScrollAreaEntry entry) {
                if (entry.isSelected()) return entry;
            }
        }
        return null;
    }

    /** Refreshes file type scroll area from current state. */
    @Override
    public void updateFileTypeScrollArea() {
        this.fileTypeScrollArea.clearEntries();
        this.currentFileTypesComponent = Component.translatable("konkrete.file_browser.file_type.types.all").append(" (*)");
        if (this.allowedFileTypes != null) {
            StringBuilder types = new StringBuilder();
            for (FileType<?> type : this.allowedFileTypes.getFileTypes()) {
                if (!type.isLocationAllowed()) continue;
                for (String s : type.getExtensions()) {
                    if (!types.isEmpty()) types.append(";");
                    types.append("*.").append(s.toUpperCase());
                }
            }
            Component fileTypeDisplayName = this.allowedFileTypes.getDisplayName();
            if (fileTypeDisplayName == null) fileTypeDisplayName = Component.empty();
            this.currentFileTypesComponent = Component.empty().append(fileTypeDisplayName).append(Component.literal(" (")).append(Component.literal(types.toString())).append(Component.literal(")"));
        }
        this.currentFileTypesComponent = this.currentFileTypesComponent.withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt()));
        TextScrollAreaEntry entry = new TextScrollAreaEntry(this.fileTypeScrollArea, this.currentFileTypesComponent, textScrollAreaEntry -> {});
        entry.setPlayClickSound(false);
        entry.setSelectable(false);
        entry.setBackgroundColorHover(entry.getBackgroundColorNormal());
        entry.setHeight(this.fileTypeScrollArea.getInnerHeight());
        this.fileTypeScrollArea.addEntry(entry);
    }

    /** Refreshes preview from current state. */
    public void updatePreview(@Nullable Identifier location) {
        this.updatePreviewForKey(location);
    }

    /** Sets preview apply callback for this resource picker window body. */
    public ResourcePickerWindowBody setPreviewApplyCallback(@Nullable Consumer<Identifier> previewApplyCallback) {
        this.previewApplyCallback = previewApplyCallback;
        return this;
    }

    /** Sets preview cancel callback for this resource picker window body. */
    public ResourcePickerWindowBody setPreviewCancelCallback(@Nullable Runnable previewCancelCallback) {
        this.previewCancelCallback = previewCancelCallback;
        return this;
    }

    /** Sets text preview for this resource picker window body. */
    protected void setTextPreview(@Nullable Identifier location) {
        if (location == null) {
            this.previewTextSupplier = null;
        } else {
            for (TextFileType type : FileTypes.getAllTextFileTypes()) {
                if (type.isFileTypeLocation(location)) {
                    this.previewTextSupplier = ResourceSupplier.text(location.toString());
                    return;
                }
            }
            this.previewTextSupplier = null;
        }
    }

    /** Refreshes resource list from current state. */
    protected void updateResourceList() {
        this.fileListScrollArea.clearEntries();
        if (!this.currentIsRootDirectory()) {
            ParentDirScrollAreaEntry e = new ParentDirScrollAreaEntry(this.fileListScrollArea);
            this.fileListScrollArea.addEntry(e);
        }

        String searchValue = this.getSearchValue();
        if (searchValue != null) {
            List<Identifier> matches = new ArrayList<>();
            this.collectSearchMatches(searchValue.toLowerCase(), matches);
            FilenameComparator comparator = new FilenameComparator();
            matches.sort((o1, o2) -> comparator.compare(this.getSearchSortKey(o1), this.getSearchSortKey(o2)));
            for (Identifier location : matches) {
                ResourceScrollAreaEntry entry = new ResourceScrollAreaEntry(this.fileListScrollArea, location);
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyLocation(location);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.fileListScrollArea.addEntry(entry);
                if (this.preselectedLocation != null && this.preselectedLocation.equals(location)) {
                    entry.setSelected(true);
                    this.updatePreview(location);
                }
            }
            return;
        }

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (this.currentNamespace == null) {
            List<String> namespaces = new ArrayList<>(resourceManager.getNamespaces());
            FilenameComparator comparator = new FilenameComparator();
            namespaces.sort(comparator);
            for (String namespace : namespaces) {
                DirectoryScrollAreaEntry entry = new DirectoryScrollAreaEntry(this.fileListScrollArea, namespace, namespace, "");
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyName(namespace);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.fileListScrollArea.addEntry(entry);
            }
        } else {
            String prefix = this.currentPath.isEmpty() ? "" : this.currentPath + "/";
            Set<String> directories = new HashSet<>();
            List<Identifier> files = new ArrayList<>();
            Set<Identifier> allLocations = this.getAllResourceLocations();

            for (Identifier location : allLocations) {
                if (!Objects.equals(location.getNamespace(), this.currentNamespace)) continue;
                String path = location.getPath();
                if (!prefix.isEmpty()) {
                    if (!path.startsWith(prefix)) continue;
                    path = path.substring(prefix.length());
                }
                if (path.isEmpty()) continue;
                int slashIndex = path.indexOf('/');
                if (slashIndex >= 0) {
                    directories.add(path.substring(0, slashIndex));
                } else {
                    if (isAllowedLocation(location)) {
                        files.add(location);
                    }
                }
            }

            FilenameComparator comparator = new FilenameComparator();
            List<String> sortedDirs = new ArrayList<>(directories);
            sortedDirs.sort(comparator);
            for (String dir : sortedDirs) {
                String fullPath = prefix.isEmpty() ? dir : prefix + dir;
                DirectoryScrollAreaEntry entry = new DirectoryScrollAreaEntry(this.fileListScrollArea, dir, this.currentNamespace, fullPath);
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyName(dir);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.fileListScrollArea.addEntry(entry);
            }

            files.sort((o1, o2) -> comparator.compare(getLocationDisplayName(o1), getLocationDisplayName(o2)));
            for (Identifier location : files) {
                ResourceScrollAreaEntry entry = new ResourceScrollAreaEntry(this.fileListScrollArea, location);
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyLocation(location);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.fileListScrollArea.addEntry(entry);
                if (this.preselectedLocation != null && this.preselectedLocation.equals(location)) {
                    entry.setSelected(true);
                    this.updatePreview(location);
                }
            }
        }
    }

    /** Collects search matches matching the supplied criteria. */
    protected void collectSearchMatches(@NotNull String searchLower, @NotNull List<Identifier> matches) {
        String prefix = this.currentPath.isEmpty() ? "" : this.currentPath + "/";
        for (Identifier location : this.getAllResourceLocations()) {
            if (!isAllowedLocation(location)) continue;
            if (this.currentNamespace != null) {
                if (!Objects.equals(location.getNamespace(), this.currentNamespace)) continue;
                String path = location.getPath();
                if (!prefix.isEmpty()) {
                    if (!path.startsWith(prefix)) continue;
                    path = path.substring(prefix.length());
                }
                if (path.isEmpty()) continue;
            }
            if (this.locationMatchesSearch(location, searchLower)) {
                matches.add(location);
            }
        }
    }

    /** Returns whether a resource location matches the active search filter. */
    protected boolean locationMatchesSearch(@NotNull Identifier location, @NotNull String searchLower) {
        String fileNameLower = getLocationDisplayName(location).toLowerCase();
        if (fileNameLower.contains(searchLower)) return true;
        String relativePath = this.getRelativePathForLocation(location);
        return (relativePath != null) && relativePath.toLowerCase().contains(searchLower);
    }

    /** Returns search sort key. */
    protected String getSearchSortKey(@NotNull Identifier location) {
        String relativePath = this.getRelativePathForLocation(location);
        if (relativePath != null) return relativePath;
        return this.getLocationDisplayName(location);
    }

    /** Returns relative path for location. */
    @Nullable
    protected String getRelativePathForLocation(@NotNull Identifier location) {
        String s = location.getNamespace() + "/" + location.getPath();
        if (this.currentNamespace == null) {
            return s;
        }
        if (!Objects.equals(location.getNamespace(), this.currentNamespace)) {
            return s;
        }
        String prefix = this.currentPath.isEmpty() ? "" : this.currentPath + "/";
        String path = location.getPath();
        if (!prefix.isEmpty() && path.startsWith(prefix)) {
            String relative = path.substring(prefix.length());
            return relative.isEmpty() ? this.getLocationDisplayName(location) : relative;
        }
        return path;
    }

    /** Returns all resource locations. */
    @NotNull
    protected Set<Identifier> getAllResourceLocations() {
        return Services.PLATFORM.getLoadedClientResourceLocations();
    }

    /** Reports whether the directory is a filesystem root. */
    protected boolean currentIsRootDirectory() {
        return this.currentNamespace == null;
    }

    /** Sets directory for this resource picker window body. */
    public void setDirectory(@Nullable String namespace, @NotNull String path, boolean playSound) {
        if (playSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.updatePreviewForKey(null);
        this.currentNamespace = namespace;
        this.currentPath = path;
        this.preselectedLocation = null;
        this.updateResourceList();
        this.updateCurrentDirectoryComponent();
    }

    /** Returns whether allowed location. */
    protected boolean isAllowedLocation(@NotNull Identifier location) {
        if (this.allowedFileTypes == null) return true;
        for (FileType<?> type : this.allowedFileTypes.getFileTypes()) {
            if (!type.isLocationAllowed()) continue;
            if (type.isFileTypeLocation(location)) return true;
        }
        return false;
    }

    /** Returns whether image location. */
    protected boolean isImageLocation(@NotNull Identifier location) {
        for (ImageFileType type : FileTypes.getAllImageFileTypes()) {
            if (type.isFileTypeLocation(location)) return true;
        }
        return false;
    }

    /** Returns whether audio location. */
    protected boolean isAudioLocation(@NotNull Identifier location) {
        for (AudioFileType type : FileTypes.getAllAudioFileTypes()) {
            if (type.isFileTypeLocation(location)) return true;
        }
        return false;
    }

    /** Returns whether video location. */
    protected boolean isVideoLocation(@NotNull Identifier location) {
        for (VideoFileType type : FileTypes.getAllVideoFileTypes()) {
            if (type.isFileTypeLocation(location)) return true;
        }
        return false;
    }

    /** Returns whether resource friendly name. */
    protected boolean isResourceFriendlyName(@NotNull String name) {
        return CharacterFilter.buildResourceNameFilter().isAllowedText(name);
    }

    /** Returns whether resource friendly location. */
    protected boolean isResourceFriendlyLocation(@NotNull Identifier location) {
        String combined = location.getNamespace() + "/" + location.getPath();
        combined = combined.replace("/", "");
        return CharacterFilter.buildResourceNameFilter().isAllowedText(combined);
    }

    /** Returns location display name. */
    protected String getLocationDisplayName(@NotNull Identifier location) {
        String path = location.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) return path.substring(lastSlash + 1);
        return path;
    }

    /** Returns display name for location. */
    protected String getDisplayNameForLocation(@NotNull Identifier location) {
        String searchValue = this.getSearchValue();
        if (searchValue != null) {
            String relative = this.getRelativePathForLocation(location);
            if (relative != null) return relative;
        }
        return this.getLocationDisplayName(location);
    }

    /** Refreshes current directory component from current state. */
    @Override
    protected void updateCurrentDirectoryComponent() {
        try {

            if (this.currentDirectoryComponent != null) {
                this.removeWidget(this.currentDirectoryComponent);
            }
            this.currentDirectoryComponent = ComponentWidget.literal("/", 0, 0)
                    .setTextSupplier(consumes -> {
                        if (consumes.isHoveredOrFocused()) return Component.literal("/").withStyle(Style.EMPTY.withUnderlined(true));
                        return Component.literal("/");
                    })
                    .setOnClick(componentWidget -> this.setDirectory(null, "", true));

            if (this.currentNamespace != null) {
                ComponentWidget namespaceWidget = ComponentWidget.empty(0, 0)
                        .setTextSupplier(consumes -> {
                            if (consumes.isHoveredOrFocused()) return Component.literal(this.currentNamespace).withStyle(Style.EMPTY.withUnderlined(true));
                            return Component.literal(this.currentNamespace);
                        })
                        .setOnClick(componentWidget -> this.setDirectory(this.currentNamespace, "", true));
                this.currentDirectoryComponent.append(namespaceWidget);
                this.currentDirectoryComponent.append(ComponentWidget.literal("/", 0, 0));
                if (!this.currentPath.isEmpty()) {
                    String[] parts = this.currentPath.split("/");
                    StringBuilder current = new StringBuilder();
                    for (String part : parts) {
                        if (!current.isEmpty()) current.append("/");
                        current.append(part);
                        String targetPath = current.toString();
                        ComponentWidget w = ComponentWidget.empty(0, 0)
                                .setTextSupplier(consumes -> {
                                    if (consumes.isHoveredOrFocused()) return Component.literal(part).withStyle(Style.EMPTY.withUnderlined(true));
                                    return Component.literal(part);
                                })
                                .setOnClick(componentWidget -> this.setDirectory(this.currentNamespace, targetPath, true));
                        this.currentDirectoryComponent.append(w);
                        this.currentDirectoryComponent.append(ComponentWidget.literal("/", 0, 0));
                    }
                }
            }

            while (this.currentDirectoryComponent.getWidth() > (this.width - 260 - 20 - 8)) {
                if (!this.currentDirectoryComponent.getChildren().isEmpty()) {
                    this.currentDirectoryComponent.getChildren().remove(0);
                } else {
                    break;
                }
            }
            if (!this.currentDirectoryComponent.getChildren().isEmpty()) {
                ComponentWidget firstChild = this.currentDirectoryComponent.getChildren().get(0);
                if (firstChild.getText().getString().equals("/")) this.currentDirectoryComponent.getChildren().remove(0);
            }

            this.currentDirectoryComponent.setShadow(false);
            this.currentDirectoryComponent.setUseUIFont(true);
            this.currentDirectoryComponent.setBaseColorSupplier(consumes -> UIBase.getUITheme().ui_interface_widget_label_color_normal);
            this.addWidget(this.currentDirectoryComponent);

        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to update current directory Component in ResourcePickerScreen!", ex);
        }
    }

    /** Base implementation for abstract resource scroll area entry. */
    public abstract class AbstractResourceScrollAreaEntry extends AbstractIconTextScrollAreaEntry {

        /** Whether the selected resource name violates resource-path rules. */
        protected boolean resourceUnfriendlyName = false;

        /** Attaches a named resource entry to its owning scroll area. */
        public AbstractResourceScrollAreaEntry(@NotNull ScrollArea parent, @NotNull String entryName) {
            super(parent, Component.literal(entryName));
        }

        /** Reports whether the entry name violates resource-path rules. */
        @Override
        protected boolean isResourceUnfriendly() {
            return this.resourceUnfriendlyName;
        }

        /** Handles click for this abstract resource scroll area entry. */
        @Override
        public abstract void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button);

    }

    /** Represents one renderable, focusable directory scroll area entry. */
    public class DirectoryScrollAreaEntry extends AbstractResourceScrollAreaEntry {

        /** Namespace entered when this directory entry is activated. */
        protected final String namespace;
        /** Namespace-relative path entered when this directory entry is activated. */
        protected final String path;

        /** Represents a child directory inside its owning browser scroll area. */
        public DirectoryScrollAreaEntry(@NotNull ScrollArea parent, @NotNull String entryName, @Nullable String namespace, @NotNull String path) {
            super(parent, entryName);
            this.namespace = namespace;
            this.path = path;
        }

        /** Returns the folder icon used for child-directory entries. */
        @Override
        protected @NotNull MaterialIcon getIcon() {
            return FOLDER_ICON;
        }

        /** Returns the per-edge icon inset in GUI units. */
        @Override
        protected int getIconInnerPadding() {
            return 5;
        }

        /** Handles click for this directory scroll area entry. */
        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                ResourcePickerWindowBody.this.setDirectory(this.namespace, this.path, true);
            }
            ResourcePickerWindowBody.this.updatePreview(null);
            this.lastClick = now;
        }

    }

    /** Represents one renderable, focusable resource scroll area entry. */
    public class ResourceScrollAreaEntry extends AbstractResourceScrollAreaEntry {

        /** Resource represented by this picker entry. */
        protected final Identifier location;
        /** Detected file type for {@link #location}, or {@code null} when unknown. */
        @Nullable
        protected final FileType<?> fileType;

        /** Represents a selectable resource identifier in its owning scroll area. */
        public ResourceScrollAreaEntry(@NotNull ScrollArea parent, @NotNull Identifier location) {
            super(parent, ResourcePickerWindowBody.this.getDisplayNameForLocation(location));
            this.location = location;
            this.fileType = FileTypes.getLocationType(this.location);
        }

        /** Returns the icon matching the resource file type, or the generic-file fallback. */
        @Override
        protected @NotNull MaterialIcon getIcon() {
            if (this.fileType != null) {
                if (this.fileType instanceof TextFileType) return TEXT_FILE_ICON;
                if (this.fileType instanceof VideoFileType) return VIDEO_FILE_ICON;
                if (this.fileType instanceof AudioFileType) return AUDIO_FILE_ICON;
                if (this.fileType instanceof ImageFileType) return IMAGE_FILE_ICON;
            }
            return GENERIC_FILE_ICON;
        }

        /** Returns the per-edge icon inset in GUI units. */
        @Override
        protected int getIconInnerPadding() {
            return 5;
        }

        /** Handles click for this resource scroll area entry. */
        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                ResourcePickerWindowBody.this.callback.accept(this.location);
                ResourcePickerWindowBody.this.closeWindow();
            }
            ResourcePickerWindowBody.this.updatePreview(this.location);
            this.lastClick = now;
        }

    }

    /** Represents one renderable, focusable parent dir scroll area entry. */
    public class ParentDirScrollAreaEntry extends AbstractIconTextScrollAreaEntry {

        /** Creates a browser entry that navigates to the current directory's parent. */
        public ParentDirScrollAreaEntry(@NotNull ScrollArea parent) {
            super(parent, Component.translatable("konkrete.ui.filechooser.go_up").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt()).withBold(true)));
        }

        /** Returns the upward-navigation icon. */
        @Override
        protected @NotNull MaterialIcon getIcon() {
            return GO_UP_ICON;
        }

        /** Returns the per-edge icon inset in GUI units. */
        @Override
        protected int getIconInnerPadding() {
            return 5;
        }

        /** Handles click for this parent dir scroll area entry. */
        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                ResourcePickerWindowBody.this.goUpDirectory();
            }
            ResourcePickerWindowBody.this.updatePreview(null);
            this.lastClick = now;
        }

    }

}
