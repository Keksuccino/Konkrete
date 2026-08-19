package de.keksuccino.konkrete.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Utility methods for world. */
public class WorldUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Returns whether the client currently owns an integrated server. */
    public static boolean isSingleplayer() {
        if (Minecraft.getInstance().level == null) return false;
        return (Minecraft.getInstance().hasSingleplayerServer()) && (Minecraft.getInstance().getSingleplayerServer() != null) && !Minecraft.getInstance().getSingleplayerServer().isPublished();
    }

    /** Returns whether the client is connected without an integrated server. */
    public static boolean isMultiplayer() {
        if (Minecraft.getInstance().level == null) return false;
        return !isSingleplayer();
    }

    /** Returns the levels. */
    @NotNull
    public static List<LevelSummary> getLevels() {
        Minecraft minecraft = Minecraft.getInstance();
        LevelStorageSource.LevelCandidates levelCandidates;
        CompletableFuture<List<LevelSummary>> future;
        try {
            levelCandidates = minecraft.getLevelSource().findLevelCandidates();
        } catch (LevelStorageException ex) {
            LOGGER.error("[KONKRETE] Couldn't load level list!", ex);
            return List.of();
        }
        if (levelCandidates.isEmpty()) {
            return List.of();
        } else {
            future = minecraft.getLevelSource().loadLevelSummaries(levelCandidates);
        }
        try {
            // loadLevelSummaries resolves asynchronously, so getNow(...) can return empty even though worlds exist.
            return Objects.requireNonNullElse(future.join(), List.of());
        } catch (CompletionException | CancellationException ex) {
            LOGGER.error("[KONKRETE] Couldn't load level summaries!", ex);
        }
        return List.of();
    }

    /** Returns the levels as data. */
    @NotNull
    public static List<LevelData> getLevelsAsData() {
        List<LevelData> data = new ArrayList<>();
        getLevels().forEach(summary -> data.add(LevelData.fromLevelSummary(summary)));
        return data;
    }

}
