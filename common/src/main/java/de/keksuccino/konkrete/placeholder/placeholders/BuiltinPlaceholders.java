package de.keksuccino.konkrete.placeholder.placeholders;

import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.placeholder.PlaceholderRegistry;
import de.keksuccino.konkrete.placeholder.placeholders.advanced.*;
import de.keksuccino.konkrete.placeholder.placeholders.client.*;
import de.keksuccino.konkrete.placeholder.placeholders.gui.*;
import de.keksuccino.konkrete.placeholder.placeholders.other.*;
import de.keksuccino.konkrete.placeholder.placeholders.other.cpu.*;
import de.keksuccino.konkrete.placeholder.placeholders.other.ram.*;
import de.keksuccino.konkrete.placeholder.placeholders.player.*;
import de.keksuccino.konkrete.placeholder.placeholders.realtime.*;
import de.keksuccino.konkrete.placeholder.placeholders.server.*;
import de.keksuccino.konkrete.placeholder.placeholders.world.*;

import java.util.ArrayList;
import java.util.List;

/** Owns and bootstraps Konkrete’s reusable built-in placeholders in deterministic order. */
public final class BuiltinPlaceholders {

    public static final MinecraftVersionPlaceholder MINECRAFT_VERSION = new MinecraftVersionPlaceholder();
    public static final ModLoaderVersionPlaceholder MOD_LOADER_VERSION = new ModLoaderVersionPlaceholder();
    public static final ModLoaderNamePlaceholder MOD_LOADER_NAME = new ModLoaderNamePlaceholder();
    public static final ModVersionPlaceholder MOD_VERSION = new ModVersionPlaceholder();
    public static final LoadedModsPlaceholder LOADED_MODS = new LoadedModsPlaceholder();
    public static final TotalModsPlaceholder TOTAL_MODS = new TotalModsPlaceholder();
    public static final MinecraftOptionValuePlaceholder MINECRAFT_OPTION_VALUE = new MinecraftOptionValuePlaceholder();
    public static final ScreenWidthPlaceholder SCREEN_WIDTH = new ScreenWidthPlaceholder();
    public static final ScreenHeightPlaceholder SCREEN_HEIGHT = new ScreenHeightPlaceholder();
    public static final MousePosXPlaceholder MOUSE_POS_X = new MousePosXPlaceholder();
    public static final MousePosYPlaceholder MOUSE_POS_Y = new MousePosYPlaceholder();
    public static final GuiScalePlaceholder GUI_SCALE = new GuiScalePlaceholder();
    public static final PlayerNamePlaceholder PLAYER_NAME = new PlayerNamePlaceholder();
    public static final PlayerUuidPlaceholder PLAYER_UUID = new PlayerUuidPlaceholder();
    public static final ServerMotdPlaceholder SERVER_MOTD = new ServerMotdPlaceholder();
    public static final ServerPingPlaceholder SERVER_PING = new ServerPingPlaceholder();
    public static final ServerVersionPlaceholder SERVER_VERSION = new ServerVersionPlaceholder();
    public static final ServerPlayerCountPlaceholder SERVER_PLAYER_COUNT = new ServerPlayerCountPlaceholder();
    public static final ServerStatusPlaceholder SERVER_STATUS = new ServerStatusPlaceholder();
    public static final RealtimeYearPlaceholder REALTIME_YEAR = new RealtimeYearPlaceholder();
    public static final RealtimeMonthPlaceholder REALTIME_MONTH = new RealtimeMonthPlaceholder();
    public static final RealtimeDayPlaceholder REALTIME_DAY = new RealtimeDayPlaceholder();
    public static final RealtimeHourPlaceholder REALTIME_HOUR = new RealtimeHourPlaceholder();
    public static final RealtimeMinutePlaceholder REALTIME_MINUTE = new RealtimeMinutePlaceholder();
    public static final RealtimeSecondPlaceholder REALTIME_SECOND = new RealtimeSecondPlaceholder();
    public static final UnixTimestampPlaceholder UNIX_TIMESTAMP = new UnixTimestampPlaceholder();
    public static final StringifyPlaceholder STRINGIFY = new StringifyPlaceholder();
    public static final Base64EncodePlaceholder BASE64_ENCODE = new Base64EncodePlaceholder();
    public static final Base64DecodePlaceholder BASE64_DECODE = new Base64DecodePlaceholder();
    public static final JsonPlaceholder JSON = new JsonPlaceholder();
    public static final ClientSideNbtDataGetPlaceholder NBT_DATA_GET = new ClientSideNbtDataGetPlaceholder();
    public static final ServerSideNbtDataGetPlaceholder NBT_DATA_GET_SERVER = new ServerSideNbtDataGetPlaceholder();
    public static final LocalizationPlaceholder LOCALIZATION = new LocalizationPlaceholder();
    public static final CalculatorPlaceholder CALCULATOR = new CalculatorPlaceholder();
    public static final RandomNumberPlaceholder RANDOM_NUMBER = new RandomNumberPlaceholder();
    public static final MaxNumberPlaceholder MAX_NUMBER = new MaxNumberPlaceholder();
    public static final MinNumberPlaceholder MIN_NUMBER = new MinNumberPlaceholder();
    public static final AbsoluteNumberPlaceholder ABSOLUTE_NUMBER = new AbsoluteNumberPlaceholder();
    public static final NegateNumberPlaceholder NEGATE_NUMBER = new NegateNumberPlaceholder();
    public static final MathPiPlaceholder MATH_PI = new MathPiPlaceholder();
    public static final MathSinPlaceholder MATH_SIN = new MathSinPlaceholder();
    public static final MathSinhPlaceholder MATH_SINH = new MathSinhPlaceholder();
    public static final MathCosPlaceholder MATH_COS = new MathCosPlaceholder();
    public static final MathCoshPlaceholder MATH_COSH = new MathCoshPlaceholder();
    public static final MathTanPlaceholder MATH_TAN = new MathTanPlaceholder();
    public static final MathTanhPlaceholder MATH_TANH = new MathTanhPlaceholder();
    public static final PercentRamPlaceholder PERCENT_RAM = new PercentRamPlaceholder();
    public static final UsedRamPlaceholder USED_RAM = new UsedRamPlaceholder();
    public static final MaxRamPlaceholder MAX_RAM = new MaxRamPlaceholder();
    public static final JvmCpuUsagePlaceholder JVM_CPU_USAGE = new JvmCpuUsagePlaceholder();
    public static final OsCpuUsagePlaceholder OS_CPU_USAGE = new OsCpuUsagePlaceholder();
    public static final CpuInfoPlaceholder CPU_INFO = new CpuInfoPlaceholder();
    public static final FpsPlaceholder FPS = new FpsPlaceholder();
    public static final GpuInfoPlaceholder GPU_INFO = new GpuInfoPlaceholder();
    public static final JavaVersionPlaceholder JAVA_VERSION = new JavaVersionPlaceholder();
    public static final JvmNamePlaceholder JVM_NAME = new JvmNamePlaceholder();
    public static final OpenGLVersionPlaceholder OPEN_GL_VERSION = new OpenGLVersionPlaceholder();
    public static final OSNamePlaceholder OS_NAME = new OSNamePlaceholder();
    public static final UptimeDurationPlaceholder UPTIME_DURATION = new UptimeDurationPlaceholder();
    public static final RandomTextPlaceholder RANDOM_TEXT = new RandomTextPlaceholder();
    public static final WebTextPlaceholder WEB_TEXT = new WebTextPlaceholder();
    public static final ActiveHotbarSlotPlaceholder ACTIVE_HOTBAR_SLOT = new ActiveHotbarSlotPlaceholder();
    public static final CurrentPlayerHealthPlaceholder CURRENT_PLAYER_HEALTH = new CurrentPlayerHealthPlaceholder();
    public static final GameTimePlaceholder GAME_TIME = new GameTimePlaceholder();
    public static final GameruleValuePlaceholder GAMERULE_VALUE = new GameruleValuePlaceholder();
    public static final SlotItemPlaceholder SLOT_ITEM = new SlotItemPlaceholder();
    public static final InventoryItemCountPlaceholder INVENTORY_ITEM_COUNT = new InventoryItemCountPlaceholder();
    public static final SlotItemCountPlaceholder SLOT_ITEM_COUNT = new SlotItemCountPlaceholder();
    public static final SlotItemDurabilityPlaceholder SLOT_ITEM_DURABILITY = new SlotItemDurabilityPlaceholder();
    public static final InventorySlotFoodPointRestoreAmountPlaceholder INVENTORY_SLOT_FOOD_POINT_RESTORE_AMOUNT = new InventorySlotFoodPointRestoreAmountPlaceholder();
    public static final ItemCategoryPlaceholder ITEM_CATEGORY = new ItemCategoryPlaceholder();
    public static final WorldDayTimePlaceholder WORLD_DAY_TIME = new WorldDayTimePlaceholder();
    public static final WorldDayTimeHourPlaceholder WORLD_DAY_TIME_HOUR = new WorldDayTimeHourPlaceholder();
    public static final WorldDayTimeMinutePlaceholder WORLD_DAY_TIME_MINUTE = new WorldDayTimeMinutePlaceholder();
    public static final WorldDifficultyPlaceholder WORLD_DIFFICULTY = new WorldDifficultyPlaceholder();
    public static final MaxPlayerHealthPlaceholder MAX_PLAYER_HEALTH = new MaxPlayerHealthPlaceholder();
    public static final CurrentPlayerHealthPercentagePlaceholder CURRENT_PLAYER_HEALTH_PERCENTAGE = new CurrentPlayerHealthPercentagePlaceholder();
    public static final CurrentPlayerAbsorptionHealthPlaceholder CURRENT_PLAYER_ABSORPTION_HEALTH = new CurrentPlayerAbsorptionHealthPlaceholder();
    public static final MaxPlayerAbsorptionHealthPlaceholder MAX_PLAYER_ABSORPTION_HEALTH = new MaxPlayerAbsorptionHealthPlaceholder();
    public static final CurrentPlayerAbsorptionHealthPercentagePlaceholder CURRENT_PLAYER_ABSORPTION_HEALTH_PERCENTAGE = new CurrentPlayerAbsorptionHealthPercentagePlaceholder();
    public static final CurrentPlayerHungerPlaceholder CURRENT_PLAYER_HUNGER = new CurrentPlayerHungerPlaceholder();
    public static final CurrentPlayerHungerSaturationPlaceholder CURRENT_PLAYER_HUNGER_SATURATION = new CurrentPlayerHungerSaturationPlaceholder();
    public static final MaxPlayerHungerPlaceholder MAX_PLAYER_HUNGER = new MaxPlayerHungerPlaceholder();
    public static final CurrentPlayerHungerPercentagePlaceholder CURRENT_PLAYER_HUNGER_PERCENTAGE = new CurrentPlayerHungerPercentagePlaceholder();
    public static final CurrentPlayerArmorPlaceholder CURRENT_PLAYER_ARMOR = new CurrentPlayerArmorPlaceholder();
    public static final PlayerArmorToughnessPlaceholder PLAYER_ARMOR_TOUGHNESS = new PlayerArmorToughnessPlaceholder();
    public static final MaxPlayerArmorPlaceholder MAX_PLAYER_ARMOR = new MaxPlayerArmorPlaceholder();
    public static final CurrentPlayerArmorPercentagePlaceholder CURRENT_PLAYER_ARMOR_PERCENTAGE = new CurrentPlayerArmorPercentagePlaceholder();
    public static final CurrentPlayerExpProgressPlaceholder CURRENT_PLAYER_EXP_PROGRESS = new CurrentPlayerExpProgressPlaceholder();
    public static final CurrentPlayerExperiencePlaceholder CURRENT_PLAYER_EXPERIENCE = new CurrentPlayerExperiencePlaceholder();
    public static final CurrentPlayerLevelPlaceholder CURRENT_PLAYER_LEVEL = new CurrentPlayerLevelPlaceholder();
    public static final CurrentMountHealthPlaceholder CURRENT_MOUNT_HEALTH = new CurrentMountHealthPlaceholder();
    public static final MaxMountHealthPlaceholder MAX_MOUNT_HEALTH = new MaxMountHealthPlaceholder();
    public static final CurrentMountHealthPercentagePlaceholder CURRENT_MOUNT_HEALTH_PERCENTAGE = new CurrentMountHealthPercentagePlaceholder();
    public static final CurrentMountJumpMeterPlaceholder CURRENT_MOUNT_JUMP_METER = new CurrentMountJumpMeterPlaceholder();
    public static final ActiveEffectsCountPlaceholder ACTIVE_EFFECTS_COUNT = new ActiveEffectsCountPlaceholder();
    public static final ActiveEffectPlaceholder ACTIVE_EFFECT = new ActiveEffectPlaceholder();
    public static final PlayerXCoordinatePlaceholder PLAYER_X_COORDINATE = new PlayerXCoordinatePlaceholder();
    public static final PlayerYCoordinatePlaceholder PLAYER_Y_COORDINATE = new PlayerYCoordinatePlaceholder();
    public static final PlayerZCoordinatePlaceholder PLAYER_Z_COORDINATE = new PlayerZCoordinatePlaceholder();
    public static final CurrentServerIpPlaceholder CURRENT_SERVER_IP = new CurrentServerIpPlaceholder();
    public static final CurrentDimensionPlaceholder CURRENT_DIMENSION = new CurrentDimensionPlaceholder();
    public static final CurrentBiomePlaceholder CURRENT_BIOME = new CurrentBiomePlaceholder();
    public static final CurrentWorldSeedPlaceholder CURRENT_WORLD_SEED = new CurrentWorldSeedPlaceholder();
    public static final PlayerAttackStrengthPercentagePlaceholder PLAYER_ATTACK_STRENGTH_PERCENTAGE = new PlayerAttackStrengthPercentagePlaceholder();
    public static final PlayerGamemodePlaceholder PLAYER_GAMEMODE = new PlayerGamemodePlaceholder();
    public static final PlayerViewDirectionPlaceholder PLAYER_VIEW_DIRECTION = new PlayerViewDirectionPlaceholder();
    public static final SplitTextPlaceholder SPLIT_TEXT = new SplitTextPlaceholder();
    public static final TrimTextPlaceholder TRIM_TEXT = new TrimTextPlaceholder();
    public static final UppercaseTextPlaceholder UPPERCASE_TEXT = new UppercaseTextPlaceholder();
    public static final LowercaseTextPlaceholder LOWERCASE_TEXT = new LowercaseTextPlaceholder();
    public static final TitleCaseTextPlaceholder TITLE_CASE_TEXT = new TitleCaseTextPlaceholder();
    public static final SentenceCaseTextPlaceholder SENTENCE_CASE_TEXT = new SentenceCaseTextPlaceholder();
    public static final SnakeCaseTextPlaceholder SNAKE_CASE_TEXT = new SnakeCaseTextPlaceholder();
    public static final KebabCaseTextPlaceholder KEBAB_CASE_TEXT = new KebabCaseTextPlaceholder();
    public static final AlternatingCaseTextPlaceholder ALTERNATING_CASE_TEXT = new AlternatingCaseTextPlaceholder();
    public static final ToggleCaseTextPlaceholder TOGGLE_CASE_TEXT = new ToggleCaseTextPlaceholder();
    public static final CropTextPlaceholder CROP_TEXT = new CropTextPlaceholder();
    public static final MathCeilPlaceholder MATH_CEIL = new MathCeilPlaceholder();
    public static final MathFloorPlaceholder MATH_FLOOR = new MathFloorPlaceholder();
    public static final MathRoundPlaceholder MATH_ROUND = new MathRoundPlaceholder();
    public static final NumberBaseConvertPlaceholder NUMBER_BASE_CONVERT = new NumberBaseConvertPlaceholder();
    public static final MathSignPlaceholder MATH_SIGN = new MathSignPlaceholder();
    public static final SwitchCasePlaceholder SWITCH_CASE = new SwitchCasePlaceholder();
    public static final ReplaceTextPlaceholder REPLACE_TEXT = new ReplaceTextPlaceholder();
    public static final CurrentPlayerOxygenPlaceholder CURRENT_PLAYER_OXYGEN = new CurrentPlayerOxygenPlaceholder();
    public static final MaxPlayerOxygenPlaceholder MAX_PLAYER_OXYGEN = new MaxPlayerOxygenPlaceholder();
    public static final CurrentPlayerOxygenPercentagePlaceholder CURRENT_PLAYER_OXYGEN_PERCENTAGE = new CurrentPlayerOxygenPercentagePlaceholder();
    public static final TextCharacterCountPlaceholder TEXT_CHARACTER_COUNT = new TextCharacterCountPlaceholder();
    public static final TextWidthPlaceholder TEXT_WIDTH = new TextWidthPlaceholder();
    public static final ClipboardContentPlaceholder CLIPBOARD_CONTENT = new ClipboardContentPlaceholder();
    public static final WorldPlayersListPlaceholder WORLD_PLAYERS_LIST = new WorldPlayersListPlaceholder();
    public static final WorldSaveNamesPlaceholder WORLD_SAVE_NAMES = new WorldSaveNamesPlaceholder();
    public static final WorldSaveDataPlaceholder WORLD_SAVE_DATA = new WorldSaveDataPlaceholder();
    public static final FileTextPlaceholder FILE_TEXT = new FileTextPlaceholder();
    public static final FileSizePlaceholder FILE_SIZE = new FileSizePlaceholder();
    public static final FileMd5Placeholder FILE_MD5 = new FileMd5Placeholder();
    public static final WorldLoadProgressPlaceholder WORLD_LOAD_PROGRESS = new WorldLoadProgressPlaceholder();
    public static final CurrentScreenIdentifierPlaceholder CURRENT_SCREEN_IDENTIFIER = new CurrentScreenIdentifierPlaceholder();
    public static final ClicksPerSecondPlaceholder CLICKS_PER_SECOND = new ClicksPerSecondPlaceholder();
    public static final AbsolutePathPlaceholder ABSOLUTE_PATH = new AbsolutePathPlaceholder();
    public static final LastDeathMessagePlaceholder LAST_DEATH_MESSAGE = new LastDeathMessagePlaceholder();
    public static final BossCountPlaceholder BOSS_COUNT = new BossCountPlaceholder();
    public static final BossNamePlaceholder BOSS_NAME = new BossNamePlaceholder();
    public static final CurrentBossHealthPlaceholder CURRENT_BOSS_HEALTH = new CurrentBossHealthPlaceholder();
    public static final CurrentTitlePlaceholder CURRENT_TITLE = new CurrentTitlePlaceholder();
    public static final HoveredInventoryItemPlaceholder HOVERED_INVENTORY_ITEM = new HoveredInventoryItemPlaceholder();
    public static final ActionBarMessagePlaceholder ACTION_BAR_MESSAGE = new ActionBarMessagePlaceholder();
    public static final ActionBarMessageTimePlaceholder ACTION_BAR_MESSAGE_TIME = new ActionBarMessageTimePlaceholder();
    public static final CameraRotationXPlaceholder CAMERA_ROTATION_X = new CameraRotationXPlaceholder();
    public static final CameraRotationYPlaceholder CAMERA_ROTATION_Y = new CameraRotationYPlaceholder();
    public static final CameraRotationDeltaXPlaceholder CAMERA_ROTATION_DELTA_X = new CameraRotationDeltaXPlaceholder();
    public static final CameraRotationDeltaYPlaceholder CAMERA_ROTATION_DELTA_Y = new CameraRotationDeltaYPlaceholder();
    public static final HighlightedItemTimePlaceholder HIGHLIGHTED_ITEM_TIME = new HighlightedItemTimePlaceholder();
    public static final PlayerItemUseProgressPlaceholder PLAYER_ITEM_USE_PROGRESS = new PlayerItemUseProgressPlaceholder();
    public static final PlayerPositionDeltaXPlaceholder PLAYER_POSITION_DELTA_X = new PlayerPositionDeltaXPlaceholder();
    public static final PlayerPositionDeltaYPlaceholder PLAYER_POSITION_DELTA_Y = new PlayerPositionDeltaYPlaceholder();
    public static final PlayerPositionDeltaZPlaceholder PLAYER_POSITION_DELTA_Z = new PlayerPositionDeltaZPlaceholder();
    public static final SlotItemDisplayNamePlaceholder SLOT_ITEM_DISPLAY_NAME = new SlotItemDisplayNamePlaceholder();
    /** Opt-in built-in for {@code last_world_server}; registration requires a consumer-owned recent-destination provider. */
    public static final LastWorldOrServerPlaceholder LAST_WORLD_OR_SERVER = new LastWorldOrServerPlaceholder();

