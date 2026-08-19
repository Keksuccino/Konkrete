package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(RealmsNotificationsScreen.class)
public interface AccessorMixinRealmsNotificationsScreen {

    @Accessor("numberOfPendingInvites") int get_numberOfPendingInvites_Konkrete();

    @Accessor("trialAvailable") static boolean get_trialAvailable_Konkrete() {
        throw new AssertionError();
    }

    @Accessor("hasUnreadNews") static boolean get_hasUnreadNews_Konkrete() {
        throw new AssertionError();
    }

    @Accessor("hasUnseenNotifications") static boolean get_hasUnseenNotifications_Konkrete() {
        throw new AssertionError();
    }

    @Accessor("validClient") CompletableFuture<Boolean> get_validClient_Konkrete();

}
