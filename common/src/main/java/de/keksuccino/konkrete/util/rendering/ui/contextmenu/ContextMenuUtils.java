package de.keksuccino.konkrete.util.rendering.ui.contextmenu;

import de.keksuccino.konkrete.util.ScreenUtils;

import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.file.FileFilter;
import de.keksuccino.konkrete.util.file.GameDirectoryUtils;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.file.type.groups.FileTypeGroup;
import de.keksuccino.konkrete.util.file.type.groups.FileTypeGroups;
import de.keksuccino.konkrete.util.input.CharacterFilter;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.konkrete.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.UIConfiguration;
import de.keksuccino.konkrete.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.konkrete.util.rendering.ui.screen.RangeSliderWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.screen.filebrowser.ChooseFileWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.screen.resource.ResourceChooserWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.util.resource.Resource;
import de.keksuccino.konkrete.util.resource.ResourceSupplier;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import de.keksuccino.konkrete.util.resource.resources.text.IText;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.resource.resources.video.IVideo;
import de.keksuccino.konkrete.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Contains stateless helpers for context menu. */
@SuppressWarnings("all")
public class ContextMenuUtils {

    private static void runPrimarySubMenuEntryClick(@NotNull ContextMenu.SubMenuContextMenuEntry entry, @NotNull String primaryEntryIdentifier) {
        ContextMenu.ContextMenuEntry<?> primaryEntry = entry.getSubContextMenu().getEntry(primaryEntryIdentifier);
        if (primaryEntry instanceof ContextMenu.ClickableContextMenuEntry<?> clickableEntry) {
            clickableEntry.runClickAction();
            return;
        }
        entry.openSubMenu();
    }

    /** Builds a submenu entry that mirrors one child as its primary parent-level action. */
    @NotNull
    public static ContextMenu.SubMenuContextMenuEntry createPrimaryActionSubMenuEntry(@NotNull String entryIdentifier, @NotNull ContextMenu parentMenu, @NotNull Component label, @NotNull ContextMenu subContextMenu, @NotNull String primaryEntryIdentifier) {
        ContextMenu.SubMenuContextMenuEntry entry = new ContextMenu.SubMenuContextMenuEntry(entryIdentifier, parentMenu, label, subContextMenu);
        entry.clickAction = (menu, clickedEntry) -> runPrimarySubMenuEntryClick((ContextMenu.SubMenuContextMenuEntry) clickedEntry, primaryEntryIdentifier);
        return entry;
    }

