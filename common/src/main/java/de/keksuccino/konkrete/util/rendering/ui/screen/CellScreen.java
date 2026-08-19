package de.keksuccino.konkrete.util.rendering.ui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.cycle.ILocalizedValueCycle;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import de.keksuccino.konkrete.util.input.InputConstants;
import de.keksuccino.konkrete.util.input.InputUtils;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.konkrete.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.util.window.WindowHandler;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.*;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Arranges reusable typed cells into searchable, keyboard-navigable screen rows. */
@SuppressWarnings("all")
public abstract class CellScreen extends Screen implements InitialWidgetFocusScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Scroll area displaying scroll area. */
    public ScrollArea scrollArea;
    /** Cell whose editor or description is currently active. */
    @Nullable
    protected RenderCell selectedCell;
    /** Widgets anchored to the right side of the screen. */
    protected final List<AbstractWidget> rightSideWidgets = new ArrayList<>();
    /** Button that accepts the current cell values and closes the screen. */
    @Nullable
    protected ExtendedButton doneButton;
    /** Button that cancels the current operation. */
    @Nullable
    protected ExtendedButton cancelButton;
    /** Width in GUI units for last. */
    protected int lastWidth = 0;
    /** Height in GUI units for last. */
    protected int lastHeight = 0;
    /** Searchable cells in their stable display order. */
    protected final List<RenderCell> allCells = new ArrayList<>();
    /** Whether the search field is enabled. */
    protected boolean searchBarEnabled = false;
    /** Optional search field used to filter cells. */
    @Nullable
    protected ExtendedEditBox searchBar;
    /** Placeholder shown while the search field is empty. */
    @NotNull
    protected Component searchBarPlaceholder = Component.translatable("konkrete.ui.generic.search");
    /** Whether the selected-cell description area is enabled. */
    protected boolean descriptionAreaEnabled = false;
    /** Scroll area displaying description scroll area. */
    @Nullable
    protected ScrollArea descriptionScrollArea;
    /** Whether the screen selects a scale that fits its content. */
    protected boolean shouldAutoScale = false;
    /** Whether one-time screen initialization has completed. */
    protected boolean initialized = false;

    /** Initializes a cell-based screen with its narration title. */
    protected CellScreen(@NotNull Component title) {
        super(title);
    }

    /**
     * This is to add cells to the cell view.<br>
     * Gets called in {@link CellScreen#init()}, before {@link CellScreen#initRightSideWidgets()}.
     */
    protected void initCells() {
    }

    /**
     * This is for custom widgets that should get added to the right side.<br>
     * Gets called in {@link CellScreen#init()}.<br>
     * The {@link CellScreen#cancelButton} and {@link CellScreen#doneButton} are NOT INITIALIZED yet when this method gets called!
     */
    protected void initRightSideWidgets() {
    }

    /** Refreshes this cell screen from current state. */
    public void rebuild() {
        this.resize(this.width, this.height);
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
     * Enable or disable the description area feature.
     * Should be called before {@link #init()} for proper initialization.
     */
    protected void setDescriptionAreaEnabled(boolean enabled) {
        this.descriptionAreaEnabled = enabled;
    }

    /**
     * Get the current description to display in the description area.
     * By default, returns the description of the selected cell.
     * Can be overridden for custom logic.
     */
    @Nullable
    protected List<Component> getCurrentDescription() {
        if (this.selectedCell != null) {
            Supplier<List<Component>> supplier = this.selectedCell.getDescriptionSupplier();
            if (supplier != null) {
                return supplier.get();
            }
        }
        return null;
    }

    /**
     * Updates the description area with the current description.
     * Called automatically when the selected cell changes.
     */
    protected void updateDescriptionArea() {

        if (this.descriptionScrollArea == null) return;
        this.descriptionScrollArea.clearEntries();

        this.descriptionScrollArea.addEntry(new SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

        List<Component> description = this.getCurrentDescription();
        if (description != null) {
            for (Component line : description) {
                this.addDescriptionLine(line);
            }
        }

        this.descriptionScrollArea.addEntry(new SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

    }

    /** Adds description line to this cell screen. */
    protected void addDescriptionLine(@NotNull Component line) {
        float maxWidth = this.descriptionScrollArea.getInnerWidth() - 15F;
        List<MutableComponent> lines = UIBase.lineWrapUIComponentsNormal(line, maxWidth);
        lines.forEach(component -> {
            TextScrollAreaEntry e = new TextScrollAreaEntry(this.descriptionScrollArea, component, (entry) -> {});
            e.setSelectable(false);
            e.setBackgroundColorHover(e.getBackgroundColorNormal());
            e.setPlayClickSound(false);
            e.setTextBaseColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt());
            this.descriptionScrollArea.addEntry(e);
        });
    }

    /** Returns whether auto scale. */
    public boolean shouldAutoScale() {
        return shouldAutoScale;
    }

    /** Sets should auto scale for this cell screen. */
    public CellScreen setShouldAutoScale(boolean shouldAutoScale) {
        this.shouldAutoScale = shouldAutoScale;
        if (this.initialized) {
            RenderingUtils.resetGuiScale();
            this.resize(this.width, this.height);
        }
        return this;
    }

    /**
     * Updates the cell list based on the search filter.
     * Only cells that match the search query are visible.
     */
    protected void updateCellsVisibility() {
        if (!this.searchBarEnabled || this.searchBar == null || this.scrollArea == null) return;

        String searchValue = this.searchBar.getValue();
        if (searchValue.isBlank()) searchValue = null;

        // Remember current scroll position
        float scrollX = this.scrollArea.horizontalScrollBar.getScroll();
        float scrollY = this.scrollArea.verticalScrollBar.getScroll();

        // Clear scroll area entries
        this.scrollArea.clearEntries();

        // Re-add only cells that match the search
        for (RenderCell cell : this.allCells) {
            if (this.cellMatchesSearch(cell, searchValue)) {
                CellScrollEntry entry = new CellScrollEntry(this.scrollArea, cell);
                this.scrollArea.addEntry(entry);
                // Update cell size
                cell.updateSize(entry);
                entry.setHeight(cell.getHeight());
            }
        }

        // Restore scroll position
        this.scrollArea.horizontalScrollBar.setScroll(scrollX);
        this.scrollArea.verticalScrollBar.setScroll(scrollY);

        if ((this.selectedCell != null) && (this.getCellEntry(this.selectedCell) == null)) {
            RenderCell oldSelected = this.selectedCell;
            this.clearFocusForCell(oldSelected);
            oldSelected.selected = false;
            this.selectedCell = null;
            this.updateDescriptionArea();
        }
    }

    /**
     * Check if a cell matches the search query.
     * Searches both the cell's search string and its description (if any).
     */
    protected boolean cellMatchesSearch(@NotNull RenderCell cell, @Nullable String searchValue) {
        if (searchValue == null || searchValue.isBlank()) return true;

        if (cell.ignoreSearch) return true;

        String searchLower = searchValue.toLowerCase();

        // Check the cell's search string
        String cellSearchString = cell.getSearchString();
        if (cellSearchString != null && cellSearchString.toLowerCase().contains(searchLower)) {
            return true;
        }

        // Check the cell's description
        Supplier<List<Component>> descSupplier = cell.getDescriptionSupplier();
        if (descSupplier != null) {
            List<Component> description = descSupplier.get();
            if (description != null) {
                for (Component c : description) {
                    if (c.getString().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Initializes resources required by this cell screen. */
    @Override
    protected void init() {

        this.initialized = true;

        this.rightSideWidgets.clear();
        this.allCells.clear();
        this.selectedCell = null;

        // Calculate scroll area dimensions based on enabled features
        int scrollAreaX = 20;
        int scrollAreaY = 50 + 15;
        int scrollAreaWidth = this.width - 40 - this.getRightSideWidgetWidth() - 20;
        int scrollAreaHeight = this.height - 85;

        // Adjust for description area if enabled
        if (this.descriptionAreaEnabled) {
            scrollAreaWidth = (this.width / 2) - 40;

            // Initialize description area
            this.descriptionScrollArea = new ScrollArea(0, 0, 0, 0);
            this.descriptionScrollArea.setWidth((this.width / 2) - 40, true);
            this.descriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
            this.descriptionScrollArea.setX(this.width - 20 - this.descriptionScrollArea.getWidthWithBorder(), true);
            this.descriptionScrollArea.setY(50 + 15, true);
            this.descriptionScrollArea.horizontalScrollBar.active = false;
            this.addRenderableWidget(this.descriptionScrollArea);
        }

        // Adjust for search bar if enabled
        if (this.searchBarEnabled) {
            scrollAreaY += 25; // Make room for search bar
            scrollAreaHeight -= 25;

            // Initialize search bar
            String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
            this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, scrollAreaX + 1, 50 + 15 + 1, scrollAreaWidth - 2, 20 - 2, Component.empty());
            if (CellScreen.this.searchBarPlaceholder != null) {
                this.searchBar.setCustomHint(consumes -> CellScreen.this.searchBarPlaceholder);
            }
            this.searchBar.setValue(oldSearchValue);
            this.searchBar.setResponder(s -> CellScreen.this.updateCellsVisibility());
            UIBase.applyDefaultWidgetSkinTo(this.searchBar);
            this.searchBar.setMaxLength(100000);
            this.addRenderableWidget(this.searchBar);
            this.setupInitialFocusWidget(this, this.searchBar);
        }

        float oldScrollX = 0.0F;
        float oldScrollY = 0.0F;
        if (this.scrollArea != null) {
            oldScrollX = this.scrollArea.horizontalScrollBar.getScroll();
            oldScrollY = this.scrollArea.verticalScrollBar.getScroll();
        }
        this.scrollArea = new ScrollArea(scrollAreaX, scrollAreaY, scrollAreaWidth, scrollAreaHeight);
        this.initCells();
        this.addWidget(this.scrollArea);
        this.scrollArea.horizontalScrollBar.setScroll(oldScrollX);
        this.scrollArea.verticalScrollBar.setScroll(oldScrollY);

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry ce) {
                ce.cell.updateSize(ce);
                ce.setHeight(ce.cell.getHeight());
            }
        }

        this.initRightSideWidgets();

        this.addRightSideDefaultSpacer();

        this.cancelButton = this.addRightSideButton(20, Component.translatable("konkrete.common_components.cancel"), button -> {
            this.onCancel();
        }).setIsActiveSupplier(consumes -> this.allowCancel())
                .setVisibilitySupplier(consumes -> this.showCancel());

        this.doneButton = this.addRightSideButton(20, Component.translatable("konkrete.common_components.done"), button -> {
            if (this.allowDone()) this.onDone();
        }).setIsActiveSupplier(consumes -> this.allowDone());

        AbstractWidget topRightSideWidget = this.layoutRightSideWidgets(true);

        this.autoScaleScreen(topRightSideWidget);

    }

    @Nullable
    private AbstractWidget layoutRightSideWidgets(boolean addRenderables) {
        int widgetWidth = this.getRightSideWidgetWidth();
        int widgetX = this.width - 20 - widgetWidth;
        int widgetY = this.height - 20;
        AbstractWidget topRightSideWidget = null;
        for (AbstractWidget w : Lists.reverse(this.rightSideWidgets)) {
            if (!(w instanceof RightSideSpacer)) {
                if (addRenderables) {
                    UIBase.applyDefaultWidgetSkinTo(w);
                    this.addRenderableWidget(w);
                }
                w.setX(widgetX);
                w.setWidth(widgetWidth);
            }
            if (!w.visible) {
                continue;
            }
            if (!(w instanceof RightSideSpacer)) {
                w.setY(widgetY - w.getHeight());
                topRightSideWidget = w;
            }
            widgetY -= w.getHeight() + this.getRightSideDefaultSpaceBetweenWidgets();
        }
        return topRightSideWidget;
    }

    /** Returns whether scale screen. */
    protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        if (!this.shouldAutoScale()) return;
        Window window = Minecraft.getInstance().getWindow();
        boolean resized = (window.getScreenWidth() != this.lastWidth) || (window.getScreenHeight() != this.lastHeight);
        this.lastWidth = window.getScreenWidth();
        this.lastHeight = window.getScreenHeight();
        //Adjust GUI scale to make all right-side buttons fit in the screen
        if ((topRightSideWidget != null) && (topRightSideWidget.getY() < 20) && (WindowHandler.getGuiScale() > 1)) {
            double newScale = WindowHandler.getGuiScale();
            newScale--;
            if (newScale < 1) newScale = 1;
            WindowHandler.setGuiScale(newScale);
            this.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        } else if ((topRightSideWidget != null) && (topRightSideWidget.getY() >= 20) && resized) {
            RenderingUtils.resetGuiScale();
            this.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }
    }

    /** Sets initial focus for this cell screen. */
    @Override
    protected void setInitialFocus() {
        //This fixes a crash related to the custom GUI scale handling in init()
    }

    /** Handles cancel for this cell screen. */
    protected abstract void onCancel();

    /** Handles done for this cell screen. */
    protected abstract void onDone();

    /** Handles close for this cell screen. */
    @Override
    public void onClose() {
        this.onCancel();
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.updateSelectedCell();
        this.layoutRightSideWidgets(false);

        this.renderCellScreenBackground(graphics, mouseX, mouseY, partial);

        this.renderTitle(graphics);

        if (this.descriptionAreaEnabled && (this.descriptionScrollArea != null)) {
            this.descriptionScrollArea.extractRenderState(graphics, mouseX, mouseY, partial);
        }

        this.scrollArea.extractRenderState(graphics, mouseX, mouseY, partial);

        super.extractRenderState(graphics, mouseX, mouseY, partial);

        this.performInitialWidgetFocusActionInRender();

    }

    /** Renders title into the active GUI extraction pass. */
    protected void renderTitle(@NotNull GuiGraphicsExtractor graphics) {
        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        UIBase.renderText(graphics, titleComp, 20, 20, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt());
    }

    /** Renders cell screen background into the active GUI extraction pass. */
    protected void renderCellScreenBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());
    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    @Override
    public final void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        // do nothing
    }

    /** Advances this object's lifecycle by one client tick. */
    @Override
    public void tick() {
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry c) {
                c.cell.tick();
            }
        }
    }

    /** Returns right side widget width. */
    public int getRightSideWidgetWidth() {
        return 150;
    }

    /** Returns right side default space between widgets. */
    public int getRightSideDefaultSpaceBetweenWidgets() {
        return 5;
    }

    /** Reports whether the current state permits completion. */
    public boolean allowDone() {
        return true;
    }

    /** Returns whether enter for done. */
    public boolean allowEnterForDone() {
        return true;
    }

    /** Opens cancel. */
    public boolean showCancel() {
        return true;
    }

    /** Returns whether cancel. */
    public boolean allowCancel() {
        return true;
    }

    /** Adds right side default spacer to this cell screen. */
    protected void addRightSideDefaultSpacer() {
        this.addRightSideSpacer(5);
    }

    /** Adds right side spacer to this cell screen. */
    protected void addRightSideSpacer(int height) {
        this.rightSideWidgets.add(new RightSideSpacer(height));
    }

    /** Adds right side cycle button to this cell screen. */
    protected <T> CycleButton<T> addRightSideCycleButton(int height, @NotNull ILocalizedValueCycle<T> cycle, @NotNull CycleButton.CycleButtonClickFeedback<T> clickFeedback) {
        return this.addRightSideWidget(new CycleButton<>(0, 0, 0, height, cycle, clickFeedback));
    }

    /** Adds right side button to this cell screen. */
    protected ExtendedButton addRightSideButton(int height, @NotNull Component label, @NotNull Consumer<ExtendedButton> onClick) {
        return this.addRightSideWidget(new ExtendedButton(0, 0, 0, height, label, var1 -> {
            onClick.accept((ExtendedButton) var1);
        }));
    }

    /** Adds right side widget to this cell screen. */
    protected <T extends AbstractWidget> T addRightSideWidget(@NotNull T widget) {
        if (widget instanceof NavigatableWidget n) {
            n.setNavigatable(false);
        }
        this.rightSideWidgets.add(widget);
        return widget;
    }

    /** Adds text input cell to this cell screen. */
    @NotNull
    protected TextInputCell addTextInputCell(@Nullable CharacterFilter characterFilter, boolean allowEditor, boolean allowEditorPlaceholders) {
        return this.addCell(new TextInputCell(characterFilter, allowEditor, allowEditorPlaceholders));
    }

    /** Adds label cell to this cell screen. */
    @NotNull
    protected CellScreen.LabelCell addLabelCell(@NotNull Component text) {
        return this.addCell(new LabelCell(text));
    }

    /** Adds description end separator cell to this cell screen. */
    protected void addDescriptionEndSeparatorCell() {
        this.addSpacerCell(5);
        this.addSeparatorCell();
        this.addSpacerCell(5);
    }

    /** Adds separator cell to this cell screen. */
    @NotNull
    protected SeparatorCell addSeparatorCell(int height) {
        return this.addCell(new SeparatorCell(height));
    }

    /** Adds separator cell to this cell screen. */
    @NotNull
    protected SeparatorCell addSeparatorCell() {
        return this.addCell(new SeparatorCell());
    }

    /** Adds cell group end spacer cell to this cell screen. */
    @NotNull
    protected SpacerCell addCellGroupEndSpacerCell() {
        return this.addSpacerCell(7);
    }

    /** Adds start end spacer cell to this cell screen. */
    @NotNull
    protected SpacerCell addStartEndSpacerCell() {
        return this.addSpacerCell(20);
    }

    /** Adds spacer cell to this cell screen. */
    @NotNull
    protected SpacerCell addSpacerCell(int height) {
        return this.addCell(new SpacerCell(height));
    }

    /** Adds cycle button cell to this cell screen. */
    @NotNull
    protected <T> CellScreen.WidgetCell addCycleButtonCell(@NotNull ILocalizedValueCycle<T> cycle, boolean applyDefaultButtonSkin, CycleButton.CycleButtonClickFeedback<T> clickFeedback) {
        return this.addWidgetCell(new CycleButton(0, 0, 20, 20, cycle, clickFeedback), applyDefaultButtonSkin);
    }

    /** Adds widget cell to this cell screen. */
    @NotNull
    protected CellScreen.WidgetCell addWidgetCell(@NotNull AbstractWidget widget, boolean applyDefaultButtonSkin) {
        return this.addCell(new WidgetCell(widget, applyDefaultButtonSkin));
    }

    /** Adds cell to this cell screen. */
    @NotNull
    protected <T extends RenderCell> T addCell(@NotNull T cell) {
        // Always add to the complete list of cells
        this.allCells.add(cell);

        // Only add to scroll area if it matches the search filter (or if search is disabled)
        if (cell.ignoreSearch || (!this.searchBarEnabled || this.searchBar == null || this.cellMatchesSearch(cell, this.searchBar.getValue()))) {
            CellScrollEntry entry = new CellScrollEntry(this.scrollArea, cell);
            this.scrollArea.addEntry(entry);
        }

        return this.addWidget(cell);
    }

    /** Refreshes selected cell from current state. */
    protected void updateSelectedCell() {
        RenderCell last = this.selectedCell;
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry c) {
                if (c.cell.selectable && c.cell.selected) {
                    this.selectedCell = c.cell;
                    if (last != this.selectedCell) {
                        this.updateDescriptionArea();
                    }
                    return;
                }
            }
        }
        this.selectedCell = null;
        if (last != this.selectedCell) {
            this.updateDescriptionArea();
        }
    }

    /** Returns selected cell. */
    @Nullable
    protected RenderCell getSelectedCell() {
        return this.selectedCell;
    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (InputUtils.isGuiShortcutModifierDown(modifiers) && !net.minecraft.client.Minecraft.getInstance().hasShiftDown() && !net.minecraft.client.Minecraft.getInstance().hasAltDown() && this.isLetterKeyPressed(keycode, scancode, "s")) {
            if ((this.doneButton != null) && this.doneButton.visible && this.doneButton.active) {
                this.doneButton.onPress(new KeyEvent(keycode, scancode, modifiers));
                return true;
            }
        }

        if (keycode == InputConstants.KEY_TAB) {
            return true;
        }

        if ((keycode == InputConstants.KEY_UP) || (keycode == InputConstants.KEY_DOWN)) {
            return this.navigateVerticalCells(keycode == InputConstants.KEY_DOWN);
        }

        if ((keycode == InputConstants.KEY_LEFT) || (keycode == InputConstants.KEY_RIGHT)) {
            return this.handleHorizontalNavigation(keycode == InputConstants.KEY_RIGHT, keycode, scancode, modifiers);
        }

        if ((keycode == InputConstants.KEY_ENTER) || (keycode == InputConstants.KEY_NUMPADENTER)) {
            if (this.handleEnterForSelectedCell(keycode, scancode, modifiers)) {
                return true;
            }
            if (this.allowDone() && this.allowEnterForDone()) {
                this.onDone();
                return true;
            }
        }

        if ((keycode == InputConstants.KEY_BACKSPACE) && this.shouldAutoFocusSearchBarForTyping()) {
            if ((this.searchBar != null) && !this.searchBar.isFocused()) {
                this.focusSearchBarForNavigation();
            }
            if (this.searchBar != null) {
                return this.searchBar.keyPressed(keycode, scancode, modifiers);
            }
        }
        return super.keyPressed(new KeyEvent(keycode, scancode, modifiers));
    }

    /** Routes typed character input and reports whether it was consumed. */
    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), 0);
    }

    /** Routes typed character input and reports whether it was consumed. */
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.shouldAutoFocusSearchBarForTyping()) {
            if ((this.searchBar != null) && !this.searchBar.isFocused()) {
                this.focusSearchBarForNavigation();
            }
            if (this.searchBar != null) {
                return this.searchBar.charTyped(codePoint, modifiers);
            }
        }
        return super.charTyped(new CharacterEvent(codePoint));
    }

    /** Returns letter key name. */
    @NotNull
    protected String getLetterKeyName(int keycode, int scancode) {
        String keyName = GLFW.glfwGetKeyName(keycode, scancode);
        if (keyName == null) {
            return "";
        }
        return keyName.toLowerCase(Locale.ROOT);
    }

    /** Returns whether letter key pressed. */
    protected boolean isLetterKeyPressed(int keycode, int scancode, @NotNull String letter) {
        return letter.toLowerCase(Locale.ROOT).equals(this.getLetterKeyName(keycode, scancode));
    }

    /** Handles enter for selected cell for this cell screen. */
    protected boolean handleEnterForSelectedCell(int keycode, int scancode, int modifiers) {
        if (this.searchBarEnabled && (this.searchBar != null) && this.searchBar.isFocused()) {
            return false;
        }

        RenderCell selected = this.getSelectedCell();
        if (selected == null) {
            return false;
        }

        List<GuiEventListener> focusTargets = this.getNavigatableTargets(selected);
        if (focusTargets.isEmpty()) {
            return false;
        }

        GuiEventListener focusedTarget = this.getFocusedTarget(selected, focusTargets);
        if (focusedTarget == null) {
            GuiEventListener preferredTarget = this.getPreferredFocusTarget(selected, focusTargets);
            if (preferredTarget != null) {
                this.focusTarget(selected, preferredTarget);
                focusedTarget = preferredTarget;
            }
        }

        if (focusedTarget != null) {
            focusedTarget.keyPressed(new KeyEvent(keycode, scancode, modifiers));
        }

        return true;
    }

    /** Moves keyboard focus to the next eligible cell above or below the current cell. */
    protected boolean navigateVerticalCells(boolean moveDown) {
        List<RenderCell> cells = this.getSelectableVisibleCells();
        boolean searchBarNavigatable = this.isSearchBarNavigatable();
        RenderCell selected = this.getSelectedCell();
        int selectedIndex = cells.indexOf(selected);
        boolean searchBarFocused = searchBarNavigatable && this.searchBar.isFocused();

        if (cells.isEmpty()) {
            if (searchBarNavigatable) {
                this.focusSearchBarForNavigation();
            }
            return true;
        }

        if (selectedIndex >= 0) {
            if (searchBarNavigatable) {
                if (moveDown) {
                    if (selectedIndex >= (cells.size() - 1)) {
                        this.focusSearchBarForNavigation();
                    } else {
                        this.selectCell(cells.get(selectedIndex + 1), true);
                    }
                } else {
                    if (selectedIndex <= 0) {
                        this.focusSearchBarForNavigation();
                    } else {
                        this.selectCell(cells.get(selectedIndex - 1), true);
                    }
                }
                return true;
            }

            if (moveDown) {
                this.selectCell(cells.get((selectedIndex + 1) % cells.size()), true);
            } else {
                this.selectCell(cells.get((selectedIndex - 1 + cells.size()) % cells.size()), true);
            }
            return true;
        }

        if (searchBarNavigatable) {
            if (searchBarFocused) {
                this.selectCell(moveDown ? cells.get(0) : cells.get(cells.size() - 1), true);
            } else if (moveDown) {
                this.focusSearchBarForNavigation();
            } else {
                this.selectCell(cells.get(cells.size() - 1), true);
            }
            return true;
        }

        this.selectCell(moveDown ? cells.get(0) : cells.get(cells.size() - 1), true);
        return true;
    }

    /** Returns whether search bar navigatable. */
    protected boolean isSearchBarNavigatable() {
        return this.searchBarEnabled
                && (this.searchBar != null)
                && this.searchBar.visible
                && this.searchBar.active;
    }

    /** Updates focus or selection for search bar for navigation. */
    protected void focusSearchBarForNavigation() {
        if (!this.isSearchBarNavigatable()) {
            return;
        }
        this.selectCell(null, false);
        this.searchBar.setFocused(true);
        this.setFocused(this.searchBar);
    }

    /** Returns whether auto focus search bar for typing. */
    protected boolean shouldAutoFocusSearchBarForTyping() {
        return this.isSearchBarNavigatable() && (this.getSelectedCell() == null);
    }

    /** Handles horizontal navigation for this cell screen. */
    protected boolean handleHorizontalNavigation(boolean moveRight, int keycode, int scancode, int modifiers) {
        if (this.searchBarEnabled && (this.searchBar != null) && this.searchBar.isFocused()) {
            return this.searchBar.keyPressed(keycode, scancode, modifiers);
        }

        RenderCell selected = this.getSelectedCell();
        if (selected == null) {
            GuiEventListener focused = this.getFocused();
            if (focused != null) {
                focused.keyPressed(new KeyEvent(keycode, scancode, modifiers));
            }
            return true;
        }

        if (selected instanceof TextInputCell textInputCell) {
            return this.handleTextInputHorizontalNavigation(textInputCell, moveRight, keycode, scancode, modifiers);
        }

        List<GuiEventListener> focusTargets = this.getNavigatableTargets(selected);
        if (focusTargets.isEmpty()) {
            return true;
        }

        if (focusTargets.size() == 1) {
            return focusTargets.get(0).keyPressed(new KeyEvent(keycode, scancode, modifiers));
        }

        return this.navigateHorizontalTargets(selected, focusTargets, moveRight);
    }

    /** Handles text input horizontal navigation for this cell screen. */
    protected boolean handleTextInputHorizontalNavigation(@NotNull TextInputCell cell, boolean moveRight, int keycode, int scancode, int modifiers) {
        List<GuiEventListener> focusTargets = this.getNavigatableTargets(cell);
        if (focusTargets.isEmpty()) {
            return true;
        }

        GuiEventListener focusedTarget = this.getFocusedTarget(cell, focusTargets);
        if (focusedTarget == cell.editBox) {
            if (!net.minecraft.client.Minecraft.getInstance().hasShiftDown() && !InputUtils.isGuiShortcutModifierDown(modifiers) && !net.minecraft.client.Minecraft.getInstance().hasAltDown()) {
                boolean canJumpToEditorButton = cell.allowEditor
                        && (cell.openEditorButton != null)
                        && this.isNavigatableTarget(cell.openEditorButton);
                if (canJumpToEditorButton) {
                    int cursorPos = cell.editBox.getCursorPosition();
                    int highlightPos = cell.editBox.getHighlightPosition();
                    int valueLength = cell.editBox.getValue().length();
                    boolean atStart = (cursorPos <= 0) && (highlightPos <= 0);
                    boolean atEnd = (cursorPos >= valueLength) && (highlightPos >= valueLength);
                    if ((!moveRight && atStart) || (moveRight && atEnd)) {
                        this.focusTarget(cell, cell.openEditorButton);
                        return true;
                    }
                }
            }
            return cell.editBox.keyPressed(keycode, scancode, modifiers);
        }

        if (focusTargets.size() == 1) {
            return focusTargets.get(0).keyPressed(new KeyEvent(keycode, scancode, modifiers));
        }

        GuiEventListener target = this.getAdjacentHorizontalTarget(cell, focusTargets, moveRight);
        if (target != null) {
            this.focusTarget(cell, target);
            if ((target == cell.editBox) && (focusedTarget != cell.editBox)) {
                if (moveRight) {
                    cell.editBox.setCursorPosition(0);
                    cell.editBox.setHighlightPos(0);
                    cell.editBox.setDisplayPosition(0);
                } else {
                    int end = cell.editBox.getValue().length();
                    cell.editBox.setCursorPosition(end);
                    cell.editBox.setHighlightPos(end);
                }
            }
        }
        return true;
    }

    /** Moves keyboard focus between eligible targets in the current row. */
    protected boolean navigateHorizontalTargets(@NotNull RenderCell cell, @NotNull List<GuiEventListener> focusTargets, boolean moveRight) {
        GuiEventListener target = this.getAdjacentHorizontalTarget(cell, focusTargets, moveRight);
        if (target == null) {
            return true;
        }
        this.focusTarget(cell, target);
        return true;
    }

    /** Returns adjacent horizontal target. */
    @Nullable
    protected GuiEventListener getAdjacentHorizontalTarget(@NotNull RenderCell cell, @NotNull List<GuiEventListener> focusTargets, boolean moveRight) {
        if (focusTargets.isEmpty()) {
            return null;
        }

        GuiEventListener focusedTarget = this.getFocusedTarget(cell, focusTargets);
        int focusedIndex = (focusedTarget != null) ? focusTargets.indexOf(focusedTarget) : -1;

        if (focusedIndex < 0) {
            return moveRight ? focusTargets.get(0) : focusTargets.get(focusTargets.size() - 1);
        }
        if (moveRight) {
            return focusTargets.get((focusedIndex + 1) % focusTargets.size());
        }
        return focusTargets.get((focusedIndex - 1 + focusTargets.size()) % focusTargets.size());
    }

    /** Returns navigatable targets. */
    @NotNull
    protected List<GuiEventListener> getNavigatableTargets(@NotNull RenderCell cell) {
        List<GuiEventListener> targets = new ArrayList<>();
        for (GuiEventListener listener : cell.children()) {
            if (this.isNavigatableTarget(listener)) {
                targets.add(listener);
            }
        }
        targets.sort(Comparator
                .comparingInt((GuiEventListener listener) -> listener.getRectangle().top())
                .thenComparingInt(listener -> listener.getRectangle().left()));
        return targets;
    }

    /** Returns focused target. */
    @Nullable
    protected GuiEventListener getFocusedTarget(@NotNull RenderCell cell, @NotNull List<GuiEventListener> focusTargets) {
        GuiEventListener focused = cell.getFocused();
        if ((focused != null) && focusTargets.contains(focused)) {
            return focused;
        }
        GuiEventListener screenFocused = this.getFocused();
        if ((screenFocused != null) && focusTargets.contains(screenFocused)) {
            return screenFocused;
        }
        return null;
    }

    /** Returns preferred focus target. */
    @Nullable
    protected GuiEventListener getPreferredFocusTarget(@NotNull RenderCell cell, @NotNull List<GuiEventListener> focusTargets) {
        if (focusTargets.isEmpty()) {
            return null;
        }
        if (cell instanceof TextInputCell textInputCell) {
            if (this.isNavigatableTarget(textInputCell.editBox)) {
                return textInputCell.editBox;
            }
        }
        return focusTargets.get(0);
    }

    /** Updates focus or selection for target. */
    protected void focusTarget(@NotNull RenderCell cell, @Nullable GuiEventListener target) {
        cell.setFocused(target);
        this.setFocused(cell);
    }

    /** Returns whether navigatable target. */
    protected boolean isNavigatableTarget(@Nullable GuiEventListener listener) {
        if (listener == null) {
            return false;
        }
        if (listener instanceof NavigatableWidget navigatableWidget) {
            if (!navigatableWidget.isFocusable() || !navigatableWidget.isNavigatable()) {
                return false;
            }
        }
        if (listener instanceof AbstractWidget widget) {
            return widget.visible && widget.active;
        }
        return true;
    }

    /** Returns selectable visible cells. */
    @NotNull
    protected List<RenderCell> getSelectableVisibleCells() {
        List<RenderCell> cells = new ArrayList<>();
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if ((entry instanceof CellScrollEntry cellEntry) && cellEntry.cell.isSelectable()) {
                cells.add(cellEntry.cell);
            }
        }
        return cells;
    }

    /** Returns cell entry. */
    @Nullable
    protected CellScrollEntry getCellEntry(@NotNull RenderCell cell) {
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if ((entry instanceof CellScrollEntry cellEntry) && (cellEntry.cell == cell)) {
                return cellEntry;
            }
        }
        return null;
    }

    /** Updates focus or selection for cell. */
    protected void selectCell(@Nullable RenderCell cell, boolean focusPreferredWidget) {
        if ((cell != null) && !cell.isSelectable()) {
            return;
        }

        RenderCell previous = this.selectedCell;
        if (previous == cell) {
            if (cell != null) {
                this.ensureCellVisible(cell);
                if (focusPreferredWidget) {
                    List<GuiEventListener> focusTargets = this.getNavigatableTargets(cell);
                    GuiEventListener preferred = this.getPreferredFocusTarget(cell, focusTargets);
                    if (preferred != null) {
                        this.focusTarget(cell, preferred);
                    }
                }
            }
            return;
        }

        this.clearFocusForAllCells();

        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry instanceof CellScrollEntry cellEntry) {
                cellEntry.cell.selected = false;
            }
        }

        this.selectedCell = null;

        if (cell != null) {
            cell.selected = true;
            this.selectedCell = cell;
            this.ensureCellVisible(cell);
            if (focusPreferredWidget) {
                List<GuiEventListener> focusTargets = this.getNavigatableTargets(cell);
                GuiEventListener preferred = this.getPreferredFocusTarget(cell, focusTargets);
                if (preferred != null) {
                    this.focusTarget(cell, preferred);
                } else {
                    this.setFocused(cell);
                }
            } else {
                this.setFocused(cell);
            }
            if (this.searchBar != null) {
                this.searchBar.setFocused(false);
            }
        }

        if (previous != this.selectedCell) {
            this.updateDescriptionArea();
        }
    }

    /** Clears focus for all cells state. */
    protected void clearFocusForAllCells() {
        this.clearFocus();
        this.setFocused(null);
        if (this.searchBar != null) {
            this.searchBar.setFocused(false);
        }
        for (RenderCell cell : this.allCells) {
            this.clearFocusForCell(cell);
        }
    }

    /** Clears focus for cell state. */
    protected void clearFocusForCell(@Nullable RenderCell cell) {
        if (cell == null) {
            return;
        }
        cell.setFocused(null);
        for (GuiEventListener child : cell.children()) {
            if (child instanceof AbstractContainerEventHandler container) {
                GuiEventListener focusedChild = container.getFocused();
                if (focusedChild != null) {
                    focusedChild.setFocused(false);
                }
                container.setFocused(null);
            }
            child.setFocused(false);
        }
    }

    /** Ensures cell visible is visible in the current viewport. */
    protected void ensureCellVisible(@NotNull RenderCell cell) {
        CellScrollEntry entry = this.getCellEntry(cell);
        if (entry == null) {
            return;
        }

        float totalScrollHeight = this.scrollArea.getTotalScrollHeight();
        if (totalScrollHeight <= 0.0F) {
            return;
        }

        float innerY = this.scrollArea.getInnerY();
        float innerHeight = this.scrollArea.getInnerHeight();
        float entryTopUnscrolled = innerY;
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e == entry) {
                break;
            }
            entryTopUnscrolled += e.getHeight();
        }

        float entryTop = entryTopUnscrolled + this.scrollArea.getEntryRenderOffsetY(totalScrollHeight);
        float entryBottom = entryTop + entry.getHeight();
        float innerBottom = innerY + innerHeight;

        float scroll = this.scrollArea.verticalScrollBar.getScroll();
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
            this.scrollArea.verticalScrollBar.setScroll(newScroll);
        }
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double $$0, double $$1, int $$2) {
        if (this.searchBarEnabled && (this.searchBar != null) && !this.searchBar.isHovered()) {
            this.searchBar.setFocused(false);
        }
        return super.mouseClicked(new MouseButtonEvent($$0, $$1, new MouseButtonInfo($$2, 0)), false);
    }

    /** Represents one renderable, focusable cell scroll entry. */
    protected class CellScrollEntry extends ScrollAreaEntry {

        /** Cell whose value and description are represented by this row. */
        public final RenderCell cell;

        /** Wraps a render cell as an entry in the supplied scroll area. */
        public CellScrollEntry(@NotNull ScrollArea parent, @NotNull RenderCell cell) {
            super(parent, 10, 10);
            this.clickable = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.playClickSound = false;
            this.setBackgroundColorHover(this.getBackgroundColorNormal());
            this.cell = cell;
        }

        /** Renders entry into the active GUI extraction pass. */
        @Override
        public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            this.cell.updateSize(this);
            this.setWidth(this.cell.getWidth() + 40);
            if (this.getWidth() < this.parent.getInnerWidth()) this.setWidth(this.parent.getInnerWidth());
            this.setHeight(this.cell.getHeight());
            this.cell.updatePosition(this);
            //Use the scroll entry position and size to check for cell hover, to cover the whole cell line and not just the (sometimes too small) actual cell size
            this.cell.hovered = UIBase.isXYInArea(mouseX, mouseY, this.getX(), this.getY(), this.parent.getInnerWidth(), this.getHeight());
            if ((cell.isSelectable() && cell.isHovered()) || (cell == CellScreen.this.selectedCell)) {
                RenderingUtils.resetShaderColor(graphics);
                this.renderRoundedEntryBackground(graphics, partial, this.cell.hoverColorSupplier.get().getColorInt());
                RenderingUtils.resetShaderColor(graphics);
            }
            this.cell.extractRenderState(graphics, mouseX, mouseY, partial);
        }

        /** Handles click for this cell scroll entry. */
        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }

    }

    /** Represents one configurable separator cell in a cell-based screen. */
    public class SeparatorCell extends RenderCell {

        /** Supplies the separator color for the current theme state. */
        protected Supplier<DrawableColor> separatorColorSupplier = () -> UIBase.getUITheme().ui_interface_widget_border_color;
        /** Separator thickness in GUI pixels. */
        protected int separatorThickness = 1;

        /** Creates an empty separator cell with default state. */
        public SeparatorCell() {
            this.setHeight(10);
            super.setSelectable(false);
        }

        /** Creates a fixed-height cell that renders a visual separator. */
        public SeparatorCell(int height) {
            this.setHeight(height);
            super.setSelectable(false);
        }

        /** Returns the vertical padding reserved above and below content in GUI units. */
        @Override
        public int getTopBottomSpace() {
            return 0;
        }

        /** Renders cell into the active GUI extraction pass. */
        @Override
        public void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            int centerY = this.getY() + (this.getHeight() / 2);
            int halfThickness = Math.max(1, this.separatorThickness / 2);
            graphics.fill(this.getX(), centerY - ((halfThickness > 1) ? halfThickness : 0), this.getX() + this.getWidth(), centerY + halfThickness, this.separatorColorSupplier.get().getColorInt());
            RenderingUtils.resetShaderColor(graphics);
        }

        /** Refreshes size from current state. */
        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int)(CellScreen.this.scrollArea.getInnerWidth() - 40));
        }

        /** Returns separator color supplier. */
        @NotNull
        public Supplier<DrawableColor> getSeparatorColorSupplier() {
            return this.separatorColorSupplier;
        }

        /** Sets separator color supplier for this separator cell. */
        public SeparatorCell setSeparatorColorSupplier(@NotNull Supplier<DrawableColor> separatorColorSupplier) {
            this.separatorColorSupplier = separatorColorSupplier;
            return this;
        }

        /** Returns separator thickness. */
        public int getSeparatorThickness() {
            return this.separatorThickness;
        }

        /** Sets separator thickness for this separator cell. */
        public SeparatorCell setSeparatorThickness(int separatorThickness) {
            this.separatorThickness = separatorThickness;
            return this;
        }

    }

    /** Represents one configurable spacer cell in a cell-based screen. */
    public class SpacerCell extends RenderCell {

        /** Creates a fixed-height empty cell. */
        public SpacerCell(int height) {
            this.setHeight(height);
            this.setWidth(10);
            super.setSelectable(false);
        }

        /** Returns the vertical padding reserved above and below content in GUI units. */
        @Override
        public int getTopBottomSpace() {
            return 0;
        }

        /** Reports whether this entry can become selected. */
        @Override
        public boolean isSelectable() {
            return false;
        }

        /** Sets selectable for this spacer cell. */
        @Override
        public RenderCell setSelectable(boolean selectable) {
            throw new RuntimeException("You can't make SpacerCells selectable.");
        }

        /** Renders cell into the active GUI extraction pass. */
        @Override
        public void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        }

        /** Refreshes size from current state. */
        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
        }

    }

    /** Represents one configurable widget cell in a cell-based screen. */
    public class WidgetCell extends RenderCell {

        /** Widget exposed as a screen cell. */
        public final AbstractWidget widget;

        /** Wraps a vanilla widget as a cell with optional Konkrete skinning. */
        public WidgetCell(@NotNull AbstractWidget widget, boolean applyDefaultSkin) {
            this.widget = widget;
            if (applyDefaultSkin) UIBase.applyDefaultWidgetSkinTo(this.widget);
            this.children().add(this.widget);
            this.setSearchStringSupplier(() -> this.widget.getMessage().getString());
        }

        /** Renders cell into the active GUI extraction pass. */
        @Override
        public void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            this.widget.setX(this.getX());
            this.widget.setY(this.getY());
            this.widget.setWidth(this.getWidth());
        }

        /** Refreshes size from current state. */
        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int)(CellScreen.this.scrollArea.getInnerWidth() - 40));
            this.setHeight(this.widget.getHeight());
        }

    }

    /** Represents one configurable label cell in a cell-based screen. */
    public class LabelCell extends RenderCell {

        /** Text rendered by this cell widget. */
        @NotNull
        protected Component text;

        /** Creates a cell that renders the supplied component label. */
        public LabelCell(@NotNull Component label) {
            this.text = label;
            this.setSearchStringSupplier(() -> this.text.getString());
            super.setSelectable(false);
        }

        /** Renders cell into the active GUI extraction pass. */
        @Override
        public void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            RenderingUtils.resetShaderColor(graphics);
            UIBase.renderText(graphics, this.text, this.getX(), this.getY());
            RenderingUtils.resetShaderColor(graphics);
        }

        /** Refreshes size from current state. */
        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int)UIBase.getUITextWidthNormal(this.text));
            this.setHeight((int)UIBase.getUITextHeightNormal());
        }

        /** Returns text. */
        @NotNull
        public Component getText() {
            return this.text;
        }

        /** Sets text for this label cell. */
        public LabelCell setText(@NotNull Component text) {
            this.text = text;
            return this;
        }

    }

    /** Represents one configurable text input cell in a cell-based screen. */
    public class TextInputCell extends RenderCell {

        /** Edit box displayed by this editable cell. */
        public ExtendedEditBox editBox;
        /** Button that opens this cell's value in the multiline editor. */
        public ExtendedButton openEditorButton;
        /** Whether opening the value editor is allowed. */
        public final boolean allowEditor;
        /** Whether editor widget bounds have been initialized. */
        protected boolean widgetSizesSet = false;
        /** Copies multiline editor output back into this cell's escaped single-line value. */
        protected BiConsumer<String, TextInputCell> editorCallback = (s, cell) -> cell.editBox.setValue(s.replace("\n", "\\n"));
        /** Converts the cell's escaped value into multiline editor input. */
        protected ConsumingSupplier<TextInputCell, String> editorSetTextSupplier = consumes -> {
            if (this.editorMultiLineMode) {
                return consumes.editBox.getValue().replace("\\n", "\n");
            }
            return consumes.editBox.getValue().replace("\n", "\\n");
        };
        /** Whether the cell editor accepts multiple lines. */
        protected boolean editorMultiLineMode = false;

        /** Configures a filtered input cell and whether external editor features are available. */
        public TextInputCell(@Nullable CharacterFilter characterFilter, boolean allowEditor, boolean allowEditorPlaceholders) {

            this.allowEditor = allowEditor;

            this.editBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 20, 18, Component.empty());
            this.editBox.setMaxLength(1000000);
            this.editBox.setCharacterFilter(characterFilter);
            UIBase.applyDefaultWidgetSkinTo(this.editBox);
            this.children().add(this.editBox);

            if (this.allowEditor) {
                this.openEditorButton = new ExtendedButton(0, 0, 20, 20, Component.translatable("konkrete.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                    if (allowEditor) {
                        TextEditorWindowBody s = new TextEditorWindowBody(Component.translatable("konkrete.ui.screens.string_builder_screen.edit_in_editor"), (characterFilter != null) ? characterFilter.convertToLegacyFilter() : null, callback -> {
                            if (callback != null) {
                                this.editorCallback.accept(callback, this);
                            }
                        });
                        s.setMultilineMode(this.editorMultiLineMode);
                        s.setPlaceholdersAllowed(allowEditorPlaceholders);
                        s.setText(this.editorSetTextSupplier.get(this));
                        Dialogs.openGeneric(s, Component.translatable("konkrete.ui.screens.string_builder_screen.edit_in_editor"), null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
                    }
                });
                UIBase.applyDefaultWidgetSkinTo(this.openEditorButton);
                this.children().add(this.openEditorButton);
            }

            this.setSearchStringSupplier(() -> this.editBox.getValue());

        }

        /** Renders cell into the active GUI extraction pass. */
        @Override
        public void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

            if (!this.widgetSizesSet) {
                this.setWidgetSizes();
                this.widgetSizesSet = true;
            }

            this.editBox.setX(this.getX() + 1);
            this.editBox.setY(this.getY() + 1);

            if (this.allowEditor) {
                this.openEditorButton.setX(this.getX() + this.getWidth() - this.openEditorButton.getWidth());
                this.openEditorButton.setY(this.getY());
            }

            if (MouseInput.isLeftMouseDown() && !this.editBox.isHovered()) {
                this.editBox.setFocused(false);
            }

        }

        /** Sets widget sizes for this text input cell. */
        protected void setWidgetSizes() {

            int editorButtonWidth = (this.allowEditor ? Minecraft.getInstance().font.width(this.openEditorButton.getLabelSupplier().get(this.openEditorButton)) : 0) + 6;

            this.editBox.setWidth(this.allowEditor ? this.getWidth() - editorButtonWidth - 5 : this.getWidth());

            if (this.allowEditor) {
                this.openEditorButton.setWidth(editorButtonWidth);
            }

        }

        /** Sets editor preset text supplier for this text input cell. */
        public TextInputCell setEditorPresetTextSupplier(@NotNull ConsumingSupplier<TextInputCell, String> supplier) {
            this.editorSetTextSupplier = Objects.requireNonNull(supplier);
            return this;
        }

        /** Sets editor callback for this text input cell. */
        public TextInputCell setEditorCallback(@NotNull BiConsumer<String, TextInputCell> callback) {
            this.editorCallback = Objects.requireNonNull(callback);
            return this;
        }

        /** Sets edit listener for this text input cell. */
        public TextInputCell setEditListener(@Nullable Consumer<String> listener) {
            this.editBox.setResponder(listener);
            return this;
        }

        /** Returns text. */
        @NotNull
        public String getText() {
            return this.editBox.getValue();
        }

        /** Sets text for this text input cell. */
        public TextInputCell setText(@Nullable String text) {
            if (text == null) text = "";
            this.editBox.setValue(text);
            this.editBox.setCursorPosition(0);
            this.editBox.setHighlightPos(0);
            this.editBox.setDisplayPosition(0);
            return this;
        }

        /** Returns whether editor multi line mode. */
        public boolean isEditorMultiLineMode() {
            return editorMultiLineMode;
        }

        /** Sets editor multi line mode for this text input cell. */
        public TextInputCell setEditorMultiLineMode(boolean editorMultiLineMode) {
            this.editorMultiLineMode = editorMultiLineMode;
            return this;
        }

        /** Sets tooltip for this text input cell. */
        public TextInputCell setTooltip(@NotNull Supplier<UITooltip> tooltip) {
            this.editBox.setUITooltip(tooltip);
            return this;
        }

    }

    /** Base implementation for render cell. */
    public abstract class RenderCell extends AbstractContainerEventHandler implements Renderable, NarratableEntry {

        /** Horizontal component of the current transform. */
        protected int x;
        /** Vertical component of the current transform. */
        protected int y;
        /** Width in GUI units for width. */
        protected int width;
        /** Height in GUI units for height. */
        protected int height;
        private boolean selectable = true;
        private boolean selected = false;
        /** Whether the pointer currently hovers this element. */
        protected boolean hovered = false;
        /** Supplies the cell hover color for the current theme state. */
        protected Supplier<DrawableColor> hoverColorSupplier = () -> {
            if (CellScreen.this.scrollArea != null
                    && CellScreen.this.scrollArea.isSetupForBlurInterface()
                    && UIBase.shouldBlur()) {
                return UIBase.getUITheme().ui_blur_interface_area_entry_selected_color;
            }
            return UIBase.getUITheme().ui_interface_area_entry_selected_color;
        };
        /** Optionally supplies description lines for the selected cell. */
        @Nullable
        protected Supplier<List<Component>> descriptionSupplier = null;
        /** Supplies the current text used to filter this section's cells. */
        @NotNull
        protected Supplier<String> searchStringSupplier = () -> null;
        /** Child listeners in focus and input-routing order. */
        protected final List<GuiEventListener> children = new ArrayList<>();
        /** Per-section scratch values retained while rebuilding filtered cells. */
        protected final Map<String, String> memory = new HashMap<>();
        /** Whether this cell remains visible regardless of the search query. */
        protected boolean ignoreSearch = false;

        /** Renders cell into the active GUI extraction pass. */
        public abstract void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial);

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

            if (!this.selectable) this.selected = false;

            this.renderCell(graphics, mouseX, mouseY, partial);

            for (GuiEventListener l : this.children) {
                if (l instanceof Renderable r) {
                    r.extractRenderState(graphics, mouseX, mouseY, partial);
                }
            }

        }

        /** Advances this object's lifecycle by one client tick. */
        public void tick() {
        }

        /** Refreshes size from current state. */
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int)(CellScreen.this.scrollArea.getInnerWidth() - 40));
            this.setHeight(20);
        }

        /** Refreshes position from current state. */
        protected void updatePosition(@NotNull CellScrollEntry scrollEntry) {
            this.setX((int)(scrollEntry.getX() + 20));
            this.setY((int)scrollEntry.getY());
        }

        /**
         * Returns a string used for searching this cell.
         * Return null to exclude this cell from search filtering.
         * By default, returns null.
         */
        @Nullable
        public String getSearchString() {
            return this.searchStringSupplier.get();
        }

        /** Returns search string supplier. */
        public @NotNull Supplier<String> getSearchStringSupplier() {
            return searchStringSupplier;
        }

        /** Sets search string supplier for this render cell. */
        public RenderCell setSearchStringSupplier(@NotNull Supplier<String> searchStringSupplier) {
            this.searchStringSupplier = searchStringSupplier;
            return this;
        }

        /**
         * Get the description supplier for this cell.
         * Returns null if no description is set.
         */
        @Nullable
        public Supplier<List<Component>> getDescriptionSupplier() {
            return this.descriptionSupplier;
        }

        /**
         * Set the description supplier for this cell.
         * The supplier should return a list of text components to display in the description area.
         * Set to null to remove the description.
         */
        public RenderCell setDescriptionSupplier(@Nullable Supplier<List<Component>> descriptionSupplier) {
            this.descriptionSupplier = descriptionSupplier;
            return this;
        }

        /** Returns the vertical padding reserved above and below content in GUI units. */
        public int getTopBottomSpace() {
            return 3;
        }

        /** Returns the horizontal position in GUI units. */
        public int getX() {
            return x;
        }

        /** Sets x for this render cell. */
        public RenderCell setX(int x) {
            this.x = x;
            return this;
        }

        /** Returns the vertical position in GUI units. */
        public int getY() {
            return y + this.getTopBottomSpace();
        }

        /** Sets y for this render cell. */
        public RenderCell setY(int y) {
            this.y = y;
            return this;
        }

        /** Returns the current width in GUI units. */
        public int getWidth() {
            return width;
        }

        /** Sets width for this render cell. */
        public RenderCell setWidth(int width) {
            this.width = width;
            return this;
        }

        /** Returns the current height in GUI units. */
        public int getHeight() {
            return height + (this.getTopBottomSpace() * 2);
        }

        /** Sets height for this render cell. */
        public RenderCell setHeight(int height) {
            this.height = height;
            return this;
        }

        /** Reports whether the pointer currently hovers this element. */
        public boolean isHovered() {
            return this.hovered;
        }

        /** Sets selected for this render cell. */
        public RenderCell setSelected(boolean selected) {
            this.selected = selected;
            if (!this.selectable) this.selected = false;
            // Update description area if enabled and selection changed
            if (CellScreen.this.descriptionAreaEnabled) {
                CellScreen.this.updateDescriptionArea();
            }
            return this;
        }

        /** Reports whether this entry is currently selected. */
        public boolean isSelected() {
            return this.selected;
        }

        /** Reports whether this entry can become selected. */
        public boolean isSelectable() {
            return this.selectable;
        }

        /** Sets selectable for this render cell. */
        public RenderCell setSelectable(boolean selectable) {
            this.selectable = selectable;
            if (!this.selectable) this.setSelected(false);
            return this;
        }

        /** Sets hover color supplier for this render cell. */
        public RenderCell setHoverColorSupplier(@NotNull Supplier<DrawableColor> hoverColorSupplier) {
            this.hoverColorSupplier = hoverColorSupplier;
            return this;
        }

        /** Writes the supplied value into native memory at the requested layout offset. */
        public RenderCell putMemoryValue(@NotNull String key, @NotNull String value) {
            this.memory.put(key, value);
            return this;
        }

        /** Sets ignore search for this render cell. */
        public RenderCell setIgnoreSearch() {
            this.ignoreSearch = true;
            return this;
        }

        /** Returns memory value. */
        @Nullable
        public String getMemoryValue(@NotNull String key) {
            return this.memory.get(key);
        }

        /** Returns child listeners used for focus and input routing. */
        @Override
        public @NotNull List<GuiEventListener> children() {
            return this.children;
        }

        /** Returns this component's priority in the narration order. */
        @Override
        public @NotNull NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        /** Refreshes narration from current state. */
        @Override
        public void updateNarration(@NotNull NarrationElementOutput var1) {
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button press and reports whether it was consumed. */
        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (CellScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            if (!CellScreen.this.scrollArea.isMouseOverInnerArea($$0, $$1)) {
                return false;
            }
            if (!this.isMouseOver($$0, $$1)) {
                return false;
            }
            if (this.selectable) {
                CellScreen.this.selectCell(this, false);
            }
            return super.mouseClicked(new MouseButtonEvent($$0, $$1, new MouseButtonInfo($$2, 0)), false);
        }

        /** Reports whether the current pointer position lies inside this element's hitbox. */
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (CellScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            if (!CellScreen.this.scrollArea.isMouseOverInnerArea(mouseX, mouseY)) {
                return false;
            }
            CellScrollEntry entry = CellScreen.this.getCellEntry(this);
            if (entry == null) {
                return false;
            }
            return UIBase.isXYInArea(mouseX, mouseY, entry.getX(), entry.getY(), CellScreen.this.scrollArea.getInnerWidth(), entry.getHeight());
        }

        /** Routes pointer dragging and reports whether it was consumed. */
        @Override
        public boolean mouseDragged(MouseButtonEvent event, double $$3, double $$4) {
            return this.mouseDragged(event.x(), event.y(), event.button(), $$3, $$4);
        }

        /** Routes pointer dragging and reports whether it was consumed. */
        public boolean mouseDragged(double $$0, double $$1, int $$2, double $$3, double $$4) {
            if (CellScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            return super.mouseDragged(new MouseButtonEvent($$0, $$1, new MouseButtonInfo($$2, 0)), $$3, $$4);
        }

        /** Routes a mouse-button release and reports whether it was consumed. */
        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            return this.mouseReleased(event.x(), event.y(), event.button());
        }

        /** Routes a mouse-button release and reports whether it was consumed. */
        public boolean mouseReleased(double $$0, double $$1, int $$2) {
            if (CellScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            return super.mouseReleased(new MouseButtonEvent($$0, $$1, new MouseButtonInfo($$2, 0)));
        }

    }

    /** Reserves adjustable horizontal space on the right side of a cell row. */
    protected class RightSideSpacer extends AbstractWidget {

        /** Reserves vertical space while keeping content clear of right-side controls. */
        protected RightSideSpacer(int height) {
            super(0, 0, 0, height, Component.empty());
        }

        /** Sets focused for this right side spacer. */
        @Override
        public void setFocused(boolean var1) {
        }

        /** Adds this widget's draw state to the active GUI extraction pass. */
        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int var2, int var3, float var4) {
        }

        /** Reports whether keyboard focus currently targets this control. */
        @Override
        public boolean isFocused() {
            return false;
        }

        /** Publishes this widget's current narration data. */
        @Override
        protected void updateWidgetNarration(NarrationElementOutput var1) {
        }

    }

    /** Represents one renderable, focusable spacer scroll area entry. */
    public static class SpacerScrollAreaEntry extends TextScrollAreaEntry {

        private int spacerHeight;

        /** Reserves a fixed vertical gap in the supplied scroll area. */
        public SpacerScrollAreaEntry(ScrollArea parent, int height) {
            super(parent, Component.empty(), button -> {});
            this.spacerHeight = height;
            this.height = height;
        }

        /** Returns the current height in GUI units. */
        @Override
        public float getHeight() {
            return this.spacerHeight;
        }

        /** Sets height for this spacer scroll area entry. */
        @Override
        public void setHeight(float height) {
            this.spacerHeight = (int) height;
        }

    }

}