    private static final List<Placeholder> ALL;
    private static final List<Placeholder> RECENT_DESTINATION_BACKED = List.of(LAST_WORLD_OR_SERVER);
    private static final List<Placeholder> MANAGED;

    static {
        List<Placeholder> placeholders = new ArrayList<>();
        // Client
        placeholders.add(MINECRAFT_VERSION);
        placeholders.add(MOD_LOADER_VERSION);
        placeholders.add(MOD_LOADER_NAME);
        placeholders.add(MOD_VERSION);
        placeholders.add(LOADED_MODS);
        placeholders.add(TOTAL_MODS);
        placeholders.add(WORLD_LOAD_PROGRESS);
        placeholders.add(MINECRAFT_OPTION_VALUE);

        // GUI
        placeholders.add(SCREEN_WIDTH);
        placeholders.add(SCREEN_HEIGHT);
        placeholders.add(CURRENT_SCREEN_IDENTIFIER);
        placeholders.add(MOUSE_POS_X);
        placeholders.add(MOUSE_POS_Y);
        placeholders.add(CLICKS_PER_SECOND);
        placeholders.add(GUI_SCALE);

        // Player
        placeholders.add(PLAYER_NAME);
        placeholders.add(PLAYER_UUID);
        placeholders.add(LAST_DEATH_MESSAGE);

        // World
        placeholders.add(ACTIVE_HOTBAR_SLOT);
        placeholders.add(CURRENT_PLAYER_HEALTH);
        placeholders.add(MAX_PLAYER_HEALTH);
        placeholders.add(CURRENT_PLAYER_HEALTH_PERCENTAGE);
        placeholders.add(CURRENT_PLAYER_ABSORPTION_HEALTH);
        placeholders.add(MAX_PLAYER_ABSORPTION_HEALTH);
        placeholders.add(CURRENT_PLAYER_ABSORPTION_HEALTH_PERCENTAGE);
        placeholders.add(CURRENT_PLAYER_HUNGER);
        placeholders.add(CURRENT_PLAYER_HUNGER_SATURATION);
        placeholders.add(MAX_PLAYER_HUNGER);
        placeholders.add(CURRENT_PLAYER_HUNGER_PERCENTAGE);
        placeholders.add(CURRENT_PLAYER_ARMOR);
        placeholders.add(PLAYER_ARMOR_TOUGHNESS);
        placeholders.add(MAX_PLAYER_ARMOR);
        placeholders.add(CURRENT_PLAYER_ARMOR_PERCENTAGE);
        placeholders.add(CURRENT_PLAYER_EXP_PROGRESS);
        placeholders.add(CURRENT_PLAYER_EXPERIENCE);
        placeholders.add(CURRENT_PLAYER_LEVEL);
        placeholders.add(CURRENT_MOUNT_HEALTH);
        placeholders.add(MAX_MOUNT_HEALTH);
        placeholders.add(CURRENT_MOUNT_HEALTH_PERCENTAGE);
        placeholders.add(CURRENT_MOUNT_JUMP_METER);
        placeholders.add(GAME_TIME);
        placeholders.add(GAMERULE_VALUE);
        placeholders.add(SLOT_ITEM);
        placeholders.add(INVENTORY_ITEM_COUNT);
        placeholders.add(SLOT_ITEM_COUNT);
        placeholders.add(SLOT_ITEM_DURABILITY);
        placeholders.add(SLOT_ITEM_DISPLAY_NAME);
        placeholders.add(INVENTORY_SLOT_FOOD_POINT_RESTORE_AMOUNT);
        placeholders.add(ITEM_CATEGORY);
        placeholders.add(HOVERED_INVENTORY_ITEM);
        placeholders.add(WORLD_DAY_TIME);
        placeholders.add(WORLD_DAY_TIME_HOUR);
        placeholders.add(WORLD_DAY_TIME_MINUTE);
        placeholders.add(WORLD_DIFFICULTY);
        placeholders.add(CURRENT_BOSS_HEALTH);
        placeholders.add(BOSS_NAME);
        placeholders.add(BOSS_COUNT);
        placeholders.add(ACTIVE_EFFECTS_COUNT);
        placeholders.add(ACTIVE_EFFECT);
        placeholders.add(ACTION_BAR_MESSAGE);
        placeholders.add(ACTION_BAR_MESSAGE_TIME);
        placeholders.add(HIGHLIGHTED_ITEM_TIME);
        placeholders.add(CURRENT_TITLE);
        placeholders.add(CAMERA_ROTATION_X);
        placeholders.add(CAMERA_ROTATION_Y);
        placeholders.add(CAMERA_ROTATION_DELTA_X);
        placeholders.add(CAMERA_ROTATION_DELTA_Y);
        placeholders.add(PLAYER_X_COORDINATE);
        placeholders.add(PLAYER_Y_COORDINATE);
        placeholders.add(PLAYER_Z_COORDINATE);
        placeholders.add(PLAYER_POSITION_DELTA_X);
        placeholders.add(PLAYER_POSITION_DELTA_Y);
        placeholders.add(PLAYER_POSITION_DELTA_Z);
        placeholders.add(PLAYER_ITEM_USE_PROGRESS);
        placeholders.add(CURRENT_SERVER_IP);
        placeholders.add(CURRENT_DIMENSION);
        placeholders.add(CURRENT_BIOME);
        placeholders.add(CURRENT_WORLD_SEED);
        placeholders.add(PLAYER_ATTACK_STRENGTH_PERCENTAGE);
        placeholders.add(PLAYER_GAMEMODE);
        placeholders.add(PLAYER_VIEW_DIRECTION);
        placeholders.add(CURRENT_PLAYER_OXYGEN);
        placeholders.add(MAX_PLAYER_OXYGEN);
        placeholders.add(CURRENT_PLAYER_OXYGEN_PERCENTAGE);
        placeholders.add(WORLD_PLAYERS_LIST);
        placeholders.add(WORLD_SAVE_NAMES);
        placeholders.add(WORLD_SAVE_DATA);

        // Server
        placeholders.add(SERVER_MOTD);
        placeholders.add(SERVER_PING);
        placeholders.add(SERVER_VERSION);
        placeholders.add(SERVER_PLAYER_COUNT);
        placeholders.add(SERVER_STATUS);

        // Realtime
        placeholders.add(REALTIME_YEAR);
        placeholders.add(REALTIME_MONTH);
        placeholders.add(REALTIME_DAY);
        placeholders.add(REALTIME_HOUR);
        placeholders.add(REALTIME_MINUTE);
        placeholders.add(REALTIME_SECOND);
        placeholders.add(UNIX_TIMESTAMP);

        // Advanced
        placeholders.add(STRINGIFY);
        placeholders.add(BASE64_ENCODE);
        placeholders.add(BASE64_DECODE);
        placeholders.add(JSON);
        placeholders.add(LOCALIZATION);
        placeholders.add(CALCULATOR);
        placeholders.add(RANDOM_NUMBER);
        placeholders.add(MAX_NUMBER);
        placeholders.add(MIN_NUMBER);
        placeholders.add(ABSOLUTE_NUMBER);
        placeholders.add(NEGATE_NUMBER);
        placeholders.add(MATH_PI);
        placeholders.add(MATH_SIN);
        placeholders.add(MATH_SINH);
        placeholders.add(MATH_COS);
        placeholders.add(MATH_COSH);
        placeholders.add(MATH_TAN);
        placeholders.add(MATH_TANH);
        placeholders.add(SPLIT_TEXT);
        placeholders.add(TRIM_TEXT);
        placeholders.add(UPPERCASE_TEXT);
        placeholders.add(LOWERCASE_TEXT);
        placeholders.add(TITLE_CASE_TEXT);
        placeholders.add(SENTENCE_CASE_TEXT);
        placeholders.add(SNAKE_CASE_TEXT);
        placeholders.add(KEBAB_CASE_TEXT);
        placeholders.add(ALTERNATING_CASE_TEXT);
        placeholders.add(TOGGLE_CASE_TEXT);
        placeholders.add(CROP_TEXT);
        placeholders.add(MATH_CEIL);
        placeholders.add(MATH_FLOOR);
        placeholders.add(MATH_ROUND);
        placeholders.add(NUMBER_BASE_CONVERT);
        placeholders.add(MATH_SIGN);
        placeholders.add(SWITCH_CASE);
        placeholders.add(REPLACE_TEXT);
        placeholders.add(NBT_DATA_GET);
        placeholders.add(NBT_DATA_GET_SERVER);
        placeholders.add(FILE_TEXT);
        placeholders.add(FILE_SIZE);
        placeholders.add(FILE_MD5);

        // Other
        placeholders.add(PERCENT_RAM);
        placeholders.add(USED_RAM);
        placeholders.add(MAX_RAM);
        placeholders.add(RANDOM_TEXT);
        placeholders.add(WEB_TEXT);
        placeholders.add(ABSOLUTE_PATH);
        placeholders.add(JVM_CPU_USAGE);
        placeholders.add(OS_CPU_USAGE);
        placeholders.add(CPU_INFO);
        placeholders.add(GPU_INFO);
        placeholders.add(FPS);
        placeholders.add(JAVA_VERSION);
        placeholders.add(JVM_NAME);
        placeholders.add(OPEN_GL_VERSION);
        placeholders.add(OS_NAME);
        placeholders.add(UPTIME_DURATION);
        placeholders.add(TEXT_CHARACTER_COUNT);
        placeholders.add(TEXT_WIDTH);
        placeholders.add(CLIPBOARD_CONTENT);
        ALL = List.copyOf(placeholders);
        List<Placeholder> managed = new ArrayList<>(ALL);
        managed.addAll(RECENT_DESTINATION_BACKED);
        MANAGED = List.copyOf(managed);
    }

