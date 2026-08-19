package de.keksuccino.konkrete.util.rendering.ui.screen.texteditor;

import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.input.InputConstants;
import de.keksuccino.konkrete.util.input.InputUtils;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.konkrete.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.konkrete.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.formattingrules.TextEditorFormattingRules;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.placeholder.PlaceholderRegistry;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinAbstractWidget;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinEditBox;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Implements the interactive window body for text editor. */
@SuppressWarnings("unused")
public class TextEditorWindowBody extends PiPWindowBody {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final HashMap<String, String> COMPILED_SINGLE_LINE_STRINGS = new HashMap<>();

    /** Escaped newline token used when moving multiline text through single-line fields. */
    public static final String NEWLINE_CODE = "%!n!%";
    /** Escaped space token used when whitespace must survive serialization. */
    public static final String SPACE_CODE = "%!s!%";
    /** Width in GUI units for PiP window. */
    public static final int PIP_WINDOW_WIDTH = 640;
    /** Height in GUI units for PiP window. */
    public static final int PIP_WINDOW_HEIGHT = 420;

    /** Character-level acceptance filter shared by editor lines. */
    protected final CharacterFilter characterFilter;
    /** Callback receiving committed editor text. */
    protected final Consumer<String> callback;
    /** Editable lines in document order. */
    protected List<TextEditorLine> textFieldLines = new ArrayList<>();
    /** Scroll bar controlling vertical scroll bar. */
    protected ScrollBar verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    /** Scroll bar controlling horizontal scroll bar. */
    protected ScrollBar horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, UIBase.HORIZONTAL_SCROLL_BAR_WIDTH, UIBase.HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    /** Scroll bar controlling vertical scroll bar placeholder menu. */
    protected ScrollBar verticalScrollBarPlaceholderMenu = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    /** Scroll bar controlling horizontal scroll bar placeholder menu. */
    protected ScrollBar horizontalScrollBarPlaceholderMenu = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, UIBase.HORIZONTAL_SCROLL_BAR_WIDTH, UIBase.HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    /** Context menu opened for editor selection and placeholder actions. */
    protected ContextMenu rightClickContextMenu;
    /** Button that cancels the current operation. */
    protected ExtendedButton cancelButton;
    /** Button that accepts the document after validation. */
    protected ExtendedButton doneButton;
    /** Button that opens the registered-placeholder insertion menu. */
    protected ExtendedButton placeholderButton;
    /** Last cursor index set directly by user input. */
    protected int lastCursorPosSetByUser = 0;
    /** Whether the previous word deletion crossed a line boundary. */
    protected boolean justSwitchedLineByWordDeletion = false;
    /** Whether cursor placement clamped a requested line above the document. */
    protected boolean triggeredFocusedLineWasTooHighInCursorPosMethod = false;
    /** Height in GUI units for header. */
    protected int headerHeight = 50;
    /** Height in GUI units for footer. */
    protected int footerHeight = 50;
    /** Horizontal GUI coordinate for border. */
    protected int borderLeft = 40;
    /** Horizontal GUI coordinate for border. */
    protected int borderRight = 20;
    /** Height in GUI units for line. */
    protected int lineHeight = 14;
    /** Horizontal GUI coordinate for line number sidebar gap. */
    protected int lineNumberSidebarGapLeft = 4;
    /** Horizontal GUI coordinate for line number sidebar gap. */
    protected int lineNumberSidebarGapRight = 4;
    /** Supplies the editor-area background color for the current theme state. */
    protected Supplier<DrawableColor> areaBackgroundColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_background_color_type_1;
        return UIBase.getUITheme().ui_interface_area_background_color_type_1;
    };
    /** Supplies the editor-area border color for the current theme state. */
    protected Supplier<DrawableColor> areaBorderColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_border_color;
        return UIBase.getUITheme().ui_interface_widget_border_color;
    };
    /** Supplies the editable text color. */
    protected Supplier<DrawableColor> textColor = () -> UIBase.getUITheme().text_editor_text_color;
    /** Supplies the focused-line background color. */
    protected Supplier<DrawableColor> focusedLineColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_entry_selected_color;
        return UIBase.getUITheme().ui_interface_area_entry_selected_color;
    };
    /** Supplies the idle scroll-grabber color. */
    protected Supplier<DrawableColor> scrollGrabberIdleColor = () -> UIBase.getUITheme().scroll_grabber_color_normal;
    /** Supplies the hovered scroll-grabber color. */
    protected Supplier<DrawableColor> scrollGrabberHoverColor = () -> UIBase.getUITheme().scroll_grabber_color_hover;
    /** Supplies the line-number sidebar background color. */
    protected Supplier<DrawableColor> lineNumberSideBarColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_background_color_type_2;
        return UIBase.getUITheme().ui_blur_interface_area_background_color_type_2;
    };
    /** Supplies the normal line-number text color. */
    protected Supplier<DrawableColor> lineNumberTextColorNormal = () -> UIBase.getUITheme().text_editor_line_number_text_color_normal;
    /** Supplies the focused line-number text color. */
    protected Supplier<DrawableColor> lineNumberTextColorFocused = () -> UIBase.getUITheme().text_editor_line_number_text_color_selected;
    /** Supplies the idle placeholder-entry background color. */
    protected Supplier<DrawableColor> placeholderEntryBackgroundColorIdle = () -> {
        if (UIBase.shouldBlur()) return DrawableColor.FULLY_TRANSPARENT;
        return UIBase.getUITheme().ui_interface_area_background_color_type_1;
    };
    /** Supplies the hovered placeholder-entry background color. */
    protected Supplier<DrawableColor> placeholderEntryBackgroundColorHover = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_entry_selected_color;
        return UIBase.getUITheme().ui_interface_area_entry_selected_color;
    };
    /** Supplies the marker color for placeholder entries. */
    protected Supplier<DrawableColor> placeholderEntryDotColorPlaceholder = () -> UIBase.getUITheme().bullet_list_dot_color_1;
    /** Supplies the marker color for placeholder categories. */
    protected Supplier<DrawableColor> placeholderEntryDotColorCategory = () -> UIBase.getUITheme().bullet_list_dot_color_2;
    /** Supplies the normal placeholder-entry label color. */
    protected Supplier<DrawableColor> placeholderEntryLabelColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_widget_label_color_normal;
        return UIBase.getUITheme().ui_interface_widget_label_color_normal;
    };
    /** Supplies the label color for the back-to-categories entry. */
    protected Supplier<DrawableColor> placeholderEntryBackToCategoriesLabelColor = () -> UIBase.getUITheme().warning_color;
    /** Width in GUI units for line. */
    protected int currentLineWidth;
    /** Focused line index observed during the previous tick, or {@code -1}. */
    protected int lastTickFocusedLineIndex = -1;
    /** Editor line containing start highlight. */
    protected TextEditorLine startHighlightLine = null;
    /** First line included in the current selection, or {@code -1}. */
    protected int startHighlightLineIndex = -1;
    /** Last line included in the current selection, or {@code -1}. */
    protected int endHighlightLineIndex = -1;
    /** Line where the current keyboard-extended selection began, or {@code -1}. */
    protected int keyboardHighlightAnchorLineIndex = -1;
    /** Cursor index anchoring a keyboard-extended selection. */
    protected int keyboardHighlightAnchorCursorPos = -1;
    /** Height in GUI units for overridden total scroll. */
    protected int overriddenTotalScrollHeight = -1;
    /** Deferred line-number draws for the current extraction pass. */
    protected List<Runnable> lineNumberRenderQueue = new ArrayList<>();
    /** Formatting rules applied to each visible line in list order. */
    public List<TextEditorFormattingRule> formattingRules = new ArrayList<>();
    /** Document-wide character index during the current formatting pass. */
    protected int currentRenderCharacterIndexTotal = 0;
    /** This is to make different instances of the editor remember the state of the placeholder menu **/
    protected static boolean extendedPlaceholderMenu = false;
    /** Width in GUI units for placeholder menu. */
    protected int placeholderMenuWidth = 120;
    /** Height in GUI units for placeholder menu entry. */
    protected int placeholderMenuEntryHeight = 16;
    /** Placeholder insertions exposed by the editor context menu. */
    protected List<PlaceholderMenuEntry> placeholderMenuEntries = new ArrayList<>();
    /** Whether the editor accepts multiple lines. */
    protected boolean multilineMode = true;
    /** Whether placeholder insertion is allowed. */
    protected boolean allowPlaceholders = true;
    /** Whether the editor title is bold. */
    protected boolean boldTitle = true;
    /** Whole-document validator used to enable or reject completion. */
    protected ConsumingSupplier<TextEditorWindowBody, Boolean> textValidator = null;
    /** Tooltip shown for text validator feedback. */
    protected UITooltip textValidatorFeedbackUITooltip = null;
    /** Whether the selected text was hovered when its context menu opened. */
    protected boolean selectedHoveredOnRightClickMenuOpen = false;
    /** Undo/redo history for the edited document. */
    protected final TextEditorHistory history = new TextEditorHistory(this);
    /** Search field used to filter placeholder entries. */
    protected ExtendedEditBox searchBar;
    /** Numeric field used by the go-to-line prompt. */
    protected ExtendedEditBox goToLineField;
    /** Whether the go-to-line prompt is open. */
    protected boolean isGoToLineOpen = false;

    private static final Comparator<Placeholder> PLACEHOLDER_DISPLAY_NAME_COMPARATOR = Comparator
            .comparing(Placeholder::getDisplayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Placeholder::getDisplayName)
            .thenComparing(Placeholder::getIdentifier);
    private static final Comparator<String> PLACEHOLDER_CATEGORY_COMPARATOR = Comparator
            .comparing((String category) -> category, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(category -> category);

    /** Renderer that emits visible indentation guides. */
    protected IndentationGuideRenderer indentGuideRenderer;
    /** Whether to draw indentation guides. */
    protected boolean showIndentationGuides = true;

    /** Creates a text editor with an optional title, character filter, and completion callback. */
    @NotNull
    public static TextEditorWindowBody build(@Nullable Component title, @Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
        return new TextEditorWindowBody(title, characterFilter, callback);
    }

    /** Initializes an editor with an optional title/filter and completion callback. */
    public TextEditorWindowBody(@Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
        this(null, characterFilter, callback);
    }

    /** Initializes an editor with an optional title/filter and completion callback. */
    public TextEditorWindowBody(@Nullable Component title, @Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
        super((title != null) ? title : Component.literal(""));
        this.characterFilter = characterFilter;
        this.callback = callback;
        this.addLine();
        this.getLine(0).setFocused(true);
        this.verticalScrollBar.setScrollWheelAllowed(true);
        this.verticalScrollBarPlaceholderMenu.setScrollWheelAllowed(true);
        this.formattingRules.addAll(TextEditorFormattingRules.getRules());
        this.indentGuideRenderer = new IndentationGuideRenderer(this);
        this.updateCurrentLineWidth();
    }

    /** Initializes resources required by this text editor window body. */
    @Override
    public void init() {

        this.placeholderMenuWidth = Math.min(300, Math.max(120, (int)((double)this.width / 3.5D)));

        this.updateRightClickContextMenu();

        this.verticalScrollBar.scrollAreaStartX = this.getEditorAreaX() + 1;
        this.verticalScrollBar.scrollAreaStartY = this.getEditorAreaY() + 1;
        this.verticalScrollBar.scrollAreaEndX = this.getEditorAreaX() + this.getEditorAreaWidth() - 2;
        this.verticalScrollBar.scrollAreaEndY = this.getEditorAreaY() + this.getEditorAreaHeight() - this.horizontalScrollBar.grabberHeight - 2;

        this.horizontalScrollBar.scrollAreaStartX = this.getEditorAreaX() + 1;
        this.horizontalScrollBar.scrollAreaStartY = this.getEditorAreaY() + 1;
        this.horizontalScrollBar.scrollAreaEndX = this.getEditorAreaX() + this.getEditorAreaWidth() - this.verticalScrollBar.grabberWidth - 2;
        this.horizontalScrollBar.scrollAreaEndY = this.getEditorAreaY() + this.getEditorAreaHeight() - 1;

        int placeholderSearchBarY = this.getPlaceholderAreaY() - 25;

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, this.getPlaceholderAreaX(), placeholderSearchBarY, this.getPlaceholderAreaWidth(), 20 - 2, Component.empty());
        this.searchBar.setCustomHint(consumes -> Component.translatable("konkrete.placeholders.text_editor.search_placeholder"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updatePlaceholdersList());
        this.searchBar.setIsVisibleSupplier(consumes -> extendedPlaceholderMenu && this.allowPlaceholders);
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar, UIBase.shouldBlur());

        this.goToLineField = new ExtendedEditBox(Minecraft.getInstance().font, this.getEditorAreaX() + this.getEditorAreaWidth() - 150 - 20, this.getEditorAreaY() + 5, 150, 20, Component.literal(""));
        this.goToLineField.setCustomHint(consumes -> Component.translatable("konkrete.editor.shortcuts.go_to_line"));
        this.goToLineField.setIsVisibleSupplier(consumes -> this.isGoToLineOpen);
        this.goToLineField.setCharacterFilter(de.keksuccino.konkrete.util.input.CharacterFilter.buildIntegerFilter());
        this.addRenderableWidget(this.goToLineField);
        UIBase.applyDefaultWidgetSkinTo(this.goToLineField, UIBase.shouldBlur());

        this.verticalScrollBarPlaceholderMenu.scrollAreaStartX = this.getPlaceholderAreaX() + 1;
        this.verticalScrollBarPlaceholderMenu.scrollAreaStartY = this.getPlaceholderAreaY() + 1;
        this.verticalScrollBarPlaceholderMenu.scrollAreaEndX = this.getPlaceholderAreaX() + this.getPlaceholderAreaWidth() - 2;
        this.verticalScrollBarPlaceholderMenu.scrollAreaEndY = this.getPlaceholderAreaY() + this.getPlaceholderAreaHeight() - this.horizontalScrollBarPlaceholderMenu.grabberHeight - 2;

        this.horizontalScrollBarPlaceholderMenu.scrollAreaStartX = this.getPlaceholderAreaX() + 1;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaStartY = this.getPlaceholderAreaY() + 1;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaEndX = this.getPlaceholderAreaX() + this.getPlaceholderAreaWidth() - this.verticalScrollBarPlaceholderMenu.grabberWidth - 2;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaEndY = this.getPlaceholderAreaY() + this.getPlaceholderAreaHeight() - 1;

        //Set scroll grabber colors
        this.verticalScrollBar.idleBarColor = this.scrollGrabberIdleColor;
        this.verticalScrollBar.hoverBarColor = this.scrollGrabberHoverColor;
        this.horizontalScrollBar.idleBarColor = this.scrollGrabberIdleColor;
        this.horizontalScrollBar.hoverBarColor = this.scrollGrabberHoverColor;
        this.verticalScrollBar.setRoundedGrabberEnabled(true);
        this.horizontalScrollBar.setRoundedGrabberEnabled(true);

        //Set placeholder menu scroll bar colors
        this.verticalScrollBarPlaceholderMenu.idleBarColor = this.scrollGrabberIdleColor;
        this.verticalScrollBarPlaceholderMenu.hoverBarColor = this.scrollGrabberHoverColor;
        this.horizontalScrollBarPlaceholderMenu.idleBarColor = this.scrollGrabberIdleColor;
        this.horizontalScrollBarPlaceholderMenu.hoverBarColor = this.scrollGrabberHoverColor;
        this.verticalScrollBarPlaceholderMenu.setRoundedGrabberEnabled(true);
        this.horizontalScrollBarPlaceholderMenu.setRoundedGrabberEnabled(true);

        this.addWidget(this.verticalScrollBar);
        this.addWidget(this.horizontalScrollBar);
        this.addWidget(this.verticalScrollBarPlaceholderMenu);
        this.addWidget(this.horizontalScrollBarPlaceholderMenu);

        this.cancelButton = new ExtendedButton(this.width - this.borderRight - 100 - 5 - 100, this.height - 35, 100, 20, Component.translatable("konkrete.common_components.cancel"), (button) -> {
            this.callback.accept(null);
            this.closeWindow();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

        this.doneButton = new ExtendedButton(this.width - this.borderRight - 100, this.height - 35, 100, 20, Component.translatable("konkrete.common_components.done"), (button) -> {
            this.triggerDoneAction();
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());

        if (this.allowPlaceholders) {
            MutableComponent placeholderButtonLabel = Component.translatable("konkrete.ui.text_editor.placeholders");
            if (extendedPlaceholderMenu) {
                placeholderButtonLabel = placeholderButtonLabel.withStyle(Style.EMPTY.withUnderlined(true));
            }
            this.placeholderButton = new ExtendedButton(this.width - this.borderRight - 100, (this.headerHeight / 2) - 10, 100, 20, placeholderButtonLabel, (button) -> {
                extendedPlaceholderMenu = !extendedPlaceholderMenu;
                this.rebuildWidgets();
            }).setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("konkrete.placeholders.desc")));
            this.addWidget(this.placeholderButton);
            UIBase.applyDefaultWidgetSkinTo(this.placeholderButton, UIBase.shouldBlur());
        } else {
            this.placeholderButton = null;
            extendedPlaceholderMenu = false;
        }

        this.updatePlaceholdersList();

    }

    /** Commits editor text through the configured completion callback. */
    protected void triggerDoneAction() {
        if (this.isTextValid()) {
            this.callback.accept(this.getText());
            this.closeWindow();
        }
    }

    /** Refreshes right click context menu from current state. */
    public void updateRightClickContextMenu() {

        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.rightClickContextMenu = new ContextMenu();

        this.rightClickContextMenu.addClickableEntry("copy", Component.translatable("konkrete.ui.text_editor.copy"), (menu, entry) -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlightedText());
                    menu.closeMenu();
                }).setIsActiveSupplier((menu, entry) -> {
                    if (!menu.isOpen()) return false;
                    return this.selectedHoveredOnRightClickMenuOpen;
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("konkrete.editor.shortcuts.copy"))
                .setIcon(MaterialIcons.CONTENT_COPY);

        this.rightClickContextMenu.addClickableEntry("paste", Component.translatable("konkrete.ui.text_editor.paste"), (menu, entry) -> {
                    this.pasteText(Minecraft.getInstance().keyboardHandler.getClipboard());
                    menu.closeMenu();
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("konkrete.editor.shortcuts.paste"))
                .setIcon(MaterialIcons.CONTENT_PASTE);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_paste");

        this.rightClickContextMenu.addClickableEntry("cut", Component.translatable("konkrete.ui.text_editor.cut"), (menu, entry) -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.cutHighlightedText());
                    menu.closeMenu();
                }).setIsActiveSupplier((menu, entry) -> {
                    if (!menu.isOpen()) return false;
                    return this.selectedHoveredOnRightClickMenuOpen;
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("konkrete.editor.shortcuts.cut"))
                .setIcon(MaterialIcons.CONTENT_CUT);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_cut");

        this.rightClickContextMenu.addClickableEntry("select_all", Component.translatable("konkrete.ui.text_editor.select_all"), (menu, entry) -> {
                    for (TextEditorLine t : this.textFieldLines) {
                        t.setHighlightPos(0);
                        t.setCursorPosition(t.getValue().length());
                    }
                    this.setFocusedLine(this.getLineCount()-1);
                    this.startHighlightLineIndex = 0;
                    this.endHighlightLineIndex = this.getLineCount()-1;
                    menu.closeMenu();
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("konkrete.editor.shortcuts.select_all"))
                .setIcon(MaterialIcons.SELECT_ALL);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_select_all");

        this.rightClickContextMenu.addClickableEntry("undo", Component.translatable("konkrete.editor.edit.undo"), (menu, entry) -> {
                    this.history.stepBack();
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("konkrete.editor.shortcuts.undo"))
                .setIcon(MaterialIcons.UNDO);

        this.rightClickContextMenu.addClickableEntry("redo", Component.translatable("konkrete.editor.edit.redo"), (menu, entry) -> {
                    this.history.stepForward();
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("konkrete.editor.shortcuts.redo"))
                .setIcon(MaterialIcons.REDO);

    }

    /** Renders body into the active GUI extraction pass. */
    @Override
    public void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        //Reset scrolls if content fits editor area
        if (this.currentLineWidth <= this.getEditorAreaWidth()) {
            this.horizontalScrollBar.setScroll(0.0F);
        }
        if (this.getTotalLineHeight() <= this.getEditorAreaHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }

        this.justSwitchedLineByWordDeletion = false;

        this.updateCurrentLineWidth();

        //Adjust the scroll wheel speed depending on the amount of lines
        this.verticalScrollBar.setWheelScrollSpeed(1.0F / ((float)this.getTotalScrollHeight() / 500.0F));

        this.renderLineNumberBackground(graphics, partial);

        this.renderEditorAreaBackground(graphics, partial);

        // Render indentation guides if enabled
        if (this.showIndentationGuides) {
            this.indentGuideRenderer.extractRenderState(graphics);
        }

        //Don't render parts of lines outside editor area
        graphics.enableScissor(this.getEditorAreaX(), this.getEditorAreaY(), this.getEditorAreaX() + this.getEditorAreaWidth(), this.getEditorAreaY() + this.getEditorAreaHeight());

        this.formattingRules.forEach((rule) -> rule.resetRule(this));
        this.currentRenderCharacterIndexTotal = 0;
        this.lineNumberRenderQueue.clear();
        //Update positions and size of lines and render them
        this.updateLines((line) -> {
            if (line.isInEditorArea()) {
                this.lineNumberRenderQueue.add(() -> this.renderLineNumber(graphics, line));
            }
            line.extractRenderState(graphics, mouseX, mouseY, partial);
        });

        graphics.disableScissor();

        //Don't render line numbers outside the line number area
        int lineNumberMinX = this.getLineNumberSidebarX();
        int lineNumberMaxX = this.getLineNumberSidebarRight();
        graphics.enableScissor(lineNumberMinX, this.getEditorAreaY() + 2, lineNumberMaxX, this.getEditorAreaY() + this.getEditorAreaHeight() - 2);

        for (Runnable r : this.lineNumberRenderQueue) {
            r.run();
        }

        graphics.disableScissor();

        this.lastTickFocusedLineIndex = this.getFocusedLineIndex();
        this.triggeredFocusedLineWasTooHighInCursorPosMethod = false;

        this.renderEditorAreaBorder(graphics, partial);

        this.verticalScrollBar.extractRenderState(graphics, mouseX, mouseY, partial);
        this.horizontalScrollBar.extractRenderState(graphics, mouseX, mouseY, partial);

        this.renderPlaceholderMenu(graphics, mouseX, mouseY, partial);

        this.cancelButton.extractRenderState(graphics, mouseX, mouseY, partial);

        this.doneButton.active = this.isTextValid();
        this.doneButton.setUITooltip(this.textValidatorFeedbackUITooltip);
        this.doneButton.extractRenderState(graphics, mouseX, mouseY, partial);

        this.renderMultilineNotSupportedNotification(graphics, mouseX, mouseY, partial);

        this.tickMouseHighlighting(this.getRenderMouseX(), this.getRenderMouseY(), true);

    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
    }

    /** Renders multiline not supported notification into the active GUI extraction pass. */
    protected void renderMultilineNotSupportedNotification(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (!this.multilineMode) {
            MutableComponent indicator = Component.translatable("konkrete.editor.text_editor.single_line_warning.indicator").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_color.getColorInt())).append(Component.literal(" [?]").withStyle(Style.EMPTY.withBold(true).withColor(UIBase.getUITheme().warning_color.getColorInt())));
            int indicatorX = this.getEditorAreaX();
            float indicatorY = this.getEditorAreaY() - UIBase.getUITextHeightNormal() - UIBase.getAreaLabelVerticalPadding();
            int indicatorWidth = Math.round(UIBase.getUITextWidthNormal(indicator));
            UIBase.renderText(graphics, indicator, indicatorX, indicatorY, -1);
            if (UIBase.isXYInArea(mouseX, mouseY, indicatorX, (int) indicatorY, indicatorWidth, (int) UIBase.getUITextHeightNormal())) {
                TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(Component.translatable("konkrete.editor.text_editor.single_line_warning").withColor(UIBase.getUITheme().error_color.getColorInt())), () -> true);
            }
        }
    }

    /** Renders placeholder menu into the active GUI extraction pass. */
    protected void renderPlaceholderMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (extendedPlaceholderMenu) {

            if (this.getTotalPlaceholderEntriesWidth() <= this.getPlaceholderAreaWidth()) {
                this.horizontalScrollBarPlaceholderMenu.setScroll(0.0F);
            }
            if (this.getTotalPlaceholderEntriesHeight() <= this.getPlaceholderAreaHeight()) {
                this.verticalScrollBarPlaceholderMenu.setScroll(0.0F);
            }

            //Render placeholder menu background
            float placeholderAreaRadius = UIBase.getInterfaceCornerRoundingRadius();
            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, this.width - this.borderRight - this.getPlaceholderAreaWidth(), this.getPlaceholderAreaY(), this.getPlaceholderAreaWidth(), this.getPlaceholderAreaHeight(), placeholderAreaRadius, placeholderAreaRadius, placeholderAreaRadius, placeholderAreaRadius, this.areaBackgroundColor.get().getColorInt(), partial);

            //Don't render parts of placeholder entries outside of placeholder menu area
            graphics.enableScissor(this.width - this.borderRight - this.getPlaceholderAreaWidth(), this.getPlaceholderAreaY() + 2, this.width - this.borderRight, this.getPlaceholderAreaY() + this.getPlaceholderAreaHeight() - 2);

            //Render placeholder entries
            List<PlaceholderMenuEntry> entries = new ArrayList<>(this.placeholderMenuEntries);
            int index = 0;
            for (PlaceholderMenuEntry e : entries) {
                e.x = (this.width - this.borderRight - this.getPlaceholderAreaWidth()) + this.getPlaceholderEntriesRenderOffsetX();
                e.y = (this.getPlaceholderAreaY()) + (this.placeholderMenuEntryHeight * index) + this.getPlaceholderEntriesRenderOffsetY();
                e.extractRenderState(graphics, mouseX, mouseY, partial);
                index++;
            }

            graphics.disableScissor();

            //Render placeholder menu border
            SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(graphics, this.width - this.borderRight - this.getPlaceholderAreaWidth() - 1, this.getPlaceholderAreaY() - 1, this.getPlaceholderAreaWidth() + 2, this.getPlaceholderAreaHeight() + 2, 1.0F, placeholderAreaRadius, placeholderAreaRadius, placeholderAreaRadius, placeholderAreaRadius, this.areaBorderColor.get().getColorInt(), partial);

            //Render placeholder menu scroll bars
            this.verticalScrollBarPlaceholderMenu.extractRenderState(graphics, mouseX, mouseY, partial);
            this.horizontalScrollBarPlaceholderMenu.extractRenderState(graphics, mouseX, mouseY, partial);

        }

        if (this.placeholderButton != null) {
            this.placeholderButton.extractRenderState(graphics, mouseX, mouseY, partial);
        }

    }

    /** Returns placeholder area x. */
    public int getPlaceholderAreaX() {
        return this.width - this.borderRight - this.getPlaceholderAreaWidth();
    }

    /** Returns placeholder area y. */
    public int getPlaceholderAreaY() {
        return this.getEditorAreaY() + 25;
    }

    /** Returns placeholder area height. */
    public int getPlaceholderAreaHeight() {
        return this.getEditorAreaHeight() - 25;
    }

    /** Returns placeholder area width. */
    public int getPlaceholderAreaWidth() {
        return this.placeholderMenuWidth;
    }

    /** Returns total placeholder entries height. */
    public int getTotalPlaceholderEntriesHeight() {
        return this.placeholderMenuEntryHeight * this.placeholderMenuEntries.size();
    }

    /** Returns total placeholder entries width. */
    public int getTotalPlaceholderEntriesWidth() {
        int i = this.getPlaceholderAreaWidth();
        for (PlaceholderMenuEntry e : this.placeholderMenuEntries) {
            if (e.getWidth() > i) {
                i = e.getWidth();
            }
        }
        return i;
    }

    /** Returns placeholder entries render offset x. */
    public int getPlaceholderEntriesRenderOffsetX() {
        int totalScrollWidth = Math.max(0, this.getTotalPlaceholderEntriesWidth() - this.getPlaceholderAreaWidth());
        return -(int)(((float)totalScrollWidth / 100.0F) * (this.horizontalScrollBarPlaceholderMenu.getScroll() * 100.0F));
    }

    /** Returns placeholder entries render offset y. */
    public int getPlaceholderEntriesRenderOffsetY() {
        int totalScrollHeight = Math.max(0, this.getTotalPlaceholderEntriesHeight() - (this.getPlaceholderAreaHeight()));
        return -(int)(((float)totalScrollHeight / 100.0F) * (this.verticalScrollBarPlaceholderMenu.getScroll() * 100.0F));
    }

    /** Returns whether a placeholder matches the current search query. */
    protected boolean placeholderFitsSearchValue(@NotNull Placeholder placeholder, @Nullable String s) {
        if ((s == null) || s.isBlank()) return true;
        s = s.toLowerCase();
        if (placeholder.getDisplayName().toLowerCase().contains(s)) return true;
        if (placeholder.getIdentifier().toLowerCase().contains(s)) return true;
        return this.placeholderDescriptionContains(placeholder, s);
    }

    /** Returns whether a placeholder description contains the normalized query. */
    protected boolean placeholderDescriptionContains(@NotNull Placeholder placeholder, @NotNull String s) {
        List<String> desc = Objects.requireNonNullElse(placeholder.getDescription(), new ArrayList<>());
        for (String line : desc) {
            if (line.toLowerCase().contains(s)) return true;
        }
        return false;
    }

    /** Refreshes placeholder entries from current state. */
    public void updatePlaceholderEntries(@Nullable String category, boolean clearList, boolean addBackButton) {

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;

        if (clearList || (searchValue != null)) {
            this.placeholderMenuEntries.clear();
        }

        if (searchValue != null) {
            List<Placeholder> placeholders = PlaceholderRegistry.getPlaceholders();
            placeholders.sort(PLACEHOLDER_DISPLAY_NAME_COMPARATOR);
            for (Placeholder p : placeholders) {
                if (!this.placeholderFitsSearchValue(p, searchValue)) continue;
                PlaceholderMenuEntry entry = new PlaceholderMenuEntry(this, Component.literal(p.getDisplayName()), () -> {
                    this.history.saveSnapshot();
                    this.pasteText(p.getDefaultPlaceholderString().toString());
                });
                List<String> desc = p.getDescription();
                if (desc != null) {
                    entry.setDescription(desc.toArray(new String[0]));
                }
                entry.dotColor = this.placeholderEntryDotColorPlaceholder.get().getColor();
                entry.entryLabelColor = this.placeholderEntryLabelColor.get().getColor();
                this.placeholderMenuEntries.add(entry);
            }
            for (PlaceholderMenuEntry e : this.placeholderMenuEntries) {
                e.backgroundColorIdle = this.placeholderEntryBackgroundColorIdle.get().getColor();
                e.backgroundColorHover = this.placeholderEntryBackgroundColorHover.get().getColor();
            }
            this.verticalScrollBarPlaceholderMenu.setScroll(0.0F);
            this.horizontalScrollBarPlaceholderMenu.setScroll(0.0F);
            return;
        }

        Map<String, List<Placeholder>> categories = this.getPlaceholdersOrderedByCategories();
        if (!categories.isEmpty()) {
            List<Placeholder> otherCategory = categories.get(I18n.get("konkrete.requirements.categories.other"));
            if (otherCategory != null) {

                if (category == null) {

                    //Add category entries
                    for (Map.Entry<String, List<Placeholder>> m : categories.entrySet()) {
                        if (m.getValue() != otherCategory) {
                            PlaceholderMenuEntry entry = new PlaceholderMenuEntry(this, Component.literal(m.getKey()), () -> {
                                this.updatePlaceholderEntries(m.getKey(), true, true);
                            });
                            entry.dotColor = this.placeholderEntryDotColorCategory.get().getColor();
                            entry.entryLabelColor = this.placeholderEntryLabelColor.get().getColor();
                            this.placeholderMenuEntries.add(entry);
                        }
                    }
                    //Add placeholder entries of the "Other" category to the end of the categories list (because other = no category)
                    this.updatePlaceholderEntries(I18n.get("konkrete.requirements.categories.other"), false, false);

                } else {

                    if (addBackButton) {
                        PlaceholderMenuEntry backToCategoriesEntry = new PlaceholderMenuEntry(this, Component.literal(I18n.get("konkrete.ui.text_editor.placeholders.back_to_categories")), () -> {
                            this.updatePlaceholderEntries(null, true, true);
                        });
                        backToCategoriesEntry.dotColor = this.placeholderEntryDotColorCategory.get().getColor();
                        backToCategoriesEntry.entryLabelColor = this.placeholderEntryBackToCategoriesLabelColor.get().getColor();
                        this.placeholderMenuEntries.add(backToCategoriesEntry);
                    }

                    List<Placeholder> placeholders = categories.get(category);
                    if (placeholders != null) {
                        for (Placeholder p : placeholders) {
                            PlaceholderMenuEntry entry = new PlaceholderMenuEntry(this, Component.literal(p.getDisplayName()), () -> {
                                this.history.saveSnapshot();
                                this.pasteText(p.getDefaultPlaceholderString().toString());
                            });
                            List<String> desc = p.getDescription();
                            if (desc != null) {
                                entry.setDescription(desc.toArray(new String[0]));
                            }
                            entry.dotColor = this.placeholderEntryDotColorPlaceholder.get().getColor();
                            entry.entryLabelColor = this.placeholderEntryLabelColor.get().getColor();
                            this.placeholderMenuEntries.add(entry);
                        }
                    }

                }

                for (PlaceholderMenuEntry e : this.placeholderMenuEntries) {
                    e.backgroundColorIdle = this.placeholderEntryBackgroundColorIdle.get().getColor();
                    e.backgroundColorHover = this.placeholderEntryBackgroundColorHover.get().getColor();
                }

                this.verticalScrollBarPlaceholderMenu.setScroll(0.0F);
                this.horizontalScrollBarPlaceholderMenu.setScroll(0.0F);
            }
        }

    }

    /** Refreshes placeholders list from current state. */
    protected void updatePlaceholdersList() {
        this.updatePlaceholderEntries(null, true, false);
    }

    /** Returns placeholders ordered by categories. */
    protected Map<String, List<Placeholder>> getPlaceholdersOrderedByCategories() {
        //Build lists of all placeholders ordered by categories
        Map<String, List<Placeholder>> categories = new LinkedHashMap<>();
        for (Placeholder p : PlaceholderRegistry.getPlaceholders()) {
            String cat = p.getCategory();
            if (cat == null) {
                cat = I18n.get("konkrete.requirements.categories.other");
            }
            List<Placeholder> l = categories.computeIfAbsent(cat, k -> new ArrayList<>());
            l.add(p);
        }
        categories.values().forEach(list -> list.sort(PLACEHOLDER_DISPLAY_NAME_COMPARATOR));
        String otherKey = I18n.get("konkrete.requirements.categories.other");
        List<String> sortedKeys = new ArrayList<>(categories.keySet());
        boolean hasOther = sortedKeys.remove(otherKey);
        sortedKeys.sort(PLACEHOLDER_CATEGORY_COMPARATOR);
        Map<String, List<Placeholder>> sortedCategories = new LinkedHashMap<>();
        for (String key : sortedKeys) {
            sortedCategories.put(key, categories.get(key));
        }
        if (hasOther) {
            sortedCategories.put(otherKey, categories.get(otherKey));
        }
        return sortedCategories;
    }

    /** Renders line number background into the active GUI extraction pass. */
    protected void renderLineNumberBackground(GuiGraphicsExtractor graphics, float partial) {
        int width = this.getLineNumberSidebarWidth();
        if (width <= 0) {
            return;
        }
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        float x = this.getLineNumberSidebarX();
        float y = this.getEditorAreaY();
        int height = this.getEditorAreaHeight();
        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, x, y, width, height, radius, radius, radius, radius, this.lineNumberSideBarColor.get().getColorInt(), partial);
        SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(graphics, x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 1.0F, radius, radius, radius, radius, this.areaBorderColor.get().getColorInt(), partial);
    }

    /** Renders line number into the active GUI extraction pass. */
    protected void renderLineNumber(GuiGraphicsExtractor graphics, TextEditorLine line) {
        String lineNumberString = "" + (line.lineIndex+1);
        int lineNumberWidth = Math.round(UIBase.getUITextWidth(lineNumberString));
        int lineNumberX = this.getLineNumberSidebarRight() - 3 - lineNumberWidth;
        float lineNumberY = line.getY() + (line.getHeight() / 2F) - (UIBase.getUITextHeightNormal() / 2F);
        UIBase.renderText(graphics, lineNumberString, lineNumberX, lineNumberY, line.isFocused() ? this.lineNumberTextColorFocused.get().getColorInt() : this.lineNumberTextColorNormal.get().getColorInt());
    }

    /** Returns line number sidebar x. */
    protected int getLineNumberSidebarX() {
        return Math.max(0, this.lineNumberSidebarGapLeft);
    }

    /** Returns line number sidebar width. */
    protected int getLineNumberSidebarWidth() {
        return Math.max(0, this.borderLeft - this.lineNumberSidebarGapLeft - this.lineNumberSidebarGapRight);
    }

    /** Returns line number sidebar right. */
    protected int getLineNumberSidebarRight() {
        return this.getLineNumberSidebarX() + this.getLineNumberSidebarWidth();
    }

    /** Renders editor area background into the active GUI extraction pass. */
    protected void renderEditorAreaBackground(GuiGraphicsExtractor graphics, float partial) {
        float editorAreaRadius = UIBase.getInterfaceCornerRoundingRadius();
        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, this.getEditorAreaX(), this.getEditorAreaY(), this.getEditorAreaWidth(), this.getEditorAreaHeight(), editorAreaRadius, editorAreaRadius, editorAreaRadius, editorAreaRadius, this.areaBackgroundColor.get().getColorInt(), partial);
    }

    /** Renders editor area border into the active GUI extraction pass. */
    protected void renderEditorAreaBorder(GuiGraphicsExtractor graphics, float partial) {
        float editorAreaRadius = UIBase.getInterfaceCornerRoundingRadius();
        SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(graphics, this.getEditorAreaX() - 1, this.getEditorAreaY() - 1, this.getEditorAreaWidth() + 2, this.getEditorAreaHeight() + 2, 1.0F, editorAreaRadius, editorAreaRadius, editorAreaRadius, editorAreaRadius, this.areaBorderColor.get().getColorInt(), partial);
    }

    /** Advances mouse highlighting by one client tick. */
    protected void tickMouseHighlighting(double mouseX, double mouseY, boolean allowAutoScroll) {

        if (!this.isInMouseHighlightingMode()) {
            return;
        }

        //Auto-scroll if mouse outside editor area and in mouse-highlighting mode
        if (allowAutoScroll) {
            float speedMult = 0.008F;
            if (mouseX < this.borderLeft) {
                float f = Math.max(0.01F, (float)(this.borderLeft - mouseX) * speedMult);
                this.horizontalScrollBar.setScroll(this.horizontalScrollBar.getScroll() - f);
            } else if (mouseX > (this.getEditorAreaX() + this.getEditorAreaWidth())) {
                float f = Math.max(0.01F, (float)(mouseX - (this.getEditorAreaX() + this.getEditorAreaWidth())) * speedMult);
                this.horizontalScrollBar.setScroll(this.horizontalScrollBar.getScroll() + f);
            }
            if (mouseY < this.headerHeight) {
                float f = Math.max(0.01F, (float)(this.headerHeight - mouseY) * speedMult);
                this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() - f);
            } else if (mouseY > (this.height - this.footerHeight)) {
                float f = Math.max(0.01F, (float)(mouseY - (this.height - this.footerHeight)) * speedMult);
                this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() + f);
            }
        }

        // Pointer capture must keep extending the selection while the pointer is outside the editor.
        // Clamp only the selection endpoint; the original coordinates above still control auto-scroll speed.
        double selectionMouseX = Mth.clamp(mouseX, this.borderLeft, this.getEditorAreaX() + this.getEditorAreaWidth());
        double selectionMouseY = Mth.clamp(mouseY, this.headerHeight, this.height - this.footerHeight);

        TextEditorLine first = this.startHighlightLine;
        // Resolve against the clipped vertical intervals instead of raw widget hit boxes. This keeps selection
        // out of fully clipped lines at the bottom edge and also supports empty horizontal editor space.
        TextEditorLine hovered = this.getClosestVisibleLineAtY(selectionMouseY);
        if ((hovered != null) && !hovered.isFocused() && (first != null)) {

            int firstIndex = this.getLineIndex(first);
            int hoveredIndex = this.getLineIndex(hovered);
            boolean firstIsBeforeHovered = hoveredIndex > firstIndex;
            boolean firstIsAfterHovered = hoveredIndex < firstIndex;

            if (first.isInMouseHighlightingMode) {
                if (firstIsAfterHovered) {
                    this.setFocusedLine(this.getLineIndex(hovered));
                    if (!hovered.isInMouseHighlightingMode) {
                        hovered.isInMouseHighlightingMode = true;
                        hovered.moveCursorTo(hovered.getValue().length(), false);
                    }
                } else if (firstIsBeforeHovered) {
                    this.setFocusedLine(this.getLineIndex(hovered));
                    if (!hovered.isInMouseHighlightingMode) {
                        hovered.isInMouseHighlightingMode = true;
                        hovered.moveCursorTo(0, false);
                    }
                } else if (first == hovered) {
                    this.setFocusedLine(this.getLineIndex(first));
                }
            }

            int startIndex = Math.min(hoveredIndex, firstIndex);
            int endIndex = Math.max(hoveredIndex, firstIndex);
            int index = 0;
            for (TextEditorLine t : this.textFieldLines) {
                //Highlight all lines between the first and current focusedLineIndex and remove highlighting from lines outside of highlight range
                if ((t != hovered) && (t != first)) {
                    if ((index > startIndex) && (index < endIndex)) {
                        if (firstIsAfterHovered) {
                            t.setCursorPosition(0);
                            t.setHighlightPos(t.getValue().length());
                        } else if (firstIsBeforeHovered) {
                            t.setCursorPosition(t.getValue().length());
                            t.setHighlightPos(0);
                        }
                    } else {
                        t.moveCursorTo(0, false);
                        t.isInMouseHighlightingMode = false;
                    }
                }
                index++;
            }
            this.startHighlightLineIndex = startIndex;
            this.endHighlightLineIndex = endIndex;

            if (first != hovered) {
                if (firstIsAfterHovered) {
                    first.moveCursorTo(0, true);
                } else if (firstIsBeforeHovered) {
                    first.moveCursorTo(first.getValue().length(), true);
                }
            }

        }

        TextEditorLine focused = this.getFocusedLine();
        if ((focused != null) && focused.isInMouseHighlightingMode) {
            if ((this.startHighlightLineIndex == -1) && (this.endHighlightLineIndex == -1)) {
                this.startHighlightLineIndex = this.getLineIndex(focused);
                this.endHighlightLineIndex = this.startHighlightLineIndex;
            }
            int cursorPos = this.getCursorPosFromMouseX(focused, selectionMouseX);
            focused.moveCursorTo(cursorPos, true);
            if ((focused.getAsAccessor().get_highlightPos_Konkrete() == focused.getCursorPosition()) && (this.startHighlightLineIndex == this.endHighlightLineIndex)) {
                this.resetHighlighting();
            }
        }

    }

    /** Refreshes lines from current state. */
    public void updateLines(@Nullable Consumer<TextEditorLine> doAfterEachLineUpdate) {
        try {
            int index = 0;
            for (TextEditorLine line : this.textFieldLines) {
                line.lineIndex = index;
                line.setY(this.headerHeight + (this.lineHeight * index) + this.getLineRenderOffsetY());
                line.setX(this.borderLeft + this.getLineRenderOffsetX());
                line.setWidth(this.currentLineWidth);
                ((AccessorMixinAbstractWidget)line).set_height_Konkrete(this.lineHeight);
                line.getAsAccessor().set_displayPos_Konkrete(0);
                if (doAfterEachLineUpdate != null) {
                    doAfterEachLineUpdate.accept(line);
                }
                index++;
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCMYENU] Failed to update lines!", ex);
        }
    }

    /** Refreshes current line width from current state. */
    public void updateCurrentLineWidth() {
        //Find width of the longest focusedLineIndex and update current focusedLineIndex width
        int longestTextWidth = 0;
        for (TextEditorLine f : this.textFieldLines) {
            if (f.textWidth > longestTextWidth) {
                //Calculating the text size for every focusedLineIndex every tick kills the CPU, so I'm calculating the size on value change in the text box
                longestTextWidth = f.textWidth;
            }
        }
        this.currentLineWidth = longestTextWidth + 30;
    }

    /** Returns line render offset x. */
    public int getLineRenderOffsetX() {
        return -(int)(((float)this.getTotalScrollWidth() / 100.0F) * (this.horizontalScrollBar.getScroll() * 100.0F));
    }

    /** Returns line render offset y. */
    public int getLineRenderOffsetY() {
        return -(int)(((float)this.getTotalScrollHeight() / 100.0F) * (this.verticalScrollBar.getScroll() * 100.0F));
    }

    /** Returns total line height. */
    public int getTotalLineHeight() {
        return this.lineHeight * this.textFieldLines.size();
    }

    /** Adds line at index to this text editor window body. */
    @Nullable
    public TextEditorLine addLineAtIndex(int index) {
        TextEditorLine f = new TextEditorLine(Minecraft.getInstance().font, 0, 0, 50, this.lineHeight, false, this.characterFilter, this);
        f.setMaxLength(Integer.MAX_VALUE);
        f.lineIndex = index;
        if (index > 0) {
            TextEditorLine before = this.getLine(index-1);
            if (before != null) {
                f.setY(before.getY() + this.lineHeight);
            }
        }
        this.textFieldLines.add(index, f);
        return f;
    }

    /** Adds line to this text editor window body. */
    @Nullable
    public TextEditorLine addLine() {
        return this.addLineAtIndex(this.getLineCount());
    }

    /** Removes line at index from this text editor window body. */
    public void removeLineAtIndex(int index) {
        if (index < 1) {
            return;
        }
        if (index <= this.getLineCount()-1) {
            this.textFieldLines.remove(index);
        }
    }

    /** Removes last line from this text editor window body. */
    public void removeLastLine() {
        this.removeLineAtIndex(this.getLineCount()-1);
    }

    /** Returns line count. */
    public int getLineCount() {
        return this.textFieldLines.size();
    }

    /** Returns line. */
    @Nullable
    public TextEditorLine getLine(int index) {
        return this.textFieldLines.get(index);
    }

    /** Sets focused line for this text editor window body. */
    public void setFocusedLine(int index) {
        if (index <= this.getLineCount()-1) {
            for (TextEditorLine f : this.textFieldLines) {
                f.setFocused(false);
            }
            this.getLine(index).setFocused(true);
        }
    }

    /**
     * Returns the cursorPos of the focused focusedLineIndex or -1 if no focusedLineIndex is focused.
     **/
    public int getFocusedLineIndex() {
        int index = 0;
        for (TextEditorLine f : this.textFieldLines) {
            if (f.isFocused()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /** Returns focused line. */
    @Nullable
    public TextEditorLine getFocusedLine() {
        int index = this.getFocusedLineIndex();
        if (index != -1) {
            return this.getLine(index);
        }
        return null;
    }

    /** Returns whether line focused. */
    public boolean isLineFocused() {
        return (this.getFocusedLineIndex() > -1);
    }

    /** Returns line after. */
    @Nullable
    public TextEditorLine getLineAfter(TextEditorLine line) {
        int index = this.getLineIndex(line);
        if ((index > -1) && (index < (this.getLineCount()-1))) {
            return this.getLine(index+1);
        }
        return null;
    }

    /** Returns line before. */
    @Nullable
    public TextEditorLine getLineBefore(TextEditorLine line) {
        int index = this.getLineIndex(line);
        if (index > 0) {
            return this.getLine(index-1);
        }
        return null;
    }

    /** Returns lines between indexes. */
    @Nullable
    public List<TextEditorLine> getLinesBetweenIndexes(int startIndex, int endIndex) {
        startIndex = Math.min(Math.max(startIndex, 0), this.textFieldLines.size()-1);
        endIndex = Math.min(Math.max(endIndex, 0), this.textFieldLines.size()-1);
        List<TextEditorLine> l = new ArrayList<>(this.textFieldLines.subList(startIndex, endIndex));
        if (!l.isEmpty()) {
            l.remove(0);
        }
        return l;
    }

    /** Returns hovered line. */
    @Nullable
    public TextEditorLine getHoveredLine() {
        return this.getHoveredLine(this.getRenderMouseX(), this.getRenderMouseY());
    }

    /** Returns hovered line. */
    @Nullable
    public TextEditorLine getHoveredLine(double mouseX, double mouseY) {
        for (TextEditorLine t : this.textFieldLines) {
            if (t.isMouseOver(mouseX, mouseY)) {
                return t;
            }
        }
        return null;
    }

    /** Returns closest visible line at y. */
    @Nullable
    protected TextEditorLine getClosestVisibleLineAtY(double mouseY) {
        TextEditorLine closest = null;
        double closestDistance = Double.MAX_VALUE;
        int editorTop = this.getEditorAreaY();
        int editorBottom = editorTop + this.getEditorAreaHeight();
        for (TextEditorLine line : this.textFieldLines) {
            int visibleTop = Math.max(line.getY(), editorTop);
            int visibleBottom = Math.min(line.getY() + line.getHeight(), editorBottom);
            if (visibleBottom <= visibleTop) {
                continue;
            }
            // Widget hit boxes use a half-open vertical range. Resolve containment first so a shared boundary belongs
            // to the following line instead of tying at distance zero and incorrectly retaining the previous line.
            if ((mouseY >= visibleTop) && (mouseY < visibleBottom)) {
                return line;
            }
            double distance = mouseY < visibleTop ? visibleTop - mouseY : mouseY - visibleBottom;
            if (distance < closestDistance) {
                closest = line;
                closestDistance = distance;
            }
        }
        return closest;
    }

    /** Returns line index. */
    public int getLineIndex(TextEditorLine inputBox) {
        return this.textFieldLines.indexOf(inputBox);
    }

    /** Moves the selection or viewport to up line. */
    public void goUpLine() {
        if (this.isLineFocused()) {
            int current = Math.max(0, this.getFocusedLineIndex());
            if (current > 0) {
                TextEditorLine currentLine = this.getLine(current);
                this.setFocusedLine(current - 1);
                if (currentLine != null) {
                    Objects.requireNonNull(this.getFocusedLine()).moveCursorTo(this.lastCursorPosSetByUser, false);
                }
            } else {
                TextEditorLine currentLine = this.getLine(current);
                if (currentLine != null) {
                    currentLine.moveCursorTo(0, false);
                }
            }
        }
    }

    /** Moves the selection or viewport to down line. */
    public void goDownLine(boolean isNewLine) {
        if (this.isLineFocused()) {
            int current = Math.max(0, this.getFocusedLineIndex());
            if (!isNewLine && (current >= this.getLineCount() - 1)) {
                TextEditorLine currentLine = this.getLine(current);
                if (currentLine != null) {
                    currentLine.moveCursorToEnd(false);
                }
                return;
            }
            if (isNewLine) {
                this.addLineAtIndex(current+1);
            }
            TextEditorLine currentLine = this.getLine(current);
            this.setFocusedLine(current+1);
            if (currentLine != null) {
                TextEditorLine nextLine = this.getFocusedLine();
                if (nextLine == null) return;
                if (isNewLine) {
                    //Split content of currentLine at cursor pos and move text after cursor to next focusedLineIndex if ENTER was pressed
                    String textBeforeCursor = currentLine.getValue().substring(0, currentLine.getCursorPosition());
                    String textAfterCursor = currentLine.getValue().substring(currentLine.getCursorPosition());
                    currentLine.setValue(textBeforeCursor);
                    nextLine.setValue(textAfterCursor);
                    nextLine.moveCursorTo(0, false);

                    //Add indentation of the old line to the new line
                    Matcher matcher = Pattern.compile("^(\\s+)").matcher(textBeforeCursor);
                    if (matcher.find()) {
                        String whitespace = matcher.group(1);
                        nextLine.setValue(whitespace + nextLine.getValue());
                        nextLine.moveCursorTo(whitespace.length(), false);
                    }
                } else {
                    nextLine.moveCursorTo(this.lastCursorPosSetByUser, false);
                }
            }
        }
    }

    /** Returns copy of lines. */
    public List<TextEditorLine> getCopyOfLines() {
        List<TextEditorLine> l = new ArrayList<>();
        for (TextEditorLine t : this.textFieldLines) {
            TextEditorLine n = new TextEditorLine(this.font, 0, 0, 0, 0, false, this.characterFilter, this);
            n.setValue(t.getValue());
            n.setFocused(t.isFocused());
            n.moveCursorTo(t.getCursorPosition(), false);
            l.add(n);
        }
        return l;
    }

    /**
     * Don't use this if you don't know what you do!<br>
     * For a safe way to get all lines, use {@link TextEditorWindowBody#getCopyOfLines()} instead.
     */
    public List<TextEditorLine> getLines() {
        return this.textFieldLines;
    }

    /** Returns whether text highlighted. */
    public boolean isTextHighlighted() {
        return (this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1);
    }

    /** Returns whether highlighted text hovered. */
    public boolean isHighlightedTextHovered() {
        if (this.isTextHighlighted()) {
            List<TextEditorLine> highlightedLines = new ArrayList<>();
            if (this.endHighlightLineIndex <= this.getLineCount()-1) {
                highlightedLines.addAll(this.textFieldLines.subList(this.startHighlightLineIndex, this.endHighlightLineIndex+1));
            }
            for (TextEditorLine t : highlightedLines) {
                if (t.isHighlightedHovered()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns highlighted text. */
    @NotNull
    public String getHighlightedText() {
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                List<TextEditorLine> lines = new ArrayList<>();
                lines.add(this.getLine(this.startHighlightLineIndex));
                if (this.startHighlightLineIndex != this.endHighlightLineIndex) {
                    lines.addAll(Objects.requireNonNull(this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex)));
                    lines.add(this.getLine(this.endHighlightLineIndex));
                }
                StringBuilder s = new StringBuilder();
                boolean b = false;
                for (TextEditorLine t : lines) {
                    if (b) {
                        s.append("\n");
                    }
                    s.append(t.getHighlighted());
                    b = true;
                }
                return s.toString();
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to highlight text!", ex);
        }
        return "";
    }

    /** Removes highlighted text from the editable text. */
    @NotNull
    public String cutHighlightedText() {
        String highlighted = this.getHighlightedText();
        this.deleteHighlightedText();
        return highlighted;
    }

    /** Removes highlighted text from the editable text. */
    public void deleteHighlightedText() {
        int linesRemoved = 0;
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                if (this.startHighlightLineIndex == this.endHighlightLineIndex) {
                    Objects.requireNonNull(this.getLine(this.startHighlightLineIndex)).insertText("");
                } else {
                    TextEditorLine start = this.getLine(this.startHighlightLineIndex);
                    if (start == null) return;
                    start.insertText("");
                    TextEditorLine end = this.getLine(this.endHighlightLineIndex);
                    if (end == null) return;
                    end.insertText("");
                    if ((this.endHighlightLineIndex - this.startHighlightLineIndex) > 1) {
                        for (TextEditorLine line : Objects.requireNonNull(this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex))) {
                            this.removeLineAtIndex(this.getLineIndex(line));
                            linesRemoved++;
                        }
                    }
                    String oldStartValue = start.getValue();
                    start.setCursorPosition(start.getValue().length());
                    start.setHighlightPos(start.getCursorPosition());
                    start.insertText(end.getValue());
                    start.setCursorPosition(oldStartValue.length());
                    start.setHighlightPos(start.getCursorPosition());
                    this.removeLineAtIndex(this.getLineIndex(end));
                    linesRemoved++;
                    this.setFocusedLine(this.startHighlightLineIndex);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to delete highlighted text!", ex);
        }
        this.correctYScroll(-linesRemoved);
        this.resetHighlighting();
    }

    /** Clears highlighting state. */
    public void resetHighlighting() {
        this.startHighlightLineIndex = -1;
        this.endHighlightLineIndex = -1;
        this.keyboardHighlightAnchorLineIndex = -1;
        this.keyboardHighlightAnchorCursorPos = -1;
        for (TextEditorLine t : this.textFieldLines) {
            t.setHighlightPos(t.getCursorPosition());
        }
    }

    /** Captures keyboard highlight anchor from current state. */
    protected void captureKeyboardHighlightAnchor() {
        if (!this.isLineFocused()) {
            return;
        }
        if ((this.keyboardHighlightAnchorLineIndex >= 0) && (this.keyboardHighlightAnchorLineIndex < this.getLineCount())) {
            return;
        }
        TextEditorLine focusedLine = this.getFocusedLine();
        if (focusedLine == null) {
            return;
        }
        int focusedLineIndex = this.getFocusedLineIndex();
        if (this.isTextHighlighted()) {
            if (focusedLineIndex == this.startHighlightLineIndex && (this.startHighlightLineIndex != this.endHighlightLineIndex)) {
                TextEditorLine anchorLine = this.getLine(this.endHighlightLineIndex);
                if (anchorLine != null) {
                    this.keyboardHighlightAnchorLineIndex = this.endHighlightLineIndex;
                    this.keyboardHighlightAnchorCursorPos = anchorLine.getAsAccessor().get_highlightPos_Konkrete();
                    return;
                }
            }
            if (focusedLineIndex == this.endHighlightLineIndex && (this.startHighlightLineIndex != this.endHighlightLineIndex)) {
                TextEditorLine anchorLine = this.getLine(this.startHighlightLineIndex);
                if (anchorLine != null) {
                    this.keyboardHighlightAnchorLineIndex = this.startHighlightLineIndex;
                    this.keyboardHighlightAnchorCursorPos = anchorLine.getAsAccessor().get_highlightPos_Konkrete();
                    return;
                }
            }
            if (focusedLine.getCursorPosition() != focusedLine.getAsAccessor().get_highlightPos_Konkrete()) {
                this.keyboardHighlightAnchorLineIndex = focusedLineIndex;
                this.keyboardHighlightAnchorCursorPos = focusedLine.getAsAccessor().get_highlightPos_Konkrete();
                return;
            }
        }
        this.keyboardHighlightAnchorLineIndex = focusedLineIndex;
        this.keyboardHighlightAnchorCursorPos = focusedLine.getCursorPosition();
    }

    /** Refreshes keyboard highlight from anchor from current state. */
    protected void syncKeyboardHighlightFromAnchor() {
        if (!this.isLineFocused()) {
            this.resetHighlighting();
            return;
        }
        if ((this.keyboardHighlightAnchorLineIndex < 0) || (this.keyboardHighlightAnchorLineIndex >= this.getLineCount())) {
            this.resetHighlighting();
            return;
        }
        TextEditorLine focusedLine = this.getFocusedLine();
        TextEditorLine anchorLine = this.getLine(this.keyboardHighlightAnchorLineIndex);
        if ((focusedLine == null) || (anchorLine == null)) {
            this.resetHighlighting();
            return;
        }

        int focusedLineIndex = this.getFocusedLineIndex();
        int focusedCursorPos = focusedLine.getCursorPosition();
        int anchorCursorPos = Mth.clamp(this.keyboardHighlightAnchorCursorPos, 0, anchorLine.getValue().length());
        if ((focusedLineIndex == this.keyboardHighlightAnchorLineIndex) && (focusedCursorPos == anchorCursorPos)) {
            this.resetHighlighting();
            return;
        }

        this.startHighlightLineIndex = Math.min(this.keyboardHighlightAnchorLineIndex, focusedLineIndex);
        this.endHighlightLineIndex = Math.max(this.keyboardHighlightAnchorLineIndex, focusedLineIndex);

        for (int i = 0; i < this.getLineCount(); i++) {
            TextEditorLine line = this.getLine(i);
            if (line == null) {
                continue;
            }
            if ((i < this.startHighlightLineIndex) || (i > this.endHighlightLineIndex)) {
                line.setHighlightPos(line.getCursorPosition());
                continue;
            }
            if (this.keyboardHighlightAnchorLineIndex == focusedLineIndex) {
                line.setHighlightPos(anchorCursorPos);
                continue;
            }
            if (this.keyboardHighlightAnchorLineIndex < focusedLineIndex) {
                if (i == this.keyboardHighlightAnchorLineIndex) {
                    line.setCursorPosition(line.getValue().length());
                    line.setHighlightPos(anchorCursorPos);
                } else if (i == focusedLineIndex) {
                    line.setHighlightPos(0);
                } else {
                    line.setCursorPosition(line.getValue().length());
                    line.setHighlightPos(0);
                }
            } else {
                if (i == this.keyboardHighlightAnchorLineIndex) {
                    line.setCursorPosition(0);
                    line.setHighlightPos(anchorCursorPos);
                } else if (i == focusedLineIndex) {
                    line.setHighlightPos(line.getValue().length());
                } else {
                    line.setCursorPosition(0);
                    line.setHighlightPos(line.getValue().length());
                }
            }
        }
    }

    /** Returns whether in mouse highlighting mode. */
    public boolean isInMouseHighlightingMode() {
        return this.startHighlightLine != null;
    }

    /**
     * The editor lines are managed manually instead of being Screen children, so the editor must own
     * their pointer capture instead of relying on vanilla's focused-child drag state.
     */
    protected void startMouseHighlighting(@NotNull TextEditorLine line) {
        this.startHighlightLine = line;
        line.isInMouseHighlightingMode = true;
    }

    /** Stops mouse highlighting and releases its active resources. */
    protected void stopMouseHighlighting() {
        this.startHighlightLine = null;
        for (TextEditorLine line : this.textFieldLines) {
            line.isInMouseHighlightingMode = false;
        }
    }

    /**
     * A captured PiP release can be intentionally dropped when the window becomes hidden, locked, or
     * loses focus to a forced window. The raw GLFW state remains authoritative in all of those paths.
     */
    protected void validateMouseHighlightingCapture() {
        if (!this.isInMouseHighlightingMode()) {
            return;
        }
        PiPWindow window = this.getWindow();
        boolean windowOwnsCapture = window == null || (window.isVisible() && !window.isInputLocked() && PiPWindowHandler.INSTANCE.isWindowFocused(window));
        boolean leftMouseDown = GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!windowOwnsCapture || !leftMouseDown) {
            this.stopMouseHighlighting();
        }
    }

    /** Inserts text at the current edit position. */
    public void pasteText(String text) {
        try {
            if ((text != null) && !text.isEmpty()) {
                int addedLinesCount = 0;
                if (this.isTextHighlighted()) {
                    this.deleteHighlightedText();
                }
                if (!this.isLineFocused()) {
                    this.setFocusedLine(this.getLineCount()-1);
                    Objects.requireNonNull(this.getFocusedLine()).moveCursorToEnd(false);
                }
                TextEditorLine focusedLine = this.getFocusedLine();
                //These two strings are for correctly pasting text within a char sequence (if the cursor is not at the end or beginning of the focusedLineIndex)
                String textBeforeCursor = "";
                String textAfterCursor = "";
                if (!focusedLine.getValue().isEmpty()) {
                    textBeforeCursor = focusedLine.getValue().substring(0, focusedLine.getCursorPosition());
                    if (focusedLine.getCursorPosition() < focusedLine.getValue().length()) {
                        textAfterCursor = this.getFocusedLine().getValue().substring(focusedLine.getCursorPosition(), focusedLine.getValue().length());
                    }
                }
                focusedLine.setValue(textBeforeCursor);
                focusedLine.setCursorPosition(textBeforeCursor.length());
                String[] lines = new String[]{text};
                if (text.contains("\n")) {
                    lines = text.split("\n", -1);
                }
                Array.set(lines, lines.length-1, lines[lines.length-1] + textAfterCursor);
                if (lines.length == 1) {
                    this.getFocusedLine().insertText(lines[0]);
                } else if (lines.length > 1) {
                    int index = -1;
                    for (String s : lines) {
                        if (index == -1) {
                            index = this.getFocusedLineIndex();
                        } else {
                            this.addLineAtIndex(index);
                            addedLinesCount++;
                        }
                        Objects.requireNonNull(this.getLine(index)).insertText(s);
                        index++;
                    }
                    this.setFocusedLine(index - 1);
                    this.getFocusedLine().setCursorPosition(Math.max(0, this.getFocusedLine().getValue().length() - textAfterCursor.length()));
                    this.getFocusedLine().setHighlightPos(this.getFocusedLine().getCursorPosition());
                }
                this.correctYScroll(addedLinesCount);
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to paste text!", ex);
        }
        if (this.indentGuideRenderer != null) {
            this.indentGuideRenderer.markDirty();
        }
        this.resetHighlighting();
    }

    /** Sets text for this text editor window body. */
    public TextEditorWindowBody setText(@Nullable String text) {
        if (text == null) text = "";
        text = text.replace(NEWLINE_CODE, "\n").replace(SPACE_CODE, " ");
        TextEditorLine t = Objects.requireNonNull(this.getLine(0));
        this.textFieldLines.clear();
        this.textFieldLines.add(t);
        this.setFocusedLine(0);
        t.setValue("");
        t.moveCursorTo(0, false);
        this.pasteText(text);
        this.setFocusedLine(0);
        t.moveCursorTo(0, false);
        this.verticalScrollBar.setScroll(0.0F);
        if (this.indentGuideRenderer != null) {
            this.indentGuideRenderer.markDirty();
        }
        return this;
    }

    /** Returns text. */
    @NotNull
    public String getText() {
        StringBuilder s = new StringBuilder();
        boolean notFirstLine = false;
        for (TextEditorLine t : this.textFieldLines) {
            String value = t.getValue();
            if (notFirstLine) {
                s.append("\n");
                if (!this.multilineMode) {
                    // Replace all leading spaces with SPACE_CODE
                    Pattern pattern = Pattern.compile("^( +)");
                    Matcher matcher = pattern.matcher(value);
                    if (matcher.find()) {
                        String replacement = matcher.group().replace(" ", SPACE_CODE);
                        value = matcher.replaceFirst(replacement);
                    }
                }
            }
            s.append(value);
            notFirstLine = true;
        }
        String text = s.toString();
        return !this.multilineMode ? text.replace("\n", NEWLINE_CODE) : text;
    }

    /** Reports whether all configured validators accept the current text. */
    protected boolean isTextValid() {
        if (this.textValidator != null) return this.textValidator.get(this);
        return true;
    }

    /** Sets text validator for this text editor window body. */
    public TextEditorWindowBody setTextValidator(@Nullable ConsumingSupplier<TextEditorWindowBody, Boolean> textValidator) {
        this.textValidator = textValidator;
        return this;
    }

    /** Sets text validator user feedback for this text editor window body. */
    public TextEditorWindowBody setTextValidatorUserFeedback(@Nullable UITooltip feedback) {
        this.textValidatorFeedbackUITooltip = feedback;
        return this;
    }

    /** Returns whether allowed. */
    public boolean placeholdersAllowed() {
        return this.allowPlaceholders;
    }

    /** Sets placeholders allowed for this text editor window body. */
    public TextEditorWindowBody setPlaceholdersAllowed(boolean allowed) {
        this.allowPlaceholders = allowed;
        this.init();
        return this;
    }

    /** Returns whether multiline mode. */
    public boolean isMultilineMode() {
        return this.multilineMode;
    }

    /** Sets multiline mode for this text editor window body. */
    public TextEditorWindowBody setMultilineMode(boolean multilineMode) {
        this.multilineMode = multilineMode;
        return this;
    }

    /** Returns whether bold title. */
    public boolean isBoldTitle() {
        return this.boldTitle;
    }

    /** Sets bold title for this text editor window body. */
    public TextEditorWindowBody setBoldTitle(boolean boldTitle) {
        this.boldTitle = boldTitle;
        return this;
    }

    /**
     * @return The text BEFORE the cursor or NULL if no focusedLineIndex is focused.
     */
    @Nullable
    public String getTextBeforeCursor() {
        if (!this.isLineFocused()) {
            return null;
        }
        int focusedLineIndex = this.getFocusedLineIndex();
        List<TextEditorLine> lines = new ArrayList<>();
        if (focusedLineIndex == 0) {
            lines.add(this.getLine(0));
        } else if (focusedLineIndex > 0) {
            lines.addAll(this.textFieldLines.subList(0, focusedLineIndex+1));
        }
        TextEditorLine lastLine = lines.get(lines.size()-1);
        StringBuilder s = new StringBuilder();
        boolean b = false;
        for (TextEditorLine t : lines) {
            if (b) {
                s.append("\n");
            }
            if (t != lastLine) {
                s.append(t.getValue());
            } else {
                s.append(t.getValue(), 0, t.getCursorPosition());
            }
            b = true;
        }
        return s.toString();
    }

    /**
     * @return The text AFTER the cursor or NULL if no focusedLineIndex is focused.
     */
    @Nullable
    public String getTextAfterCursor() {
        if (!this.isLineFocused()) {
            return null;
        }
        int focusedLineIndex = this.getFocusedLineIndex();
        List<TextEditorLine> lines = new ArrayList<>();
        if (focusedLineIndex == this.getLineCount()-1) {
            lines.add(this.getLine(this.getLineCount()-1));
        } else if (focusedLineIndex < this.getLineCount()-1) {
            lines.addAll(this.textFieldLines.subList(focusedLineIndex, this.getLineCount()));
        }
        TextEditorLine firstLine = lines.get(0);
        StringBuilder s = new StringBuilder();
        boolean b = false;
        for (TextEditorLine t : lines) {
            if (b) {
                s.append("\n");
            }
            if (t != firstLine) {
                s.append(t.getValue());
            } else {
                s.append(t.getValue(), t.getCursorPosition(), t.getValue().length());
            }
            b = true;
        }
        return s.toString();
    }

    /** Routes typed character input and reports whether it was consumed. */
    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), 0);
    }

    /** Routes typed character input and reports whether it was consumed. */
    public boolean charTyped(char character, int modifiers) {

        if (this.indentGuideRenderer != null) {
            this.indentGuideRenderer.markDirty();
        }

        if (this.isGoToLineOpen && (this.goToLineField != null) && this.goToLineField.isFocused()) {
            return this.goToLineField.charTyped(character, modifiers);
        }

        if (this.placeholdersAllowed() && extendedPlaceholderMenu && (this.searchBar != null) && this.searchBar.isFocused()) {
            return this.searchBar.charTyped(character, modifiers);
        }

        if (this.isLineFocused()) {
            this.history.saveSnapshot();
        }

        for (TextEditorLine l : this.textFieldLines) {
            l.charTyped(character, modifiers);
        }

        return super.charTyped(new CharacterEvent(character));

    }


    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keycode, int scancode, int modifiers) {

        if (this.indentGuideRenderer != null) {
            this.indentGuideRenderer.markDirty();
        }

        if (this.isGoToLineOpen && (this.goToLineField != null)) {
            if (keycode == InputConstants.KEY_ESCAPE) {
                this.isGoToLineOpen = false;
                this.goToLineField.setFocused(false);
                return true;
            }
            if (keycode == InputConstants.KEY_ENTER) {
                try {
                    String val = this.goToLineField.getValue();
                    if (!val.isEmpty()) {
                        int line = Integer.parseInt(val);
                        line = Math.max(1, Math.min(line, this.getLineCount()));
                        this.setFocusedLine(line - 1);
                        this.correctYScroll(0);
                        TextEditorLine l = this.getFocusedLine();
                        if (l != null) l.moveCursorTo(0, false);
                    }
                } catch (Exception ignored) {}
                this.isGoToLineOpen = false;
                this.goToLineField.setFocused(false);
                return true;
            }
            if (this.goToLineField.keyPressed(keycode, scancode, modifiers)) return true;
        }

        //GUI shortcut modifier + G | GO TO LINE
        if (InputUtils.isGuiShortcutModifierDown(modifiers) && (keycode == GLFW.GLFW_KEY_G)) {
            this.isGoToLineOpen = !this.isGoToLineOpen;
            if (this.isGoToLineOpen) {
                this.goToLineField.setValue("");
                this.goToLineField.setFocused(true);
                this.setFocused(this.goToLineField);
            }
            return true;
        }

        if (this.placeholdersAllowed() && extendedPlaceholderMenu && (this.searchBar != null) && this.searchBar.isFocused()) {
            return this.searchBar.keyPressed(keycode, scancode, modifiers);
        }

        boolean isHorizontalArrow = (keycode == InputConstants.KEY_RIGHT) || (keycode == InputConstants.KEY_LEFT);
        boolean isVerticalArrow = (keycode == InputConstants.KEY_UP) || (keycode == InputConstants.KEY_DOWN);
        if ((net.minecraft.client.Minecraft.getInstance().hasShiftDown() && (isHorizontalArrow || isVerticalArrow)) && !this.isInMouseHighlightingMode()) {
            this.captureKeyboardHighlightAnchor();
        }

        for (TextEditorLine l : new ArrayList<>(this.textFieldLines)) {
            l.keyPressed(keycode, scancode, modifiers);
        }

        String key = GLFW.glfwGetKeyName(keycode, scancode);
        if (key == null) key = "";

        //GUI shortcut modifier + Z | STEP BACK
        if (InputUtils.isGuiShortcutModifierDown(modifiers) && (key.equals("z"))) {
            this.history.stepBack();
            return true;
        }
        //GUI shortcut modifier + Y | STEP FORWARD
        if (InputUtils.isGuiShortcutModifierDown(modifiers) && (key.equals("y"))) {
            this.history.stepForward();
            return true;
        }
        //GUI shortcut modifier + S | DONE
        if (InputUtils.isGuiShortcutModifierDown(modifiers) && (keycode == GLFW.GLFW_KEY_S)) {
            this.triggerDoneAction();
            return true;
        }
        //ALT + UP | MOVE LINE UP
        boolean altDown = InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
        if (altDown && ((keycode == InputConstants.KEY_UP) || (keycode == GLFW.GLFW_KEY_PAGE_UP))) {
            if (this.isLineFocused()) {
                int index = this.getFocusedLineIndex();
                if (index > 0) {
                    this.history.saveSnapshot();
                    TextEditorLine current = this.getLine(index);
                    TextEditorLine above = this.getLine(index - 1);
                    if ((current != null) && (above != null)) {
                        String currentVal = current.getValue();
                        String aboveVal = above.getValue();
                        current.setValue(aboveVal);
                        above.setValue(currentVal);
                        this.setFocusedLine(index - 1);
                        above.moveCursorTo(current.getCursorPosition(), false);
                        this.resetHighlighting();
                    }
                }
            }
            return true;
        }

        //ALT + DOWN | MOVE LINE DOWN
        if (altDown && ((keycode == InputConstants.KEY_DOWN) || (keycode == GLFW.GLFW_KEY_PAGE_DOWN))) {
            if (this.isLineFocused()) {
                int index = this.getFocusedLineIndex();
                if (index < this.getLineCount() - 1) {
                    this.history.saveSnapshot();
                    TextEditorLine current = this.getLine(index);
                    TextEditorLine below = this.getLine(index + 1);
                    if ((current != null) && (below != null)) {
                        String currentVal = current.getValue();
                        String belowVal = below.getValue();
                        current.setValue(belowVal);
                        below.setValue(currentVal);
                        this.setFocusedLine(index + 1);
                        below.moveCursorTo(current.getCursorPosition(), false);
                        this.resetHighlighting();
                    }
                }
            }
            return true;
        }

        //ENTER
        if (keycode == InputConstants.KEY_ENTER) {
            if (!this.isInMouseHighlightingMode()) {
                if (this.isLineFocused()) {
                    this.history.saveSnapshot();
                    this.resetHighlighting();
                    this.goDownLine(true);
                    this.correctYScroll(1);
                }
            }
            return true;
        }
        //ARROW UP
        if (keycode == InputConstants.KEY_UP) {
            if (!this.isInMouseHighlightingMode()) {
                this.goUpLine();
                if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
                    this.syncKeyboardHighlightFromAnchor();
                } else {
                    this.resetHighlighting();
                }
                this.correctYScroll(0);
            }
            return true;
        }
        //ARROW DOWN
        if (keycode == InputConstants.KEY_DOWN) {
            if (!this.isInMouseHighlightingMode()) {
                this.goDownLine(false);
                if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
                    this.syncKeyboardHighlightFromAnchor();
                } else {
                    this.resetHighlighting();
                }
                this.correctYScroll(0);
            }
            return true;
        }

        //BACKSPACE
        if (keycode == InputConstants.KEY_BACKSPACE) {
            if (!this.isInMouseHighlightingMode()) {
                if (this.isTextHighlighted()) {
                    this.history.saveSnapshot();
                    this.deleteHighlightedText();
                } else {
                    if (this.isLineFocused()) {
                        if (!this.getText().isEmpty()) this.history.saveSnapshot();
                        TextEditorLine focused = Objects.requireNonNull(this.getFocusedLine());
                        focused.deleteText(-1);
                    }
                }
                this.resetHighlighting();
            }
            return true;
        }
        //GUI shortcut modifier + C
        if (((keycode) == 67 && InputUtils.isGuiShortcutModifierDown(modifiers))) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlightedText());
            return true;
        }
        //GUI shortcut modifier + V
        if (((keycode) == 86 && InputUtils.isGuiShortcutModifierDown(modifiers))) {
            this.history.saveSnapshot();
            this.pasteText(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        //GUI shortcut modifier + A
        if (((keycode) == 65 && InputUtils.isGuiShortcutModifierDown(modifiers))) {
            for (TextEditorLine t : new ArrayList<>(this.textFieldLines)) {
                t.setHighlightPos(0);
                t.setCursorPosition(t.getValue().length());
            }
            this.setFocusedLine(this.getLineCount()-1);
            this.startHighlightLineIndex = 0;
            this.endHighlightLineIndex = this.getLineCount()-1;
            return true;
        }
        //GUI shortcut modifier + X
        if (((keycode) == 88 && InputUtils.isGuiShortcutModifierDown(modifiers))) {
            this.history.saveSnapshot();
            Minecraft.getInstance().keyboardHandler.setClipboard(this.cutHighlightedText());
            this.resetHighlighting();
            return true;
        }
        //Reset highlighting when pressing left/right arrow keys
        if ((keycode == InputConstants.KEY_RIGHT) || (keycode == InputConstants.KEY_LEFT)) {
            if (net.minecraft.client.Minecraft.getInstance().hasShiftDown() && !this.isInMouseHighlightingMode()) {
                this.syncKeyboardHighlightFromAnchor();
            } else {
                this.resetHighlighting();
            }
            return true;
        }

        //GUI shortcut modifier + D | DUPLICATE LINE
        if (InputUtils.isGuiShortcutModifierDown(modifiers) && (keycode == GLFW.GLFW_KEY_D)) {
            if (this.isLineFocused()) {
                this.history.saveSnapshot();
                int index = this.getFocusedLineIndex();
                TextEditorLine current = this.getLine(index);
                if (current != null) {
                    TextEditorLine newLine = this.addLineAtIndex(index + 1);
                    if (newLine != null) {
                        newLine.setValue(current.getValue());
                        this.setFocusedLine(index + 1);
                        newLine.moveCursorTo(current.getCursorPosition(), false);
                        this.correctYScroll(1);
                    }
                }
            }
            return true;
        }



        //GUI shortcut modifier + HOME | GO TO START
        if (InputUtils.isGuiShortcutModifierDown(modifiers) && (keycode == GLFW.GLFW_KEY_HOME)) {
            this.resetHighlighting();
            if (this.getLineCount() > 0) {
                this.setFocusedLine(0);
                TextEditorLine line = this.getLine(0);
                if (line != null) line.moveCursorTo(0, false);
                this.correctYScroll(-this.getTotalLineHeight());
            }
            return true;
        }

        //GUI shortcut modifier + END | GO TO END
        if (InputUtils.isGuiShortcutModifierDown(modifiers) && (keycode == GLFW.GLFW_KEY_END)) {
            this.resetHighlighting();
            if (this.getLineCount() > 0) {
                int lastIndex = this.getLineCount() - 1;
                this.setFocusedLine(lastIndex);
                TextEditorLine line = this.getLine(lastIndex);
                if (line != null) line.moveCursorToEnd(false);
                this.correctYScroll(this.getTotalLineHeight());
            }
            return true;
        }

        return super.keyPressed(keycode, scancode, modifiers);

    }

    /** Routes a key release and reports whether it was consumed. */
    @Override
    public boolean keyReleased(KeyEvent event) {
        return this.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key release and reports whether it was consumed. */
    public boolean keyReleased(int i1, int i2, int i3) {

        if (this.isGoToLineOpen && (this.goToLineField != null) && this.goToLineField.isFocused()) {
            return this.goToLineField.keyReleased(new KeyEvent(i1, i2, i3));
        }

        if (this.placeholdersAllowed() && extendedPlaceholderMenu && (this.searchBar != null) && this.searchBar.isFocused()) {
            return this.searchBar.keyReleased(new KeyEvent(i1, i2, i3));
        }

        for (TextEditorLine l : this.textFieldLines) {
            l.keyReleased(new KeyEvent(i1, i2, i3));
        }

        return super.keyReleased(new KeyEvent(i1, i2, i3));

    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (this.isInMouseHighlightingMode()) {
            // Recover cleanly if the previous capture ended because its window disappeared before release.
            this.stopMouseHighlighting();
        }

        this.setFocused(null);

        if (super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false)) return true;

        if (this.isGoToLineOpen && (this.goToLineField != null)) {
            if (!this.goToLineField.isMouseOver(mouseX, mouseY)) {
                this.isGoToLineOpen = false;
                this.goToLineField.setFocused(false);
            }
        }

        this.selectedHoveredOnRightClickMenuOpen = false;

        if (!this.isMouseInteractingWithGrabbers()) {

            for (TextEditorLine l : this.textFieldLines) {
                l.mouseClicked(mouseX, mouseY, button);
            }

            if (this.isMouseInsideEditorArea(mouseX, mouseY)) {
                if (button == 1) {
                    this.rightClickContextMenu.closeMenu();
                }
                if ((button == 0) || (button == 1)) {
                    boolean isHighlightedHovered = this.isHighlightedTextHovered();
                    TextEditorLine hoveredLine = this.getHoveredLine(mouseX, mouseY);
                    if (!this.rightClickContextMenu.isOpen()) {
                        if ((button == 0) || !isHighlightedHovered) {
                            this.resetHighlighting();
                        }
                        if (hoveredLine == null) {
                            TextEditorLine focus = this.getLine(this.getLineCount()-1);
                            for (TextEditorLine t : this.textFieldLines) {
                                if ((mouseY >= t.getY()) && (mouseY <= t.getY() + t.getHeight())) {
                                    focus = t;
                                    break;
                                }
                            }
                            this.setFocusedLine(this.getLineIndex(focus));
                            Objects.requireNonNull(this.getFocusedLine()).moveCursorToEnd(false);
                            this.correctYScroll(0);
                        } else if ((button == 1) && !isHighlightedHovered) {
                            //Focus focusedLineIndex in case it is right-clicked
                            this.setFocusedLine(this.getLineIndex(hoveredLine));
                            //Set cursor in case focusedLineIndex is right-clicked
                            int cursorPos = this.getCursorPosFromMouseX(hoveredLine, mouseX);
                            hoveredLine.moveCursorTo(cursorPos, false);
                        }
                    }
                    if (button == 1) {
                        this.selectedHoveredOnRightClickMenuOpen = this.isHighlightedTextHovered();
                        ContextMenuHandler.INSTANCE.setAndOpenAtMouse(this.rightClickContextMenu);
                    } else if (this.rightClickContextMenu.isOpen() && !this.rightClickContextMenu.isHovered()) {
                        this.rightClickContextMenu.closeMenu();
                        //Call mouseClicked of lines after closing the menu, so the focused focusedLineIndex and cursor pos gets updated
                        this.textFieldLines.forEach((line) -> {
                            line.mouseClicked(mouseX, mouseY, button);
                        });
                        //Call mouseClicked of editor again to do everything that would happen when clicked without the context menu opened
                        this.mouseClicked(mouseX, mouseY, button);
                    }
                }
            }

        }

        List<PlaceholderMenuEntry> entries = new ArrayList<>(this.placeholderMenuEntries);
        for (PlaceholderMenuEntry e : entries) {
            e.buttonBase.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
        }

        return false;

    }

    /** Routes pointer dragging and reports whether it was consumed. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if ((event.button() == 0) && this.isInMouseHighlightingMode()) {
            this.tickMouseHighlighting(event.x(), event.y(), false);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = super.mouseReleased(event);
        if ((event.button() == 0) && this.isInMouseHighlightingMode()) {
            this.tickMouseHighlighting(event.x(), event.y(), false);
            this.stopMouseHighlighting();
            return true;
        }
        return handled;
    }

    /** Advances this object's lifecycle by one client tick. */
    @Override
    public void tick() {

        this.validateMouseHighlightingCapture();

        for (TextEditorLine l : this.textFieldLines) {
            l.tick();
        }

        super.tick();

    }

    /** Handles window closed externally for this text editor window body. */
    @Override
    public void onWindowClosedExternally() {
        this.stopMouseHighlighting();
        this.callback.accept(null);
    }

    /** Handles screen closed for this text editor window body. */
    @Override
    public void onScreenClosed() {
        super.onScreenClosed();
        this.stopMouseHighlighting();
    }

    /** Returns whether mouse interacting with grabbers. */
    public boolean isMouseInteractingWithGrabbers() {
        return this.verticalScrollBar.isGrabberGrabbed() || this.verticalScrollBar.isGrabberHovered() || this.horizontalScrollBar.isGrabberGrabbed() || this.horizontalScrollBar.isGrabberHovered();
    }

    /** Returns whether mouse interacting with placeholder grabbers. */
    public boolean isMouseInteractingWithPlaceholderGrabbers() {
        return this.verticalScrollBarPlaceholderMenu.isGrabberGrabbed() || this.verticalScrollBarPlaceholderMenu.isGrabberHovered() || this.horizontalScrollBarPlaceholderMenu.isGrabberGrabbed() || this.horizontalScrollBarPlaceholderMenu.isGrabberHovered();
    }

    /** Returns edit box cursor x. */
    public int getEditBoxCursorX(EditBox editBox) {
        try {
            AccessorMixinEditBox b = (AccessorMixinEditBox) editBox;
            String s = this.getTextByWidth(editBox.getValue().substring(b.get_displayPos_Konkrete()), editBox.getInnerWidth());
            int j = editBox.getCursorPosition() - b.get_displayPos_Konkrete();
            boolean flag = j >= 0 && j <= s.length();
            boolean flag2 = editBox.getCursorPosition() < editBox.getValue().length() || editBox.getValue().length() >= b.get_maxLength_Konkrete();
            int l = b.get_bordered_Konkrete() ? editBox.getX() + 4 : editBox.getX();
            int j1 = l;
            if (!s.isEmpty()) {
                String s1 = flag ? s.substring(0, j) : s;
                j1 += Math.round(this.getTextWidthAtUIScale(s1));
            }
            int k1 = j1;
            if (!flag) {
                k1 = j > 0 ? l + editBox.getWidth() : l;
            } else if (flag2) {
                k1 = j1 - 1;
                --j1;
            }
            return k1;
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get cursor X position!", ex);
        }
        return 0;
    }

    int getCursorPosFromMouseX(@NotNull TextEditorLine line, double mouseX) {
        int displayPos = Math.max(0, line.getAsAccessor().get_displayPos_Konkrete());
        String value = line.getValue();
        if (value.isEmpty() || displayPos >= value.length()) {
            return value.length();
        }
        int localX = Mth.floor(mouseX) - line.getX();
        if (line.getAsAccessor().get_bordered_Konkrete()) {
            localX -= 4;
        }
        if (localX <= 0) {
            return displayPos;
        }
        float maxWidth = line.getInnerWidth();
        float targetWidth = (maxWidth > 0.0F) ? Math.min(localX, maxWidth) : localX;
        String remaining = value.substring(displayPos);
        int offset = this.getTextIndexByWidth(remaining, targetWidth);
        return Math.min(value.length(), displayPos + offset);
    }

    private String getTextByWidth(@NotNull String text, float maxWidth) {
        if (text.isEmpty()) {
            return text;
        }
        int length = this.getTextIndexByWidth(text, maxWidth);
        return text.substring(0, length);
    }

    private int getTextIndexByWidth(@NotNull String text, float targetWidth) {
        if (text.isEmpty()) {
            return 0;
        }
        if (!Float.isFinite(targetWidth)) {
            return targetWidth > 0.0F ? text.length() : 0;
        }
        if (targetWidth <= 0.0F) {
            return 0;
        }
        if (UIBase.isCurrentlyRenderingAtUIScale()) {
            return this.getTextIndexByWidthInternal(text, targetWidth);
        }
        UIBase.startUIScaleRendering();
        try {
            return this.getTextIndexByWidthInternal(text, targetWidth);
        } finally {
            UIBase.stopUIScaleRendering();
        }
    }

    private int getTextIndexByWidthInternal(@NotNull String text, float targetWidth) {
        float fullWidth = UIBase.getUITextWidth(text);
        if (targetWidth >= fullWidth) {
            return text.length();
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            float width = UIBase.getUITextWidth(text.substring(0, mid));
            if (width <= targetWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    float getTextWidthAtUIScale(@NotNull String text) {
        if (text.isEmpty()) {
            return 0.0F;
        }
        if (UIBase.isCurrentlyRenderingAtUIScale()) {
            return UIBase.getUITextWidth(text);
        }
        UIBase.startUIScaleRendering();
        try {
            return UIBase.getUITextWidth(text);
        } finally {
            UIBase.stopUIScaleRendering();
        }
    }

    /** Scrolls the editor viewport until the requested line is visible. */
    public void scrollToLine(int lineIndex, boolean bottom) {
        if (bottom) {
            this.scrollToLine(lineIndex, -Math.max(0, this.getEditorAreaHeight() - this.lineHeight));
        } else {
            this.scrollToLine(lineIndex, 0);
        }
    }

    /** Scrolls the editor viewport until the requested line is visible. */
    public void scrollToLine(int lineIndex, int offset) {
        int totalLineHeight = this.getTotalScrollHeight();
        float f = (float)Math.max(0, ((lineIndex + 1) * this.lineHeight) - this.lineHeight) / (float)totalLineHeight;
        if (offset != 0) {
            if (offset > 0) {
                f += ((float)offset / (float)totalLineHeight);
            } else {
                f -= ((float)Math.abs(offset) / (float)totalLineHeight);
            }
        }
        this.verticalScrollBar.setScroll(f);
    }

    /** Returns total scroll height. */
    public int getTotalScrollHeight() {
        if (this.overriddenTotalScrollHeight != -1) {
            return this.overriddenTotalScrollHeight;
        }
        return this.getTotalLineHeight();
    }

    /** Returns total scroll width. */
    public int getTotalScrollWidth() {
        //return Math.max(0, this.currentLineWidth - this.getEditorAreaWidth())
        return this.currentLineWidth;
    }

    /** Clamps vertical editor scrolling to the current document bounds. */
    public void correctYScroll(int lineCountOffsetAfterRemovingAdding) {

        //Don't fix scroll if in mouse-highlighting mode or no focusedLineIndex is focused
        if (this.isInMouseHighlightingMode() || !this.isLineFocused()) {
            return;
        }

        int minY = this.getEditorAreaY();
        int maxY = this.getEditorAreaY() + this.getEditorAreaHeight();
        int currentLineY = Objects.requireNonNull(this.getFocusedLine()).getY();

        if (currentLineY < minY) {
            this.scrollToLine(this.getFocusedLineIndex(), false);
        } else if ((currentLineY + this.lineHeight) > maxY) {
            this.scrollToLine(this.getFocusedLineIndex(), true);
        } else if (lineCountOffsetAfterRemovingAdding != 0) {
            this.overriddenTotalScrollHeight = -1;
            int removedAddedLineCount = Math.abs(lineCountOffsetAfterRemovingAdding);
            if (lineCountOffsetAfterRemovingAdding > 0) {
                this.overriddenTotalScrollHeight = this.getTotalScrollHeight() - (this.lineHeight * removedAddedLineCount);
            } else if (lineCountOffsetAfterRemovingAdding < 0) {
                this.overriddenTotalScrollHeight = this.getTotalScrollHeight() + (this.lineHeight * removedAddedLineCount);
            }
            this.updateLines(null);
            this.overriddenTotalScrollHeight = -1;
            int diffToTop = Math.max(0, this.getFocusedLine().getY() - this.getEditorAreaY());
            this.scrollToLine(this.getFocusedLineIndex(), -diffToTop);
            this.correctYScroll(0);
        }

        if (this.getTotalLineHeight() <= this.getEditorAreaHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }

    }

    /** Clamps horizontal editor scrolling to the current document bounds. */
    public void correctXScroll(TextEditorLine line) {

        //Don't fix scroll if in mouse-highlighting mode
        if (this.isInMouseHighlightingMode()) {
            return;
        }

        if (this.isLineFocused() && (this.getFocusedLine() == line)) {

            int oldX = line.getX();

            this.updateCurrentLineWidth();
            this.updateLines(null);

            int newX = line.getX();
            String oldValue = line.lastTickValue;
            String newValue = line.getValue();

            //Make the lines scroll horizontally with the cursor position if the cursor is too far to the left or right
            int cursorWidth = 2;
            if (line.getCursorPosition() >= newValue.length()) {
                cursorWidth = 6;
            }
            int editorAreaCenterX = this.getEditorAreaX() + (this.getEditorAreaWidth() / 2);
            int cursorX = this.getEditBoxCursorX(line);
            if (cursorX > editorAreaCenterX) {
                cursorX += cursorWidth + 5;
            } else if (cursorX < editorAreaCenterX) {
                cursorX -= cursorWidth + 5;
            }
            int maxToRight = this.getEditorAreaX() + this.getEditorAreaWidth();
            int maxToLeft = this.getEditorAreaX();
            float currentScrollX = this.horizontalScrollBar.getScroll();
            int currentLineW = this.getTotalScrollWidth();
            boolean textGotDeleted = oldValue.length() > newValue.length();
            boolean textGotAdded = oldValue.length() < newValue.length();
            if (cursorX > maxToRight) {
                float f = (float)(cursorX - maxToRight) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX + f);
            } else if (cursorX < maxToLeft) {
                //By default, move back the focusedLineIndex just a little when moving the cursor to the left side by using the mouse or arrow keys
                float f = (float)(maxToLeft - cursorX) / (float)currentLineW;
                //But move it back a big chunk when deleting chars (by pressing backspace)
                if (textGotDeleted) {
                    f = (float)(maxToRight - maxToLeft) / (float)currentLineW;
                }
                this.horizontalScrollBar.setScroll(currentScrollX - f);
            } else if (textGotDeleted && (oldX < newX)) {
                float f = (float)(newX - oldX) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX + f);
            } else if (textGotAdded && (oldX > newX)) {
                float f = (float)(oldX - newX) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX - f);
            }
            if (line.getCursorPosition() == 0) {
                this.horizontalScrollBar.setScroll(0.0F);
            }

        }

    }

    /** Returns whether mouse inside editor area. */
    public boolean isMouseInsideEditorArea() {
        return this.isMouseInsideEditorArea(this.getRenderMouseX(), this.getRenderMouseY());
    }

    /** Returns whether mouse inside editor area. */
    public boolean isMouseInsideEditorArea(double mouseX, double mouseY) {
        int xStart = this.borderLeft;
        int yStart = this.headerHeight;
        int xEnd = this.getEditorAreaX() + this.getEditorAreaWidth();
        int yEnd = this.height - this.footerHeight;
        return (mouseX >= xStart) && (mouseX <= xEnd) && (mouseY >= yStart) && (mouseY <= yEnd);
    }

    /** Returns editor area width. */
    public int getEditorAreaWidth() {
        int i = (this.width - this.borderRight) - this.borderLeft;
        if (extendedPlaceholderMenu) {
            i = i - this.getPlaceholderAreaWidth() - 15;
        }
        return i;
    }

    /** Returns editor area height. */
    public int getEditorAreaHeight() {
        return (this.height - this.footerHeight) - this.headerHeight;
    }

    /** Returns editor area x. */
    public int getEditorAreaX() {
        return this.borderLeft;
    }

    /** Returns editor area y. */
    public int getEditorAreaY() {
        return this.headerHeight;
    }

    /** Toggles indentation guides. */
    public void toggleIndentationGuides() {
        this.showIndentationGuides = !this.showIndentationGuides;
    }

    /** Returns whether indentation guides visible. */
    public boolean areIndentationGuidesVisible() {
        return this.showIndentationGuides;
    }

    /**
     * @return The compiled version of the input string or NULL if the input was NULL.
     */
    public static String compileSingleLineString(@Nullable String s) {
        if (s == null) return null;
        String compiled = COMPILED_SINGLE_LINE_STRINGS.computeIfAbsent(s, s1 -> s1.replace(NEWLINE_CODE, "").replace(SPACE_CODE, ""));
        return compiled;
    }

    /** Clears compiled single line cache state. */
    public static void clearCompiledSingleLineCache() {
        LOGGER.info("[KONKRETE] Clearing text editor's compiled single line string cache..");
        COMPILED_SINGLE_LINE_STRINGS.clear();
    }

    /** Represents one renderable, focusable placeholder menu entry. */
    public static class PlaceholderMenuEntry extends UIBase {

        /** Editor receiving selection actions from this placeholder entry. */
        public TextEditorWindowBody parent;
        /** Label displayed beside the color swatch. */
        public final Component label;
        /** Action run when this color entry is activated. */
        public Runnable clickAction;
        /** Horizontal component of the current transform. */
        public int x;
        /** Vertical component of the current transform. */
        public int y;
        /** Width in GUI units for label. */
        public final int labelWidth;
        /** Swatch background while idle. */
        protected Color backgroundColorIdle = Color.GRAY;
        /** Swatch background while hovered. */
        protected Color backgroundColorHover = Color.LIGHT_GRAY;
        /** Color of the swatch marker. */
        protected Color dotColor = Color.BLUE;
        /** Color of the swatch label. */
        protected Color entryLabelColor = Color.WHITE;
        /** Underlying button used for focus, narration, and activation. */
        public ExtendedButton buttonBase;
        /** Creates a labeled editor entry that inserts a placeholder when activated. */
        public PlaceholderMenuEntry(@NotNull TextEditorWindowBody parent, @NotNull Component label, @NotNull Runnable clickAction) {
            this.parent = parent;
            this.label = label;
            this.clickAction = clickAction;
            this.labelWidth = Math.round(UIBase.getUITextWidthNormal(this.label));
            this.buttonBase = new ExtendedButton(0, 0, this.getWidth(), this.getHeight(), "", (button) -> {
                this.clickAction.run();
            }) {
                /** Returns whether hovered or focused. */
                @Override
                public boolean isHoveredOrFocused() {
                    if (PlaceholderMenuEntry.this.parent.isMouseInteractingWithPlaceholderGrabbers()) {
                        return false;
                    }
                    return super.isHoveredOrFocused();
                }
                /** Handles click for this placeholder menu entry. */
                @Override
                public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
                    if (PlaceholderMenuEntry.this.parent.isMouseInteractingWithPlaceholderGrabbers()) {
                        return;
                    }
                    super.onClick(event, isDoubleClick);
                }
                /** Adds this component's content draw state to the active GUI extraction pass. */
                @Override
                protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int p_93658_, int p_93659_, float p_93660_) {
                    if (PlaceholderMenuEntry.this.parent.isMouseInteractingWithPlaceholderGrabbers()) {
                        this.isHovered = false;
                    }
                    super.extractContents(graphics, p_93658_, p_93659_, p_93660_);
                }
            };
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            //Update the button colors
            this.buttonBase.setBackgroundColor(
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT
            );
            //Update the button pos
            this.buttonBase.setX(this.x);
            this.buttonBase.setY(this.y);
            int yCenter = this.y + (this.getHeight() / 2);
            //Render hover effect
            if (!this.parent.isMouseInteractingWithPlaceholderGrabbers() && this.buttonBase.isMouseOver(mouseX, mouseY)) {
                int areaY = this.parent.getPlaceholderAreaY() + 2;
                int areaBottom = this.parent.getPlaceholderAreaY() + this.parent.getPlaceholderAreaHeight() - 2;
                int entryY = this.y;
                int entryHeight = this.getHeight();
                int visibleTop = Math.max(entryY, areaY);
                int visibleBottom = Math.min(entryY + entryHeight, areaBottom);
                int visibleHeight = visibleBottom - visibleTop;
                if (visibleHeight > 0) {
                    boolean roundTop = entryY <= areaY + 5;
                    boolean roundBottom = (entryY + entryHeight) >= areaBottom - 5;
                    int hoverColor = this.backgroundColorHover.getRGB();
                    if (roundTop || roundBottom) {
                        float radius = UIBase.getInterfaceCornerRoundingRadius();
                        if (roundTop && roundBottom) {
                            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, this.x, visibleTop, this.getWidth(), visibleHeight, radius, radius, radius, radius, hoverColor, partial);
                        } else if (roundTop) {
                            SmoothRectangleRenderer.renderSmoothRectRoundTopCornersScaled(graphics, this.x, visibleTop, this.getWidth(), visibleHeight, radius, hoverColor, partial);
                        } else {
                            SmoothRectangleRenderer.renderSmoothRectRoundBottomCornersScaled(graphics, this.x, visibleTop, this.getWidth(), visibleHeight, radius, hoverColor, partial);
                        }
                    } else {
                        graphics.fill(this.x, visibleTop, this.x + this.getWidth(), visibleTop + visibleHeight, hoverColor);
                    }
                }
            }
            //Render the button
            this.buttonBase.extractRenderState(graphics, mouseX, mouseY, partial);
            //Render dot
            renderListingDot(graphics, this.x + 5, yCenter - 2, this.dotColor);
            //Render label
            UIBase.renderText(graphics, this.label, this.x + 5 + 4 + 3, yCenter - (UIBase.getUITextHeightNormal() / 2F), this.entryLabelColor.getRGB());
        }

        /** Returns the current width in GUI units. */
        public int getWidth() {
            return Math.max(this.parent.placeholderMenuWidth, 5 + 4 + 3 + this.labelWidth + 5);
        }

        /** Returns the current height in GUI units. */
        public int getHeight() {
            return this.parent.placeholderMenuEntryHeight;
        }

        /** Reports whether the pointer currently hovers this element. */
        public boolean isHovered() {
            return this.buttonBase.isHoveredOrFocused();
        }

        /** Sets description for this placeholder menu entry. */
        public void setDescription(String... desc) {
            this.buttonBase.setUITooltip(UITooltip.of(desc));
        }

    }

}
