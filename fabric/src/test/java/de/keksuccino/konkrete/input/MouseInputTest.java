package de.keksuccino.konkrete.input;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.sdl.SDLMouse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MouseInputTest {

    @BeforeEach
    @AfterEach
    void resetState() {
        MouseInput.resetScreenMouseButtons();
    }

    @Test
    void leftPressAndReleaseUseSdlButtonIds() {
        MouseInput.updateScreenMouseButton(SDLMouse.SDL_BUTTON_LEFT, true);
        assertTrue(MouseInput.isLeftMouseDown());
        assertFalse(MouseInput.isRightMouseDown());

        MouseInput.updateScreenMouseButton(SDLMouse.SDL_BUTTON_LEFT, false);
        assertButtonsReleased();
    }

    @Test
    void rightPressAndReleaseUseSdlButtonIds() {
        MouseInput.updateScreenMouseButton(SDLMouse.SDL_BUTTON_RIGHT, true);
        assertFalse(MouseInput.isLeftMouseDown());
        assertTrue(MouseInput.isRightMouseDown());

        MouseInput.updateScreenMouseButton(SDLMouse.SDL_BUTTON_RIGHT, false);
        assertButtonsReleased();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, SDLMouse.SDL_BUTTON_MIDDLE, SDLMouse.SDL_BUTTON_X1, SDLMouse.SDL_BUTTON_X2})
    void otherButtonsAreNeitherLeftNorRight(int button) {
        MouseInput.updateScreenMouseButton(button, true);
        assertButtonsReleased();
        MouseInput.updateScreenMouseButton(button, false);
        assertButtonsReleased();
    }

    @Test
    void eachScreenEventReplacesPreviousEventState() {
        MouseInput.updateScreenMouseButton(SDLMouse.SDL_BUTTON_LEFT, true);
        MouseInput.updateScreenMouseButton(SDLMouse.SDL_BUTTON_RIGHT, true);
        assertFalse(MouseInput.isLeftMouseDown());
        assertTrue(MouseInput.isRightMouseDown());

        MouseInput.updateScreenMouseButton(SDLMouse.SDL_BUTTON_MIDDLE, true);
        assertButtonsReleased();
    }

    @ParameterizedTest
    @ValueSource(ints = {SDLMouse.SDL_BUTTON_LEFT, SDLMouse.SDL_BUTTON_RIGHT})
    void screenLifecycleResetClearsButtonState(int button) {
        MouseInput.updateScreenMouseButton(button, true);
        MouseInput.resetScreenMouseButtons();
        assertButtonsReleased();
        MouseInput.resetScreenMouseButtons();
        assertButtonsReleased();
    }

    private static void assertButtonsReleased() {
        assertFalse(MouseInput.isLeftMouseDown());
        assertFalse(MouseInput.isRightMouseDown());
    }

}
