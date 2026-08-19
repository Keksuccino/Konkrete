package de.keksuccino.konkrete.placeholder.placeholders;

import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.placeholder.PlaceholderRegistry;
import de.keksuccino.konkrete.placeholder.remote.RemotePlaceholderProviders;
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

    /** Shared built-in instance for the serialized {@code mcversion} identifier. */
    public static final MinecraftVersionPlaceholder MINECRAFT_VERSION = new MinecraftVersionPlaceholder();
    /** Shared built-in instance for the serialized {@code loaderver} identifier. */
    public static final ModLoaderVersionPlaceholder MOD_LOADER_VERSION = new ModLoaderVersionPlaceholder();
    /** Shared built-in instance for the serialized {@code loadername} identifier. */
    public static final ModLoaderNamePlaceholder MOD_LOADER_NAME = new ModLoaderNamePlaceholder();
    /** Shared built-in instance for the serialized {@code modversion} identifier. */
    public static final ModVersionPlaceholder MOD_VERSION = new ModVersionPlaceholder();
    /** Shared built-in instance for the serialized {@code loadedmods} identifier. */
    public static final LoadedModsPlaceholder LOADED_MODS = new LoadedModsPlaceholder();
    /** Shared built-in instance for the serialized {@code totalmods} identifier. */
    public static final TotalModsPlaceholder TOTAL_MODS = new TotalModsPlaceholder();
    /** Shared built-in instance for the serialized {@code minecraft_option_value} identifier. */
    public static final MinecraftOptionValuePlaceholder MINECRAFT_OPTION_VALUE = new MinecraftOptionValuePlaceholder();
    /** Shared built-in instance for the serialized {@code guiwidth} identifier. */
    public static final ScreenWidthPlaceholder SCREEN_WIDTH = new ScreenWidthPlaceholder();
    /** Shared built-in instance for the serialized {@code guiheight} identifier. */
    public static final ScreenHeightPlaceholder SCREEN_HEIGHT = new ScreenHeightPlaceholder();
    /** Shared built-in instance for the serialized {@code mouseposx} identifier. */
    public static final MousePosXPlaceholder MOUSE_POS_X = new MousePosXPlaceholder();
    /** Shared built-in instance for the serialized {@code mouseposy} identifier. */
    public static final MousePosYPlaceholder MOUSE_POS_Y = new MousePosYPlaceholder();
    /** Shared built-in instance for the serialized {@code guiscale} identifier. */
    public static final GuiScalePlaceholder GUI_SCALE = new GuiScalePlaceholder();
    /** Shared built-in instance for the serialized {@code playername} identifier. */
    public static final PlayerNamePlaceholder PLAYER_NAME = new PlayerNamePlaceholder();
    /** Shared built-in instance for the serialized {@code playeruuid} identifier. */
    public static final PlayerUuidPlaceholder PLAYER_UUID = new PlayerUuidPlaceholder();
    /** Shared built-in instance for the serialized {@code servermotd} identifier. */
    public static final ServerMotdPlaceholder SERVER_MOTD = new ServerMotdPlaceholder();
    /** Shared built-in instance for the serialized {@code serverping} identifier. */
    public static final ServerPingPlaceholder SERVER_PING = new ServerPingPlaceholder();
    /** Shared built-in instance for the serialized {@code serverversion} identifier. */
    public static final ServerVersionPlaceholder SERVER_VERSION = new ServerVersionPlaceholder();
    /** Shared built-in instance for the serialized {@code serverplayercount} identifier. */
    public static final ServerPlayerCountPlaceholder SERVER_PLAYER_COUNT = new ServerPlayerCountPlaceholder();
    /** Shared built-in instance for the serialized {@code serverstatus} identifier. */
    public static final ServerStatusPlaceholder SERVER_STATUS = new ServerStatusPlaceholder();
    /** Shared built-in instance for the serialized {@code realtimeyear} identifier. */
    public static final RealtimeYearPlaceholder REALTIME_YEAR = new RealtimeYearPlaceholder();
    /** Shared built-in instance for the serialized {@code realtimemonth} identifier. */
    public static final RealtimeMonthPlaceholder REALTIME_MONTH = new RealtimeMonthPlaceholder();
    /** Shared built-in instance for the serialized {@code realtimeday} identifier. */
    public static final RealtimeDayPlaceholder REALTIME_DAY = new RealtimeDayPlaceholder();
    /** Shared built-in instance for the serialized {@code realtimehour} identifier. */
    public static final RealtimeHourPlaceholder REALTIME_HOUR = new RealtimeHourPlaceholder();
    /** Shared built-in instance for the serialized {@code realtimeminute} identifier. */
    public static final RealtimeMinutePlaceholder REALTIME_MINUTE = new RealtimeMinutePlaceholder();
    /** Shared built-in instance for the serialized {@code realtimesecond} identifier. */
    public static final RealtimeSecondPlaceholder REALTIME_SECOND = new RealtimeSecondPlaceholder();
    /** Shared built-in instance for the serialized {@code unix_time} identifier. */
    public static final UnixTimestampPlaceholder UNIX_TIMESTAMP = new UnixTimestampPlaceholder();
    /** Shared built-in instance for the serialized {@code stringify} identifier. */
    public static final StringifyPlaceholder STRINGIFY = new StringifyPlaceholder();
    /** Shared built-in instance for the serialized {@code base64_encode} identifier. */
    public static final Base64EncodePlaceholder BASE64_ENCODE = new Base64EncodePlaceholder();
    /** Shared built-in instance for the serialized {@code base64_decode} identifier. */
    public static final Base64DecodePlaceholder BASE64_DECODE = new Base64DecodePlaceholder();
    /** Shared built-in instance for the serialized {@code json} identifier. */
    public static final JsonPlaceholder JSON = new JsonPlaceholder();
    /** Shared built-in instance for the serialized {@code nbt_data_get} identifier. */
    public static final ClientSideNbtDataGetPlaceholder NBT_DATA_GET = new ClientSideNbtDataGetPlaceholder();
    /** Opt-in built-in for {@code nbt_data_get_server}; registration requires a consumer-owned multiplayer transport. */
    public static final ServerSideNbtDataGetPlaceholder NBT_DATA_GET_SERVER = new ServerSideNbtDataGetPlaceholder();
    /** Shared built-in instance for the serialized {@code local} identifier. */
    public static final LocalizationPlaceholder LOCALIZATION = new LocalizationPlaceholder();
    /** Shared built-in instance for the serialized {@code calc} identifier. */
    public static final CalculatorPlaceholder CALCULATOR = new CalculatorPlaceholder();
    /** Shared built-in instance for the serialized {@code random_number} identifier. */
    public static final RandomNumberPlaceholder RANDOM_NUMBER = new RandomNumberPlaceholder();
    /** Shared built-in instance for the serialized {@code maxnum} identifier. */
    public static final MaxNumberPlaceholder MAX_NUMBER = new MaxNumberPlaceholder();
    /** Shared built-in instance for the serialized {@code minnum} identifier. */
    public static final MinNumberPlaceholder MIN_NUMBER = new MinNumberPlaceholder();
    /** Shared built-in instance for the serialized {@code absnum} identifier. */
    public static final AbsoluteNumberPlaceholder ABSOLUTE_NUMBER = new AbsoluteNumberPlaceholder();
    /** Shared built-in instance for the serialized {@code negnum} identifier. */
    public static final NegateNumberPlaceholder NEGATE_NUMBER = new NegateNumberPlaceholder();
    /** Shared built-in instance for the serialized {@code math_pi} identifier. */
    public static final MathPiPlaceholder MATH_PI = new MathPiPlaceholder();
    /** Shared built-in instance for the serialized {@code math_sin} identifier. */
    public static final MathSinPlaceholder MATH_SIN = new MathSinPlaceholder();
    /** Shared built-in instance for the serialized {@code math_sinh} identifier. */
    public static final MathSinhPlaceholder MATH_SINH = new MathSinhPlaceholder();
    /** Shared built-in instance for the serialized {@code math_cos} identifier. */
    public static final MathCosPlaceholder MATH_COS = new MathCosPlaceholder();
    /** Shared built-in instance for the serialized {@code math_cosh} identifier. */
    public static final MathCoshPlaceholder MATH_COSH = new MathCoshPlaceholder();
    /** Shared built-in instance for the serialized {@code math_tan} identifier. */
    public static final MathTanPlaceholder MATH_TAN = new MathTanPlaceholder();
    /** Shared built-in instance for the serialized {@code math_tanh} identifier. */
    public static final MathTanhPlaceholder MATH_TANH = new MathTanhPlaceholder();
    /** Shared built-in instance for the serialized {@code percentram} identifier. */
    public static final PercentRamPlaceholder PERCENT_RAM = new PercentRamPlaceholder();
    /** Shared built-in instance for the serialized {@code usedram} identifier. */
    public static final UsedRamPlaceholder USED_RAM = new UsedRamPlaceholder();
    /** Shared built-in instance for the serialized {@code maxram} identifier. */
    public static final MaxRamPlaceholder MAX_RAM = new MaxRamPlaceholder();
    /** Shared built-in instance for the serialized {@code jvmcpu} identifier. */
    public static final JvmCpuUsagePlaceholder JVM_CPU_USAGE = new JvmCpuUsagePlaceholder();
    /** Shared built-in instance for the serialized {@code oscpu} identifier. */
    public static final OsCpuUsagePlaceholder OS_CPU_USAGE = new OsCpuUsagePlaceholder();
    /** Shared built-in instance for the serialized {@code cpuinfo} identifier. */
    public static final CpuInfoPlaceholder CPU_INFO = new CpuInfoPlaceholder();
    /** Shared built-in instance for the serialized {@code fps} identifier. */
    public static final FpsPlaceholder FPS = new FpsPlaceholder();
    /** Shared built-in instance for the serialized {@code gpuinfo} identifier. */
    public static final GpuInfoPlaceholder GPU_INFO = new GpuInfoPlaceholder();
    /** Shared built-in instance for the serialized {@code javaver} identifier. */
    public static final JavaVersionPlaceholder JAVA_VERSION = new JavaVersionPlaceholder();
    /** Shared built-in instance for the serialized {@code jvmname} identifier. */
    public static final JvmNamePlaceholder JVM_NAME = new JvmNamePlaceholder();
    /** Shared built-in instance for the serialized {@code glver} identifier. */
    public static final OpenGLVersionPlaceholder OPEN_GL_VERSION = new OpenGLVersionPlaceholder();
    /** Shared built-in instance for the serialized {@code osname} identifier. */
    public static final OSNamePlaceholder OS_NAME = new OSNamePlaceholder();
    /** Shared built-in instance for the serialized {@code uptime_duration} identifier. */
    public static final UptimeDurationPlaceholder UPTIME_DURATION = new UptimeDurationPlaceholder();
    /** Shared built-in instance for the serialized {@code randomtext} identifier. */
    public static final RandomTextPlaceholder RANDOM_TEXT = new RandomTextPlaceholder();
    /** Shared built-in instance for the serialized {@code webtext} identifier. */
    public static final WebTextPlaceholder WEB_TEXT = new WebTextPlaceholder();
    /** Shared built-in instance for the serialized {@code active_hotbar_slot} identifier. */
    public static final ActiveHotbarSlotPlaceholder ACTIVE_HOTBAR_SLOT = new ActiveHotbarSlotPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_health} identifier. */
    public static final CurrentPlayerHealthPlaceholder CURRENT_PLAYER_HEALTH = new CurrentPlayerHealthPlaceholder();
    /** Shared built-in instance for the serialized {@code game_time} identifier. */
    public static final GameTimePlaceholder GAME_TIME = new GameTimePlaceholder();
    /** Opt-in built-in for {@code gamerule_value}; registration requires a consumer-owned multiplayer transport. */
    public static final GameruleValuePlaceholder GAMERULE_VALUE = new GameruleValuePlaceholder();
    /** Shared built-in instance for the serialized {@code slot_item} identifier. */
    public static final SlotItemPlaceholder SLOT_ITEM = new SlotItemPlaceholder();
    /** Shared built-in instance for the serialized {@code inventory_item_count} identifier. */
    public static final InventoryItemCountPlaceholder INVENTORY_ITEM_COUNT = new InventoryItemCountPlaceholder();
    /** Shared built-in instance for the serialized {@code slot_item_count} identifier. */
    public static final SlotItemCountPlaceholder SLOT_ITEM_COUNT = new SlotItemCountPlaceholder();
    /** Shared built-in instance for the serialized {@code slot_item_durability} identifier. */
    public static final SlotItemDurabilityPlaceholder SLOT_ITEM_DURABILITY = new SlotItemDurabilityPlaceholder();
    /** Shared built-in instance for the serialized {@code inventory_slot_food_point_restore_amount} identifier. */
    public static final InventorySlotFoodPointRestoreAmountPlaceholder INVENTORY_SLOT_FOOD_POINT_RESTORE_AMOUNT = new InventorySlotFoodPointRestoreAmountPlaceholder();
    /** Shared built-in instance for the serialized {@code item_category} identifier. */
    public static final ItemCategoryPlaceholder ITEM_CATEGORY = new ItemCategoryPlaceholder();
    /** Shared built-in instance for the serialized {@code world_daytime} identifier. */
    public static final WorldDayTimePlaceholder WORLD_DAY_TIME = new WorldDayTimePlaceholder();
    /** Shared built-in instance for the serialized {@code world_daytime_hour} identifier. */
    public static final WorldDayTimeHourPlaceholder WORLD_DAY_TIME_HOUR = new WorldDayTimeHourPlaceholder();
    /** Shared built-in instance for the serialized {@code world_daytime_minute} identifier. */
    public static final WorldDayTimeMinutePlaceholder WORLD_DAY_TIME_MINUTE = new WorldDayTimeMinutePlaceholder();
    /** Shared built-in instance for the serialized {@code world_difficulty} identifier. */
    public static final WorldDifficultyPlaceholder WORLD_DIFFICULTY = new WorldDifficultyPlaceholder();
    /** Shared built-in instance for the serialized {@code max_player_health} identifier. */
    public static final MaxPlayerHealthPlaceholder MAX_PLAYER_HEALTH = new MaxPlayerHealthPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_health_percent} identifier. */
    public static final CurrentPlayerHealthPercentagePlaceholder CURRENT_PLAYER_HEALTH_PERCENTAGE = new CurrentPlayerHealthPercentagePlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_absorption_health} identifier. */
    public static final CurrentPlayerAbsorptionHealthPlaceholder CURRENT_PLAYER_ABSORPTION_HEALTH = new CurrentPlayerAbsorptionHealthPlaceholder();
    /** Shared built-in instance for the serialized {@code max_player_absorption_health} identifier. */
    public static final MaxPlayerAbsorptionHealthPlaceholder MAX_PLAYER_ABSORPTION_HEALTH = new MaxPlayerAbsorptionHealthPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_absorption_health_percent} identifier. */
    public static final CurrentPlayerAbsorptionHealthPercentagePlaceholder CURRENT_PLAYER_ABSORPTION_HEALTH_PERCENTAGE = new CurrentPlayerAbsorptionHealthPercentagePlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_hunger} identifier. */
    public static final CurrentPlayerHungerPlaceholder CURRENT_PLAYER_HUNGER = new CurrentPlayerHungerPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_hunger_saturation} identifier. */
    public static final CurrentPlayerHungerSaturationPlaceholder CURRENT_PLAYER_HUNGER_SATURATION = new CurrentPlayerHungerSaturationPlaceholder();
    /** Shared built-in instance for the serialized {@code max_player_hunger} identifier. */
    public static final MaxPlayerHungerPlaceholder MAX_PLAYER_HUNGER = new MaxPlayerHungerPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_hunger_percent} identifier. */
    public static final CurrentPlayerHungerPercentagePlaceholder CURRENT_PLAYER_HUNGER_PERCENTAGE = new CurrentPlayerHungerPercentagePlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_armor} identifier. */
    public static final CurrentPlayerArmorPlaceholder CURRENT_PLAYER_ARMOR = new CurrentPlayerArmorPlaceholder();
    /** Shared built-in instance for the serialized {@code player_armor_toughness} identifier. */
    public static final PlayerArmorToughnessPlaceholder PLAYER_ARMOR_TOUGHNESS = new PlayerArmorToughnessPlaceholder();
    /** Shared built-in instance for the serialized {@code max_player_armor} identifier. */
    public static final MaxPlayerArmorPlaceholder MAX_PLAYER_ARMOR = new MaxPlayerArmorPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_armor_percent} identifier. */
    public static final CurrentPlayerArmorPercentagePlaceholder CURRENT_PLAYER_ARMOR_PERCENTAGE = new CurrentPlayerArmorPercentagePlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_exp_progress} identifier. */
    public static final CurrentPlayerExpProgressPlaceholder CURRENT_PLAYER_EXP_PROGRESS = new CurrentPlayerExpProgressPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_exp} identifier. */
    public static final CurrentPlayerExperiencePlaceholder CURRENT_PLAYER_EXPERIENCE = new CurrentPlayerExperiencePlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_level} identifier. */
    public static final CurrentPlayerLevelPlaceholder CURRENT_PLAYER_LEVEL = new CurrentPlayerLevelPlaceholder();
    /** Shared built-in instance for the serialized {@code current_mount_health} identifier. */
    public static final CurrentMountHealthPlaceholder CURRENT_MOUNT_HEALTH = new CurrentMountHealthPlaceholder();
    /** Shared built-in instance for the serialized {@code max_mount_health} identifier. */
    public static final MaxMountHealthPlaceholder MAX_MOUNT_HEALTH = new MaxMountHealthPlaceholder();
    /** Shared built-in instance for the serialized {@code current_mount_health_percent} identifier. */
    public static final CurrentMountHealthPercentagePlaceholder CURRENT_MOUNT_HEALTH_PERCENTAGE = new CurrentMountHealthPercentagePlaceholder();
    /** Shared built-in instance for the serialized {@code current_mount_jump_meter} identifier. */
    public static final CurrentMountJumpMeterPlaceholder CURRENT_MOUNT_JUMP_METER = new CurrentMountJumpMeterPlaceholder();
    /** Shared built-in instance for the serialized {@code effects_count} identifier. */
    public static final ActiveEffectsCountPlaceholder ACTIVE_EFFECTS_COUNT = new ActiveEffectsCountPlaceholder();
    /** Shared built-in instance for the serialized {@code active_effect} identifier. */
    public static final ActiveEffectPlaceholder ACTIVE_EFFECT = new ActiveEffectPlaceholder();
    /** Shared built-in instance for the serialized {@code player_x_coordinate} identifier. */
    public static final PlayerXCoordinatePlaceholder PLAYER_X_COORDINATE = new PlayerXCoordinatePlaceholder();
    /** Shared built-in instance for the serialized {@code player_y_coordinate} identifier. */
    public static final PlayerYCoordinatePlaceholder PLAYER_Y_COORDINATE = new PlayerYCoordinatePlaceholder();
    /** Shared built-in instance for the serialized {@code player_z_coordinate} identifier. */
    public static final PlayerZCoordinatePlaceholder PLAYER_Z_COORDINATE = new PlayerZCoordinatePlaceholder();
    /** Shared built-in instance for the serialized {@code current_server_ip} identifier. */
    public static final CurrentServerIpPlaceholder CURRENT_SERVER_IP = new CurrentServerIpPlaceholder();
    /** Shared built-in instance for the serialized {@code current_dimension} identifier. */
    public static final CurrentDimensionPlaceholder CURRENT_DIMENSION = new CurrentDimensionPlaceholder();
    /** Shared built-in instance for the serialized {@code current_biome} identifier. */
    public static final CurrentBiomePlaceholder CURRENT_BIOME = new CurrentBiomePlaceholder();
    /** Shared built-in instance for the serialized {@code current_world_seed} identifier. */
    public static final CurrentWorldSeedPlaceholder CURRENT_WORLD_SEED = new CurrentWorldSeedPlaceholder();
    /** Shared built-in instance for the serialized {@code player_attack_strength} identifier. */
    public static final PlayerAttackStrengthPercentagePlaceholder PLAYER_ATTACK_STRENGTH_PERCENTAGE = new PlayerAttackStrengthPercentagePlaceholder();
    /** Shared built-in instance for the serialized {@code player_gamemode} identifier. */
    public static final PlayerGamemodePlaceholder PLAYER_GAMEMODE = new PlayerGamemodePlaceholder();
    /** Shared built-in instance for the serialized {@code player_view_direction} identifier. */
    public static final PlayerViewDirectionPlaceholder PLAYER_VIEW_DIRECTION = new PlayerViewDirectionPlaceholder();
    /** Shared built-in instance for the serialized {@code split_text} identifier. */
    public static final SplitTextPlaceholder SPLIT_TEXT = new SplitTextPlaceholder();
    /** Shared built-in instance for the serialized {@code trim_text} identifier. */
    public static final TrimTextPlaceholder TRIM_TEXT = new TrimTextPlaceholder();
    /** Shared built-in instance for the serialized {@code uppercase_text} identifier. */
    public static final UppercaseTextPlaceholder UPPERCASE_TEXT = new UppercaseTextPlaceholder();
    /** Shared built-in instance for the serialized {@code lowercase_text} identifier. */
    public static final LowercaseTextPlaceholder LOWERCASE_TEXT = new LowercaseTextPlaceholder();
    /** Shared built-in instance for the serialized {@code title_case_text} identifier. */
    public static final TitleCaseTextPlaceholder TITLE_CASE_TEXT = new TitleCaseTextPlaceholder();
    /** Shared built-in instance for the serialized {@code sentence_case_text} identifier. */
    public static final SentenceCaseTextPlaceholder SENTENCE_CASE_TEXT = new SentenceCaseTextPlaceholder();
    /** Shared built-in instance for the serialized {@code snake_case_text} identifier. */
    public static final SnakeCaseTextPlaceholder SNAKE_CASE_TEXT = new SnakeCaseTextPlaceholder();
    /** Shared built-in instance for the serialized {@code kebab_case_text} identifier. */
    public static final KebabCaseTextPlaceholder KEBAB_CASE_TEXT = new KebabCaseTextPlaceholder();
    /** Shared built-in instance for the serialized {@code alternating_case_text} identifier. */
    public static final AlternatingCaseTextPlaceholder ALTERNATING_CASE_TEXT = new AlternatingCaseTextPlaceholder();
    /** Shared built-in instance for the serialized {@code toggle_case_text} identifier. */
    public static final ToggleCaseTextPlaceholder TOGGLE_CASE_TEXT = new ToggleCaseTextPlaceholder();
    /** Shared built-in instance for the serialized {@code crop_text} identifier. */
    public static final CropTextPlaceholder CROP_TEXT = new CropTextPlaceholder();
    /** Shared built-in instance for the serialized {@code math_ceil} identifier. */
    public static final MathCeilPlaceholder MATH_CEIL = new MathCeilPlaceholder();
    /** Shared built-in instance for the serialized {@code math_floor} identifier. */
    public static final MathFloorPlaceholder MATH_FLOOR = new MathFloorPlaceholder();
    /** Shared built-in instance for the serialized {@code math_round} identifier. */
    public static final MathRoundPlaceholder MATH_ROUND = new MathRoundPlaceholder();
    /** Shared built-in instance for the serialized {@code number_base_convert} identifier. */
    public static final NumberBaseConvertPlaceholder NUMBER_BASE_CONVERT = new NumberBaseConvertPlaceholder();
    /** Shared built-in instance for the serialized {@code math_sign} identifier. */
    public static final MathSignPlaceholder MATH_SIGN = new MathSignPlaceholder();
    /** Shared built-in instance for the serialized {@code switch_case} identifier. */
    public static final SwitchCasePlaceholder SWITCH_CASE = new SwitchCasePlaceholder();
    /** Shared built-in instance for the serialized {@code replace_text} identifier. */
    public static final ReplaceTextPlaceholder REPLACE_TEXT = new ReplaceTextPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_oxygen} identifier. */
    public static final CurrentPlayerOxygenPlaceholder CURRENT_PLAYER_OXYGEN = new CurrentPlayerOxygenPlaceholder();
    /** Shared built-in instance for the serialized {@code max_player_oxygen} identifier. */
    public static final MaxPlayerOxygenPlaceholder MAX_PLAYER_OXYGEN = new MaxPlayerOxygenPlaceholder();
    /** Shared built-in instance for the serialized {@code current_player_oxygen_percent} identifier. */
    public static final CurrentPlayerOxygenPercentagePlaceholder CURRENT_PLAYER_OXYGEN_PERCENTAGE = new CurrentPlayerOxygenPercentagePlaceholder();
    /** Shared built-in instance for the serialized {@code text_character_count} identifier. */
    public static final TextCharacterCountPlaceholder TEXT_CHARACTER_COUNT = new TextCharacterCountPlaceholder();
    /** Shared built-in instance for the serialized {@code text_width} identifier. */
    public static final TextWidthPlaceholder TEXT_WIDTH = new TextWidthPlaceholder();
    /** Shared built-in instance for the serialized {@code clipboard_content} identifier. */
    public static final ClipboardContentPlaceholder CLIPBOARD_CONTENT = new ClipboardContentPlaceholder();
    /** Shared built-in instance for the serialized {@code world_players_list} identifier. */
    public static final WorldPlayersListPlaceholder WORLD_PLAYERS_LIST = new WorldPlayersListPlaceholder();
    /** Shared built-in instance for the serialized {@code level_save_names} identifier. */
    public static final WorldSaveNamesPlaceholder WORLD_SAVE_NAMES = new WorldSaveNamesPlaceholder();
    /** Shared built-in instance for the serialized {@code level_save_data} identifier. */
    public static final WorldSaveDataPlaceholder WORLD_SAVE_DATA = new WorldSaveDataPlaceholder();
    /** Shared built-in instance for the serialized {@code file_text} identifier. */
    public static final FileTextPlaceholder FILE_TEXT = new FileTextPlaceholder();
    /** Shared built-in instance for the serialized {@code file_size} identifier. */
    public static final FileSizePlaceholder FILE_SIZE = new FileSizePlaceholder();
    /** Shared built-in instance for the serialized {@code file_md5} identifier. */
    public static final FileMd5Placeholder FILE_MD5 = new FileMd5Placeholder();
    /** Shared built-in instance for the serialized {@code world_load_progress} identifier. */
    public static final WorldLoadProgressPlaceholder WORLD_LOAD_PROGRESS = new WorldLoadProgressPlaceholder();
    /** Shared built-in instance for the serialized {@code screenid} identifier. */
    public static final CurrentScreenIdentifierPlaceholder CURRENT_SCREEN_IDENTIFIER = new CurrentScreenIdentifierPlaceholder();
    /** Shared built-in instance for the serialized {@code clicks_per_second} identifier. */
    public static final ClicksPerSecondPlaceholder CLICKS_PER_SECOND = new ClicksPerSecondPlaceholder();
    /** Shared built-in instance for the serialized {@code absolute_path} identifier. */
    public static final AbsolutePathPlaceholder ABSOLUTE_PATH = new AbsolutePathPlaceholder();
    /** Shared built-in instance for the serialized {@code lastdeathmessage} identifier. */
    public static final LastDeathMessagePlaceholder LAST_DEATH_MESSAGE = new LastDeathMessagePlaceholder();
    /** Shared built-in instance for the serialized {@code boss_count} identifier. */
    public static final BossCountPlaceholder BOSS_COUNT = new BossCountPlaceholder();
    /** Shared built-in instance for the serialized {@code boss_name} identifier. */
    public static final BossNamePlaceholder BOSS_NAME = new BossNamePlaceholder();
    /** Shared built-in instance for the serialized {@code current_boss_health} identifier. */
    public static final CurrentBossHealthPlaceholder CURRENT_BOSS_HEALTH = new CurrentBossHealthPlaceholder();
    /** Shared built-in instance for the serialized {@code current_title} identifier. */
    public static final CurrentTitlePlaceholder CURRENT_TITLE = new CurrentTitlePlaceholder();
    /** Shared built-in instance for the serialized {@code hovered_inventory_item} identifier. */
    public static final HoveredInventoryItemPlaceholder HOVERED_INVENTORY_ITEM = new HoveredInventoryItemPlaceholder();
    /** Shared built-in instance for the serialized {@code action_bar_message} identifier. */
    public static final ActionBarMessagePlaceholder ACTION_BAR_MESSAGE = new ActionBarMessagePlaceholder();
    /** Shared built-in instance for the serialized {@code action_bar_message_time} identifier. */
    public static final ActionBarMessageTimePlaceholder ACTION_BAR_MESSAGE_TIME = new ActionBarMessageTimePlaceholder();
    /** Shared built-in instance for the serialized {@code camera_rotation_x} identifier. */
    public static final CameraRotationXPlaceholder CAMERA_ROTATION_X = new CameraRotationXPlaceholder();
    /** Shared built-in instance for the serialized {@code camera_rotation_y} identifier. */
    public static final CameraRotationYPlaceholder CAMERA_ROTATION_Y = new CameraRotationYPlaceholder();
    /** Shared built-in instance for the serialized {@code camera_rotation_delta_x} identifier. */
    public static final CameraRotationDeltaXPlaceholder CAMERA_ROTATION_DELTA_X = new CameraRotationDeltaXPlaceholder();
    /** Shared built-in instance for the serialized {@code camera_rotation_delta_y} identifier. */
    public static final CameraRotationDeltaYPlaceholder CAMERA_ROTATION_DELTA_Y = new CameraRotationDeltaYPlaceholder();
    /** Shared built-in instance for the serialized {@code highlighted_item_time} identifier. */
    public static final HighlightedItemTimePlaceholder HIGHLIGHTED_ITEM_TIME = new HighlightedItemTimePlaceholder();
    /** Shared built-in instance for the serialized {@code player_item_use_progress} identifier. */
    public static final PlayerItemUseProgressPlaceholder PLAYER_ITEM_USE_PROGRESS = new PlayerItemUseProgressPlaceholder();
    /** Shared built-in instance for the serialized {@code player_position_delta_x} identifier. */
    public static final PlayerPositionDeltaXPlaceholder PLAYER_POSITION_DELTA_X = new PlayerPositionDeltaXPlaceholder();
    /** Shared built-in instance for the serialized {@code player_position_delta_y} identifier. */
    public static final PlayerPositionDeltaYPlaceholder PLAYER_POSITION_DELTA_Y = new PlayerPositionDeltaYPlaceholder();
    /** Shared built-in instance for the serialized {@code player_position_delta_z} identifier. */
    public static final PlayerPositionDeltaZPlaceholder PLAYER_POSITION_DELTA_Z = new PlayerPositionDeltaZPlaceholder();
    /** Shared built-in instance for the serialized {@code slot_item_display_name} identifier. */
    public static final SlotItemDisplayNamePlaceholder SLOT_ITEM_DISPLAY_NAME = new SlotItemDisplayNamePlaceholder();
    /** Opt-in built-in for {@code last_world_server}; registration requires a consumer-owned recent-destination provider. */
    public static final LastWorldOrServerPlaceholder LAST_WORLD_OR_SERVER = new LastWorldOrServerPlaceholder();

    private static final List<Placeholder> ALL;
    private static final List<Placeholder> REMOTE_BACKED = List.of(NBT_DATA_GET_SERVER, GAMERULE_VALUE);
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
        managed.addAll(REMOTE_BACKED);
        managed.addAll(RECENT_DESTINATION_BACKED);
        MANAGED = List.copyOf(managed);
    }

    private BuiltinPlaceholders() {
    }

    /** Returns every dependency-free retained built-in in default registration order. */
    public static List<Placeholder> all() {
        return ALL;
    }

    /** Returns built-ins that require a consumer-owned multiplayer transport and are excluded from {@link #registerAll()}. */
    public static List<Placeholder> remoteBacked() {
        return REMOTE_BACKED;
    }

    /** Returns the recent-destination built-in, which is excluded from defaults until a history provider is installed. */
    public static List<Placeholder> recentDestinationBacked() {
        return RECENT_DESTINATION_BACKED;
    }

    /** Idempotently registers every retained built-in under the {@code konkrete} namespace. */
    public static synchronized void registerAll() {
        register(ALL);
    }

    /** Registers server-NBT and multiplayer-gamerule placeholders after a consumer installs its own transport. */
    public static synchronized void registerRemoteBacked() {
        if (!RemotePlaceholderProviders.isConfigured()) throw new IllegalStateException("A RemotePlaceholderProvider must be configured before remote-backed built-ins are registered");
        register(REMOTE_BACKED);
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
