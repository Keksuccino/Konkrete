//TODO übernehmen 1.5.1
package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.command.ClientCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {

    @Inject(at = @At(value = "HEAD"), method = "sendMessage(Ljava/lang/String;Z)V", cancellable = true)
    private void onSendMessage(String msg, boolean addToChatHistory, CallbackInfo info) {

        if (ClientCommandHandler.executeClientCommand(msg)) {
            if (addToChatHistory) {
                Minecraft.getInstance().gui.getChat().addRecentChat(msg);
            }
            info.cancel();
        }

    }

}
