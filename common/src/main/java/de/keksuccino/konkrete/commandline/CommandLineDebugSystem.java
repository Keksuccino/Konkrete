package de.keksuccino.konkrete.commandline;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.konkrete.mixin.mixins.common.client.IMixinKeyboardHandler;
import de.keksuccino.konkrete.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.konkrete.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CommandLineDebugSystem {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final String BASE_COMMAND = "konkretedebug";
    private static final String DEBUG_WORLD_ID = "konkrete_debug_world";
    private static final long WORLD_LOAD_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(3L);

    @Nullable
    private static volatile WorldLoadRequest activeWorldLoadRequest;

    private CommandLineDebugSystem() {
    }

    public static void start() {
        if (!Services.PLATFORM.isDevelopmentEnvironment()) {
            return;
        }

        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(CommandLineDebugSystem::runConsoleLoop, "KonkreteCommandLineDebug");
        thread.setDaemon(true);
        thread.start();

        printLine(false, "Command line debugging is ready. Use 'konkretedebug help'.");
    }

    public static void onClientTick() {
        if (!Services.PLATFORM.isDevelopmentEnvironment()) {
            return;
        }

        WorldLoadRequest request = activeWorldLoadRequest;
        if (request != null) {
            request.tick(Minecraft.getInstance());
        }
    }

    private static void runConsoleLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                handleConsoleLine(line);
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Command line debug console stopped unexpectedly.", ex);
            printResponse(CommandResponse.failure("The command line debug console stopped unexpectedly: " + formatThrowable(ex)));
        }
    }

    private static void handleConsoleLine(final String rawLine) {
        if (!Services.PLATFORM.isDevelopmentEnvironment()) {
            return;
        }

        String line = rawLine.trim();
        if (line.isEmpty()) {
            return;
        }

        List<String> tokens;
        try {
            tokens = tokenize(line);
        } catch (IllegalArgumentException ex) {
            printResponse(CommandResponse.failure(ex.getMessage()));
            return;
        }

        if (tokens.isEmpty() || !BASE_COMMAND.equalsIgnoreCase(tokens.get(0))) {
            return;
        }

        try {
            printResponse(execute(tokens));
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to execute command line debug command '{}'.", line, ex);
            printResponse(CommandResponse.failure("The command failed: " + formatThrowable(ex)));
        }
    }

    private static CommandResponse execute(final List<String> tokens) throws Exception {
        String subCommand = tokens.size() > 1 ? tokens.get(1).toLowerCase(Locale.ROOT) : "help";

        return switch (subCommand) {
            case "help" -> createHelpResponse();
            case "screen" -> runOnGameThread(CommandLineDebugSystem::describeCurrentScreen);
            case "widgets" -> runOnGameThread(CommandLineDebugSystem::describeWidgets);
            case "widgetclick" -> {
                ensureMinimumArgumentCount(tokens, 4);
                double x = parseDouble(tokens.get(2), "x");
                double y = parseDouble(tokens.get(3), "y");
                yield runOnGameThread(() -> invokeWidgetClick(x, y));
            }
            case "click" -> {
                ensureMinimumArgumentCount(tokens, 4);
                double x = parseDouble(tokens.get(2), "x");
                double y = parseDouble(tokens.get(3), "y");
                int button = tokens.size() > 4 ? parseMouseButton(tokens.get(4)) : InputConstants.MOUSE_BUTTON_LEFT;
                yield runOnGameThread(() -> invokeMouseClick(x, y, button));
            }
            case "key" -> {
                ensureMinimumArgumentCount(tokens, 3);
                RequestedKey requestedKey = parseRequestedKey(tokens.get(2));
                yield runOnGameThread(() -> invokeKeyPress(requestedKey));
            }
            case "loadworld" -> awaitResponse(runOnGameThread(CommandLineDebugSystem::startDebugWorldLoad));
            case "screenshot" -> {
                ensureMinimumArgumentCount(tokens, 3);
                yield awaitResponse(runOnGameThread(() -> beginScreenshot(joinArguments(tokens, 2))));
            }
            case "scroll" -> {
                ensureMinimumArgumentCount(tokens, 5);
                double x = parseDouble(tokens.get(2), "x");
                double y = parseDouble(tokens.get(3), "y");
                double amount = parseScrollAmount(tokens);
                yield runOnGameThread(() -> invokeScroll(x, y, amount));
            }
            case "fullscreen" -> {
                String mode = tokens.size() > 2 ? tokens.get(2) : "toggle";
                yield runOnGameThread(() -> setFullscreen(mode));
            }
            case "chat" -> {
                ensureMinimumArgumentCount(tokens, 3);
                yield runOnGameThread(() -> sendChatMessage(joinArguments(tokens, 2)));
            }
            case "command" -> {
                ensureMinimumArgumentCount(tokens, 3);
                yield runOnGameThread(() -> sendCommand(joinArguments(tokens, 2)));
            }
            default -> CommandResponse.failure("Unknown subcommand '" + subCommand + "'. Use 'konkretedebug help'.");
        };
    }

    private static CommandResponse createHelpResponse() {
        return CommandResponse.success(List.of(
            "Konkrete command line debug commands:",
            BASE_COMMAND + " help - Lists every debug command and shows how to use it.",
            BASE_COMMAND + " screen - Prints the full class name of the current screen, or null if no screen is open.",
            BASE_COMMAND + " widgets - Lists targetable widgets from the current screen renderable list with bounds, labels, and class names.",
            BASE_COMMAND + " widgetclick <x> <y> - Invokes a widget's onClick flow by targeting a point inside its bounds.",
            BASE_COMMAND + " click <x> <y> [left|right] - Sends a normal mouse click to the current screen at GUI coordinates.",
            BASE_COMMAND + " key <key> - Simulates a key press. Supports inputs like enter, f11, a, and ctrl+tab.",
            BASE_COMMAND + " loadworld - Loads the singleplayer debug world '" + DEBUG_WORLD_ID + "' and waits for it to finish loading.",
            BASE_COMMAND + " screenshot <path> - Takes a screenshot and saves it to the provided path.",
            BASE_COMMAND + " scroll <x> <y> <up|down> [amount] - Scrolls at GUI coordinates in the current screen.",
            BASE_COMMAND + " fullscreen [on|off|toggle] - Turns fullscreen on, off, or toggles it.",
            BASE_COMMAND + " chat <message> - Sends a chat message while in a world.",
            BASE_COMMAND + " command <command> - Sends a command while in a world. A leading '/' is optional.",
            "Use double quotes around paths or messages with spaces."
        ));
    }

    private static CommandResponse describeCurrentScreen() {
        Screen screen = Minecraft.getInstance().screen;
        return CommandResponse.success(screen == null ? "null" : screen.getClass().getName());
    }

    private static CommandResponse describeWidgets() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return CommandResponse.failure("There is no open screen right now.");
        }

        List<String> lines = new ArrayList<>();
        lines.add("Targetable widgets for screen " + screen.getClass().getName() + ":");

        int widgetCount = 0;
        for (Renderable renderable : ((IMixinScreen) screen).get_renderables_Konkrete()) {
            WidgetSnapshot snapshot = WidgetSnapshot.create(widgetCount, renderable);
            if (snapshot != null) {
                lines.add(snapshot.describe());
                widgetCount++;
            }
        }

        if (widgetCount == 0) {
            lines.add("No targetable widgets are currently registered in the screen renderable list.");
        }

        return CommandResponse.success(lines);
    }

    private static CommandResponse invokeWidgetClick(final double x, final double y) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return CommandResponse.failure("There is no open screen right now.");
        }

        AbstractWidget widget = findWidgetAt(screen, x, y);
        if (widget == null) {
            return CommandResponse.failure("No widget contains the coordinates " + formatPoint(x, y) + ".");
        }

        MouseButtonEvent event = new MouseButtonEvent(
            x,
            y,
            new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)
        );

        screen.afterMouseAction();
        if (widget.shouldTakeFocusAfterInteraction()) {
            screen.setFocused(widget);
        }
        widget.onClick(event, false);

        return CommandResponse.success(
            "Invoked widget onClick on " + widget.getClass().getName() + " at " + formatPoint(x, y) + "."
        );
    }

    private static CommandResponse invokeMouseClick(final double x, final double y, final int button) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return CommandResponse.failure("There is no open screen right now.");
        }

        MouseButtonEvent event = new MouseButtonEvent(
            x,
            y,
            new MouseButtonInfo(button, 0)
        );

        screen.afterMouseAction();
        boolean handled = screen.mouseClicked(event, false);
        handled |= screen.mouseReleased(event);

        return CommandResponse.success(
            "Sent a " + buttonName(button) + " mouse click at " + formatPoint(x, y) + ". Handled=" + handled + "."
        );
    }

    private static CommandResponse invokeScroll(final double x, final double y, final double amount) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return CommandResponse.failure("There is no open screen right now.");
        }

        screen.afterMouseAction();
        boolean handled = screen.mouseScrolled(x, y, 0.0D, amount);

        return CommandResponse.success(
            "Scrolled the current screen at " + formatPoint(x, y) + " by " + formatDecimal(amount) + ". Handled=" + handled + "."
        );
    }

    private static CommandResponse setFullscreen(final String mode) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean fullscreen = minecraft.getWindow().isFullscreen();
        boolean target = switch (mode.toLowerCase(Locale.ROOT)) {
            case "toggle" -> !fullscreen;
            case "on", "true" -> true;
            case "off", "false" -> false;
            default -> throw new IllegalArgumentException("Invalid fullscreen mode '" + mode + "'. Use on, off, or toggle.");
        };

        minecraft.options.fullscreen().set(target);
        minecraft.options.save();

        return CommandResponse.success("Fullscreen is now " + minecraft.getWindow().isFullscreen() + ".");
    }

    private static CommandResponse sendChatMessage(final String message) {
        if (message.isBlank()) {
            return CommandResponse.failure("The chat message cannot be empty.");
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return CommandResponse.failure("This command only works while the client is inside a world.");
        }

        minecraft.player.connection.sendChat(message);
        return CommandResponse.success("Sent the chat message.");
    }

    private static CommandResponse sendCommand(final String rawCommand) {
        String command = rawCommand.trim();
        while (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (command.isBlank()) {
            return CommandResponse.failure("The command cannot be empty.");
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return CommandResponse.failure("This command only works while the client is inside a world.");
        }

        minecraft.player.connection.sendCommand(command);
        return CommandResponse.success("Sent the command.");
    }

    private static CompletableFuture<CommandResponse> beginScreenshot(final String rawPath) {
        if (rawPath.isBlank()) {
            return CompletableFuture.completedFuture(CommandResponse.failure("The screenshot path cannot be empty."));
        }

        Minecraft minecraft = Minecraft.getInstance();
        Path outputPath = resolveScreenshotPath(minecraft, rawPath);
        CompletableFuture<CommandResponse> future = new CompletableFuture<>();

        try {
            Screenshot.takeScreenshot(
                minecraft.getMainRenderTarget(),
                image -> Util.ioPool().execute(() -> saveScreenshot(outputPath, image, future))
            );
        } catch (Throwable throwable) {
            future.complete(CommandResponse.failure("Failed to save the screenshot: " + formatThrowable(throwable)));
        }

        return future;
    }

    private static RequestedKey parseRequestedKey(final String rawKey) {
        String[] parts = rawKey.toLowerCase(Locale.ROOT).split("\\+");
        int modifiers = 0;
        int keyCode = Integer.MIN_VALUE;
        int scanCode = 0;
        @Nullable Integer typedCodepoint = null;

        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                throw new IllegalArgumentException("Invalid key specification '" + rawKey + "'.");
            }

            if (parts.length > 1) {
                @Nullable Integer modifierMask = modifierMask(part);
                if (modifierMask != null) {
                    modifiers |= modifierMask;
                    continue;
                }
            }

            if (keyCode != Integer.MIN_VALUE) {
                throw new IllegalArgumentException("Invalid key specification '" + rawKey + "'.");
            }

            KeyDefinition keyDefinition = parseKeyDefinition(part, rawKey);
            keyCode = keyDefinition.keyCode();
            scanCode = keyDefinition.scanCode();
            if (parts.length == 1) {
                typedCodepoint = keyDefinition.typedCodepoint();
            }
        }

        if (keyCode == Integer.MIN_VALUE) {
            KeyDefinition keyDefinition = parseKeyDefinition(parts[parts.length - 1].trim(), rawKey);
            keyCode = keyDefinition.keyCode();
            scanCode = keyDefinition.scanCode();
            typedCodepoint = keyDefinition.typedCodepoint();
        }

        return new RequestedKey(rawKey, keyCode, scanCode, modifiers, typedCodepoint);
    }

    private static CommandResponse invokeKeyPress(final RequestedKey requestedKey) {
        Minecraft minecraft = Minecraft.getInstance();
        IMixinKeyboardHandler keyboardHandler = (IMixinKeyboardHandler) minecraft.keyboardHandler;
        long windowHandle = minecraft.getWindow().handle();
        KeyEvent event = new KeyEvent(requestedKey.keyCode(), requestedKey.scanCode(), requestedKey.modifiers());

        keyboardHandler.invoke_keyPress_Konkrete(windowHandle, InputConstants.PRESS, event);
        if (requestedKey.typedCodepoint() != null) {
            keyboardHandler.invoke_charTyped_Konkrete(windowHandle, new CharacterEvent(requestedKey.typedCodepoint()));
        }
        keyboardHandler.invoke_keyPress_Konkrete(windowHandle, InputConstants.RELEASE, event);

        return CommandResponse.success("Pressed key '" + requestedKey.raw() + "'.");
    }

    private static CompletableFuture<CommandResponse> startDebugWorldLoad() {
        WorldLoadRequest currentRequest = activeWorldLoadRequest;
        if (currentRequest != null && !currentRequest.future().isDone()) {
            return CompletableFuture.completedFuture(CommandResponse.failure("A debug world load is already running."));
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean worldExists = minecraft.getLevelSource().levelExists(DEBUG_WORLD_ID);
        WorldLoadRequest request = new WorldLoadRequest(DEBUG_WORLD_ID, !worldExists);
        activeWorldLoadRequest = request;

        try {
            if (minecraft.level != null || minecraft.getConnection() != null) {
                minecraft.disconnectFromWorld(Component.literal("Konkrete debug quick load"));
            }

            if (worldExists) {
                printLine(false, "Loading existing debug world '" + DEBUG_WORLD_ID + "'...");
                minecraft.createWorldOpenFlows().openWorld(DEBUG_WORLD_ID, () -> request.fail("World loading was cancelled before the client joined the world."));
            } else {
                printLine(false, "Creating and loading debug world '" + DEBUG_WORLD_ID + "'...");
                Screen parentScreen = minecraft.screen;
                LevelSettings levelSettings = new LevelSettings(
                    DEBUG_WORLD_ID,
                    GameType.CREATIVE,
                    LevelSettings.DifficultySettings.DEFAULT,
                    true,
                    WorldDataConfiguration.DEFAULT
                );
                minecraft
                    .createWorldOpenFlows()
                    .createFreshLevel(DEBUG_WORLD_ID, levelSettings, WorldOptions.testWorldWithRandomSeed(), WorldPresets::createFlatWorldDimensions, parentScreen);
            }
        } catch (Throwable throwable) {
            request.fail("Failed to start loading the debug world: " + formatThrowable(throwable));
        }

        return request.future();
    }

    @Nullable
    private static AbstractWidget findWidgetAt(final Screen screen, final double x, final double y) {
        int targetX = (int) Math.floor(x);
        int targetY = (int) Math.floor(y);

        for (Renderable renderable : ((IMixinScreen) screen).get_renderables_Konkrete()) {
            if (renderable instanceof AbstractWidget widget && widget.getRectangle().containsPoint(targetX, targetY)) {
                return widget;
            }
        }

        return null;
    }

    private static List<String> tokenize(final String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (Character.isWhitespace(character) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(character);
        }

        if (inQuotes) {
            throw new IllegalArgumentException("The command line contains an unclosed double quote.");
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private static void ensureMinimumArgumentCount(final List<String> tokens, final int minimumCount) {
        if (tokens.size() < minimumCount) {
            throw new IllegalArgumentException("Not enough arguments. Use 'konkretedebug help'.");
        }
    }

    private static String joinArguments(final List<String> tokens, final int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < tokens.size(); index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(tokens.get(index));
        }
        return builder.toString();
    }

    private static double parseDouble(final String rawValue, final String argumentName) {
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + argumentName + " value: '" + rawValue + "'.");
        }
    }

    private static int parseMouseButton(final String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "left", "0" -> InputConstants.MOUSE_BUTTON_LEFT;
            case "right", "1" -> InputConstants.MOUSE_BUTTON_RIGHT;
            default -> throw new IllegalArgumentException("Invalid mouse button '" + value + "'. Use left or right.");
        };
    }

    private static double parseScrollAmount(final List<String> tokens) {
        String direction = tokens.get(4).toLowerCase(Locale.ROOT);
        double amount = tokens.size() > 5 ? parseDouble(tokens.get(5), "amount") : 1.0D;

        return switch (direction) {
            case "up" -> Math.abs(amount);
            case "down" -> -Math.abs(amount);
            default -> throw new IllegalArgumentException("Invalid scroll direction '" + tokens.get(4) + "'. Use up or down.");
        };
    }

    private static int parseInteger(final String rawValue, final String argumentName) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + argumentName + " value: '" + rawValue + "'.");
        }
    }

    private static void saveScreenshot(final Path outputPath, final NativeImage image, final CompletableFuture<CommandResponse> future) {
        try (NativeImage nativeImage = image) {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            nativeImage.writeToFile(outputPath.toFile());
            future.complete(CommandResponse.success("Saved screenshot to " + outputPath.toAbsolutePath().normalize() + "."));
        } catch (Exception ex) {
            future.complete(CommandResponse.failure("Failed to save the screenshot: " + formatThrowable(ex)));
        }
    }

    private static Path resolveScreenshotPath(final Minecraft minecraft, final String rawPath) {
        Path path = Paths.get(rawPath);
        if (!path.isAbsolute()) {
            path = minecraft.gameDirectory.toPath().resolve(path);
        }

        path = path.normalize();

        if (Files.isDirectory(path)) {
            return path.resolve("konkrete_debug_" + Util.getFilenameFormattedDateTime() + ".png");
        }

        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        if (!fileName.contains(".")) {
            return path.resolveSibling(fileName + ".png");
        }

        return path;
    }

    private static KeyDefinition parseKeyDefinition(final String keyToken, final String rawKey) {
        if (keyToken.startsWith("code:")) {
            int keyCode = parseInteger(keyToken.substring("code:".length()), "keyCode");
            return new KeyDefinition(keyCode, GLFW.glfwGetKeyScancode(keyCode), null);
        }

        if (keyToken.startsWith("key.keyboard.") || keyToken.startsWith("scancode.")) {
            InputConstants.Key inputKey = InputConstants.getKey(keyToken);
            if (inputKey.getType() == InputConstants.Type.KEYSYM) {
                return new KeyDefinition(inputKey.getValue(), GLFW.glfwGetKeyScancode(inputKey.getValue()), null);
            }
            if (inputKey.getType() == InputConstants.Type.SCANCODE) {
                return new KeyDefinition(-1, inputKey.getValue(), null);
            }
            throw new IllegalArgumentException("Invalid key specification '" + rawKey + "'.");
        }

        if (keyToken.length() == 1) {
            return parseSingleCharacterKey(keyToken.charAt(0), rawKey);
        }

        if (keyToken.matches("f([1-9]|1[0-9]|2[0-5])")) {
            int functionIndex = Integer.parseInt(keyToken.substring(1));
            int keyCode = InputConstants.KEY_F1 + (functionIndex - 1);
            return new KeyDefinition(keyCode, GLFW.glfwGetKeyScancode(keyCode), null);
        }

        if (keyToken.matches("numpad[0-9]")) {
            int number = keyToken.charAt(keyToken.length() - 1) - '0';
            int keyCode = InputConstants.KEY_NUMPAD0 + number;
            return new KeyDefinition(keyCode, GLFW.glfwGetKeyScancode(keyCode), (int) ('0' + number));
        }

        return switch (keyToken) {
            case "enter", "return" -> keyDefinition(InputConstants.KEY_RETURN, null);
            case "escape", "esc" -> keyDefinition(InputConstants.KEY_ESCAPE, null);
            case "space" -> keyDefinition(InputConstants.KEY_SPACE, (int) ' ');
            case "tab" -> keyDefinition(InputConstants.KEY_TAB, null);
            case "backspace" -> keyDefinition(InputConstants.KEY_BACKSPACE, null);
            case "delete", "del" -> keyDefinition(InputConstants.KEY_DELETE, null);
            case "insert", "ins" -> keyDefinition(InputConstants.KEY_INSERT, null);
            case "home" -> keyDefinition(InputConstants.KEY_HOME, null);
            case "end" -> keyDefinition(InputConstants.KEY_END, null);
            case "pageup", "pgup" -> keyDefinition(InputConstants.KEY_PAGEUP, null);
            case "pagedown", "pgdn" -> keyDefinition(InputConstants.KEY_PAGEDOWN, null);
            case "up" -> keyDefinition(InputConstants.KEY_UP, null);
            case "down" -> keyDefinition(InputConstants.KEY_DOWN, null);
            case "left" -> keyDefinition(InputConstants.KEY_LEFT, null);
            case "right" -> keyDefinition(InputConstants.KEY_RIGHT, null);
            case "minus" -> keyDefinition(InputConstants.KEY_MINUS, (int) '-');
            case "equals", "equal" -> keyDefinition(InputConstants.KEY_EQUALS, (int) '=');
            case "comma" -> keyDefinition(InputConstants.KEY_COMMA, (int) ',');
            case "period", "dot" -> keyDefinition(InputConstants.KEY_PERIOD, (int) '.');
            case "slash" -> keyDefinition(InputConstants.KEY_SLASH, (int) '/');
            case "backslash" -> keyDefinition(InputConstants.KEY_BACKSLASH, (int) '\\');
            case "semicolon" -> keyDefinition(InputConstants.KEY_SEMICOLON, (int) ';');
            case "apostrophe", "quote" -> keyDefinition(InputConstants.KEY_APOSTROPHE, (int) '\'');
            case "grave", "backtick" -> keyDefinition(InputConstants.KEY_GRAVE, (int) '`');
            case "lbracket", "leftbracket" -> keyDefinition(InputConstants.KEY_LBRACKET, (int) '[');
            case "rbracket", "rightbracket" -> keyDefinition(InputConstants.KEY_RBRACKET, (int) ']');
            case "lshift", "shift" -> keyDefinition(InputConstants.KEY_LSHIFT, null);
            case "rshift" -> keyDefinition(InputConstants.KEY_RSHIFT, null);
            case "lctrl", "lcontrol", "ctrl", "control" -> keyDefinition(InputConstants.KEY_LCONTROL, null);
            case "rctrl", "rcontrol" -> keyDefinition(InputConstants.KEY_RCONTROL, null);
            case "lalt", "alt" -> keyDefinition(InputConstants.KEY_LALT, null);
            case "ralt" -> keyDefinition(InputConstants.KEY_RALT, null);
            case "lsuper", "super", "meta", "cmd", "win" -> keyDefinition(InputConstants.KEY_LSUPER, null);
            case "rsuper" -> keyDefinition(InputConstants.KEY_RSUPER, null);
            case "capslock" -> keyDefinition(InputConstants.KEY_CAPSLOCK, null);
            case "numlock" -> keyDefinition(InputConstants.KEY_NUMLOCK, null);
            case "pause" -> keyDefinition(InputConstants.KEY_PAUSE, null);
            case "scrolllock" -> keyDefinition(InputConstants.KEY_SCROLLLOCK, null);
            case "printscreen" -> keyDefinition(InputConstants.KEY_PRINTSCREEN, null);
            case "numpadenter" -> keyDefinition(InputConstants.KEY_NUMPADENTER, null);
            case "numpadequals" -> keyDefinition(InputConstants.KEY_NUMPADEQUALS, null);
            case "numpadcomma" -> keyDefinition(InputConstants.KEY_NUMPADCOMMA, null);
            case "add" -> keyDefinition(InputConstants.KEY_ADD, null);
            case "multiply" -> keyDefinition(InputConstants.KEY_MULTIPLY, null);
            default -> throw new IllegalArgumentException("Invalid key specification '" + rawKey + "'.");
        };
    }

    private static KeyDefinition parseSingleCharacterKey(final char character, final String rawKey) {
        if (character >= 'a' && character <= 'z') {
            int keyCode = InputConstants.KEY_A + (character - 'a');
            return new KeyDefinition(keyCode, GLFW.glfwGetKeyScancode(keyCode), (int) character);
        }

        if (character >= '0' && character <= '9') {
            int keyCode = InputConstants.KEY_0 + (character - '0');
            return new KeyDefinition(keyCode, GLFW.glfwGetKeyScancode(keyCode), (int) character);
        }

        return switch (character) {
            case '-' -> keyDefinition(InputConstants.KEY_MINUS, (int) '-');
            case '=' -> keyDefinition(InputConstants.KEY_EQUALS, (int) '=');
            case ',' -> keyDefinition(InputConstants.KEY_COMMA, (int) ',');
            case '.' -> keyDefinition(InputConstants.KEY_PERIOD, (int) '.');
            case '/' -> keyDefinition(InputConstants.KEY_SLASH, (int) '/');
            case '\\' -> keyDefinition(InputConstants.KEY_BACKSLASH, (int) '\\');
            case ';' -> keyDefinition(InputConstants.KEY_SEMICOLON, (int) ';');
            case '\'' -> keyDefinition(InputConstants.KEY_APOSTROPHE, (int) '\'');
            case '`' -> keyDefinition(InputConstants.KEY_GRAVE, (int) '`');
            case '[' -> keyDefinition(InputConstants.KEY_LBRACKET, (int) '[');
            case ']' -> keyDefinition(InputConstants.KEY_RBRACKET, (int) ']');
            default -> throw new IllegalArgumentException("Invalid key specification '" + rawKey + "'.");
        };
    }

    private static KeyDefinition keyDefinition(final int keyCode, @Nullable final Integer typedCodepoint) {
        return new KeyDefinition(keyCode, GLFW.glfwGetKeyScancode(keyCode), typedCodepoint);
    }

    @Nullable
    private static Integer modifierMask(final String value) {
        return switch (value) {
            case "shift", "lshift", "rshift" -> InputConstants.MOD_SHIFT;
            case "ctrl", "control", "lctrl", "lcontrol", "rctrl", "rcontrol" -> InputConstants.MOD_CONTROL;
            case "alt", "lalt", "ralt" -> InputConstants.MOD_ALT;
            case "super", "meta", "cmd", "win", "lsuper", "rsuper" -> InputConstants.MOD_SUPER;
            default -> null;
        };
    }

    private static <T> T runOnGameThread(final ThrowingSupplier<T> supplier) throws Exception {
        Minecraft minecraft = Minecraft.getInstance();

        try {
            return minecraft.submit(() -> {
                try {
                    return supplier.get();
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new CompletionException(ex);
                }
            }).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        } catch (ExecutionException ex) {
            throw unwrapExecutionException(ex);
        }
    }

    private static CommandResponse awaitResponse(final CompletableFuture<CommandResponse> future) throws Exception {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        } catch (ExecutionException ex) {
            throw unwrapExecutionException(ex);
        }
    }

    private static Exception unwrapExecutionException(final ExecutionException executionException) {
        Throwable cause = executionException.getCause();
        if (cause instanceof CompletionException completionException && completionException.getCause() != null) {
            cause = completionException.getCause();
        }

        if (cause instanceof Exception ex) {
            return ex;
        }

        return new RuntimeException(cause);
    }

    private static void printResponse(final CommandResponse response) {
        for (String line : response.lines()) {
            printLine(!response.success(), line);
        }
    }

    private static synchronized void printLine(final boolean error, final String line) {
        if (error) {
            System.out.println("[KONKRETE DEBUG][ERROR] " + line);
        } else {
            System.out.println("[KONKRETE DEBUG] " + line);
        }
    }

    private static String formatThrowable(final Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getName();
        }

        return current.getClass().getSimpleName() + ": " + message;
    }

    private static String buttonName(final int button) {
        return button == InputConstants.MOUSE_BUTTON_RIGHT ? "right" : "left";
    }

    private static String formatPoint(final double x, final double y) {
        return "(" + formatDecimal(x) + ", " + formatDecimal(y) + ")";
    }

    private static String formatDecimal(final double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String normalizeForConsole(final String value) {
        String normalized = value.replace("\r", "").replace("\n", "\\n").replace("\"", "\\\"").trim();
        return normalized.isEmpty() ? "<empty>" : normalized;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {

        T get() throws Exception;
    }

    private record CommandResponse(boolean success, List<String> lines) {

        private static CommandResponse success(final String line) {
            return new CommandResponse(true, List.of(line));
        }

        private static CommandResponse success(final List<String> lines) {
            return new CommandResponse(true, List.copyOf(lines));
        }

        private static CommandResponse failure(final String line) {
            return new CommandResponse(false, List.of(line));
        }
    }

    private record RequestedKey(String raw, int keyCode, int scanCode, int modifiers, @Nullable Integer typedCodepoint) {
    }

    private record KeyDefinition(int keyCode, int scanCode, @Nullable Integer typedCodepoint) {
    }

    private record WidgetSnapshot(int index, int left, int top, int right, int bottom, String message, String className) {

        @Nullable
        private static WidgetSnapshot create(final int index, final Renderable renderable) {
            if (!(renderable instanceof GuiEventListener listener)) {
                return null;
            }

            ScreenRectangle rectangle = listener.getRectangle();
            if (rectangle.width() <= 0 || rectangle.height() <= 0) {
                return null;
            }

            String message = renderable instanceof AbstractWidget widget ? normalizeForConsole(widget.getMessage().getString()) : "<no message>";
            return new WidgetSnapshot(index, rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom(), message, renderable.getClass().getName());
        }

        private String describe() {
            return String.format(
                Locale.ROOT,
                "#%02d [%d,%d -> %d,%d] \"%s\" (%s)",
                this.index,
                this.left,
                this.top,
                this.right,
                this.bottom,
                this.message,
                this.className
            );
        }
    }

    private static final class WorldLoadRequest {

        private final String worldId;
        private final boolean created;
        private final long startedAtMillis;
        private final CompletableFuture<CommandResponse> future = new CompletableFuture<>();

        private WorldLoadRequest(final String worldId, final boolean created) {
            this.worldId = worldId;
            this.created = created;
            this.startedAtMillis = Util.getMillis();
            this.future.whenComplete((response, throwable) -> activeWorldLoadRequest = null);
        }

        private CompletableFuture<CommandResponse> future() {
            return this.future;
        }

        private void fail(final String message) {
            this.future.complete(CommandResponse.failure(message));
        }

        private void tick(final Minecraft minecraft) {
            if (this.future.isDone()) {
                return;
            }

            if (minecraft.player != null && minecraft.level != null && minecraft.isSingleplayer()) {
                String loadedWorldId = getLoadedWorldId(minecraft);
                if (this.worldId.equals(loadedWorldId)) {
                    String result = this.created ? "Created and loaded" : "Loaded";
                    this.future.complete(CommandResponse.success(result + " debug world '" + this.worldId + "' successfully."));
                } else {
                    this.future.complete(
                        CommandResponse.failure(
                            "A different singleplayer world finished loading ('" + loadedWorldId + "') while waiting for '" + this.worldId + "'."
                        )
                    );
                }
                return;
            }

            long ageMillis = Util.getMillis() - this.startedAtMillis;
            Screen screen = minecraft.screen;
            if (ageMillis > 2000L && screen != null && !isLoadingScreen(screen)) {
                this.future.complete(
                    CommandResponse.failure(
                        "World loading stopped on screen " + screen.getClass().getName() + " [" + screen.getTitle().getString() + "]."
                    )
                );
                return;
            }

            if (ageMillis > WORLD_LOAD_TIMEOUT_MILLIS) {
                String screenName = screen == null ? "null" : screen.getClass().getName();
                this.future.complete(
                    CommandResponse.failure(
                        "Timed out while waiting for '" + this.worldId + "' to finish loading. Current screen=" + screenName + "."
                    )
                );
            }
        }

        @Nullable
        private static String getLoadedWorldId(final Minecraft minecraft) {
            if (minecraft.getSingleplayerServer() == null) {
                return null;
            }

            Path worldPath = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize();
            Path fileName = worldPath.getFileName();
            return fileName == null ? worldPath.toString() : fileName.toString();
        }

        private static boolean isLoadingScreen(final Screen screen) {
            return screen instanceof GenericMessageScreen || screen instanceof LevelLoadingScreen || screen instanceof ProgressScreen;
        }
    }
}