    /** Adds image resource chooser context menu entry to to this context menu utils. */
    public static ContextMenu.ClickableContextMenuEntry<?> addImageResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, ResourceSupplier<ITexture> defaultValue, @NotNull Supplier<ResourceSupplier<ITexture>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, () -> ResourceChooserWindowBody.image(null, file -> {}), ResourceSupplier::image, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.IMAGE_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /** Adds audio resource chooser context menu entry to to this context menu utils. */
    public static ContextMenu.ClickableContextMenuEntry<?> addAudioResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, ResourceSupplier<IAudio> defaultValue, @NotNull Supplier<ResourceSupplier<IAudio>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, () -> ResourceChooserWindowBody.audio(null, file -> {}), ResourceSupplier::audio, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.AUDIO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /** Adds video resource chooser context menu entry to to this context menu utils. */
    public static ContextMenu.ClickableContextMenuEntry<?> addVideoResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, ResourceSupplier<IVideo> defaultValue, @NotNull Supplier<ResourceSupplier<IVideo>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, () -> ResourceChooserWindowBody.video(null, file -> {}), ResourceSupplier::video, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.VIDEO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /** Adds text resource chooser context menu entry to to this context menu utils. */
    public static ContextMenu.ClickableContextMenuEntry<?> addTextResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, ResourceSupplier<IText> defaultValue, @NotNull Supplier<ResourceSupplier<IText>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
        return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, () -> ResourceChooserWindowBody.text(null, file -> {}), ResourceSupplier::text, defaultValue, targetFieldGetter, targetFieldSetter, label, addResetOption, FileTypeGroups.TEXT_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
    }

    /** Adds generic resource chooser context menu entry to to this context menu utils. */
    public static <R extends Resource, F extends FileType<R>> ContextMenu.ClickableContextMenuEntry<?> addGenericResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Supplier<ResourceChooserWindowBody<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull Supplier<ResourceSupplier<R>> targetFieldGetter, @NotNull Consumer<ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {

        ContextMenu subMenu = new ContextMenu();

        subMenu.addClickableEntry("choose_file", Component.translatable("konkrete.ui.resources.choose"),
                (menu, entry) -> {
                    menu.closeMenuChain();
                    ResourceSupplier<R> supplier = targetFieldGetter.get();
                    String preSelectedSource = (supplier != null) ? supplier.getSourceWithPrefix() : null;
                    ResourceChooserWindowBody<R,F> chooserScreen = resourceChooserScreenBuilder.get();
                    chooserScreen.setFileFilter(fileFilter);
                    chooserScreen.setAllowedFileTypes(fileTypes);
                    chooserScreen.setSource(preSelectedSource, false);
                    chooserScreen.setLocationSourceAllowed(allowLocation);
                    chooserScreen.setLocalSourceAllowed(allowLocal);
                    chooserScreen.setWebSourceAllowed(allowWeb);
                    chooserScreen.setResourceSourceCallback(source -> {
                        if (source != null) {
                            targetFieldSetter.accept(resourceSupplierBuilder.get(source));
                        }
                    });
                    chooserScreen.openInWindow(null);
                }).setStackable(false)
                .setIcon(MaterialIcons.FILE_OPEN);

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("konkrete.ui.resources.reset"),
                    (menu, entry) -> {
                        targetFieldSetter.accept(defaultValue);
                    }).setStackable(false)
                    .setIcon(MaterialIcons.UNDO);
        }

        Supplier<Component> currentValueDisplayLabelSupplier = () -> {
            Component valueComponent;
            ResourceSupplier<R> supplier = targetFieldGetter.get();
            String val = (supplier != null) ? supplier.getSourceWithoutPrefix() : null;
            if (val == null) {
                valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_color.getColorInt()));
            } else {
                val = GameDirectoryUtils.getPathWithoutGameDirectory(val);
                if (Minecraft.getInstance().font.width(val) > 150) {
                    val = new StringBuilder(val).reverse().toString();
                    val = Minecraft.getInstance().font.plainSubstrByWidth(val, 150);
                    val = new StringBuilder(val).reverse().toString();
                    val = ".." + val;
                }
                valueComponent = Component.literal(val).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_color.getColorInt()));
            }
            return Component.translatable("konkrete.ui.resources.current", valueComponent);
        };
        subMenu.addSeparatorEntry("separator_before_current_value_display");
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
                .setClickSoundEnabled(false)
                .setIcon(MaterialIcons.INFO);

        return addTo.addEntry(createPrimaryActionSubMenuEntry(entryIdentifier, addTo, label, subMenu, "choose_file")
                .setStackable(true)
                .setIcon(MaterialIcons.FOLDER_OPEN));

    }

    /** Adds file chooser context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFileChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable FileFilter fileFilter) {
        return addFileChooserContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, fileFilter, null);
    }

    /** Adds file chooser context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFileChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable FileFilter fileFilter, @Nullable FileTypeGroup<?> fileTypes) {
        return addFileChooserContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, fileFilter, fileTypes, null);
    }

    /** Adds file chooser context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFileChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable FileFilter fileFilter, @Nullable FileTypeGroup<?> fileTypes, @Nullable BiConsumer<Screen, File> onCloseFileChooser) {

        ContextMenu subMenu = new ContextMenu();

        subMenu.addClickableEntry("choose_file", Component.translatable("konkrete.ui.filechooser.choose.file"), (menu, entry) -> {
            menu.closeMenuChain();
            File rootDirectory = UIConfiguration.get().dataDirectory().toFile();
            File startDir = rootDirectory;
            String path = getter.get();
            if (path != null) {
                startDir = new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(path)).getParentFile();
                if (startDir == null) startDir = rootDirectory;
            }
            Screen current = ScreenUtils.getScreen();
            ChooseFileWindowBody fileChooser = new ChooseFileWindowBody(rootDirectory, startDir, (call) -> {
                if (call != null) {
                    setter.accept(call.getPath());
                }
                menu.closeMenu();
                if (onCloseFileChooser != null) {
                    onCloseFileChooser.accept(current, call);
                }
            });
            fileChooser.setVisibleDirectoryLevelsAboveRoot(2);
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setFileTypes(fileTypes);
            fileChooser.openInWindow(null);
        }).setIcon(MaterialIcons.FILE_OPEN);

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("konkrete.editor.filechooser.reset"), (menu, entry) -> {
                setter.accept(defaultValue);
            }).setIcon(MaterialIcons.UNDO);
        }

        Supplier<Component> currentValueDisplayLabelSupplier = () -> {
            Component valueComponent;
            String val = getter.get();
            if (val == null) {
                valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_color.getColorInt()));
            } else {
                val = GameDirectoryUtils.getPathWithoutGameDirectory(val);
                if (Minecraft.getInstance().font.width(val) > 150) {
                    val = new StringBuilder(val).reverse().toString();
                    val = Minecraft.getInstance().font.plainSubstrByWidth(val, 150);
                    val = new StringBuilder(val).reverse().toString();
                    val = ".." + val;
                }
                valueComponent = Component.literal(val).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_color.getColorInt()));
            }
            return Component.translatable("konkrete.context_menu.entries.choose_or_set.current", valueComponent);
        };
        subMenu.addSeparatorEntry("separator_before_current_value_display");
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .setIcon(MaterialIcons.INFO);

        return addTo.addEntry(createPrimaryActionSubMenuEntry(entryIdentifier, addTo, label, subMenu, "choose_file")
                .setIcon(MaterialIcons.FOLDER_OPEN));
    }

    /** Adds generic input context menu entry to to this context menu utils. */
    @NotNull
    public static <T> ContextMenu.ClickableContextMenuEntry<?> addGenericInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter, boolean addResetOption, T defaultValue, @NotNull Consumer<Consumer<T>> inputLogic) {
        return addGenericInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, inputLogic, MaterialIcons.TEXT_FIELDS, MaterialIcons.EDIT);
    }

    @NotNull
    private static <T> ContextMenu.ClickableContextMenuEntry<?> addGenericInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter, boolean addResetOption, T defaultValue, @NotNull Consumer<Consumer<T>> inputLogic, @NotNull MaterialIcon inputIcon, @NotNull MaterialIcon parentIcon) {

        ContextMenu subMenu = new ContextMenu();

        subMenu.addClickableEntry("input_value", Component.translatable("konkrete.common_components.set"), (menu, entry) -> {
            menu.closeMenuChain();
            inputLogic.accept(setter);
        }).setIcon(inputIcon);

        if (addResetOption) {
            subMenu.addClickableEntry("reset_to_default", Component.translatable("konkrete.common_components.reset"), (menu, entry) -> {
                setter.accept(defaultValue);
            }).setIcon(MaterialIcons.UNDO);
        }

        Supplier<Component> currentValueDisplayLabelSupplier = () -> {
            Component valueComponent;
            T val = getter.get();
            if (val == null) {
                valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_color.getColorInt()));
            } else {
                String valString = val.toString();
                if (Minecraft.getInstance().font.width(valString) > 150) {
                    valString = new StringBuilder(valString).reverse().toString();
                    valString = Minecraft.getInstance().font.plainSubstrByWidth(valString, 150);
                    valString = new StringBuilder(valString).reverse().toString();
                    valString = ".." + valString;
                }
                valueComponent = Component.literal(valString).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_color.getColorInt()));
            }
            return Component.translatable("konkrete.context_menu.entries.choose_or_set.current", valueComponent);
        };
        subMenu.addSeparatorEntry("separator_before_current_value_display");
        subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
                .setClickSoundEnabled(false)
                .setChangeBackgroundColorOnHover(false)
                .setIcon(MaterialIcons.INFO);

        return addTo.addEntry(createPrimaryActionSubMenuEntry(entryIdentifier, addTo, label, subMenu, "input_value")
                .setIcon(parentIcon));
    }

    /** Adds toggle context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addToggleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Supplier<Boolean> targetFieldGetter, @NotNull Consumer<Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
        return addTo.addClickableEntry(entryIdentifier, Component.literal(""), (menu, entry) -> {
            boolean current = Boolean.TRUE.equals(targetFieldGetter.get());
            targetFieldSetter.accept(!current);
        }).setLabelSupplier((menu, entry) -> {
            boolean enabledValue = Boolean.TRUE.equals(targetFieldGetter.get());
            if (enabledValue && entry.isActive()) {
                MutableComponent enabled = Component.translatable("konkrete.general.cycle.enabled_disabled.enabled")
                        .withStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_color.getColorInt()));
                return Component.translatable(labelLocalizationKeyBase, enabled);
            }
            MutableComponent disabled = Component.translatable("konkrete.general.cycle.enabled_disabled.disabled")
                    .withStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_color.getColorInt()));
            return Component.translatable(labelLocalizationKeyBase, disabled);
        }).setIcon(MaterialIcons.TOGGLE_ON);
    }

    /** Adds range slider input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addRangeSliderInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Double> getter, @NotNull Consumer<Double> setter, boolean addResetOption, double defaultValue, double minSliderValue, double maxSliderValue, @NotNull ConsumingSupplier<Double, Component> sliderLabelSupplier) {
        return addGenericInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, valueSetter -> {
            double presetValue = Objects.requireNonNullElse(getter.get(), 0.0D);
            RangeSliderWindowBody sliderScreen = new RangeSliderWindowBody(minSliderValue, maxSliderValue, presetValue, sliderLabelSupplier,
                    valueSetter::accept,
                    valueSetter::accept,
                    valueSetter::accept);
            PiPWindow window = new PiPWindow(label)
                    .setScreen(sliderScreen)
                    .setForceKonkreteUiScale(true)
                    .setMinSize(RangeSliderWindowBody.PIP_WINDOW_WIDTH, RangeSliderWindowBody.PIP_WINDOW_HEIGHT)
                    .setSize(RangeSliderWindowBody.PIP_WINDOW_WIDTH, RangeSliderWindowBody.PIP_WINDOW_HEIGHT);
            PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        }, MaterialIcons.SLIDERS, MaterialIcons.SLIDERS);
    }

    /** Adds input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, inputCharacterFilter, multiLineInput, allowPlaceholders, textValidator, textValidatorUserFeedback, null);
    }

    /** Adds input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<String> getter, @NotNull Consumer<String> setter, boolean addResetOption, String defaultValue, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        return addGenericInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, valueSetter -> {
            Screen current = ScreenUtils.getScreen();
            Screen inputScreen;
            if (!multiLineInput && !allowPlaceholders) {
                TextInputWindowBody s = new TextInputWindowBody(inputCharacterFilter, call -> {
                    if (call != null) {
                        valueSetter.accept(call);
                    }
                    if (onCloseEditor != null) {
                        onCloseEditor.accept(current, call);
                    }
                });
                if (textValidator != null) {
                    s.setTextValidator(consumes -> {
                        if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
                        return textValidator.get(consumes.getText());
                    });
                }
                Dialogs.openGeneric(s, label, null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                        .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                s.setText(getter.get());
                inputScreen = null;
            } else {
                TextEditorWindowBody s = new TextEditorWindowBody(label, (inputCharacterFilter != null) ? inputCharacterFilter.convertToLegacyFilter() : null, (call) -> {
                    if (call != null) {
                        valueSetter.accept(call);
                    }
                    if (onCloseEditor != null) {
                        onCloseEditor.accept(current, call);
                    }
                });
                if (textValidator != null) {
                    s.setTextValidator(consumes -> {
                        if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
                        return textValidator.get(consumes.getText());
                    });
                }
                s.setMultilineMode(multiLineInput);
                s.setPlaceholdersAllowed(allowPlaceholders);
                s.setText(getter.get());
                Dialogs.openGeneric(s, label, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                        .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                inputScreen = null;
            }
            if (inputScreen != null) {
                ScreenUtils.setScreen(inputScreen);
            }
        });
    }

    /** Adds integer input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Integer> getter, @NotNull Consumer<Integer> setter, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addIntegerInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, textValidator, textValidatorUserFeedback, null);
    }

    /** Adds integer input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Integer> getter, @NotNull Consumer<Integer> setter, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        ConsumingSupplier<String, Boolean> defaultIntegerValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isInteger(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isInteger(s)) setter.accept(Integer.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildIntegerFilter(),
                false, false, (textValidator != null) ? textValidator : defaultIntegerValidator, textValidatorUserFeedback, onCloseEditor);
    }

    /** Adds double input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Double> getter, @NotNull Consumer<Double> setter, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addDoubleInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, textValidator, textValidatorUserFeedback, null);
    }

    /** Adds double input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Double> getter, @NotNull Consumer<Double> setter, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        ConsumingSupplier<String, Boolean> defaultDoubleValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isDouble(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isDouble(s)) setter.accept(Double.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildDecimalFiler(),
                false, false, (textValidator != null) ? textValidator : defaultDoubleValidator, textValidatorUserFeedback, onCloseEditor);
    }

    /** Adds float input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Float> getter, @NotNull Consumer<Float> setter, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addFloatInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, textValidator, textValidatorUserFeedback, null);
    }

    /** Adds float input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Float> getter, @NotNull Consumer<Float> setter, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        ConsumingSupplier<String, Boolean> defaultFloatValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isFloat(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isFloat(s)) setter.accept(Float.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildDecimalFiler(),
                false, false, (textValidator != null) ? textValidator : defaultFloatValidator, textValidatorUserFeedback, onCloseEditor);
    }

    /** Adds long input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Long> getter, @NotNull Consumer<Long> setter, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback) {
        return addLongInputContextMenuEntryTo(addTo, entryIdentifier, label, getter, setter, addResetOption, defaultValue, textValidator, textValidatorUserFeedback, null);
    }

    /** Adds long input context menu entry to to this context menu utils. */
    @NotNull
    public static ContextMenu.ClickableContextMenuEntry<?> addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label, @NotNull Supplier<Long> getter, @NotNull Consumer<Long> setter, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, UITooltip> textValidatorUserFeedback, @Nullable BiConsumer<Screen, String> onCloseEditor) {
        ConsumingSupplier<String, Boolean> defaultLongValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isLong(consumes);
        return addInputContextMenuEntryTo(addTo, entryIdentifier, label,
                () -> String.valueOf(Objects.requireNonNullElse(getter.get(), "")),
                s -> {
                    if (MathUtils.isLong(s)) setter.accept(Long.valueOf(s));
                }, addResetOption, "" + defaultValue, CharacterFilter.buildIntegerFilter(),
                false, false, (textValidator != null) ? textValidator : defaultLongValidator, textValidatorUserFeedback, onCloseEditor);
    }

}
