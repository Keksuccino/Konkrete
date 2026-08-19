package de.keksuccino.konkrete.util.enums;

import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalizedEnumStylesTest {

    @AfterEach
    void resetStyles() {
        LocalizedEnumStyles.reset();
    }

    @Test
    void localizedEnumSuppliersFollowConfiguredStyles() {
        Style success = Style.EMPTY.withBold(true);
        Style warning = Style.EMPTY.withItalic(true);
        Style error = Style.EMPTY.withUnderlined(true);

        LocalizedEnumStyles.configure(() -> success, () -> warning, () -> error);

        assertEquals(success, LocalizedEnum.SUCCESS_TEXT_STYLE.get());
        assertEquals(warning, LocalizedEnum.WARNING_TEXT_STYLE.get());
        assertEquals(error, LocalizedEnum.ERROR_TEXT_STYLE.get());
    }

    @Test
    void resetRestoresDefaultsAndInvalidHooksAreRejected() {
        LocalizedEnumStyles.configure(() -> Style.EMPTY, () -> Style.EMPTY, () -> Style.EMPTY);
        LocalizedEnumStyles.reset();

        assertEquals(0x55FF55, LocalizedEnumStyles.success().getColor().getValue());
        assertEquals(0xFFFF55, LocalizedEnumStyles.warning().getColor().getValue());
        assertEquals(0xFF5555, LocalizedEnumStyles.error().getColor().getValue());
        assertThrows(NullPointerException.class, () -> LocalizedEnumStyles.configure(null, () -> Style.EMPTY, () -> Style.EMPTY));
    }
}
