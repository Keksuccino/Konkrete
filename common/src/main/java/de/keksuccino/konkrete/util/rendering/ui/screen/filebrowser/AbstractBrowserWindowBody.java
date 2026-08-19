package de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.VanillaEvents;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.input.InputConstants;
import de.keksuccino.konkrete.util.rendering.AspectRatio;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.konkrete.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.UIIconButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.konkrete.util.resource.ResourceSupplier;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import de.keksuccino.konkrete.util.resource.resources.text.IText;
import de.keksuccino.konkrete.util.resource.resources.texture.ApngTexture;
import de.keksuccino.konkrete.util.resource.resources.texture.GifTexture;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.resource.resources.video.IVideo;
import de.keksuccino.konkrete.util.watermedia.WatermediaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Implements the interactive window body for abstract browser. */
public abstract class AbstractBrowserWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    /** Icon pixel size in GUI pixels. */
    protected static final int ICON_PIXEL_SIZE = 32;
    /** Width in GUI units for PiP window. */
    public static final int PIP_WINDOW_WIDTH = 600;
    /** Height in GUI units for PiP window. */
    public static final int PIP_WINDOW_HEIGHT = 446;

    // All icon renders are 32x32 pixels
    /** Icon for navigating to the parent location. */
    protected static final MaterialIcon GO_UP_ICON = MaterialIcons.ARROW_UPWARD;
    /** Fallback icon for unclassified files. */
    protected static final MaterialIcon GENERIC_FILE_ICON = MaterialIcons.DRAFTS;
    /** Icon for text-like files. */
    protected static final MaterialIcon TEXT_FILE_ICON = MaterialIcons.ARTICLE;
    /** Icon for audio files. */
    protected static final MaterialIcon AUDIO_FILE_ICON = MaterialIcons.MUSIC_NOTE;
    /** Icon for video files. */
    protected static final MaterialIcon VIDEO_FILE_ICON = MaterialIcons.MOVIE;
    /** Icon for image files. */
    protected static final MaterialIcon IMAGE_FILE_ICON = MaterialIcons.IMAGE;
    /** Icon for directories. */
    protected static final MaterialIcon FOLDER_ICON = MaterialIcons.FOLDER;

    /** Play control icon for audio previews. */
    protected static final MaterialIcon AUDIO_PREVIEW_PLAY_ICON = MaterialIcons.PLAY_ARROW;
    /** Pause control icon for audio previews. */
    protected static final MaterialIcon AUDIO_PREVIEW_PAUSE_ICON = MaterialIcons.PAUSE;

    /** Audio preview button size in GUI pixels. */
    protected static final int AUDIO_PREVIEW_BUTTON_SIZE = 26;
    /** Audio preview button spacing in GUI pixels. */
    protected static final int AUDIO_PREVIEW_BUTTON_SPACING = 6;
    /** Height in GUI units for audio preview slider. */
    protected static final int AUDIO_PREVIEW_SLIDER_HEIGHT = 20;
    /** Height in GUI units for audio preview progress bar. */
    protected static final int AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT = 6;
    /** Audio preview progress bar spacing in GUI pixels. */
    protected static final int AUDIO_PREVIEW_PROGRESS_BAR_SPACING = 4;
    /** Audio preview time spacing in GUI pixels. */
    protected static final int AUDIO_PREVIEW_TIME_SPACING = 2;
    /** Background color for the missing-video-provider warning. */
    protected static final DrawableColor VIDEO_PREVIEW_WATERMEDIA_WARNING_BACKGROUND_COLOR = DrawableColor.of(180, 0, 0);
    /** Project downloads page shown when the WaterMedia integration is unavailable. */
    protected static final String WATERMEDIA_V3_DOWNLOAD_URL_KONKRETE = "https://www.curseforge.com/minecraft/mc-mods/watermedia/files/all?page=1&pageSize=20&showAlphaFiles=show";
    /** Native-binaries downloads page shown when WaterMedia lacks its runtime bundle. */
    protected static final String WATERMEDIA_BINARIES_DOWNLOAD_URL_KONKRETE = "https://www.curseforge.com/minecraft/mc-mods/watermedia-binaries/files/all?page=1&pageSize=20&showAlphaFiles=show";

    /** Preview delay in milliseconds. */
    protected static final long PREVIEW_DELAY_MS = 1000L;
    /** Localized prefix for the selected file type. */
    protected static final Component FILE_TYPE_PREFIX_TEXT = Component.translatable("konkrete.file_browser.file_type");

    /** Scroll area displaying file list scroll area. */
    protected ScrollArea fileListScrollArea = new ScrollArea(0, 0, 0, 0);
    /** Scroll area displaying file type scroll area. */
    protected ScrollArea fileTypeScrollArea = new ScrollArea(0, 0, 0, 20);
    /** Scroll area displaying preview text scroll area. */
    protected ScrollArea previewTextScrollArea = new ScrollArea(0, 0, 0, 0);
    /** Whether the search field is enabled. */
    protected boolean searchBarEnabled = true;
    /** Optional search field used to filter browser entries. */
    @Nullable
    protected ExtendedEditBox searchBar;
    /** Placeholder shown while the browser search field is empty. */
    @NotNull
    protected Component searchBarPlaceholder = Component.translatable("konkrete.ui.generic.search");
    /** Whether Enter activates the browser's done action. */
    protected boolean enterKeyForDoneEnabled = true;
    /** Optionally loads a texture preview for the selected resource. */
    @Nullable
    protected ResourceSupplier<ITexture> previewTextureSupplier;
    /** Optionally loads a text preview for the selected resource. */
    @Nullable
    protected ResourceSupplier<IText> previewTextSupplier;
    /** Optionally loads an audio preview for the selected resource. */
    @Nullable
    protected ResourceSupplier<IAudio> previewAudioSupplier;
    /** Optionally loads a video preview for the selected resource. */
    @Nullable
    protected ResourceSupplier<IVideo> previewVideoSupplier;
    /** Loaded text resource shown in the preview pane. */
    @Nullable
    protected IText currentPreviewText;
    /** Audio resource currently loaded for preview. */
    @Nullable
    protected IAudio currentPreviewAudio;
    /** Video resource currently loaded for preview. */
    @Nullable
    protected IVideo currentPreviewVideo;
    /** Resource key for pending preview. */
    @Nullable
    protected Object pendingPreviewKey;
    /** Identity token for the preview currently requested. */
    @Nullable
    protected Object activePreviewKey;
    /** Pending preview load at in milliseconds. */
    protected long pendingPreviewLoadAtMs = 0L;
    /** Whether an asynchronous preview load is pending. */
    protected boolean previewPending = false;
    /** Whether the audio preview is playing. */
    protected boolean previewAudioPlaying = false;
    /** Generation token invalidating stale asynchronous audio-preview loads. */
    protected long previewAudioSeed = 0L;
    /** Whether the video preview is playing. */
    protected boolean previewVideoPlaying = false;
    /** Horizontal GUI coordinate for audio preview progress bar. */
    protected int audioPreviewProgressBarX = 0;
    /** Vertical GUI coordinate for audio preview progress bar. */
    protected int audioPreviewProgressBarY = 0;
    /** Width in GUI units for audio preview progress bar. */
    protected int audioPreviewProgressBarWidth = 0;
    /** Height in GUI units for audio preview progress bar. */
    protected int audioPreviewProgressBarHeight = 0;
    /** Horizontal GUI coordinate for video preview progress bar. */
    protected int videoPreviewProgressBarX = 0;
    /** Vertical GUI coordinate for video preview progress bar. */
    protected int videoPreviewProgressBarY = 0;
    /** Width in GUI units for video preview progress bar. */
    protected int videoPreviewProgressBarWidth = 0;
    /** Height in GUI units for video preview progress bar. */
    protected int videoPreviewProgressBarHeight = 0;
    /** Whether the audio scrubber is being dragged. */
    protected boolean audioPreviewProgressDragging = false;
    /** Whether the video scrubber is being dragged. */
    protected boolean videoPreviewProgressDragging = false;
    /** Horizontal GUI coordinate for watermedia download. */
    protected float watermediaDownloadX_Konkrete = Float.NaN;
    /** Vertical GUI coordinate for watermedia download. */
    protected float watermediaDownloadY_Konkrete = Float.NaN;
    /** Width in GUI units for watermedia download. */
    protected float watermediaDownloadWidth_Konkrete = Float.NaN;
    /** Height in GUI units for watermedia download. */
    protected float watermediaDownloadHeight_Konkrete = Float.NaN;
    /** Horizontal GUI coordinate for watermedia binaries download. */
    protected float watermediaBinariesDownloadX_Konkrete = Float.NaN;
    /** Vertical GUI coordinate for watermedia binaries download. */
    protected float watermediaBinariesDownloadY_Konkrete = Float.NaN;
    /** Width in GUI units for watermedia binaries download. */
    protected float watermediaBinariesDownloadWidth_Konkrete = Float.NaN;
    /** Height in GUI units for watermedia binaries download. */
    protected float watermediaBinariesDownloadHeight_Konkrete = Float.NaN;
    /** Button that confirms the current preview selection. */
    protected ExtendedButton confirmButton;
    /** Button that applies the current source without closing the browser. */
    @Nullable
    protected ExtendedButton applyButton;
    /** Whether the browser apply action is enabled. */
    protected boolean applyButtonEnabled = false;
    /** Button that cancels the current operation. */
    protected ExtendedButton cancelButton;
    /** Toggle controlling automatic audio previews. */
    protected UIIconButton audioPreviewToggleButton;
    /** Scroll component displaying audio preview volume slider. */
    protected RangeSlider audioPreviewVolumeSlider;
    /** Toggle controlling automatic video previews. */
    protected UIIconButton videoPreviewToggleButton;
    /** Scroll component displaying video preview volume slider. */
    protected RangeSlider videoPreviewVolumeSlider;
    /** Breadcrumb displaying the current directory or resource path. */
    protected ComponentWidget currentDirectoryComponent;
    /** File scroll list height offset in GUI pixels. */
    protected int fileScrollListHeightOffset = 0;
    /** File type scroll list y offset in GUI pixels. */
    protected int fileTypeScrollListYOffset = 0;
    /** Label displaying the active file-type filter. */
    @Nullable
    protected MutableComponent currentFileTypesComponent;
    /** Whether the browser window stays above sibling windows. */
    protected boolean windowAlwaysOnTop = true;
    /** Whether the browser window blocks input to the underlying Minecraft screen. */
    protected boolean windowBlocksMinecraftScreenInputs = true;
    /** Whether the browser window should force keyboard focus. */
    protected boolean windowForceFocus = true;

    /** Initializes shared preview-browser controls under the supplied title. */
    protected AbstractBrowserWindowBody(@NotNull Component title) {
        super(title);
    }

    /** Opens in window. */
    public @NotNull PiPWindow openInWindow(@Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(this.getTitle())
                .setScreen(this)
                .setForceKonkreteUiScale(true)
                .setAlwaysOnTop(this.windowAlwaysOnTop)
                .setBlockMinecraftScreenInputs(this.windowBlocksMinecraftScreenInputs)
                .setForceFocus(this.windowForceFocus)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    /** Initializes resources required by this abstract browser window body. */
    @Override
    protected void init() {

        boolean blur = UIBase.shouldBlur();
        this.fileListScrollArea.setSetupForBlurInterface(blur);
        this.fileTypeScrollArea.setSetupForBlurInterface(blur);
        this.previewTextScrollArea.setSetupForBlurInterface(blur);

        if (this.searchBar != null) {
            this.removeWidget(this.searchBar);
        }
        if (this.searchBarEnabled) {
            String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
            this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 0, 20 - 2, Component.empty());
            this.searchBar.setCustomHint(consumes -> AbstractBrowserWindowBody.this.searchBarPlaceholder);
            this.searchBar.setValue(oldSearchValue);
            this.searchBar.setResponder(s -> AbstractBrowserWindowBody.this.updateEntryList());
            UIBase.applyDefaultWidgetSkinTo(this.searchBar, UIBase.shouldBlur());
            this.searchBar.setMaxLength(100000);
            this.addWidget(this.searchBar);
            this.setupInitialFocusWidget(this, this.searchBar);
        }

        this.confirmButton = this.buildConfirmButton();
        if (this.confirmButton != null) {
            Button.OnPress originalConfirmAction = this.confirmButton.getPressAction();
            this.confirmButton.setPressAction(button -> {
                this.stopPreviewMedia();
                if (originalConfirmAction != null) {
                    originalConfirmAction.onPress(button);
                }
            });
        }
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultWidgetSkinTo(this.confirmButton, UIBase.shouldBlur());

        if (this.applyButtonEnabled) {
            this.applyButton = this.buildApplyButton();
            if (this.applyButton != null) {
                this.addWidget(this.applyButton);
                UIBase.applyDefaultWidgetSkinTo(this.applyButton, UIBase.shouldBlur());
            }
        }

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("konkrete.common_components.cancel"), (button) -> {
            this.stopPreviewMedia();
            this.onCancel();
            this.closeWindow();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

        this.initAudioPreviewButton();
        this.initAudioPreviewVolumeSlider();
        this.initVideoPreviewButton();
        this.initVideoPreviewVolumeSlider();

        this.updateCurrentDirectoryComponent();

        this.updateFileTypeScrollArea();

        this.initExtraButtons();

        this.addWidget(this.fileListScrollArea);
        this.addWidget(this.fileTypeScrollArea);
        this.addWidget(this.previewTextScrollArea);

    }

    /** Builds the control that commits the browser's current selection. */
    @NotNull
    protected abstract ExtendedButton buildConfirmButton();

    /** Builds an optional non-closing apply control for the current selection. */
    @Nullable
    protected ExtendedButton buildApplyButton() {
        return null;
    }

    /** Refreshes entry list from current state. */
    protected abstract void updateEntryList();

    /** Refreshes file type scroll area from current state. */
    protected abstract void updateFileTypeScrollArea();

    /** Refreshes current directory component from current state. */
    protected abstract void updateCurrentDirectoryComponent();

    /** Returns the label displayed for the current browser entries. */
    @NotNull
    protected abstract Component getEntriesLabel();

    /** Handles cancel for this abstract browser window body. */
    protected abstract void onCancel();

    /** Moves the selection or viewport to up directory. */
    protected abstract boolean goUpDirectory();

    /** Reports whether this entry navigates to the parent location. */
    protected abstract boolean isGoUpEntry(@NotNull ScrollAreaEntry entry);

    /** Opens directory entry. */
    protected abstract boolean openDirectoryEntry(@NotNull ScrollAreaEntry entry);

    /** Returns preview key for entry. */
    @Nullable
    protected abstract Object getPreviewKeyForEntry(@NotNull ScrollAreaEntry entry);

    /** Loads preview for key from the supplied source. */
    protected abstract void loadPreviewForKey(@NotNull Object previewKey);

    /** Initializes extra buttons. */
    protected void initExtraButtons() {
    }

    /** Renders extra buttons into the active GUI extraction pass. */
    protected void renderExtraButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
    }

    /** Handles screen closed for this abstract browser window body. */
    @Override
    public void onScreenClosed() {
        this.stopPreviewMedia();
    }

    /** Handles window closed externally for this abstract browser window body. */
    @Override
    public void onWindowClosedExternally() {
        this.stopPreviewMedia();
        this.onCancel();
    }

    /** Renders body into the active GUI extraction pass. */
    @Override
    public void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();

        if (this.currentFileTypesComponent != null) {
            float textWidth = UIBase.getUITextWidthNormal(this.currentFileTypesComponent);
            this.fileTypeScrollArea.horizontalScrollBar.active = (textWidth > (this.fileTypeScrollArea.getInnerWidth() - 10));
        }

        float labelY = this.getMainAreaLabelY();
        float contentTopY = this.getMainAreaTopY();
        UIBase.renderText(graphics, this.getEntriesLabel(), 20.0F, labelY, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt());

        int leftAreaWidth = this.width - 260 - 20;
        int currentDirFieldY = (int) contentTopY;
        if (this.searchBarEnabled) {
            this.renderSearchBar(graphics, mouseX, mouseY, partial, 20, currentDirFieldY, leftAreaWidth, 20);
            currentDirFieldY += 25;
        }
        int currentDirFieldYEnd = this.renderCurrentDirectoryField(graphics, mouseX, mouseY, partial, 20, currentDirFieldY, leftAreaWidth, this.font.lineHeight + 6);

        this.renderFileScrollArea(graphics, mouseX, mouseY, partial, currentDirFieldYEnd);

        this.renderFileTypeScrollArea(graphics, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("konkrete.ui.filechooser.preview");
        float previewLabelWidth = UIBase.getUITextWidthNormal(previewLabel);
        float previewLabelX = this.width - 20 - previewLabelWidth;
        UIBase.renderText(graphics, previewLabel, previewLabelX, labelY, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt());

        this.renderConfirmButton(graphics, mouseX, mouseY, partial);

        this.renderApplyButton(graphics, mouseX, mouseY, partial);

        this.renderCancelButton(graphics, mouseX, mouseY, partial);

        this.renderExtraButtons(graphics, mouseX, mouseY, partial);

        this.renderPreview(graphics, mouseX, mouseY, partial);

    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (keycode == InputConstants.KEY_TAB) {
            return true;
        }
        if ((keycode == InputConstants.KEY_ENTER) || (keycode == InputConstants.KEY_NUMPADENTER)) {
            return this.handleEnterKey();
        }
        if ((keycode == InputConstants.KEY_UP) || (keycode == InputConstants.KEY_DOWN)) {
            return this.handleVerticalNavigation(keycode == InputConstants.KEY_DOWN);
        }
        if ((keycode == InputConstants.KEY_LEFT) || (keycode == InputConstants.KEY_RIGHT)) {
            return this.forwardKeyToFocusedWidget(keycode, scancode, modifiers);
        }
        return super.keyPressed(keycode, scancode, modifiers);
    }

    /** Renders confirm button into the active GUI extraction pass. */
    protected void renderConfirmButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.confirmButton.setX(this.width - 20 - this.confirmButton.getWidth());
        this.confirmButton.setY(this.height - 20 - 20);
        this.confirmButton.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Renders apply button into the active GUI extraction pass. */
    protected void renderApplyButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (this.applyButton == null) return;
        this.applyButton.setX(this.width - 20 - this.applyButton.getWidth());
        this.applyButton.setY(this.confirmButton.getY() - 5 - 20);
        this.applyButton.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Renders cancel button into the active GUI extraction pass. */
    protected void renderCancelButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        int anchorY = (this.applyButton != null) ? this.applyButton.getY() : this.confirmButton.getY();
        this.cancelButton.setY(anchorY - 5 - 20);
        this.cancelButton.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Renders file type scroll area into the active GUI extraction pass. */
    protected void renderFileTypeScrollArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.fileTypeScrollArea.verticalScrollBar.active = false;
        this.fileTypeScrollArea.setWidth(this.getBelowFileScrollAreaElementWidth());
        this.fileTypeScrollArea.setX(this.fileListScrollArea.getXWithBorder() + this.fileListScrollArea.getWidthWithBorder() - this.fileTypeScrollArea.getWidthWithBorder());
        this.fileTypeScrollArea.setY(this.fileListScrollArea.getYWithBorder() + this.fileListScrollArea.getHeightWithBorder() + 5 + this.fileTypeScrollListYOffset);
        this.fileTypeScrollArea.extractRenderState(graphics, mouseX, mouseY, partial);
        float labelPadding = UIBase.getAreaLabelVerticalPadding();
        float labelWidth = UIBase.getUITextWidthNormal(FILE_TYPE_PREFIX_TEXT);
        float labelHeight = UIBase.getUITextHeightNormal();
        float labelX = this.fileTypeScrollArea.getXWithBorder() - labelWidth - labelPadding;
        float labelY = this.fileTypeScrollArea.getYWithBorder() + (this.fileTypeScrollArea.getHeightWithBorder() / 2.0F) - (labelHeight / 2.0F);
        UIBase.renderText(graphics, FILE_TYPE_PREFIX_TEXT, labelX, labelY, UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt());
    }

    /** Renders file scroll area into the active GUI extraction pass. */
    protected void renderFileScrollArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, int currentDirFieldYEnd) {
        this.fileListScrollArea.setWidth(this.width - 260 - 20, true);
        int listHeight = this.height - 85 - (this.font.lineHeight + 6) - 2 - 25 + this.fileScrollListHeightOffset;
        if (this.searchBarEnabled) listHeight -= 25;
        this.fileListScrollArea.setHeight(listHeight, true);
        this.fileListScrollArea.setX(20, true);
        this.fileListScrollArea.setY(currentDirFieldYEnd + 2, true);
        this.fileListScrollArea.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Renders preview into the active GUI extraction pass. */
    protected void renderPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.audioPreviewProgressBarWidth = 0;
        this.audioPreviewProgressBarHeight = 0;
        this.videoPreviewProgressBarWidth = 0;
        this.videoPreviewProgressBarHeight = 0;
        this.resetWatermediaDownloadLinkBounds_Konkrete();
        this.tickPreviewDelay();
        this.tickAudioPreview();
        this.tickVideoPreview();
        this.tickTextPreview();
        boolean showVideoDependencyWarning = this.shouldRenderWatermediaMissingWarning_Konkrete();
        if (this.previewAudioSupplier == null && this.audioPreviewVolumeSlider != null) {
            this.audioPreviewVolumeSlider.visible = false;
            this.audioPreviewVolumeSlider.active = false;
        }
        if ((this.previewVideoSupplier == null || showVideoDependencyWarning) && this.videoPreviewVolumeSlider != null) {
            this.videoPreviewVolumeSlider.visible = false;
            this.videoPreviewVolumeSlider.active = false;
        }
        if (this.previewAudioSupplier != null) {
            this.renderAudioPreview(graphics, mouseX, mouseY, partial);
        } else if (this.previewVideoSupplier != null) {
            if (showVideoDependencyWarning) {
                this.renderWatermediaMissingWarning_Konkrete(graphics, mouseX, mouseY);
            } else {
                this.renderVideoPreview(graphics, mouseX, mouseY, partial);
            }
        } else if (this.previewTextureSupplier != null) {
            ITexture t = this.previewTextureSupplier.get();
            Identifier loc = (t != null) ? t.getResourceLocation() : null;
            if (loc != null) {
                int previewBackgroundColor = UIBase.shouldBlur()
                        ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                        : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
                int previewBorderColor = UIBase.shouldBlur()
                        ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                        : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
                AspectRatio ratio = t.getAspectRatio();
                int previewMaxWidth = 200;
                int previewTopY = (int) this.getMainAreaTopY();
                int availableHeight = Math.max(12, (this.cancelButton.getY() - 50) - previewTopY);
                boolean showAnimatedImageWatermediaRecommendation = this.shouldRenderAnimatedImageWatermediaRecommendation_Konkrete(t);
                int recommendationSpacing = 4;
                int recommendationHeight = showAnimatedImageWatermediaRecommendation
                        ? this.getAnimatedImageWatermediaRecommendationHeight_Konkrete(previewMaxWidth)
                        : 0;
                int maxPreviewHeight = showAnimatedImageWatermediaRecommendation
                        ? Math.max(12, availableHeight - recommendationHeight - recommendationSpacing)
                        : availableHeight;
                int[] size = ratio.getAspectRatioSizeByMaximumSize(previewMaxWidth, maxPreviewHeight);
                int w = Math.max(1, size[0]);
                int h = Math.max(1, size[1]);
                int x = this.width - 20 - w;
                int y = previewTopY;
                UIBase.resetShaderColor(graphics);
                graphics.fill(x, y, x + w, y + h, previewBackgroundColor);
                RenderingUtils.resetShaderColor(graphics);
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, loc, x, y, 0.0F, 0.0F, w, h, w, h);
                UIBase.resetShaderColor(graphics);
                UIBase.renderBorder(graphics, x, y, x + w, y + h, UIBase.ELEMENT_BORDER_THICKNESS, previewBorderColor, true, true, true, true);
                if (showAnimatedImageWatermediaRecommendation) {
                    int recommendationX = this.width - 20 - previewMaxWidth;
                    int recommendationY = y + h + recommendationSpacing;
                    this.renderAnimatedImageWatermediaRecommendation_Konkrete(graphics, mouseX, mouseY, recommendationX, recommendationY, previewMaxWidth);
                }
            }
        } else {
            this.previewTextScrollArea.setWidth(200, true);
            this.previewTextScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
            this.previewTextScrollArea.setX(this.width - 20 - this.previewTextScrollArea.getWidthWithBorder(), true);
            this.previewTextScrollArea.setY((int) this.getMainAreaTopY(), true);
            this.previewTextScrollArea.extractRenderState(graphics, mouseX, mouseY, partial);
        }
        UIBase.resetShaderColor(graphics);
    }

    /** Renders current directory field into the active GUI extraction pass. */
    protected int renderCurrentDirectoryField(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, int x, int y, int width, int height) {
        int xEnd = x + width;
        int yEnd = y + height;
        int backgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int borderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        UIBase.renderRoundedRect(graphics, x + 1, y + 1, width - 2, height - 2, radius, radius, radius, radius, backgroundColor);
        UIBase.renderRoundedBorder(graphics, x, y, xEnd, yEnd, 1, radius, radius, radius, radius, borderColor);
        this.currentDirectoryComponent.setX(x + 4);
        this.currentDirectoryComponent.setY(y + (height / 2) - (this.currentDirectoryComponent.getHeight() / 2));
        this.currentDirectoryComponent.extractRenderState(graphics, mouseX, mouseY, partial);
        return yEnd;
    }

    /** Renders search bar into the active GUI extraction pass. */
    protected void renderSearchBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, int x, int y, int width, int height) {
        if (this.searchBar == null) return;
        this.searchBar.setX(x + 1);
        this.searchBar.setY(y + 1);
        this.searchBar.setWidth(width - 2);
        this.searchBar.setHeight(height - 2);
        this.searchBar.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Returns below file scroll area element width. */
    protected int getBelowFileScrollAreaElementWidth() {
        float labelPadding = UIBase.getAreaLabelVerticalPadding();
        float labelWidth = UIBase.getUITextWidthNormal(FILE_TYPE_PREFIX_TEXT);
        return (int) (this.fileListScrollArea.getWidthWithBorder() - labelWidth - labelPadding);
    }

    /**
     * Enable or disable the search bar feature.
     * Should be called before {@link #init()} for proper initialization.
     */
    protected void setSearchBarEnabled(boolean enabled) {
        this.searchBarEnabled = enabled;
    }

    /**
     * Set the placeholder text for the search bar.
     * Only used when search bar is enabled.
     */
    protected void setSearchBarPlaceholder(@NotNull Component placeholder) {
        this.searchBarPlaceholder = placeholder;
    }

    /**
     * Enable or disable the enter key action (acts like pressing the confirm button).
     */
    protected void setEnterKeyForDoneEnabled(boolean enabled) {
        this.enterKeyForDoneEnabled = enabled;
    }

    /** Returns whether enter for done. */
    protected boolean allowEnterForDone() {
        return this.enterKeyForDoneEnabled;
    }

    /**
     * Enable or disable the optional apply button.
     * Should be called before {@link #init()} for proper initialization.
     */
    public void setApplyButtonEnabled(boolean enabled) {
        this.applyButtonEnabled = enabled;
    }

    /**
     * Set whether the PiP window should stay above other windows.
     * Should be called before {@link #openInWindow(PiPWindow)}.
     */
    public void setWindowAlwaysOnTop(boolean alwaysOnTop) {
        this.windowAlwaysOnTop = alwaysOnTop;
    }

    /**
     * Set whether the PiP window blocks Minecraft screen inputs.
     * Should be called before {@link #openInWindow(PiPWindow)}.
     */
    public void setWindowBlocksMinecraftScreenInputs(boolean blockInputs) {
        this.windowBlocksMinecraftScreenInputs = blockInputs;
    }

    /**
     * Set whether the PiP window forces focus on open.
     * Should be called before {@link #openInWindow(PiPWindow)}.
     */
    public void setWindowForceFocus(boolean forceFocus) {
        this.windowForceFocus = forceFocus;
    }

    /** Returns selected scroll entry. */
    @Nullable
    protected ScrollAreaEntry getSelectedScrollEntry() {
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e.isSelected()) return e;
        }
        return null;
    }

    /** Returns search value. */
    @Nullable
    protected String getSearchValue() {
        if (!this.searchBarEnabled || this.searchBar == null) return null;
        String value = this.searchBar.getValue();
        if (value == null || value.isBlank()) return null;
        return value;
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0) && this.handleWatermediaMissingWarningClick_Konkrete(mouseX, mouseY)) {
            return true;
        }
        if ((button == 0) && this.handleProgressBarClick(mouseX, mouseY)) {
            return true;
        }
        if ((button == 0) && (this.previewAudioSupplier != null) && (this.audioPreviewToggleButton != null)) {
            if (this.audioPreviewToggleButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if ((button == 0) && (this.previewVideoSupplier != null) && !this.shouldRenderWatermediaMissingWarning_Konkrete() && (this.videoPreviewToggleButton != null)) {
            if (this.videoPreviewToggleButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if ((button == 0) && !this.fileListScrollArea.isMouseOverInnerArea(mouseX, mouseY) && !this.fileListScrollArea.isMouseInteractingWithGrabbers() && !this.previewTextScrollArea.isMouseOverInnerArea(mouseX, mouseY) && !this.previewTextScrollArea.isMouseInteractingWithGrabbers() && !this.isAudioProgressBarHovered(mouseX, mouseY) && !this.isVideoProgressBarHovered(mouseX, mouseY) && !this.isWidgetHovered()) {
            for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
                e.setSelected(false);
            }
            this.updatePreviewForKey(null);
        }

        return super.mouseClicked(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), false);

    }

    /** Routes pointer dragging and reports whether it was consumed. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (this.audioPreviewProgressDragging) {
                this.seekAudioPreviewByMouseX(mouseX);
                return true;
            }
            if (this.videoPreviewProgressDragging) {
                this.seekVideoPreviewByMouseX(mouseX);
                return true;
            }
        }
        return super.mouseDragged(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), dragX, dragY);
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (button == 0) {
            if (this.audioPreviewProgressDragging) {
                this.seekAudioPreviewByMouseX(mouseX);
                handled = true;
            }
            if (this.videoPreviewProgressDragging) {
                this.seekVideoPreviewByMouseX(mouseX);
                handled = true;
            }
            this.audioPreviewProgressDragging = false;
            this.videoPreviewProgressDragging = false;
        }
        if (handled) {
            return true;
        }
        return super.mouseReleased(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button));
    }

    /** Reports whether the pointer currently hovers the associated widget. */
    protected boolean isWidgetHovered() {
        for (GuiEventListener l : this.children()) {
            if (l instanceof AbstractWidget w) {
                if (w.isHovered()) return true;
            }
        }
        if (this.previewAudioSupplier != null && this.audioPreviewToggleButton != null && this.audioPreviewToggleButton.isHovered()) {
            return true;
        }
        if (this.previewVideoSupplier != null && !this.shouldRenderWatermediaMissingWarning_Konkrete() && this.videoPreviewToggleButton != null && this.videoPreviewToggleButton.isHovered()) {
            return true;
        }
        return false;
    }

    /** Handles progress bar click for this abstract browser window body. */
    protected boolean handleProgressBarClick(double mouseX, double mouseY) {
        this.audioPreviewProgressDragging = false;
        this.videoPreviewProgressDragging = false;
        if (this.isAudioProgressBarHovered(mouseX, mouseY)) {
            this.audioPreviewProgressDragging = true;
            this.seekAudioPreviewByMouseX(mouseX);
            return true;
        }
        if (this.isVideoProgressBarHovered(mouseX, mouseY)) {
            this.videoPreviewProgressDragging = true;
            this.seekVideoPreviewByMouseX(mouseX);
            return true;
        }
        return false;
    }

    /** Handles watermedia missing warning click for this abstract browser window body. */
    protected boolean handleWatermediaMissingWarningClick_Konkrete(double mouseX, double mouseY) {
        if (!this.shouldHandleWatermediaDownloadLinks_Konkrete()) return false;
        if (this.isMouseOverWatermediaDownloadLink_Konkrete(mouseX, mouseY)) {
            WebUtils.openWebLink(WATERMEDIA_V3_DOWNLOAD_URL_KONKRETE);
            return true;
        }
        if (this.isMouseOverWatermediaBinariesDownloadLink_Konkrete(mouseX, mouseY)) {
            WebUtils.openWebLink(WATERMEDIA_BINARIES_DOWNLOAD_URL_KONKRETE);
            return true;
        }
        return false;
    }

    /** Returns whether audio progress bar hovered. */
    protected boolean isAudioProgressBarHovered(double mouseX, double mouseY) {
        if (this.previewAudioSupplier == null) return false;
        return this.isPreviewProgressBarHovered(mouseX, mouseY, this.audioPreviewProgressBarX, this.audioPreviewProgressBarY, this.audioPreviewProgressBarWidth, this.audioPreviewProgressBarHeight);
    }

    /** Returns whether video progress bar hovered. */
    protected boolean isVideoProgressBarHovered(double mouseX, double mouseY) {
        if (this.shouldRenderWatermediaMissingWarning_Konkrete()) return false;
        if (this.previewVideoSupplier == null) return false;
        return this.isPreviewProgressBarHovered(mouseX, mouseY, this.videoPreviewProgressBarX, this.videoPreviewProgressBarY, this.videoPreviewProgressBarWidth, this.videoPreviewProgressBarHeight);
    }

    /** Returns whether preview progress bar hovered. */
    protected boolean isPreviewProgressBarHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return false;
        return mouseX >= x && mouseX < (x + width) && mouseY >= y && mouseY < (y + height);
    }

    /** Seeks audio preview by mouse x to the supplied position. */
    protected void seekAudioPreviewByMouseX(double mouseX) {
        if (this.audioPreviewProgressBarWidth <= 0) return;
        float progress = (float) ((mouseX - this.audioPreviewProgressBarX) / (double) this.audioPreviewProgressBarWidth);
        this.seekAudioPreviewByProgress(progress);
    }

    /** Seeks video preview by mouse x to the supplied position. */
    protected void seekVideoPreviewByMouseX(double mouseX) {
        if (this.videoPreviewProgressBarWidth <= 0) return;
        float progress = (float) ((mouseX - this.videoPreviewProgressBarX) / (double) this.videoPreviewProgressBarWidth);
        this.seekVideoPreviewByProgress(progress);
    }

    /** Seeks audio preview by progress to the supplied position. */
    protected void seekAudioPreviewByProgress(float progress) {
        IAudio audio = this.getPreviewAudio();
        if (audio == null) return;
        float duration = Math.max(0.0F, audio.getDuration());
        if (duration <= 0.0F) return;
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        audio.setPlayTime(duration * clamped);
    }

    /** Seeks video preview by progress to the supplied position. */
    protected void seekVideoPreviewByProgress(float progress) {
        IVideo video = this.getPreviewVideo();
        if (video == null) return;
        float duration = Math.max(0.0F, video.getDuration());
        if (duration <= 0.0F) return;
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        video.setPlayTime(duration * clamped);
    }

    /** Refreshes preview for entry from current state. */
    protected void updatePreviewForEntry(@Nullable ScrollAreaEntry entry) {
        if (entry == null) {
            this.updatePreviewForKey(null);
            return;
        }
        Object previewKey = this.getPreviewKeyForEntry(entry);
        this.updatePreviewForKey(previewKey);
    }

    /** Refreshes preview for key from current state. */
    protected void updatePreviewForKey(@Nullable Object previewKey) {
        if (previewKey != null) {
            if (!this.previewPending && (this.activePreviewKey != null) && this.activePreviewKey.equals(previewKey)) {
                return;
            }
            this.pendingPreviewKey = previewKey;
            this.pendingPreviewLoadAtMs = System.currentTimeMillis() + PREVIEW_DELAY_MS;
            this.previewPending = true;
            this.activePreviewKey = null;
            this.clearPreviewDisplay(false);
        } else {
            this.cancelPendingPreview();
            this.activePreviewKey = null;
            this.clearPreviewDisplay(true);
        }
    }

    /** Advances preview delay by one client tick. */
    protected void tickPreviewDelay() {
        if (!this.previewPending) return;
        if (this.pendingPreviewKey == null) {
            this.previewPending = false;
            return;
        }
        if (System.currentTimeMillis() < this.pendingPreviewLoadAtMs) return;
        Object pending = this.pendingPreviewKey;
        this.pendingPreviewKey = null;
        this.previewPending = false;
        if (this.isPreviewKeyStillSelected(pending)) {
            this.loadPreviewForKey(pending);
            if (this.previewTextureSupplier == null && this.previewTextSupplier == null && this.previewAudioSupplier == null && this.previewVideoSupplier == null) {
                this.setNoTextPreview();
            }
            this.activePreviewKey = pending;
        }
    }

    /** Returns whether preview key still selected. */
    protected boolean isPreviewKeyStillSelected(@NotNull Object previewKey) {
        Object selectedKey = this.getSelectedPreviewKey();
        return Objects.equals(previewKey, selectedKey);
    }

    /** Returns selected preview key. */
    @Nullable
    protected Object getSelectedPreviewKey() {
        ScrollAreaEntry selected = this.getSelectedScrollEntry();
        if (selected == null) return null;
        return this.getPreviewKeyForEntry(selected);
    }

    /** Clears preview display state. */
    protected void clearPreviewDisplay(boolean showNoPreview) {
        this.previewTextureSupplier = null;
        this.previewTextSupplier = null;
        this.previewAudioSupplier = null;
        this.previewVideoSupplier = null;
        this.audioPreviewProgressBarWidth = 0;
        this.audioPreviewProgressBarHeight = 0;
        this.videoPreviewProgressBarWidth = 0;
        this.videoPreviewProgressBarHeight = 0;
        this.currentPreviewText = null;
        this.resetWatermediaDownloadLinkBounds_Konkrete();
        this.stopPreviewMedia();
        if (showNoPreview) {
            this.setNoTextPreview();
        } else if (this.previewTextScrollArea != null) {
            this.previewTextScrollArea.clearEntries();
        }
    }

    /** Returns whether cel pending preview. */
    protected void cancelPendingPreview() {
        this.pendingPreviewKey = null;
        this.previewPending = false;
    }

    /** Advances text preview by one client tick. */
    protected void tickTextPreview() {
        if (this.previewAudioSupplier != null || this.previewVideoSupplier != null) return;
        if (this.previewPending) return;
        if (this.previewTextScrollArea == null) return;
        if (this.previewTextSupplier != null) {
            IText text = this.previewTextSupplier.get();
            if (!Objects.equals(this.currentPreviewText, text)) {
                if (text == null) {
                    this.setNoTextPreview();
                } else {
                    this.previewTextScrollArea.clearEntries();
                    List<String> lines = text.getTextLines();
                    if (lines != null) {
                        int line = 0;
                        for (String s : lines) {
                            line++;
                            if (line < 70) {
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal(s).withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                            } else {
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("......").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                                TextScrollAreaEntry e2 = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("  ").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())), (entry) -> {});
                                e2.setSelectable(false);
                                e2.setBackgroundColorHover(e2.getBackgroundColorNormal());
                                e2.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e2);
                                break;
                            }
                        }
                        float totalWidth = this.previewTextScrollArea.getTotalEntryWidth();
                        for (ScrollAreaEntry e : this.previewTextScrollArea.getEntries()) {
                            e.setWidth(totalWidth);
                        }
                    } else {
                        return;
                    }
                }
                this.currentPreviewText = text;
            }
        } else {
            if (this.currentPreviewText != null) this.setNoTextPreview();
            this.currentPreviewText = null;
        }
    }

    /** Sets no text preview for this abstract browser window body. */
    protected void setNoTextPreview() {
        if (this.previewAudioSupplier != null || this.previewVideoSupplier != null) return;
        if (this.previewTextScrollArea == null) return;
        this.previewTextScrollArea.clearEntries();
        TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.translatable("konkrete.ui.filechooser.no_preview").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())), (entry) -> {});
        e.setSelectable(false);
        e.setBackgroundColorHover(e.getBackgroundColorNormal());
        e.setPlayClickSound(false);
        this.previewTextScrollArea.addEntry(e);
    }

    /** Handles enter key for this abstract browser window body. */
    protected boolean handleEnterKey() {
        ScrollAreaEntry selectedEntry = this.getSelectedScrollEntry();
        if (selectedEntry != null) {
            if (this.isGoUpEntry(selectedEntry)) {
                this.goUpDirectory();
                return true;
            }
            if (this.openDirectoryEntry(selectedEntry)) {
                return true;
            }
        }
        if (!this.allowEnterForDone()) return true;
        if (this.confirmButton != null && this.confirmButton.active) {
            this.confirmButton.onPress(VanillaEvents.keyEvent(InputConstants.KEY_ENTER, 0, 0));
            return true;
        }
        return true;
    }

    /** Handles vertical navigation for this abstract browser window body. */
    protected boolean handleVerticalNavigation(boolean moveDown) {
        List<ScrollAreaEntry> entries = this.getSelectableFileEntries();
        if (entries.isEmpty()) {
            if (!moveDown) {
                this.focusSearchBar();
            }
            return true;
        }
        if (this.searchBarEnabled && this.searchBar != null && this.searchBar.isFocused()) {
            if (moveDown) {
                this.selectEntry(entries.get(0));
            }
            return true;
        }
        ScrollAreaEntry selected = this.getSelectedScrollEntry();
        int selectedIndex = (selected != null) ? entries.indexOf(selected) : -1;
        if (moveDown) {
            if (selectedIndex < 0) {
                this.selectEntry(entries.get(0));
            } else if (selectedIndex < entries.size() - 1) {
                this.selectEntry(entries.get(selectedIndex + 1));
            }
        } else {
            if (selectedIndex <= 0) {
                if (this.searchBarEnabled && this.searchBar != null) {
                    this.clearSelectedEntries();
                    this.focusSearchBar();
                }
            } else {
                this.selectEntry(entries.get(selectedIndex - 1));
            }
        }
        return true;
    }

    /** Returns selectable file entries. */
    @NotNull
    protected List<ScrollAreaEntry> getSelectableFileEntries() {
        List<ScrollAreaEntry> entries = new ArrayList<>();
        for (ScrollAreaEntry entry : this.fileListScrollArea.getEntries()) {
            if (entry.isSelectable()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /** Updates focus or selection for entry. */
    protected void selectEntry(@NotNull ScrollAreaEntry entry) {
        entry.setSelected(true);
        this.ensureEntryVisible(entry);
        this.updatePreviewForEntry(entry);
        this.setFocused(null);
    }

    /** Clears selected entries state. */
    protected void clearSelectedEntries() {
        for (ScrollAreaEntry entry : this.fileListScrollArea.getEntries()) {
            entry.setSelected(false);
        }
        this.updatePreviewForKey(null);
    }

    /** Ensures entry visible is visible in the current viewport. */
    protected void ensureEntryVisible(@NotNull ScrollAreaEntry entry) {
        float totalScrollHeight = this.fileListScrollArea.getTotalScrollHeight();
        if (totalScrollHeight <= 0.0F) return;
        float innerY = this.fileListScrollArea.getInnerY();
        float innerHeight = this.fileListScrollArea.getInnerHeight();
        float entryTopUnscrolled = innerY;
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e == entry) break;
            entryTopUnscrolled += e.getHeight();
        }
        float entryTop = entryTopUnscrolled + this.fileListScrollArea.getEntryRenderOffsetY(totalScrollHeight);
        float entryBottom = entryTop + entry.getHeight();
        float innerBottom = innerY + innerHeight;
        float scroll = this.fileListScrollArea.verticalScrollBar.getScroll();
        float newScroll = scroll;
        if (entryTop < innerY) {
            float delta = innerY - entryTop;
            newScroll = scroll - (delta / totalScrollHeight);
        } else if (entryBottom > innerBottom) {
            float delta = entryBottom - innerBottom;
            newScroll = scroll + (delta / totalScrollHeight);
        }
        if (newScroll < 0.0F) newScroll = 0.0F;
        if (newScroll > 1.0F) newScroll = 1.0F;
        if (newScroll != scroll) {
            this.fileListScrollArea.verticalScrollBar.setScroll(newScroll);
        }
    }

    /** Updates focus or selection for search bar. */
    protected void focusSearchBar() {
        if (!this.searchBarEnabled || this.searchBar == null) return;
        this.setFocused(this.searchBar);
    }

    /** Routes key to focused widget to the active target. */
    protected boolean forwardKeyToFocusedWidget(int keycode, int scancode, int modifiers) {
        GuiEventListener focused = this.getFocused();
        if (focused != null) {
            return focused.keyPressed(VanillaEvents.keyEvent(keycode, scancode, modifiers));
        }
        return false;
    }

    /** Initializes audio preview button. */
    protected void initAudioPreviewButton() {
        this.audioPreviewToggleButton = new UIIconButton(0.0F, 0.0F, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_PLAY_ICON, button -> {
            this.togglePreviewAudio();
        });
    }

    /** Initializes audio preview volume slider. */
    protected void initAudioPreviewVolumeSlider() {
        if (this.audioPreviewVolumeSlider != null) {
            this.removeWidget(this.audioPreviewVolumeSlider);
        }
        float volume = BrowserAudioSettings.getVolume();
        RangeSlider slider = new RangeSlider(0, 0, 100, AUDIO_PREVIEW_SLIDER_HEIGHT, Component.empty(), 0.0D, 1.0D, volume);
        slider.setRoundingDecimalPlace(2);
        slider.setLabelSupplier(consumes -> Component.empty());
        slider.setSliderValueUpdateListener((s, valueDisplayText, value) -> {
            float newVolume = (float) ((RangeSlider) s).getRangeValue();
            BrowserAudioSettings.setVolume(newVolume);
            this.applyPreviewAudioVolume(newVolume);
        });
        this.audioPreviewVolumeSlider = slider;
        this.audioPreviewVolumeSlider.visible = false;
        this.audioPreviewVolumeSlider.active = false;
        this.addWidget(this.audioPreviewVolumeSlider);
        UIBase.applyDefaultWidgetSkinTo(this.audioPreviewVolumeSlider, UIBase.shouldBlur());
    }

    /** Initializes video preview button. */
    protected void initVideoPreviewButton() {
        this.videoPreviewToggleButton = new UIIconButton(0.0F, 0.0F, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_PLAY_ICON, button -> {
            this.togglePreviewVideo();
        });
    }

    /** Initializes video preview volume slider. */
    protected void initVideoPreviewVolumeSlider() {
        if (this.videoPreviewVolumeSlider != null) {
            this.removeWidget(this.videoPreviewVolumeSlider);
        }
        float volume = BrowserVideoSettings.getVolume();
        RangeSlider slider = new RangeSlider(0, 0, 100, AUDIO_PREVIEW_SLIDER_HEIGHT, Component.empty(), 0.0D, 1.0D, volume);
        slider.setRoundingDecimalPlace(2);
        slider.setLabelSupplier(consumes -> Component.empty());
        slider.setSliderValueUpdateListener((s, valueDisplayText, value) -> {
            float newVolume = (float) ((RangeSlider) s).getRangeValue();
            BrowserVideoSettings.setVolume(newVolume);
            this.applyPreviewVideoVolume(newVolume);
        });
        this.videoPreviewVolumeSlider = slider;
        this.videoPreviewVolumeSlider.visible = false;
        this.videoPreviewVolumeSlider.active = false;
        this.addWidget(this.videoPreviewVolumeSlider);
        UIBase.applyDefaultWidgetSkinTo(this.videoPreviewVolumeSlider, UIBase.shouldBlur());
    }

    /** Toggles preview audio. */
    protected void togglePreviewAudio() {
        if (this.previewAudioSupplier == null) return;
        this.setPreviewAudioPlaying(!this.previewAudioPlaying);
    }

    /** Toggles preview video. */
    protected void togglePreviewVideo() {
        if (this.previewVideoSupplier == null) return;
        this.setPreviewVideoPlaying(!this.previewVideoPlaying);
    }

    /** Sets preview audio for this abstract browser window body. */
    protected void setPreviewAudio(@Nullable ResourceSupplier<IAudio> supplier, @Nullable Object previewKey) {
        this.stopPreviewAudio();
        this.previewAudioSupplier = supplier;
        this.previewAudioPlaying = false;
        this.previewAudioSeed = (previewKey != null) ? previewKey.hashCode() * 37L : System.nanoTime();
    }

    /** Sets preview video for this abstract browser window body. */
    protected void setPreviewVideo(@Nullable ResourceSupplier<IVideo> supplier, @Nullable Object previewKey) {
        this.stopPreviewVideo();
        this.previewVideoSupplier = supplier;
        this.previewVideoPlaying = false;
    }

    /** Sets preview audio playing for this abstract browser window body. */
    protected void setPreviewAudioPlaying(boolean playing) {
        this.previewAudioPlaying = playing;
        IAudio audio = this.getPreviewAudio();
        if (audio == null) {
            this.previewAudioPlaying = false;
            return;
        }
        if (playing) {
            if (!audio.isPlaying()) audio.play();
        } else {
            if (audio.isPlaying()) audio.pause();
        }
    }

    /** Sets preview video playing for this abstract browser window body. */
    protected void setPreviewVideoPlaying(boolean playing) {
        this.previewVideoPlaying = playing;
        IVideo video = this.getPreviewVideo();
        if (video == null) {
            this.previewVideoPlaying = false;
            return;
        }
        if (playing) {
            if (!video.isPlaying()) video.play();
        } else {
            if (video.isPlaying()) video.pause();
        }
    }

    /** Returns preview audio. */
    @Nullable
    protected IAudio getPreviewAudio() {
        if (this.previewAudioSupplier == null) return null;
        IAudio audio = this.previewAudioSupplier.get();
        if (audio == null || audio.isClosed()) return null;
        if (!Objects.equals(this.currentPreviewAudio, audio)) {
            this.stopPreviewAudio();
            this.currentPreviewAudio = audio;
            audio.pause();
            this.applyPreviewAudioVolume(BrowserAudioSettings.getVolume());
        }
        return this.currentPreviewAudio;
    }

    /** Returns preview video. */
    @Nullable
    protected IVideo getPreviewVideo() {
        if (this.previewVideoSupplier == null) return null;
        IVideo video = this.previewVideoSupplier.get();
        if (video == null || video.isClosed()) return null;
        if (!Objects.equals(this.currentPreviewVideo, video)) {
            this.stopPreviewVideo();
            this.currentPreviewVideo = video;
            video.pause();
            video.setLooping(false);
            this.applyPreviewVideoVolume(BrowserVideoSettings.getVolume());
        }
        return this.currentPreviewVideo;
    }

    /** Stops preview audio and releases its active resources. */
    protected void stopPreviewAudio() {
        if (this.currentPreviewAudio != null) {
            this.currentPreviewAudio.stop();
        }
        this.currentPreviewAudio = null;
        this.previewAudioPlaying = false;
    }

    /** Stops preview video and releases its active resources. */
    protected void stopPreviewVideo() {
        if (this.currentPreviewVideo != null) {
            this.currentPreviewVideo.stop();
        }
        this.currentPreviewVideo = null;
        this.previewVideoPlaying = false;
    }

    /** Stops preview media and releases its active resources. */
    protected void stopPreviewMedia() {
        this.stopPreviewAudio();
        this.stopPreviewVideo();
        this.audioPreviewProgressDragging = false;
        this.videoPreviewProgressDragging = false;
    }

    /** Advances audio preview by one client tick. */
    protected void tickAudioPreview() {
        if (this.previewAudioSupplier == null) {
            this.stopPreviewAudio();
            return;
        }
        IAudio audio = this.getPreviewAudio();
        if (audio == null) {
            this.previewAudioPlaying = false;
            return;
        }
        if (this.previewAudioPlaying) {
            if (!audio.isPlaying()) audio.play();
            if (audio.getDuration() > 0.0F && audio.getPlayTime() >= audio.getDuration()) {
                this.previewAudioPlaying = false;
                audio.stop();
            }
        } else if (audio.isPlaying()) {
            audio.pause();
        }
    }

    /** Advances video preview by one client tick. */
    protected void tickVideoPreview() {
        if (this.previewVideoSupplier == null) {
            this.stopPreviewVideo();
            return;
        }
        if (this.shouldRenderWatermediaMissingWarning_Konkrete()) {
            this.stopPreviewVideo();
            return;
        }
        IVideo video = this.getPreviewVideo();
        if (video == null) {
            this.previewVideoPlaying = false;
            return;
        }
        this.applyPreviewVideoVolume(BrowserVideoSettings.getVolume());
        if (this.previewVideoPlaying) {
            if (!video.isPlaying()) video.play();
            if (video.isEnded() || (video.getDuration() > 0.0F && video.getPlayTime() >= video.getDuration())) {
                this.previewVideoPlaying = false;
                video.stop();
            }
        } else if (video.isPlaying()) {
            video.pause();
        }
    }

    /** Returns whether render watermedia missing warning. */
    protected boolean shouldRenderWatermediaMissingWarning_Konkrete() {
        return (this.previewVideoSupplier != null) && !WatermediaUtil.isWatermediaVideoPlaybackAvailable();
    }

    /** Returns whether render animated image watermedia recommendation. */
    protected boolean shouldRenderAnimatedImageWatermediaRecommendation_Konkrete(@Nullable ITexture texture) {
        return !WatermediaUtil.isWatermediaVideoPlaybackAvailable()
                && ((texture instanceof ApngTexture) || (texture instanceof GifTexture));
    }

    /** Returns whether handle watermedia download links. */
    protected boolean shouldHandleWatermediaDownloadLinks_Konkrete() {
        if (this.shouldRenderWatermediaMissingWarning_Konkrete()) return true;
        if (this.previewTextureSupplier == null) return false;
        return this.shouldRenderAnimatedImageWatermediaRecommendation_Konkrete(this.previewTextureSupplier.get());
    }

    /** Returns animated image watermedia recommendation height. */
    protected int getAnimatedImageWatermediaRecommendationHeight_Konkrete(int width) {
        float contentWidth = Math.max(1.0F, width);
        Component infoText = Component.translatable("konkrete.ui.filechooser.preview.watermedia_recommended_apng_gif");
        List<MutableComponent> infoLines = UIBase.lineWrapUIComponentsSmall(infoText, contentWidth);
        float textSize = UIBase.getUITextSizeSmall();
        float textHeight = UIBase.getUITextHeight(textSize);
        float spacing = Math.max(2.0F, UIBase.getUITextHeightSmall() * 0.5F);
        float totalHeight = (infoLines.size() * textHeight) + spacing + textHeight + spacing + textHeight;
        return Math.max(1, Math.round(totalHeight));
    }

    /** Renders animated image watermedia recommendation into the active GUI extraction pass. */
    protected void renderAnimatedImageWatermediaRecommendation_Konkrete(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width) {
        Component infoText = Component.translatable("konkrete.ui.filechooser.preview.watermedia_recommended_apng_gif");
        Component downloadText = Component.translatable("konkrete.backgrounds.video.watermedia_missing.download");
        Component downloadBinariesText = Component.translatable("konkrete.backgrounds.video.watermedia_missing.download_binaries");
        Component boldDownloadText = downloadText.copy().setStyle(Style.EMPTY.withBold(true));
        Component boldDownloadBinariesText = downloadBinariesText.copy().setStyle(Style.EMPTY.withBold(true));

        float contentWidth = Math.max(1.0F, width);
        float textSize = UIBase.getUITextSizeSmall();
        float lineHeight = UIBase.getUITextHeight(textSize);
        float spacing = Math.max(2.0F, UIBase.getUITextHeightSmall() * 0.5F);
        List<MutableComponent> infoLines = UIBase.lineWrapUIComponentsSmall(infoText, contentWidth);
        float infoHeight = infoLines.size() * lineHeight;
        float downloadTextWidth = UIBase.getUITextWidth(boldDownloadText, textSize);
        float downloadBinariesTextWidth = UIBase.getUITextWidth(boldDownloadBinariesText, textSize);

        float currentY = y;
        int textColor = UIBase.getUITheme().warning_color.getColorInt();
        for (MutableComponent line : infoLines) {
            float lineWidth = UIBase.getUITextWidth(line, textSize);
            float lineX = x + contentWidth - lineWidth;
            UIBase.renderText(graphics, line, lineX, currentY, textColor, textSize);
            currentY += lineHeight;
        }
        float downloadX = x + contentWidth - downloadTextWidth;
        float downloadY = y + infoHeight + spacing;
        float downloadBinariesX = x + contentWidth - downloadBinariesTextWidth;
        float downloadBinariesY = downloadY + lineHeight + spacing;

        this.watermediaDownloadX_Konkrete = downloadX;
        this.watermediaDownloadY_Konkrete = downloadY;
        this.watermediaDownloadWidth_Konkrete = downloadTextWidth;
        this.watermediaDownloadHeight_Konkrete = lineHeight;
        this.watermediaBinariesDownloadX_Konkrete = downloadBinariesX;
        this.watermediaBinariesDownloadY_Konkrete = downloadBinariesY;
        this.watermediaBinariesDownloadWidth_Konkrete = downloadBinariesTextWidth;
        this.watermediaBinariesDownloadHeight_Konkrete = lineHeight;

        boolean hoveredMain = this.isMouseOverWatermediaDownloadLink_Konkrete(mouseX, mouseY);
        boolean hoveredBinaries = this.isMouseOverWatermediaBinariesDownloadLink_Konkrete(mouseX, mouseY);
        if (hoveredMain || hoveredBinaries) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }

        Component renderedDownloadText = downloadText.copy().setStyle(Style.EMPTY.withBold(true).withUnderlined(hoveredMain));
        Component renderedDownloadBinariesText = downloadBinariesText.copy().setStyle(Style.EMPTY.withBold(true).withUnderlined(hoveredBinaries));
        UIBase.renderText(graphics, renderedDownloadText, downloadX, downloadY, textColor, textSize);
        UIBase.renderText(graphics, renderedDownloadBinariesText, downloadBinariesX, downloadBinariesY, textColor, textSize);
    }

    /** Renders watermedia missing warning into the active GUI extraction pass. */
    protected void renderWatermediaMissingWarning_Konkrete(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int previewMaxWidth = 200;
        int topY = (int) this.getMainAreaTopY();
        int availableHeight = Math.max(40, (this.cancelButton.getY() - 50) - topY);
        AspectRatio previewAspectRatio = new AspectRatio(16, 9);
        int[] previewSize = previewAspectRatio.getAspectRatioSizeByMaximumSize(previewMaxWidth, availableHeight);
        int previewWidth = Math.max(1, previewSize[0]);
        int previewHeight = Math.max(1, previewSize[1]);
        int x = this.width - 20 - previewWidth;
        int y = topY;

        int warningBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        graphics.fill(x, y, x + previewWidth, y + previewHeight, VIDEO_PREVIEW_WATERMEDIA_WARNING_BACKGROUND_COLOR.getColorInt());
        UIBase.renderBorder(graphics, x, y, x + previewWidth, y + previewHeight, UIBase.ELEMENT_BORDER_THICKNESS, warningBorderColor, true, true, true, true);

        Component infoText = Component.translatable("konkrete.backgrounds.video.watermedia_missing.info");
        Component downloadText = Component.translatable("konkrete.backgrounds.video.watermedia_missing.download");
        Component downloadBinariesText = Component.translatable("konkrete.backgrounds.video.watermedia_missing.download_binaries");
        Component boldDownloadText = downloadText.copy().setStyle(Style.EMPTY.withBold(true));
        Component boldDownloadBinariesText = downloadBinariesText.copy().setStyle(Style.EMPTY.withBold(true));

        float maxTextWidth = previewWidth - 12.0F;
        float spacing = Math.max(3.0F, UIBase.getUITextHeightSmall() * 0.5F);
        float infoTextSize = UIBase.getUITextSizeNormal();
        float linkTextSize = UIBase.getUITextSizeLarge();
        List<MutableComponent> infoLines = UIBase.lineWrapUIComponentsNormal(infoText, maxTextWidth);
        float infoLineHeight = UIBase.getUITextHeight(infoTextSize);
        float infoHeight = infoLines.size() * infoLineHeight;
        float downloadTextWidth = UIBase.getUITextWidth(boldDownloadText, linkTextSize);
        float downloadTextHeight = UIBase.getUITextHeight(linkTextSize);
        float downloadBinariesTextWidth = UIBase.getUITextWidth(boldDownloadBinariesText, linkTextSize);
        float downloadBinariesTextHeight = UIBase.getUITextHeight(linkTextSize);
        if ((downloadTextWidth > maxTextWidth) || (downloadBinariesTextWidth > maxTextWidth)) {
            linkTextSize = UIBase.getUITextSizeNormal();
            downloadTextWidth = UIBase.getUITextWidth(boldDownloadText, linkTextSize);
            downloadTextHeight = UIBase.getUITextHeight(linkTextSize);
            downloadBinariesTextWidth = UIBase.getUITextWidth(boldDownloadBinariesText, linkTextSize);
            downloadBinariesTextHeight = UIBase.getUITextHeight(linkTextSize);
        }
        if ((downloadTextWidth > maxTextWidth) || (downloadBinariesTextWidth > maxTextWidth)) {
            linkTextSize = UIBase.getUITextSizeSmall();
            downloadTextWidth = UIBase.getUITextWidth(boldDownloadText, linkTextSize);
            downloadTextHeight = UIBase.getUITextHeight(linkTextSize);
            downloadBinariesTextWidth = UIBase.getUITextWidth(boldDownloadBinariesText, linkTextSize);
            downloadBinariesTextHeight = UIBase.getUITextHeight(linkTextSize);
        }
        float totalHeight = infoHeight + spacing + downloadTextHeight + spacing + downloadBinariesTextHeight;
        float maxContentHeight = previewHeight - 8.0F;
        if (totalHeight > maxContentHeight) {
            infoTextSize = UIBase.getUITextSizeSmall();
            infoLines = UIBase.lineWrapUIComponentsSmall(infoText, maxTextWidth);
            infoLineHeight = UIBase.getUITextHeight(infoTextSize);
            infoHeight = infoLines.size() * infoLineHeight;
            linkTextSize = UIBase.getUITextSizeSmall();
            downloadTextWidth = UIBase.getUITextWidth(boldDownloadText, linkTextSize);
            downloadTextHeight = UIBase.getUITextHeight(linkTextSize);
            downloadBinariesTextWidth = UIBase.getUITextWidth(boldDownloadBinariesText, linkTextSize);
            downloadBinariesTextHeight = UIBase.getUITextHeight(linkTextSize);
            totalHeight = infoHeight + spacing + downloadTextHeight + spacing + downloadBinariesTextHeight;
        }
        float currentY = y + (previewHeight / 2.0F) - (totalHeight / 2.0F);

        int textColor = DrawableColor.WHITE.getColorInt();
        for (MutableComponent line : infoLines) {
            float lineWidth = UIBase.getUITextWidth(line, infoTextSize);
            float lineX = x + (previewWidth / 2.0F) - (lineWidth / 2.0F);
            UIBase.renderText(graphics, line, lineX, currentY, textColor, infoTextSize);
            currentY += infoLineHeight;
        }
        float downloadX = x + (previewWidth / 2.0F) - (downloadTextWidth / 2.0F);
        float downloadY = currentY + spacing;
        float downloadBinariesX = x + (previewWidth / 2.0F) - (downloadBinariesTextWidth / 2.0F);
        float downloadBinariesY = downloadY + downloadTextHeight + spacing;

        this.watermediaDownloadX_Konkrete = downloadX;
        this.watermediaDownloadY_Konkrete = downloadY;
        this.watermediaDownloadWidth_Konkrete = downloadTextWidth;
        this.watermediaDownloadHeight_Konkrete = downloadTextHeight;
        this.watermediaBinariesDownloadX_Konkrete = downloadBinariesX;
        this.watermediaBinariesDownloadY_Konkrete = downloadBinariesY;
        this.watermediaBinariesDownloadWidth_Konkrete = downloadBinariesTextWidth;
        this.watermediaBinariesDownloadHeight_Konkrete = downloadBinariesTextHeight;

        boolean hoveredMain = this.isMouseOverWatermediaDownloadLink_Konkrete(mouseX, mouseY);
        boolean hoveredBinaries = this.isMouseOverWatermediaBinariesDownloadLink_Konkrete(mouseX, mouseY);
        if (hoveredMain || hoveredBinaries) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }
        Component renderedDownloadText = downloadText.copy().setStyle(Style.EMPTY.withBold(true).withUnderlined(hoveredMain));
        Component renderedDownloadBinariesText = downloadBinariesText.copy().setStyle(Style.EMPTY.withBold(true).withUnderlined(hoveredBinaries));
        UIBase.renderText(graphics, renderedDownloadText, downloadX, downloadY, textColor, linkTextSize);
        UIBase.renderText(graphics, renderedDownloadBinariesText, downloadBinariesX, downloadBinariesY, textColor, linkTextSize);
    }

    /** Returns whether mouse over watermedia download link. */
    protected boolean isMouseOverWatermediaDownloadLink_Konkrete(double mouseX, double mouseY) {
        if (!Float.isFinite(this.watermediaDownloadX_Konkrete)
                || !Float.isFinite(this.watermediaDownloadY_Konkrete)
                || !Float.isFinite(this.watermediaDownloadWidth_Konkrete)
                || !Float.isFinite(this.watermediaDownloadHeight_Konkrete)) {
            return false;
        }
        return (mouseX >= this.watermediaDownloadX_Konkrete)
                && (mouseX <= (this.watermediaDownloadX_Konkrete + this.watermediaDownloadWidth_Konkrete))
                && (mouseY >= this.watermediaDownloadY_Konkrete)
                && (mouseY <= (this.watermediaDownloadY_Konkrete + this.watermediaDownloadHeight_Konkrete));
    }

    /** Returns whether mouse over watermedia binaries download link. */
    protected boolean isMouseOverWatermediaBinariesDownloadLink_Konkrete(double mouseX, double mouseY) {
        if (!Float.isFinite(this.watermediaBinariesDownloadX_Konkrete)
                || !Float.isFinite(this.watermediaBinariesDownloadY_Konkrete)
                || !Float.isFinite(this.watermediaBinariesDownloadWidth_Konkrete)
                || !Float.isFinite(this.watermediaBinariesDownloadHeight_Konkrete)) {
            return false;
        }
        return (mouseX >= this.watermediaBinariesDownloadX_Konkrete)
                && (mouseX <= (this.watermediaBinariesDownloadX_Konkrete + this.watermediaBinariesDownloadWidth_Konkrete))
                && (mouseY >= this.watermediaBinariesDownloadY_Konkrete)
                && (mouseY <= (this.watermediaBinariesDownloadY_Konkrete + this.watermediaBinariesDownloadHeight_Konkrete));
    }

    /** Clears watermedia download link bounds state. */
    protected void resetWatermediaDownloadLinkBounds_Konkrete() {
        this.watermediaDownloadX_Konkrete = Float.NaN;
        this.watermediaDownloadY_Konkrete = Float.NaN;
        this.watermediaDownloadWidth_Konkrete = Float.NaN;
        this.watermediaDownloadHeight_Konkrete = Float.NaN;
        this.watermediaBinariesDownloadX_Konkrete = Float.NaN;
        this.watermediaBinariesDownloadY_Konkrete = Float.NaN;
        this.watermediaBinariesDownloadWidth_Konkrete = Float.NaN;
        this.watermediaBinariesDownloadHeight_Konkrete = Float.NaN;
    }

    /** Renders video preview into the active GUI extraction pass. */
    protected void renderVideoPreview(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        int previewMaxWidth = 200;
        int topY = (int) this.getMainAreaTopY();
        int availableHeight = (this.cancelButton.getY() - 50) - topY;
        int textHeight = Math.round(UIBase.getUITextHeightNormal());
        int progressAreaHeight = AUDIO_PREVIEW_PROGRESS_BAR_SPACING + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + textHeight;
        int controlsAreaHeight = AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING + progressAreaHeight;
        int maxFrameHeight = Math.max(12, availableHeight - controlsAreaHeight);
        IVideo video = this.getPreviewVideo();
        AspectRatio ratio = this.getPreviewVideoAspectRatio(video);
        int[] frameSize = ratio.getAspectRatioSizeByMaximumSize(previewMaxWidth, maxFrameHeight);
        int previewWidth = Math.max(1, frameSize[0]);
        int previewHeight = Math.max(1, frameSize[1]);
        int x = this.width - 20 - previewWidth;
        int y = topY;
        int previewBackgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int previewBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();

        graphics.fill(x, y, x + previewWidth, y + previewHeight, previewBackgroundColor);

        Identifier location = (video != null) ? video.getResourceLocation() : null;
        if (location != null) {
            RenderingUtils.resetShaderColor(graphics);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, location, x, y, 0.0F, 0.0F, previewWidth, previewHeight, previewWidth, previewHeight);
            UIBase.resetShaderColor(graphics);
        }

        UIBase.renderBorder(graphics, x, y, x + previewWidth, y + previewHeight, UIBase.ELEMENT_BORDER_THICKNESS, previewBorderColor, true, true, true, true);
        int progressY = y + previewHeight + AUDIO_PREVIEW_PROGRESS_BAR_SPACING;
        this.renderVideoPreviewProgress(graphics, x, progressY, previewWidth);
        int controlsY = progressY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + textHeight + AUDIO_PREVIEW_BUTTON_SPACING;
        int buttonX = x;
        int buttonY = controlsY;
        int sliderX = buttonX + AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING;
        int sliderWidth = Math.max(10, previewWidth - (AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING));
        int sliderY = controlsY + (AUDIO_PREVIEW_BUTTON_SIZE - AUDIO_PREVIEW_SLIDER_HEIGHT) / 2;
        this.renderVideoPreviewButton(graphics, mouseX, mouseY, partial, buttonX, buttonY);
        this.renderVideoPreviewVolumeSlider(graphics, mouseX, mouseY, partial, sliderX, sliderY, sliderWidth);
    }

    /** Renders video preview button into the active GUI extraction pass. */
    protected void renderVideoPreviewButton(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, int buttonX, int buttonY) {
        if (this.videoPreviewToggleButton == null || this.previewVideoSupplier == null) return;
        this.videoPreviewToggleButton
                .setX(buttonX)
                .setY(buttonY)
                .setWidth(AUDIO_PREVIEW_BUTTON_SIZE)
                .setHeight(AUDIO_PREVIEW_BUTTON_SIZE)
                .setIcon(this.previewVideoPlaying ? AUDIO_PREVIEW_PAUSE_ICON : AUDIO_PREVIEW_PLAY_ICON);
        this.videoPreviewToggleButton.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Renders video preview volume slider into the active GUI extraction pass. */
    protected void renderVideoPreviewVolumeSlider(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, int sliderX, int sliderY, int sliderWidth) {
        if (this.videoPreviewVolumeSlider == null) return;
        this.videoPreviewVolumeSlider.setX(sliderX);
        this.videoPreviewVolumeSlider.setY(sliderY);
        this.videoPreviewVolumeSlider.setWidth(sliderWidth);
        this.videoPreviewVolumeSlider.setHeight(AUDIO_PREVIEW_SLIDER_HEIGHT);
        this.videoPreviewVolumeSlider.visible = true;
        this.videoPreviewVolumeSlider.active = (this.previewVideoSupplier != null);
        this.videoPreviewVolumeSlider.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Renders video preview progress into the active GUI extraction pass. */
    protected void renderVideoPreviewProgress(@NotNull GuiGraphicsExtractor graphics, int previewX, int progressY, int previewWidth) {
        int barX = previewX;
        int barWidth = previewWidth;
        int barY = progressY;
        int barYEnd = barY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT;
        this.videoPreviewProgressBarX = barX;
        this.videoPreviewProgressBarY = barY;
        this.videoPreviewProgressBarWidth = barWidth;
        this.videoPreviewProgressBarHeight = AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT;
        int progressBackgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int progressBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        graphics.fill(barX, barY, barX + barWidth, barYEnd, progressBackgroundColor);
        UIBase.renderBorder(graphics, barX, barY, barX + barWidth, barYEnd, 1, progressBorderColor, true, true, true, true);

        IVideo video = this.getPreviewVideo();
        float duration = video != null ? Math.max(0.0F, video.getDuration()) : 0.0F;
        float playTime = video != null ? Math.max(0.0F, video.getPlayTime()) : 0.0F;
        float progress = duration > 0.0F ? Math.min(1.0F, playTime / duration) : 0.0F;
        int filledWidth = (int) (barWidth * progress);
        if (filledWidth > 0) {
            int fillColor = this.getAudioVisualizerGradientColor(progress);
            graphics.fill(barX, barY, barX + filledWidth, barYEnd, fillColor);
        }

        String timeText = this.formatAudioTime(playTime) + " / " + this.formatAudioTime(duration);
        int timeY = barYEnd + AUDIO_PREVIEW_TIME_SPACING;
        float textWidth = UIBase.getUITextWidth(timeText);
        float textX = previewX + (previewWidth / 2.0F) - (textWidth / 2.0F);
        UIBase.renderText(graphics, timeText, textX, timeY, UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt());
    }

    /** Renders audio preview into the active GUI extraction pass. */
    protected void renderAudioPreview(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        int previewWidth = 200;
        int topY = (int) this.getMainAreaTopY();
        int availableHeight = (this.cancelButton.getY() - 50) - topY;
        int textHeight = Math.round(UIBase.getUITextHeightNormal());
        int progressAreaHeight = AUDIO_PREVIEW_PROGRESS_BAR_SPACING + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + textHeight;
        int basePreviewHeight = Math.max(40, availableHeight - (AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING + progressAreaHeight));
        int previewHeight = Math.max(12, basePreviewHeight / 3);
        int x = this.width - 20 - previewWidth;
        int y = topY;
        int previewBackgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int previewBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        graphics.fill(x, y, x + previewWidth, y + previewHeight, previewBackgroundColor);
        this.renderAudioVisualizer(graphics, x + 4, y + 4, previewWidth - 8, previewHeight - 8);
        UIBase.renderBorder(graphics, x, y, x + previewWidth, y + previewHeight, UIBase.ELEMENT_BORDER_THICKNESS, previewBorderColor, true, true, true, true);
        int progressY = y + previewHeight + AUDIO_PREVIEW_PROGRESS_BAR_SPACING;
        this.renderAudioPreviewProgress(graphics, x, progressY, previewWidth);
        int controlsY = progressY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + textHeight + AUDIO_PREVIEW_BUTTON_SPACING;
        int buttonX = x;
        int buttonY = controlsY;
        int sliderX = buttonX + AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING;
        int sliderWidth = Math.max(10, previewWidth - (AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING));
        int sliderY = controlsY + (AUDIO_PREVIEW_BUTTON_SIZE - AUDIO_PREVIEW_SLIDER_HEIGHT) / 2;
        this.renderAudioPreviewButton(graphics, mouseX, mouseY, partial, buttonX, buttonY);
        this.renderAudioPreviewVolumeSlider(graphics, mouseX, mouseY, partial, sliderX, sliderY, sliderWidth);
    }

    /** Renders audio preview button into the active GUI extraction pass. */
    protected void renderAudioPreviewButton(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, int buttonX, int buttonY) {
        if (this.audioPreviewToggleButton == null || this.previewAudioSupplier == null) return;
        this.audioPreviewToggleButton
                .setX(buttonX)
                .setY(buttonY)
                .setWidth(AUDIO_PREVIEW_BUTTON_SIZE)
                .setHeight(AUDIO_PREVIEW_BUTTON_SIZE)
                .setIcon(this.previewAudioPlaying ? AUDIO_PREVIEW_PAUSE_ICON : AUDIO_PREVIEW_PLAY_ICON);
        this.audioPreviewToggleButton.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Renders audio preview volume slider into the active GUI extraction pass. */
    protected void renderAudioPreviewVolumeSlider(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, int sliderX, int sliderY, int sliderWidth) {
        if (this.audioPreviewVolumeSlider == null) return;
        this.audioPreviewVolumeSlider.setX(sliderX);
        this.audioPreviewVolumeSlider.setY(sliderY);
        this.audioPreviewVolumeSlider.setWidth(sliderWidth);
        this.audioPreviewVolumeSlider.setHeight(AUDIO_PREVIEW_SLIDER_HEIGHT);
        this.audioPreviewVolumeSlider.visible = true;
        this.audioPreviewVolumeSlider.active = (this.previewAudioSupplier != null);
        this.audioPreviewVolumeSlider.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    /** Renders audio visualizer into the active GUI extraction pass. */
    protected void renderAudioVisualizer(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int barWidth = Math.max(2, width / 64);
        int gap = Math.max(1, barWidth / 2);
        int barCount = Math.max(8, (width + gap) / (barWidth + gap));
        float time = this.getAudioVisualizerTime();
        float baseIntensity = this.previewAudioPlaying ? 1.0F : 0.35F;
        for (int i = 0; i < barCount; i++) {
            int barX = x + i * (barWidth + gap);
            if (barX + barWidth > x + width) break;
            float phase = (time * 2.6F) + (i * 0.35F);
            float wave = (float)((Math.sin(phase) + 1.0) * 0.5);
            float wave2 = (float)((Math.sin(phase * 0.7F + 1.3F) + 1.0) * 0.5);
            float intensity = (wave * 0.7F + wave2 * 0.3F) * baseIntensity;
            int barHeight = Math.max(2, (int)(intensity * height));
            int barY = y + (height - barHeight);
            int color = this.getAudioVisualizerGradientColor((float)i / (float)Math.max(1, barCount - 1));
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, color);
        }
    }

    /** Renders audio preview progress into the active GUI extraction pass. */
    protected void renderAudioPreviewProgress(@NotNull GuiGraphicsExtractor graphics, int previewX, int progressY, int previewWidth) {
        int barX = previewX;
        int barWidth = previewWidth;
        int barY = progressY;
        int barYEnd = barY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT;
        this.audioPreviewProgressBarX = barX;
        this.audioPreviewProgressBarY = barY;
        this.audioPreviewProgressBarWidth = barWidth;
        this.audioPreviewProgressBarHeight = AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT;
        int progressBackgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int progressBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        graphics.fill(barX, barY, barX + barWidth, barYEnd, progressBackgroundColor);
        UIBase.renderBorder(graphics, barX, barY, barX + barWidth, barYEnd, 1, progressBorderColor, true, true, true, true);

        IAudio audio = this.getPreviewAudio();
        float duration = audio != null ? Math.max(0.0F, audio.getDuration()) : 0.0F;
        float playTime = audio != null ? Math.max(0.0F, audio.getPlayTime()) : 0.0F;
        float progress = duration > 0.0F ? Math.min(1.0F, playTime / duration) : 0.0F;
        int filledWidth = (int)(barWidth * progress);
        if (filledWidth > 0) {
            int fillColor = this.getAudioVisualizerGradientColor(progress);
            graphics.fill(barX, barY, barX + filledWidth, barYEnd, fillColor);
        }

        String timeText = this.formatAudioTime(playTime) + " / " + this.formatAudioTime(duration);
        int timeY = barYEnd + AUDIO_PREVIEW_TIME_SPACING;
        float textWidth = UIBase.getUITextWidth(timeText);
        float textX = previewX + (previewWidth / 2.0F) - (textWidth / 2.0F);
        UIBase.renderText(graphics, timeText, textX, timeY, UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt());
    }

    /** Returns main area label y. */
    protected float getMainAreaLabelY() {
        return 50.0F;
    }

    /** Returns main area top y. */
    protected float getMainAreaTopY() {
        return this.getMainAreaLabelY() + UIBase.getUITextHeightNormal() + UIBase.getAreaLabelVerticalPadding();
    }

    /** Transforms audio time using the supplied settings. */
    @NotNull
    protected String formatAudioTime(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0.0F) seconds = 0.0F;
        int totalSeconds = (int) seconds;
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, secs);
    }

    /** Applies preview audio volume to the supplied target. */
    protected void applyPreviewAudioVolume(float volume) {
        if (this.currentPreviewAudio != null) {
            this.currentPreviewAudio.setVolume(volume);
        }
    }

    /** Applies preview video volume to the supplied target. */
    protected void applyPreviewVideoVolume(float volume) {
        float clampedVolume = Math.max(0.0F, Math.min(1.0F, volume));
        float masterVolume = this.getPreviewVideoMasterVolume();
        float effectiveVolume = Math.max(0.0F, Math.min(1.0F, clampedVolume * masterVolume));
        if (this.currentPreviewVideo != null) {
            this.currentPreviewVideo.setVolume(effectiveVolume);
        }
    }

    /** Returns preview video aspect ratio. */
    @NotNull
    protected AspectRatio getPreviewVideoAspectRatio(@Nullable IVideo video) {
        if (video == null) {
            return new AspectRatio(16, 9);
        }
        int videoWidth = video.getWidth();
        int videoHeight = video.getHeight();
        if (videoWidth <= 16 || videoHeight <= 16) {
            return new AspectRatio(16, 9);
        }
        return video.getAspectRatio();
    }

    /** Returns preview video master volume. */
    protected float getPreviewVideoMasterVolume() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, minecraft.options.getSoundSourceVolume(SoundSource.MASTER)));
    }

    /** Returns audio visualizer time. */
    protected float getAudioVisualizerTime() {
        if (this.previewAudioPlaying && this.currentPreviewAudio != null) {
            return this.currentPreviewAudio.getPlayTime();
        }
        return (float)(this.previewAudioSeed % 10000L) / 1000.0F;
    }

    /** Returns audio visualizer gradient color. */
    protected int getAudioVisualizerGradientColor(float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        int orange = 0xFFFFA500;
        int red = 0xFFFF0000;
        int magenta = 0xFFFF00FF;
        int purple = 0xFF8000FF;
        if (clamped <= 0.33F) {
            return this.lerpColor(orange, red, clamped / 0.33F);
        }
        if (clamped <= 0.66F) {
            return this.lerpColor(red, magenta, (clamped - 0.33F) / 0.33F);
        }
        return this.lerpColor(magenta, purple, (clamped - 0.66F) / 0.34F);
    }

    /** Interpolates two ARGB colors by the supplied progress value. */
    protected int lerpColor(int start, int end, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int a1 = (start >> 24) & 0xFF;
        int r1 = (start >> 16) & 0xFF;
        int g1 = (start >> 8) & 0xFF;
        int b1 = start & 0xFF;
        int a2 = (end >> 24) & 0xFF;
        int r2 = (end >> 16) & 0xFF;
        int g2 = (end >> 8) & 0xFF;
        int b2 = end & 0xFF;
        int a = (int)(a1 + (a2 - a1) * clamped);
        int r = (int)(r1 + (r2 - r1) * clamped);
        int g = (int)(g1 + (g2 - g1) * clamped);
        int b = (int)(b1 + (b2 - b1) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Represents one renderable, focusable abstract icon text scroll area entry. */
    public abstract static class AbstractIconTextScrollAreaEntry extends ScrollAreaEntry {

        /** Border in GUI pixels. */
        protected static final int BORDER = 3;

        /** Label rendered beside the entry icon. */
        protected final MutableComponent entryNameComponent;
        /** Millisecond timestamp used to recognize double-click activation. */
        protected long lastClick = -1;

        /** Attaches labeled icon content to its owning browser scroll area. */
        public AbstractIconTextScrollAreaEntry(@NotNull ScrollArea parent, @NotNull MutableComponent entryNameComponent) {
            super(parent, 100, 30);
            this.entryNameComponent = entryNameComponent;

            this.setWidth((int)UIBase.getUITextWidthNormal(this.entryNameComponent) + (BORDER * 2) + ICON_PIXEL_SIZE + 3);
            this.setHeight((BORDER * 2) + ICON_PIXEL_SIZE);

            this.playClickSound = false;
        }

        /** Returns the Material icon representing this browser entry. */
        @NotNull
        protected abstract MaterialIcon getIcon();

        /** Returns the icon layout-box width in GUI units. */
        protected int getIconRenderWidth() {
            return ICON_PIXEL_SIZE;
        }

        /** Returns the icon layout-box height in GUI units. */
        protected int getIconRenderHeight() {
            return ICON_PIXEL_SIZE;
        }

        /** Returns the per-edge inset inside the icon layout box in GUI units. */
        protected int getIconInnerPadding() {
            return 0;
        }

        /** Reports whether the entry name violates resource-path rules. */
        protected boolean isResourceUnfriendly() {
            return false;
        }

        /** Returns the packed color used to draw the current text. */
        protected int getTextColor() {
            if (this.isResourceUnfriendly()) {
                return UIBase.getUITheme().error_color.getColorInt();
            }
            return UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
        }

        /** Renders entry into the active GUI extraction pass. */
        @Override
        public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

            int iconWidth = this.getIconRenderWidth();
            int iconHeight = this.getIconRenderHeight();
            int padding = Math.max(0, this.getIconInnerPadding());
            float areaX = this.x + BORDER + padding;
            float areaY = this.y + BORDER + padding;
            float areaWidth = iconWidth - (padding * 2);
            float areaHeight = iconHeight - (padding * 2);
            IconRenderData iconData = resolveMaterialIconData(this.getIcon(), areaWidth, areaHeight);
            if (iconData != null) {
                UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
                blitScaledIcon(graphics, iconData, areaX, areaY, areaWidth, areaHeight);
                UIBase.resetShaderColor(graphics);
            }

            UIBase.renderText(graphics, this.entryNameComponent, this.x + BORDER + ICON_PIXEL_SIZE + 3, this.y + (this.height / 2f) - (UIBase.getUITextHeightNormal() / 2f), this.getTextColor());

            if (this.isResourceUnfriendly() && this.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height) && this.parent.isMouseOverInnerArea(mouseX, mouseY)) {
                TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(Component.translatable("konkrete.ui.filechooser.resource_name_check.not_passed.tooltip")), () -> true);
            }

        }

        @Nullable
        private static IconRenderData resolveMaterialIconData(@Nullable MaterialIcon icon, float renderWidth, float renderHeight) {
            if (icon == null) {
                return null;
            }
            Identifier location = icon.getTextureLocationForUI(renderWidth, renderHeight);
            if (location == null) {
                return null;
            }
            int iconSize = icon.calculateBestTextureSizeForUI(renderWidth, renderHeight);
            int width = icon.getWidth(iconSize);
            int height = icon.getHeight(iconSize);
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new IconRenderData(location, width, height);
        }

        private static void blitScaledIcon(@NotNull GuiGraphicsExtractor graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
            if (areaWidth <= 0.0F || areaHeight <= 0.0F) {
                return;
            }
            float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
            if (!Float.isFinite(scale) || scale <= 0.0F) {
                return;
            }
            float scaledWidth = iconData.width * scale;
            float scaledHeight = iconData.height * scale;
            float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
            float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(drawX, drawY);
            graphics.pose().scale(scale, scale);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
            graphics.pose().popMatrix();
        }

        private static final class IconRenderData {
            private final Identifier texture;
            private final int width;
            private final int height;

            private IconRenderData(@NotNull Identifier texture, int width, int height) {
                this.texture = texture;
                this.width = width;
                this.height = height;
            }
        }

    }

}
