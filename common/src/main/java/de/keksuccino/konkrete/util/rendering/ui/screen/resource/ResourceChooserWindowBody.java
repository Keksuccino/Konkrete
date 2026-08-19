package de.keksuccino.konkrete.util.rendering.ui.screen.resource;

import de.keksuccino.konkrete.placeholder.PlaceholderParser;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.konkrete.util.enums.LocalizedCycleEnum;
import de.keksuccino.konkrete.util.file.FileFilter;
import de.keksuccino.konkrete.util.file.GameDirectoryUtils;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.file.type.groups.FileTypeGroup;
import de.keksuccino.konkrete.util.file.type.groups.FileTypeGroups;
import de.keksuccino.konkrete.util.file.type.types.FileTypes;
import de.keksuccino.konkrete.util.file.type.types.AudioFileType;
import de.keksuccino.konkrete.util.file.type.types.ImageFileType;
import de.keksuccino.konkrete.util.file.type.types.TextFileType;
import de.keksuccino.konkrete.util.file.type.types.VideoFileType;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.UIConfiguration;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser.ChooseFileWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.util.resource.Resource;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import de.keksuccino.konkrete.util.resource.resources.text.IText;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.resource.resources.texture.PngTexture;
import de.keksuccino.konkrete.util.resource.resources.video.IVideo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Implements the interactive window body for resource chooser. */
@SuppressWarnings("unused")
public class ResourceChooserWindowBody<R extends Resource, F extends FileType<R>> extends PiPCellWindowBody {

    private static final Logger LOGGER = LogManager.getLogger();
    /** Resource identifier for warning. */
    protected static final PngTexture WARNING_TEXTURE = PngTexture.location(Identifier.fromNamespaceAndPath("konkrete", "textures/warning_framed_24x24.png"));
    /** Width in GUI units for PiP window. */
    public static final int PIP_WINDOW_WIDTH = 600;
    /** Height in GUI units for PiP window. */
    public static final int PIP_WINDOW_HEIGHT = 446;

    /** File types accepted by the current browser or chooser. */
    @Nullable
    protected FileTypeGroup<F> allowedFileTypes;
    /** Optional local-file filter applied before chooser entries are shown. */
    @Nullable
    protected FileFilter fileFilter;
    /** Callback receiving the committed resource source string. */
    @NotNull
    protected Consumer<String> resourceSourceCallback;
    /** Serialized source selected by the active local, URL, or pack-resource tab. */
    @Nullable
    protected String resourceSource;
    /** Resource-source type currently selected by the location cycle. */
    @NotNull
    protected ResourceSourceType resourceSourceType = ResourceSourceType.LOCATION;
    /** Whether manual resource locations may be selected. */
    protected boolean allowLocation = true;
    /** Whether local resources may be selected. */
    protected boolean allowLocal = true;
    /** Whether web resources may be selected. */
    protected boolean allowWeb = true;
    /** Callback applying the resource currently shown in the preview pane. */
    @Nullable
    protected Consumer<String> previewApplyCallback;
    /** Optional action that cancels an active resource preview. */
    @Nullable
    protected Runnable previewCancelCallback;
    /** Cycles between local, web, and resource-location input modes. */
    @Nullable
    protected CycleButton<ResourceSourceType> resourceSourceTypeCycleButton;
    /** Edit box containing the resource location. */
    protected ExtendedEditBox editBox;
    /** Whether to warn about legacy local-resource syntax. */
    protected boolean showWarningLegacyLocal = false;
    /** Whether to warn when the location has no file extension. */
    protected boolean showWarningNoExtension = false;
    /** Whether the pointer currently hovers the warning indicator. */
    protected boolean warningHovered = false;