    private BuiltinPlaceholders() {
    }

    /** Returns every dependency-free retained built-in in default registration order. */
    public static List<Placeholder> all() {
        return ALL;
    }

    /** Returns the recent-destination built-in, which is excluded from defaults until a history provider is installed. */
    public static List<Placeholder> recentDestinationBacked() {
        return RECENT_DESTINATION_BACKED;
    }

    /** Idempotently registers every retained built-in under the {@code konkrete} namespace. */
    public static synchronized void registerAll() {
        register(ALL);
    }

    /** Registers recent world/server history after a consumer installs its own history provider. */
    public static synchronized void registerRecentDestinationBacked() {
        if (!RecentDestinationProviders.isConfigured()) throw new IllegalStateException("A RecentDestinationProvider must be configured before the recent-destination built-in is registered");
        register(RECENT_DESTINATION_BACKED);
    }

    private static void register(List<Placeholder> placeholders) {
        List<Placeholder> registeredNow = new ArrayList<>();
        try {
            for (Placeholder placeholder : placeholders) {
                Placeholder existing = PlaceholderRegistry.getPlaceholder(PlaceholderRegistry.KONKRETE_NAMESPACE + ":" + placeholder.getIdentifier());
                if (existing == placeholder) continue;
                if (existing != null) throw new IllegalStateException("[KONKRETE] Built-in identifier is owned by another instance: " + placeholder.getIdentifier());
                PlaceholderRegistry.register(placeholder);
                registeredNow.add(placeholder);
            }
        } catch (RuntimeException | Error exception) {
            for (int index = registeredNow.size() - 1; index >= 0; index--) PlaceholderRegistry.unregister(PlaceholderRegistry.KONKRETE_NAMESPACE, registeredNow.get(index).getIdentifier());
            throw exception;
        }
    }

    /** Unregisters only Konkrete’s built-in instances, leaving third-party namespaces untouched. */
    public static synchronized void unregisterAll() {
        for (int index = MANAGED.size() - 1; index >= 0; index--) {
            Placeholder placeholder = MANAGED.get(index);
            if (PlaceholderRegistry.getPlaceholder(PlaceholderRegistry.KONKRETE_NAMESPACE + ":" + placeholder.getIdentifier()) == placeholder) PlaceholderRegistry.unregister(PlaceholderRegistry.KONKRETE_NAMESPACE, placeholder.getIdentifier());
        }
    }

}
