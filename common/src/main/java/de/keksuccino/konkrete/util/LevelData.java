package de.keksuccino.konkrete.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/** A stable snapshot of one local Minecraft world. */
public class LevelData {

    private static final Gson GSON = new GsonBuilder().create();

    /** Filesystem level identifier. */
    public String file_name;
    /** Player-visible world name. */
    public String display_name;
    /** Whether Minecraft requires manual format conversion before loading. */
    public boolean requires_manual_conversion;
    /** Whether another process currently locks the world. */
    public boolean locked;
    /** Whether the world enables experimental features. */
    public boolean experimental;
    /** Absolute path to the world icon. */
    public String icon_path;
    /** Serialized game-mode name. */
    public String game_type;
    /** Serialized difficulty name. */
    public String difficulty;
    /** Whether commands are enabled. */
    public boolean allow_commands;
    /** Level name stored in world settings, with display-name fallback. */
    public String settings_level_name;
    /** Last-played epoch timestamp supplied by Minecraft. */
    public long last_played;
    /** Numeric level-data format version. */
    public int level_data_version;
    /** Minecraft version that last saved the world. */
    public String minecraft_version_name;
    /** Whether the saving Minecraft version was a snapshot. */
    public boolean snapshot;
    /** Whether Minecraft permits editing this world's settings. */
    public boolean can_edit;
    /** Whether Minecraft permits recreating this world. */
    public boolean can_recreate;
    /** Whether Minecraft permits deleting this world. */
    public boolean can_delete;

    /** Builds a snapshot from a Minecraft level summary. */
    @NotNull
    public static LevelData fromLevelSummary(@NotNull LevelSummary summary) {

        LevelData data = new LevelData();

        data.file_name = summary.getLevelId();
        data.display_name = summary.getLevelName();
        data.requires_manual_conversion = summary.requiresManualConversion();
        data.locked = summary.isLocked();
        data.experimental = summary.isExperimental();
        data.icon_path = summary.getIcon().toAbsolutePath().toString();
        data.game_type = summary.getGameMode().getSerializedName();
        data.difficulty = summary.getSettings().difficultySettings().difficulty().getSerializedName();
        data.allow_commands = summary.getSettings().allowCommands();
        String settingsLevelName = summary.getSettings().levelName();
        data.settings_level_name = (settingsLevelName == null || settingsLevelName.isEmpty()) ? summary.getLevelName() : settingsLevelName;
        data.last_played = summary.getLastPlayed();
        data.level_data_version = summary.levelVersion().levelDataVersion();
        data.minecraft_version_name = summary.levelVersion().minecraftVersionName();
        data.snapshot = summary.levelVersion().snapshot();
        data.can_edit = summary.canEdit();
        data.can_recreate = summary.canRecreate();
        data.can_delete = summary.canDelete();

        return data;

    }

    /** Deserializes JSON, returning an empty snapshot when the JSON literal is {@code null}. */
    @NotNull
    public static LevelData deserialize(@NotNull String json) {
        return Objects.requireNonNullElse(GSON.fromJson(json, LevelData.class), new LevelData());
    }

    /** Serializes this snapshot as JSON. */
    @NotNull
    public String serialize() {
        return GSON.toJson(this);
    }

}
