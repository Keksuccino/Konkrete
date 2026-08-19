package de.keksuccino.konkrete.util.rendering.ui.widget.editbox;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinCommandSuggestions;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinSuggestionsList;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Computes, displays, and routes keyboard input for completion suggestions attached to an edit box. */
@SuppressWarnings("unused")
public class EditBoxSuggestions extends CommandSuggestions {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Pattern locating the final whitespace-delimited input segment. */
    protected static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
    /** Style applied to input that Brigadier could not parse. */
    protected static final Style UNPARSED_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
    /** Style applied to parsed command literals. */
    protected static final Style LITERAL_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
    /** Rotating Brigadier styles used to distinguish command arguments. */
    protected static final List<Style> ARGUMENT_STYLES = Stream.of(ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD).map(Style.EMPTY::withColor).collect(ImmutableList.toImmutableList());

    /** Client instance providing command and suggestion services. */
    protected final Minecraft minecraft;
    /** Screen receiving the suggestion popup's focus and narration updates. */
    protected final Screen screen;
    /** Editable text field used for input. */
    protected final EditBox input;
    /** Font used to measure and draw suggestions. */
    protected final Font font;
    /** Whether input is parsed strictly as commands. */
    protected final boolean commandsOnly;
    /** Whether suggestions stay hidden until the cursor passes the parse error. */
    protected final boolean onlyShowIfCursorPastError;
    /** Line start offset in GUI pixels. */
    protected final int lineStartOffset;
    /** Configured suggestion line limit. */
    protected final int suggestionLineLimit;
    /** Whether the suggestion popup grows upward from the input. */
    protected final boolean anchorToBottom;
    /** Caller-supplied suggestions considered alongside command completions. */
    protected final List<String> customSuggestionsList = new ArrayList<>();
    /** Whether to suppress Brigadier suggestions in favor of custom entries. */
    protected boolean onlyCustomSuggestions = false;
    /** Whether the command-usage line may be rendered. */
    protected boolean allowRenderUsage = true;
    /** Placement strategy used for the suggestion popup. */
    @NotNull
    protected SuggestionsRenderPosition renderPosition = SuggestionsRenderPosition.VANILLA;
    /** Suggestion-popup background color. */
    protected DrawableColor backgroundColor = DrawableColor.of(new Color(0,0,0));
    /** Color of unselected suggestions. */
    protected DrawableColor normalTextColor = DrawableColor.of(new Color(-5592406));
    /** Color of the selected suggestion. */
    protected DrawableColor selectedTextColor = DrawableColor.of(new Color(-256));
    /** Whether editable text is drawn with a shadow. */
    protected boolean textShadow = true;
    /** Whether suggestions open automatically as input changes. */
    protected boolean autoSuggestions = true;

    /** Creates a completion controller backed by a fixed list of literal suggestions. */
    @NotNull
    public static EditBoxSuggestions createWithCustomSuggestions(@NotNull Screen screen, @NotNull EditBox editBox, @NotNull SuggestionsRenderPosition renderPosition, @NotNull List<String> suggestions) {
        EditBoxSuggestions variableNameSuggestions = new EditBoxSuggestions(Minecraft.getInstance(), screen, editBox, Minecraft.getInstance().font, false, true, 0, 7, false);
        variableNameSuggestions.setAllowSuggestions(true);
        variableNameSuggestions.enableOnlyCustomSuggestionsMode(true);
        variableNameSuggestions.setSuggestionsRenderPosition(renderPosition);
        variableNameSuggestions.setAllowRenderUsage(false);
        variableNameSuggestions.setCustomSuggestions(suggestions);
        variableNameSuggestions.updateCommandInfo();
        return variableNameSuggestions;
    }

