package de.keksuccino.konkrete.util.rendering.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Plays UI feedback sounds under the active UI configuration. */
public class UISounds {

    /** Plays default beep through Minecraft's sound system. */
    public static void playDefaultBeep() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
    }

}
