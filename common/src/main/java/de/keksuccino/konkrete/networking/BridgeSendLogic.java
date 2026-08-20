package de.keksuccino.konkrete.networking;

import de.keksuccino.konkrete.networking.bridge.BridgeMessageSender;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
interface BridgeSendLogic<E> {

    @NotNull BridgeMessageSender.SendResult send(@NotNull E endpoint, @NotNull String message, boolean bridgeProtocolV1Advertised);

}