    /** Attaches filtered, positioned command suggestions to a target edit box. */
    public EditBoxSuggestions(@NotNull Minecraft mc, @NotNull Screen parentScreen, @NotNull EditBox targetEditBox, @NotNull Font font, boolean commandsOnly, boolean onlyShowIfCursorPastError, int lineStartOffset, int suggestionLineLimit, boolean anchorToBottom) {
        super(mc, parentScreen, targetEditBox, font, commandsOnly, onlyShowIfCursorPastError, lineStartOffset, suggestionLineLimit, anchorToBottom, Integer.MIN_VALUE);
        this.minecraft = mc;
        this.screen = parentScreen;
        this.input = targetEditBox;
        this.font = font;
        this.commandsOnly = commandsOnly;
        this.onlyShowIfCursorPastError = onlyShowIfCursorPastError;
        this.lineStartOffset = lineStartOffset;
        this.suggestionLineLimit = suggestionLineLimit;
        this.anchorToBottom = anchorToBottom;
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.input.isFocused()) {
            this.setSuggestions(null);
        }
        super.extractRenderState(graphics, mouseX, mouseY);
    }

    /** Extracts usage from the supplied UI state. */
    @Override
    public void extractUsage(@NotNull GuiGraphicsExtractor graphics) {
        if (!this.isAllowRenderUsage()) return;
        super.extractUsage(graphics);
    }

    /** Refreshes command info from current state. */
    @Override
    public void updateCommandInfo() {

        String editBoxValue = this.input.getValue();

        if ((this.getCurrentParse() != null) && !this.getCurrentParse().getReader().getString().equals(editBoxValue)) {
            this.setCurrentParse(null);
        }

        if (!this.isKeepSuggestions()) {
            this.input.setSuggestion(null);
            this.setSuggestions(null);
        }

        this.getCommandUsage().clear();
        StringReader valueReader = new StringReader(editBoxValue);
        boolean isReaderCursorAtSlash = valueReader.canRead() && valueReader.peek() == '/';
        if (isReaderCursorAtSlash) {
            valueReader.skip();
        }

        boolean treatAsCommand = this.commandsOnly || isReaderCursorAtSlash;
        if (this.onlyCustomSuggestions) treatAsCommand = false;
        int editBoxCursorPos = this.input.getCursorPosition();
        if (treatAsCommand) {
            if (this.minecraft.player != null) {
                var commands = this.minecraft.player.connection.getCommands();
                if (this.getCurrentParse() == null) {
                    this.setCurrentParse(commands.parse(valueReader, this.minecraft.player.connection.getSuggestionsProvider()));
                }
                int readerCursorPos = this.onlyShowIfCursorPastError ? valueReader.getCursor() : 1;
                if ((editBoxCursorPos >= readerCursorPos) && ((this.getSuggestions() == null) || !this.isKeepSuggestions())) {
                    ParseResults<ClientSuggestionProvider> currentParse = this.getCurrentParse();
                    CompletableFuture<Suggestions> pendingSuggestions = commands.getCompletionSuggestions(currentParse, editBoxCursorPos);
                    this.setPendingSuggestions(pendingSuggestions);
                    pendingSuggestions.thenAccept(suggestionResult -> {
                        if (this.getPendingSuggestions() == pendingSuggestions) {
                            this.updateUsageInfo(currentParse, suggestionResult);
                        }
                    });
                }
            }
        } else {
            String editBoxSubValue = editBoxValue.substring(0, editBoxCursorPos);
            int lastWordIndex = getLastWordIndex(editBoxSubValue);
            Collection<String> suggestionStringList = new ArrayList<>(this.customSuggestionsList);
            if (suggestionStringList.isEmpty() && (this.minecraft.player != null)) {
                suggestionStringList = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSuggestions();
            }
            this.setPendingSuggestions(SharedSuggestionProvider.suggest(suggestionStringList, new SuggestionsBuilder(editBoxSubValue, lastWordIndex)));
            //Always show suggestions without pressing TAB
            if (this.autoSuggestions && this.suggestionsAllowed() && this.minecraft.options.autoSuggestions().get()) {
                this.showSuggestions(false);
            }
        }

    }

    /** Opens suggestions. */
    @Override
    public void showSuggestions(boolean someNarratingRelatedBoolean) {
        if ((this.getPendingSuggestions() != null) && this.getPendingSuggestions().isDone()) {

            Suggestions suggestions = this.getPendingSuggestions().join();
            if (!suggestions.isEmpty()) {

                List<Suggestion> sortedSuggestions = this.sortSuggestions(suggestions);

                int totalSuggestionsWidth = 0;
                for(Suggestion suggestion : suggestions.getList()) {
                    totalSuggestionsWidth = Math.max(totalSuggestionsWidth, this.font.width(suggestion.getText()));
                }

                int listX = Mth.clamp(this.input.getScreenX(suggestions.getRange().getStart()), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - totalSuggestionsWidth);
                int listY = this.anchorToBottom ? this.screen.height - 12 : 72;
                int listHeight = Math.min(sortedSuggestions.size(), this.suggestionLineLimit) * 12;
                if (this.renderPosition == SuggestionsRenderPosition.ABOVE_EDIT_BOX) {
                    listY = this.input.getY() - listHeight - 2;
                }
                if (this.renderPosition == SuggestionsRenderPosition.BELOW_EDIT_BOX) {
                    listY = this.input.getY() + this.input.getHeight() + 2;
                }
                this.setSuggestions(new EditBoxSuggestionsList(listX, listY, totalSuggestionsWidth, sortedSuggestions, someNarratingRelatedBoolean));

            }

        }
    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (!this.input.isFocused()) return false;
        if ((this.getSuggestions() != null) && this.getSuggestions().keyPressed(new net.minecraft.client.input.KeyEvent(keycode, scancode, modifiers))) {
            return true;
        } else if (keycode == 258) {
            this.showSuggestions(true);
            return true;
        } else {
            return false;
        }
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event) {
        if (!this.input.isFocused()) return false;
        return super.mouseClicked(event);
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double $$0, double $$1, int $$2) {
        return this.mouseClicked(new net.minecraft.client.input.MouseButtonEvent($$0, $$1, new net.minecraft.client.input.MouseButtonInfo($$2, 0)));
    }

    /** Routes wheel scrolling and reports whether it was consumed. */
    @Override
    public boolean mouseScrolled(double $$0) {
        if (!this.input.isFocused()) return false;
        return super.mouseScrolled($$0);
    }

    /** Refreshes usage info from current state. */
    protected void updateUsageInfo(ParseResults<ClientSuggestionProvider> currentParse, Suggestions suggestions) {
        this.getAccessor().invoke_updateUsageInfo_Konkrete(currentParse, suggestions);
    }

    /** Sorts completion suggestions using the current input and cursor position. */
    protected List<Suggestion> sortSuggestions(Suggestions suggestions) {
        return this.getAccessor().invoke_sortSuggestions_Konkrete(suggestions);
    }

    /** Returns whether allowed. */
    public boolean suggestionsAllowed() {
        return this.getAccessor().get_allowSuggestions_Konkrete();
    }

    /** Returns whether suggestions enabled. */
    public boolean autoSuggestionsEnabled() {
        return this.autoSuggestions;
    }

    /** Sets auto suggestions enabled for this edit box suggestions. */
    public void setAutoSuggestionsEnabled(boolean enabled) {
        this.autoSuggestions = enabled;
    }

    /** Reports whether editable text is drawn with a shadow. */
    public boolean isTextShadow() {
        return this.textShadow;
    }

    /** Sets text shadow for this edit box suggestions. */
    public void setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
    }

    /** Returns the background color resolved for the current state. */
    public DrawableColor getBackgroundColor() {
        return this.backgroundColor;
    }

    /** Sets background color for this edit box suggestions. */
    public void setBackgroundColor(@NotNull DrawableColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /** Returns normal text color. */
    public DrawableColor getNormalTextColor() {
        return this.normalTextColor;
    }

    /** Sets normal text color for this edit box suggestions. */
    public void setNormalTextColor(@NotNull DrawableColor normalTextColor) {
        this.normalTextColor = normalTextColor;
    }

    /** Returns selected text color. */
    public DrawableColor getSelectedTextColor() {
        return this.selectedTextColor;
    }

    /** Sets selected text color for this edit box suggestions. */
    public void setSelectedTextColor(@NotNull DrawableColor selectedTextColor) {
        this.selectedTextColor = selectedTextColor;
    }

    /** Sets suggestions render position for this edit box suggestions. */
    public void setSuggestionsRenderPosition(@NotNull SuggestionsRenderPosition position) {
        this.renderPosition = Objects.requireNonNull(position);
    }

    /** Returns suggestions render position. */
    @NotNull
    public SuggestionsRenderPosition getSuggestionsRenderPosition() {
        return this.renderPosition;
    }

    /** Sets allow render usage for this edit box suggestions. */
    public void setAllowRenderUsage(boolean allow) {
        this.allowRenderUsage = allow;
    }

    /** Returns whether allow render usage. */
    public boolean isAllowRenderUsage() {
        return this.allowRenderUsage;
    }

    /** Enables only custom suggestions mode. */
    public void enableOnlyCustomSuggestionsMode(boolean enable) {
        this.onlyCustomSuggestions = enable;
    }

    /** Returns whether only custom suggestions mode. */
    public boolean isOnlyCustomSuggestionsMode() {
        return this.onlyCustomSuggestions;
    }

    /** Sets custom suggestions for this edit box suggestions. */
    public void setCustomSuggestions(@Nullable List<String> customSuggestions) {
        if (this.commandsOnly) throw new RuntimeException("Can't set custom suggestions in commands-only mode!");
        this.customSuggestionsList.clear();
        if (customSuggestions != null) {
            this.customSuggestionsList.addAll(customSuggestions);
        }
    }

    /** Returns suggestions. */
    public CommandSuggestions.SuggestionsList getSuggestions() {
        return this.getAccessor().get_suggestions_Konkrete();
    }

    /** Sets suggestions for this edit box suggestions. */
    public void setSuggestions(CommandSuggestions.SuggestionsList suggestions) {
        this.getAccessor().set_suggestions_Konkrete(suggestions);
    }

    /** Returns pending suggestions. */
    public CompletableFuture<Suggestions> getPendingSuggestions() {
        return this.getAccessor().get_pendingSuggestions_Konkrete();
    }

    /** Sets pending suggestions for this edit box suggestions. */
    public void setPendingSuggestions(CompletableFuture<Suggestions> pendingSuggestions) {
        this.getAccessor().set_pendingSuggestions_Konkrete(pendingSuggestions);
    }

    /** Returns current parse. */
    public ParseResults<ClientSuggestionProvider> getCurrentParse() {
        return this.getAccessor().get_currentParse_Konkrete();
    }

    /** Sets current parse for this edit box suggestions. */
    public void setCurrentParse(ParseResults<ClientSuggestionProvider> currentParse) {
        this.getAccessor().set_currentParse_Konkrete(currentParse);
    }

    /** Returns whether keep suggestions. */
    public boolean isKeepSuggestions() {
        return this.getAccessor().get_keepSuggestions_Konkrete();
    }

    /** Returns accessor. */
    public AccessorMixinCommandSuggestions getAccessor() {
        return ((AccessorMixinCommandSuggestions)this);
    }

    /** Returns command usage. */
    public List<FormattedCharSequence> getCommandUsage() {
        return this.getAccessor().get_commandUsage_Konkrete();
    }

    /** Returns last word index. */
    @SuppressWarnings("all")
    protected static int getLastWordIndex(String editBoxValue) {
        if (Strings.isNullOrEmpty(editBoxValue)) {
            return 0;
        } else {
            int index = 0;
            for(Matcher matcher = WHITESPACE_PATTERN.matcher(editBoxValue); matcher.find(); index = matcher.end()) {}
            return index;
        }
    }

    /** Stores the position and dimensions of suggestions render. */
    public enum SuggestionsRenderPosition {
        /** Positions the element at vanilla. */
        VANILLA,
        /** Positions the element at above edit box. */
        ABOVE_EDIT_BOX,
        /** Positions the element at below edit box. */
        BELOW_EDIT_BOX
    }

    /** Extends Minecraft's suggestion popup with explicit bounds and event forwarding. */
    public class EditBoxSuggestionsList extends SuggestionsList {

        /** Suggestions shown by this popup in sorted display order. */
        protected List<Suggestion> suggestionList;

        /** Positions a visible subset of command suggestions for the owning suggestion controller. */
        public EditBoxSuggestionsList(int x, int y, int width, List<Suggestion> suggestionList, boolean someNarratingRelatedBoolean) {
            super(x, y, width, suggestionList, someNarratingRelatedBoolean);
            this.suggestionList = suggestionList;
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            Message message;
            int suggestionLineCount = Math.min(this.suggestionList.size(), EditBoxSuggestions.this.suggestionLineLimit);
            boolean bl = this.getOffset() > 0;
            boolean bl2 = this.suggestionList.size() > this.getOffset() + suggestionLineCount;
            boolean bl3 = bl || bl2;
            boolean bl4 = (this.getLastMouse().x != (float)mouseX) || (this.getLastMouse().y != (float)mouseY);
            if (bl4) {
                this.setLastMouse(new Vec2(mouseX, mouseY));
            }
            if (bl3) {
                int m;
                graphics.fill(this.getRect().getX(), this.getRect().getY() - 1, this.getRect().getX() + this.getRect().getWidth(), this.getRect().getY(), EditBoxSuggestions.this.backgroundColor.getColorInt());
                graphics.fill(this.getRect().getX(), this.getRect().getY() + this.getRect().getHeight(), this.getRect().getX() + this.getRect().getWidth(), this.getRect().getY() + this.getRect().getHeight() + 1, EditBoxSuggestions.this.backgroundColor.getColorInt());
                if (bl) {
                    for (m = 0; m < this.getRect().getWidth(); ++m) {
                        if (m % 2 != 0) continue;
                        graphics.fill(this.getRect().getX() + m, this.getRect().getY() - 1, this.getRect().getX() + m + 1, this.getRect().getY(), -1);
                    }
                }
                if (bl2) {
                    for (m = 0; m < this.getRect().getWidth(); ++m) {
                        if (m % 2 != 0) continue;
                        graphics.fill(this.getRect().getX() + m, this.getRect().getY() + this.getRect().getHeight(), this.getRect().getX() + m + 1, this.getRect().getY() + this.getRect().getHeight() + 1, -1);
                    }
                }
            }
            boolean bl52 = false;
            for (int n = 0; n < suggestionLineCount; ++n) {
                Suggestion suggestion = this.suggestionList.get(n + this.getOffset());
                graphics.fill(this.getRect().getX(), this.getRect().getY() + 12 * n, this.getRect().getX() + this.getRect().getWidth(), this.getRect().getY() + 12 * n + 12, EditBoxSuggestions.this.backgroundColor.getColorInt());
                if (mouseX > this.getRect().getX() && mouseX < this.getRect().getX() + this.getRect().getWidth() && mouseY > this.getRect().getY() + 12 * n && mouseY < this.getRect().getY() + 12 * n + 12) {
                    if (bl4) {
                        this.select(n + this.getOffset());
                    }
                    bl52 = true;
                }
                graphics.text(EditBoxSuggestions.this.font, suggestion.getText(), (this.getRect().getX() + 1), (this.getRect().getY() + 2 + 12 * n), ((n + this.getOffset()) == this.getCurrent()) ? EditBoxSuggestions.this.selectedTextColor.getColorInt() : EditBoxSuggestions.this.normalTextColor.getColorInt(), EditBoxSuggestions.this.textShadow);
            }
            if (bl52 && (message = this.suggestionList.get(this.getCurrent()).getTooltip()) != null) {
                graphics.setTooltipForNextFrame(EditBoxSuggestions.this.font, ComponentUtils.fromMessage(message), mouseX, mouseY);
            }
        }

        /** Returns rect. */
        public Rect2i getRect() {
            return this.getAccessor().get_rect_Konkrete();
        }

        /** Returns the sampled animation translation. */
        public int getOffset() {
            return this.getAccessor().get_offset_Konkrete();
        }

        /** Returns current. */
        public int getCurrent() {
            return this.getAccessor().get_current_Konkrete();
        }

        /** Returns last mouse. */
        public Vec2 getLastMouse() {
            return this.getAccessor().get_lastMouse_Konkrete();
        }

        /** Sets last mouse for this edit box suggestions list. */
        public void setLastMouse(Vec2 lastMouse) {
            this.getAccessor().set_lastMouse_Konkrete(lastMouse);
        }

        /** Returns accessor. */
        public AccessorMixinSuggestionsList getAccessor() {
            return (AccessorMixinSuggestionsList) this;
        }

    }

}
