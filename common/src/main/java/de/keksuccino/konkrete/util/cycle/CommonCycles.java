package de.keksuccino.konkrete.util.cycle;

import de.keksuccino.konkrete.util.enums.LocalizedEnum;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Creates commonly used localized value cycles. */
@SuppressWarnings("unused")
public class CommonCycles {

    /** Creates a localized on/off cycle. */
    @NotNull
    public static LocalizedEnumValueCycle<CycleOnOff> cycleOnOff(@NotNull String cycleLocalizationKey) {
        return LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleOnOff.ON, CycleOnOff.OFF);
    }

    /** Creates a localized on/off cycle. */
    @NotNull
    public static LocalizedEnumValueCycle<CycleOnOff> cycleOnOff(@NotNull String cycleLocalizationKey, @NotNull CycleOnOff selectedValue) {
        return (LocalizedEnumValueCycle<CycleOnOff>) LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleOnOff.ON, CycleOnOff.OFF).setCurrentValue(selectedValue);
    }

    /** Creates a localized on/off cycle. */
    @NotNull
    public static LocalizedEnumValueCycle<CycleOnOff> cycleOnOff(@NotNull String cycleLocalizationKey, boolean selectedValue) {
        return (LocalizedEnumValueCycle<CycleOnOff>) LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleOnOff.ON, CycleOnOff.OFF).setCurrentValue(CycleOnOff.getByBoolean(selectedValue));
    }

    /** Creates a localized enabled/disabled cycle. */
    @NotNull
    public static LocalizedEnumValueCycle<CycleEnabledDisabled> cycleEnabledDisabled(@NotNull String cycleLocalizationKey) {
        return LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleEnabledDisabled.ENABLED, CycleEnabledDisabled.DISABLED);
    }

    /** Creates a localized enabled/disabled cycle. */
    @NotNull
    public static LocalizedEnumValueCycle<CycleEnabledDisabled> cycleEnabledDisabled(@NotNull String cycleLocalizationKey, @NotNull CycleEnabledDisabled selectedValue) {
        return (LocalizedEnumValueCycle<CycleEnabledDisabled>) LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleEnabledDisabled.ENABLED, CycleEnabledDisabled.DISABLED).setCurrentValue(selectedValue);
    }

    /** Creates a localized enabled/disabled cycle. */
    @NotNull
    public static LocalizedEnumValueCycle<CycleEnabledDisabled> cycleEnabledDisabled(@NotNull String cycleLocalizationKey, boolean selectedValue) {
        return (LocalizedEnumValueCycle<CycleEnabledDisabled>) LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleEnabledDisabled.ENABLED, CycleEnabledDisabled.DISABLED).setCurrentValue(CycleEnabledDisabled.getByBoolean(selectedValue));
    }

    /** Creates a localized orange-value cycle. */
    @SuppressWarnings("all")
    @NotNull
    public static <T> LocalizedGenericValueCycle<T> cycleOrangeValue(@NotNull String cycleLocalizationKey, @NotNull List<T> values) {
        return (LocalizedGenericValueCycle<T>) LocalizedGenericValueCycle.of(cycleLocalizationKey, values.toArray()).setValueComponentStyleSupplier(consumes -> LocalizedEnum.WARNING_TEXT_STYLE.get());
    }

    /** Creates a localized orange-value cycle. */
    @NotNull
    public static <T> LocalizedGenericValueCycle<T> cycleOrangeValue(@NotNull String cycleLocalizationKey, @NotNull List<T> values, @NotNull T selectedValue) {
        return (LocalizedGenericValueCycle<T>) cycleOrangeValue(cycleLocalizationKey, values).setCurrentValue(selectedValue);
    }

    /** Creates a cycle from ordered values. */
    @SuppressWarnings("all")
    @NotNull
    public static <T> LocalizedGenericValueCycle<T> cycle(@NotNull String cycleLocalizationKey, @NotNull List<T> values) {
        return (LocalizedGenericValueCycle<T>) LocalizedGenericValueCycle.of(cycleLocalizationKey, values.toArray());
    }

    /** Creates a cycle from ordered values. */
    @NotNull
    public static <T> LocalizedGenericValueCycle<T> cycle(@NotNull String cycleLocalizationKey, @NotNull List<T> values, @NotNull T selectedValue) {
        return (LocalizedGenericValueCycle<T>) cycle(cycleLocalizationKey, values).setCurrentValue(selectedValue);
    }

    /** Cycles through cycle on off values. */
    public enum CycleOnOff implements LocalizedEnum<CycleOnOff> {

        /** The enabled on-state. */
        ON("on", true, LocalizedEnum.SUCCESS_TEXT_STYLE),
        /** The disabled off-state. */
        OFF("off", false, LocalizedEnum.ERROR_TEXT_STYLE);

        final String name;
        final Supplier<Style> style;
        final boolean valueBoolean;

        CycleOnOff(String name, boolean valueBoolean, Supplier<Style> style) {
            this.name = name;
            this.style = style;
            this.valueBoolean = valueBoolean;
        }

        /** {@inheritDoc} */
        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "konkrete.general.cycle.on_off";
        }

        /** Returns the as boolean. */
        public boolean getAsBoolean() {
            return this.valueBoolean;
        }

        /** {@inheritDoc} */
        @Override
        public @NotNull String getName() {
            return this.name;
        }

        /** {@inheritDoc} */
        @Override
        public @NotNull CycleOnOff[] getValues() {
            return CycleOnOff.values();
        }

        /** {@inheritDoc} */
        @Override
        public @Nullable CycleOnOff getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        /** {@inheritDoc} */
        @Override
        public @NotNull Style getValueComponentStyle() {
            return this.style.get();
        }

        /** Returns the by boolean. */
        public static CycleOnOff getByBoolean(boolean b) {
            if (b) return ON;
            return OFF;
        }

        /** Returns the by name. */
        @Nullable
        public static CycleOnOff getByName(@NotNull String name) {
            for (CycleOnOff e : CycleOnOff.values()) {
                if (e.getName().equals(name)) return e;
            }
            return null;
        }

    }

    /** Cycles through cycle enabled disabled values. */
    public enum CycleEnabledDisabled implements LocalizedEnum<CycleEnabledDisabled> {

        /** The enabled state. */
        ENABLED("enabled", true, LocalizedEnum.SUCCESS_TEXT_STYLE),
        /** The disabled state. */
        DISABLED("disabled", false, LocalizedEnum.ERROR_TEXT_STYLE);

        final String name;
        final Supplier<Style> style;
        final boolean valueBoolean;

        CycleEnabledDisabled(String name, boolean valueBoolean, Supplier<Style> style) {
            this.name = name;
            this.style = style;
            this.valueBoolean = valueBoolean;
        }

        /** {@inheritDoc} */
        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "konkrete.general.cycle.enabled_disabled";
        }

        /** Returns the as boolean. */
        public boolean getAsBoolean() {
            return this.valueBoolean;
        }

        /** {@inheritDoc} */
        @Override
        public @NotNull String getName() {
            return this.name;
        }

        /** {@inheritDoc} */
        @Override
        public @NotNull CycleEnabledDisabled[] getValues() {
            return CycleEnabledDisabled.values();
        }

        /** {@inheritDoc} */
        @Override
        public @Nullable CycleEnabledDisabled getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        /** {@inheritDoc} */
        @Override
        public @NotNull Style getValueComponentStyle() {
            return this.style.get();
        }

        /** Returns the by boolean. */
        public static CycleEnabledDisabled getByBoolean(boolean b) {
            if (b) return ENABLED;
            return DISABLED;
        }

        /** Returns the by name. */
        @Nullable
        public static CycleEnabledDisabled getByName(@NotNull String name) {
            for (CycleEnabledDisabled e : CycleEnabledDisabled.values()) {
                if (e.getName().equals(name)) return e;
            }
            return null;
        }

    }

}
