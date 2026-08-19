package de.keksuccino.konkrete.util.ffmpeg.downloader;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Optional vanilla-widget UI for an injected {@link FFMPEGDownloader}; no global config or owning-mod screen framework is required.
 */
public class FFMPEGDownloaderScreen extends Screen {

    private final FFMPEGDownloader downloader;
    private final Consumer<FFMPEGDownloaderScreenResult> onClosed;
    private final Consumer<Path> folderOpener;
    private final Labels labels;
    private final boolean autoStart;
    @Nullable private Button actionButton;
    @Nullable private Button cancelButton;
    @Nullable private Button openFolderButton;
    @Nullable private Button closeButton;
    private boolean startRequested;

    /** Creates an auto-starting screen with English literal labels and a no-op folder action. */
    public FFMPEGDownloaderScreen(@NotNull FFMPEGDownloader downloader, @NotNull Consumer<FFMPEGDownloaderScreenResult> onClosed) {
        this(downloader, onClosed, path -> {}, Labels.english(), true);
    }

    /** Creates a fully configured downloader screen. */
    public FFMPEGDownloaderScreen(@NotNull FFMPEGDownloader downloader, @NotNull Consumer<FFMPEGDownloaderScreenResult> onClosed, @NotNull Consumer<Path> folderOpener, @NotNull Labels labels, boolean autoStart) {
        super(Objects.requireNonNull(labels, "labels").title());
        this.downloader = Objects.requireNonNull(downloader, "downloader");
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed");
        this.folderOpener = Objects.requireNonNull(folderOpener, "folderOpener");
        this.labels = labels;
        this.autoStart = autoStart;
    }

    /** {@inheritDoc} */
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height - 34;
        this.actionButton = this.addRenderableWidget(Button.builder(this.labels.start(), button -> this.startDownload()).bounds(centerX - 204, buttonY, 96, 20).build());
        this.cancelButton = this.addRenderableWidget(Button.builder(this.labels.cancel(), button -> this.downloader.cancelCurrentDownload()).bounds(centerX - 104, buttonY, 96, 20).build());
        this.openFolderButton = this.addRenderableWidget(Button.builder(this.labels.openFolder(), button -> this.openDownloadFolder()).bounds(centerX - 4, buttonY, 108, 20).build());
        this.closeButton = this.addRenderableWidget(Button.builder(this.labels.close(), button -> this.onClose()).bounds(centerX + 108, buttonY, 96, 20).build());
        this.updateButtonStates(this.downloader.getSnapshot());
        if (this.autoStart && !this.startRequested) this.startDownload();
    }

    /** {@inheritDoc} */
    @Override
    public void tick() {
        super.tick();
        this.updateButtonStates(this.downloader.getSnapshot());
    }

    /** {@inheritDoc} */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FFMPEGDownloadSnapshot snapshot = this.downloader.getSnapshot();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int progressWidth = Math.max(120, this.width / 3);
        graphics.centeredText(this.font, this.title, centerX, centerY - 48, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal(snapshot.getTask()), centerX, centerY - 30, 0xFFFFFFFF);
        if (snapshot.getDetail() != null) graphics.centeredText(this.font, Component.literal(snapshot.getDetail()), centerX, centerY - 17, 0xFFB0B0B0);
        graphics.fill(centerX - progressWidth / 2, centerY, centerX + progressWidth / 2, centerY + 14, 0xFFFFFFFF);
        graphics.fill(centerX - progressWidth / 2 + 2, centerY + 2, centerX + progressWidth / 2 - 2, centerY + 12, 0xFF000000);
        int fillWidth = (int)((progressWidth - 4) * snapshot.getProgress());
        if (fillWidth > 0) graphics.fill(centerX - progressWidth / 2 + 2, centerY + 2, centerX - progressWidth / 2 + 2 + fillWidth, centerY + 12, 0xFFFFFFFF);
        if (snapshot.isActive()) graphics.centeredText(this.font, Component.literal(Math.round(snapshot.getProgress() * 100.0D) + "%"), centerX, centerY + 21, 0xFFFFFFFF);
        if (snapshot.getFailureMessage() != null) graphics.centeredText(this.font, Component.literal(snapshot.getFailureMessage()), centerX, centerY + 38, 0xFFFF7A7A);
    }

    /** Emits the current snapshot-derived result without imposing parent-screen navigation. */
    @Override
    public void onClose() {
        this.onClosed.accept(FFMPEGDownloaderScreenResult.fromSnapshot(this.downloader.getSnapshot()));
    }

    /** Prevents escape from bypassing the explicit close/result callback. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /** Returns true because installation UI should pause single-player. */
    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private void startDownload() {
        this.startRequested = true;
        this.downloader.startDownloadIfNeededAsync();
    }

    private void openDownloadFolder() {
        java.io.File installedDirectory = this.downloader.getInstalledDirectory();
        Path directory = installedDirectory == null ? this.downloader.getDownloadDirectory().toPath() : installedDirectory.toPath();
        this.folderOpener.accept(directory);
    }

    private void updateButtonStates(FFMPEGDownloadSnapshot snapshot) {
        if (this.actionButton == null || this.cancelButton == null || this.openFolderButton == null || this.closeButton == null) return;
        boolean active = snapshot.isActive();
        boolean complete = snapshot.getStage() == FFMPEGDownloadSnapshot.Stage.COMPLETE;
        this.actionButton.visible = !active && !complete;
        this.actionButton.active = !active;
        if (snapshot.getStage() == FFMPEGDownloadSnapshot.Stage.FAILED || snapshot.getStage() == FFMPEGDownloadSnapshot.Stage.CANCELLED) this.actionButton.setMessage(this.labels.retry());
        else this.actionButton.setMessage(this.labels.start());
        this.cancelButton.visible = active;
        this.cancelButton.active = active;
        this.closeButton.visible = !active;
        this.closeButton.active = !active;
        this.openFolderButton.visible = !active;
        this.openFolderButton.active = this.downloader.getDownloadDirectory().isDirectory();
    }

    /**
     * Caller-localizable screen labels.
     *
     * @param title screen title
     * @param start start label
     * @param retry retry label
     * @param cancel cancel label
     * @param openFolder folder label
     * @param close close label
     */
    public record Labels(@NotNull Component title, @NotNull Component start, @NotNull Component retry, @NotNull Component cancel, @NotNull Component openFolder, @NotNull Component close) {

        /** Validates label components. */
        public Labels {
            title = Objects.requireNonNull(title, "title");
            start = Objects.requireNonNull(start, "start");
            retry = Objects.requireNonNull(retry, "retry");
            cancel = Objects.requireNonNull(cancel, "cancel");
            openFolder = Objects.requireNonNull(openFolder, "openFolder");
            close = Objects.requireNonNull(close, "close");
        }

        /** Returns dependency-free English literal labels. */
        @NotNull
        public static Labels english() {
            return new Labels(Component.literal("FFmpeg Installer"), Component.literal("Install"), Component.literal("Retry"), Component.literal("Cancel"), Component.literal("Open Folder"), Component.literal("Close"));
        }

    }

}