    /** Creates a resource chooser restricted to generic resources. */
    @NotNull
    public static ResourceChooserWindowBody<Resource, FileType<Resource>> generic(@Nullable FileTypeGroup<FileType<Resource>> fileTypes, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserWindowBody<>(Component.translatable("konkrete.resources.chooser_screen.choose.generic"), fileTypes, fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to generic resources. */
    @NotNull
    public static ResourceChooserWindowBody<Resource, FileType<Resource>> generic(@NotNull Component title, @Nullable FileTypeGroup<FileType<Resource>> fileTypes, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserWindowBody<>(title, fileTypes, fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to image resources. */
    @NotNull
    public static ResourceChooserWindowBody<ITexture, ImageFileType> image(@NotNull Component title, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserWindowBody<>(title, FileTypeGroups.IMAGE_TYPES, fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to image resources. */
    @NotNull
    public static ResourceChooserWindowBody<ITexture, ImageFileType> image(@Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return image(Component.translatable("konkrete.resources.chooser_screen.choose.image"), fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to audio resources. */
    @NotNull
    public static ResourceChooserWindowBody<IAudio, AudioFileType> audio(@NotNull Component title, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserWindowBody<>(title, FileTypeGroups.AUDIO_TYPES, fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to audio resources. */
    @NotNull
    public static ResourceChooserWindowBody<IAudio, AudioFileType> audio(@Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return audio(Component.translatable("konkrete.resources.chooser_screen.choose.audio"), fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to video resources. */
    @NotNull
    public static ResourceChooserWindowBody<IVideo, VideoFileType> video(@NotNull Component title, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserWindowBody<>(title, FileTypeGroups.VIDEO_TYPES, fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to video resources. */
    @NotNull
    public static ResourceChooserWindowBody<IVideo, VideoFileType> video(@Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return video(Component.translatable("konkrete.resources.chooser_screen.choose.video"), fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to text resources. */
    @NotNull
    public static ResourceChooserWindowBody<IText, TextFileType> text(@NotNull Component title, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserWindowBody<>(title, FileTypeGroups.TEXT_TYPES, fileFilter, resourceSourceCallback);
    }

    /** Creates a resource chooser restricted to text resources. */
    @NotNull
    public static ResourceChooserWindowBody<IText, TextFileType> text(@Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return text(Component.translatable("konkrete.resources.chooser_screen.choose.text"), fileFilter, resourceSourceCallback);
    }

    /** Builds a resource chooser with file-type filtering and a serialized-source callback. */
    public ResourceChooserWindowBody(@NotNull Component title, @Nullable FileTypeGroup<F> allowedFileTypes, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        super(title);
        this.allowedFileTypes = allowedFileTypes;
        this.fileFilter = fileFilter;
        this.resourceSourceCallback = resourceSourceCallback;
    }

    /** Opens in window. */
    public @NotNull PiPWindow openInWindow(@Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(this.getTitle())
                .setScreen(this)
                .setForceKonkreteUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }


    /** Initializes cells. */
    @Override
    protected void initCells() {

        List<ResourceSourceType> allowedSourceTypes = new ArrayList<>(3);
        if (this.allowLocation) allowedSourceTypes.add(ResourceSourceType.LOCATION);
        if (this.allowLocal) allowedSourceTypes.add(ResourceSourceType.LOCAL);
        if (this.allowWeb) allowedSourceTypes.add(ResourceSourceType.WEB);

        if (allowedSourceTypes.isEmpty()) {
            throw new IllegalStateException("There needs to be at least one allowed source type!");
        }

        //Fix resource type if selected is not allowed
        if (!allowedSourceTypes.contains(this.resourceSourceType)) {
            this.resourceSource = null;
            this.resourceSourceType = allowedSourceTypes.get(0);
        }

        boolean isLocal = (this.resourceSourceType == ResourceSourceType.LOCAL);
        boolean isLegacyLocal = (isLocal && (this.resourceSource != null) && !this.resourceSource.trim().isEmpty() && !(this.resourceSource.startsWith("config/konkrete/assets/") || this.resourceSource.startsWith("/config/konkrete/assets/")));

        if (isLocal && (this.resourceSource == null)) this.resourceSource = "/config/konkrete/assets/";

        //Fix local sources that don't start with "/"
        if (isLocal && !this.resourceSource.startsWith("/") && this.resourceSource.startsWith("config/konkrete/assets/")) {
            this.resourceSource = "/" + this.resourceSource;
        }

        this.addStartEndSpacerCell();

        if (allowedSourceTypes.size() > 1) {
            LocalizedGenericValueCycle<ResourceSourceType> sourceTypeCycle = buildSourceTypeCycle(allowedSourceTypes, this.resourceSourceType);
            this.resourceSourceTypeCycleButton = new CycleButton<>(0, 0, 20, 20, sourceTypeCycle, (value, button) -> {
                //Reset the source when changing the source type, because it is not valid anymore
                this.resourceSource = null;
                this.resourceSourceType = value;
                this.rebuild();
            });
            this.resourceSourceTypeCycleButton.setUITooltipSupplier(consumes -> {
                if (this.resourceSourceType == ResourceSourceType.LOCATION) return UITooltip.of(LocalizationUtils.splitLocalizedLines("konkrete.resources.source_type.location.desc"));
                if (this.resourceSourceType == ResourceSourceType.LOCAL) return UITooltip.of(LocalizationUtils.splitLocalizedLines("konkrete.resources.source_type.local.desc"));
                return UITooltip.of(LocalizationUtils.splitLocalizedLines("konkrete.resources.source_type.web.desc"));
            });
            this.resourceSourceTypeCycleButton.setSelectedValue(this.resourceSourceType);
            this.addWidgetCell(this.resourceSourceTypeCycleButton, true);
        } else {
            ResourceSourceType singleType = allowedSourceTypes.get(0);
            Component sourceTypeLabel = Component.translatable(singleType.getLocalizationKeyBase(), singleType.getValueComponent());
            this.addLabelCell(sourceTypeLabel);
            this.resourceSourceTypeCycleButton = null;
        }

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("konkrete.resources.chooser_screen.source"));

        TextInputCell sourceInputCell = this.addTextInputCell(null, !isLegacyLocal, !isLegacyLocal);
        this.editBox = sourceInputCell.editBox;
        if (isLocal && !isLegacyLocal) this.editBox.setInputPrefix("/config/konkrete/assets/");
        this.editBox.setValue((this.resourceSource != null) ? this.resourceSource : "");
        this.editBox.setCursorPosition(0);
        this.editBox.setDisplayPosition(0);
        this.editBox.setHighlightPos(0);
        this.editBox.applyInputPrefixSuffixCharacterRenderFormatter();
        this.editBox.setEditable(!isLegacyLocal);
        this.editBox.setDeleteAllAllowed(false);
        this.editBox.setResponder(s -> this.resourceSource = s);
        sourceInputCell.setEditorCallback((s, textInputCell) -> {
            String value = (s != null) ? s : "";
            if (isLocal && !isLegacyLocal && !value.startsWith("/config/konkrete/assets/")) {
                value = "/config/konkrete/assets/" + value;
            }
            value = value.replace("\n", "\\n");
            this.resourceSource = value;
            textInputCell.editBox.setValue(value);
        });
        sourceInputCell.setEditorPresetTextSupplier(consumes -> consumes.editBox.getValueWithoutPrefixSuffix());

        if (this.resourceSourceType == ResourceSourceType.LOCATION) {
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("konkrete.resources.chooser_screen.choose_location"), var1 -> {
                Identifier startLocation = null;
                if ((this.resourceSource != null) && !this.resourceSource.trim().isEmpty()) {
                    String source = PlaceholderParser.replacePlaceholders(this.resourceSource);
                    startLocation = Identifier.tryParse(source);
                }
                ResourcePickerWindowBody picker = new ResourcePickerWindowBody(startLocation, this.allowedFileTypes, location -> {
                    if (location != null) {
                        String s = ResourceSourceType.LOCATION.getSourcePrefix() + location;
                        this.setSource(s, false);
                        this.onDone();
                        return;
                    }
                    this.rebuild();
                    PiPWindow window = this.getWindow();
                    if (window != null) {
                        window.setVisible(true);
                    }
                });
                if (this.previewApplyCallback != null) {
                    picker.setApplyButtonEnabled(true);
                    picker.setPreviewApplyCallback(location -> {
                        String source = ResourceSourceType.LOCATION.getSourcePrefix() + location;
                        if (this.validateSourceBeforeApply_Konkrete(source)) {
                            this.previewApplyCallback.accept(source);
                        }
                    });
                    picker.setPreviewCancelCallback(this.previewCancelCallback);
                }
                PiPWindow window = this.getWindow();
                if (window != null) {
                    window.setVisible(false);
                }
                PiPWindow pickerWindow = picker.openInWindow(window);
                if (window != null) {
                    pickerWindow.setPosition(window.getX(), window.getY());
                }
            }), true);
        }

        if (isLocal) {
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("konkrete.resources.chooser_screen.choose_local"), var1 -> {
                File localResourceRoot = getLocalResourceRoot();
                File startDir = localResourceRoot;
                String path = this.resourceSource;
                if (path != null) {
                    startDir = new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(path)).getParentFile();
                    if (startDir == null) startDir = localResourceRoot;
                }
                ChooseFileWindowBody fileChooser = new ChooseFileWindowBody(localResourceRoot, startDir, call -> {
                    if (call != null) {
                        String s = GameDirectoryUtils.getPathWithoutGameDirectory(call.getAbsolutePath());
                        if (!s.startsWith("/")) s = "/" + s;
                        s = ResourceSourceType.LOCAL.getSourcePrefix() + s;
                        this.setSource(s, false);
                        this.onDone();
                        return;
                    }
                    this.rebuild();
                    PiPWindow window = this.getWindow();
                    if (window != null) {
                        window.setVisible(true);
                    }
                });
                if (this.previewApplyCallback != null) {
                    fileChooser.setApplyButtonEnabled(true);
                    fileChooser.setPreviewApplyCallback(file -> {
                        String s = GameDirectoryUtils.getPathWithoutGameDirectory(file.getAbsolutePath());
                        if (!s.startsWith("/")) s = "/" + s;
                        String source = ResourceSourceType.LOCAL.getSourcePrefix() + s;
                        if (this.validateSourceBeforeApply_Konkrete(source)) {
                            this.previewApplyCallback.accept(source);
                        }
                    });
                    fileChooser.setPreviewCancelCallback(this.previewCancelCallback);
                }
                fileChooser.setVisibleDirectoryLevelsAboveRoot(2);
                fileChooser.setFileTypes(this.allowedFileTypes);
                fileChooser.setFileFilter(this.fileFilter);
                PiPWindow window = this.getWindow();
                if (window != null) {
                    window.setVisible(false);
                }
                PiPWindow fileChooserWindow = fileChooser.openInWindow(window);
                if (window != null) {
                    fileChooserWindow.setPosition(window.getX(), window.getY());
                }
            }), true);
        }

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("konkrete.resources.chooser_screen.allowed_file_types"));

        MutableComponent typesComponent = Component.translatable("konkrete.file_browser.file_type.types.all").append(" (*)");
        if (this.allowedFileTypes != null) {
            StringBuilder types = new StringBuilder();
            for (FileType<?> type : this.allowedFileTypes.getFileTypes()) {
                for (String s : type.getExtensions()) {
                    if (!types.isEmpty()) types.append(";");
                    types.append("*.").append(s.toUpperCase());
                }
            }
            Component fileTypeDisplayName = this.allowedFileTypes.getDisplayName();
            if (fileTypeDisplayName == null) fileTypeDisplayName = Component.empty();
            typesComponent = Component.empty().append(fileTypeDisplayName).append(Component.literal(" (")).append(Component.literal(types.toString())).append(Component.literal(")"));
        }
        this.addLabelCell(typesComponent.setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())));

        this.addCellGroupEndSpacerCell();

        this.addStartEndSpacerCell();

    }

    /** Builds the localized cycle used to select an allowed resource-source type. */
    @NotNull
    protected LocalizedGenericValueCycle<ResourceSourceType> buildSourceTypeCycle(@NotNull List<ResourceSourceType> allowedSourceTypes, @NotNull ResourceSourceType selected) {
        LocalizedGenericValueCycle<ResourceSourceType> cycle = LocalizedGenericValueCycle.of(ResourceSourceType.LOCATION.getLocalizationKeyBase(), allowedSourceTypes.toArray(new ResourceSourceType[0]));
        cycle.setCycleComponentStyleSupplier(LocalizedCycleEnum::getCycleComponentStyle);
        cycle.setValueComponentStyleSupplier(ResourceSourceType::getValueComponentStyle);
        cycle.setValueNameSupplier(consumes -> I18n.get(consumes.getValueLocalizationKey()));
        cycle.setCurrentValue(selected);
        return cycle;
    }

    /**
     * Returns the root exposed by the local-resource picker.
     *
     * @return local resource root
     */
    @NotNull
    protected File getLocalResourceRoot() {
        return UIConfiguration.get().dataDirectory().resolve("assets").toFile();
    }

    /** Renders body into the active GUI extraction pass. */
    @Override
    public void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.updateLegacyLocalWarning();
        this.updateNoExtensionWarning();
    }

    /** Renders late body into the active GUI extraction pass. */
    @Override
    public void renderLateBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.renderWarning(graphics, mouseX, mouseY, partial);
        this.updateInputFieldTooltips();
    }

    /** Refreshes input field tooltips from current state. */
    protected void updateInputFieldTooltips() {
        //Update Location Source Input Box Tooltip
        if (!this.showWarningNoExtension && (this.resourceSourceType == ResourceSourceType.LOCATION) && (this.editBox != null) && (this.editBox.isHovered() || this.warningHovered)) {
            TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(Component.translatable("konkrete.resources.source_type.location.desc.input")), () -> true);
        }
        //Update Legacy Local Warning Tooltip
        if (this.showWarningLegacyLocal && (this.editBox != null) && (this.editBox.isHovered() || this.warningHovered)) {
            TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(
                    Component.translatable("konkrete.resources.chooser_screen.legacy_local.warning").withColor(UIBase.getUITheme().warning_color.getColorInt())), () -> true);
        }
        //Update No Extension Warning Tooltip
        if (!this.showWarningLegacyLocal && this.showWarningNoExtension && (this.editBox != null) && (this.editBox.isHovered() || this.warningHovered)) {
            TooltipHandler.INSTANCE.addRenderTickTooltip(
                    UITooltip.of(Component.translatable("konkrete.resources.chooser_screen.no_extension.warning").withColor(UIBase.getUITheme().warning_color.getColorInt())), () -> true);
        }
    }

    /** Renders warning into the active GUI extraction pass. */
    protected void renderWarning(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if ((this.showWarningLegacyLocal || this.showWarningNoExtension) && (this.editBox != null)) {
            Identifier loc = WARNING_TEXTURE.getResourceLocation();
            if (loc != null) {
                int h = this.editBox.getHeight() - 4;
                int w = WARNING_TEXTURE.getAspectRatio().getAspectRatioWidth(h);
                int x = this.editBox.getX() - w - 2;
                int y = this.editBox.getY() + 2;
                this.warningHovered = UIBase.isXYInArea(mouseX, mouseY, x, y, w, h);
                RenderingUtils.setShaderColor(graphics, UIBase.getUITheme().warning_color, 1.0F);
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, loc, x, y, 0.0F, 0.0F, w, h, w, h);
                RenderingUtils.resetShaderColor(graphics);
            }
        }
    }

    /** Refreshes legacy local warning from current state. */
    protected void updateLegacyLocalWarning() {
        this.showWarningLegacyLocal = ((this.resourceSourceType == ResourceSourceType.LOCAL) && (this.resourceSource != null) && !this.resourceSource.trim().isEmpty() && !(this.resourceSource.startsWith("config/konkrete/assets/") || this.resourceSource.startsWith("/config/konkrete/assets/")));
    }

    /** Refreshes no extension warning from current state. */
    protected void updateNoExtensionWarning() {
        boolean emptyAssetDir = (this.resourceSourceType == ResourceSourceType.LOCAL) && (this.resourceSource != null) && this.resourceSource.equals("/config/konkrete/assets/");
        if (!emptyAssetDir && (this.resourceSource != null) && !this.resourceSource.replace(" ", "").isEmpty()) {
            boolean extensionFound = false;
            if (this.allowedFileTypes != null) {
                for (FileType<?> fileType : this.allowedFileTypes.getFileTypes()) {
                    for (String extension : fileType.getExtensions()) {
                        if (this.resourceSource.toLowerCase().endsWith("." + extension)) {
                            extensionFound = true;
                            break;
                        }
                    }
                    if (extensionFound) break;
                }
            } else {
                extensionFound = true;
            }
            if (!extensionFound) {
                this.showWarningNoExtension = true;
                return;
            }
        }
        this.showWarningNoExtension = false;
    }

    /** Sets source for this resource chooser window body. */
    public ResourceChooserWindowBody<R,F> setSource(@Nullable String resourceSource, boolean updateScreen) {
        if (resourceSource == null) {
            this.resourceSource = null;
            this.resourceSourceType = ResourceSourceType.LOCATION;
        } else {
            resourceSource = resourceSource.trim();
            this.resourceSourceType = ResourceSourceType.getSourceTypeOf(PlaceholderParser.replacePlaceholders(resourceSource));
            //Remove the prefix for easier handling inside the chooser screen (source type is saved as variable)
            this.resourceSource = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
        }
        if (updateScreen) this.rebuild();
        return this;
    }

    /** Sets allowed file types for this resource chooser window body. */
    public ResourceChooserWindowBody<R,F> setAllowedFileTypes(@Nullable FileTypeGroup<F> allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
        this.rebuild();
        return this;
    }

    /** Sets file filter for this resource chooser window body. */
    public ResourceChooserWindowBody<R,F> setFileFilter(@Nullable FileFilter fileFilter) {
        this.fileFilter = fileFilter;
        this.rebuild();
        return this;
    }

    /** Sets resource source callback for this resource chooser window body. */
    public ResourceChooserWindowBody<R,F> setResourceSourceCallback(@NotNull Consumer<String> resourceSourceCallback) {
        this.resourceSourceCallback = Objects.requireNonNull(resourceSourceCallback);
        return this;
    }

    /** Sets preview callbacks for this resource chooser window body. */
    public ResourceChooserWindowBody<R,F> setPreviewCallbacks(@Nullable Consumer<String> previewApplyCallback, @Nullable Runnable previewCancelCallback) {
        this.previewApplyCallback = previewApplyCallback;
        this.previewCancelCallback = previewCancelCallback;
        return this;
    }

    /** Returns whether location source allowed. */
    public boolean isLocationSourceAllowed() {
        return this.allowLocation;
    }

    /** Sets location source allowed for this resource chooser window body. */
    public ResourceChooserWindowBody<R,F> setLocationSourceAllowed(boolean allowLocation) {
        this.allowLocation = allowLocation;
        this.rebuild();
        return this;
    }

    /** Returns whether local source allowed. */
    public boolean isLocalSourceAllowed() {
        return this.allowLocal;
    }

    /** Sets local source allowed for this resource chooser window body. */
    public ResourceChooserWindowBody<R,F> setLocalSourceAllowed(boolean allowLocal) {
        this.allowLocal = allowLocal;
        this.rebuild();
        return this;
    }

    /** Returns whether web source allowed. */
    public boolean isWebSourceAllowed() {
        return this.allowWeb;
    }

    /** Sets web source allowed for this resource chooser window body. */
    public ResourceChooserWindowBody<R,F> setWebSourceAllowed(boolean allowWeb) {
        this.allowWeb = allowWeb;
        this.rebuild();
        return this;
    }

    /** Reports whether the current state permits completion. */
    @SuppressWarnings("all")
    @Override
    public boolean allowDone() {
        if ((this.resourceSource == null) || this.resourceSource.replace(" ", "").isEmpty()) return false;
        if ((this.resourceSourceType == ResourceSourceType.LOCAL) && this.resourceSource.equals("/config/konkrete/assets/")) return false;
        return true;
    }

    /** Handles cancel for this resource chooser window body. */
    @Override
    protected void onCancel() {
        if (this.previewCancelCallback != null) {
            this.previewCancelCallback.run();
        }
        this.resourceSourceCallback.accept(null);
        this.closeWindow();
    }

    /** Handles done for this resource chooser window body. */
    @Override
    protected void onDone() {
        if (this.resourceSource != null && !this.validateSourceBeforeApply_Konkrete(this.resourceSourceType.getSourcePrefix() + this.resourceSource)) {
            PiPWindow window = this.getWindow();
            if (window != null) {
                window.setVisible(true);
            }
            return;
        }

        if (this.resourceSource == null) {
            this.resourceSourceCallback.accept(null);
        } else {
            //Return the resource source with prefix
            this.resourceSourceCallback.accept(this.resourceSourceType.getSourcePrefix() + this.resourceSource);
        }
        this.closeWindow();
    }

    /** Validates source before apply against current constraints. */
    protected boolean validateSourceBeforeApply_Konkrete(@NotNull String sourceWithPrefix) {
        return true;
    }

    /** Handles window closed externally for this resource chooser window body. */
    @Override
    public void onWindowClosedExternally() {
        if (this.previewCancelCallback != null) {
            this.previewCancelCallback.run();
        }
        this.resourceSourceCallback.accept(null);
    }

}
