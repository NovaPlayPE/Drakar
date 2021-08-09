package net.novatech.drakar.administration;

import net.novatech.drakar.Connection;
import net.novatech.drakar.packet.Packet;

public interface OwnerObject {
    void onMessageReceived(Packet<?, ?> receivedMessage, Connection<?> client);
    boolean isVerbose();
    void detachConnection(Connection<?> connection);
    void clientConnectionClosed(Connection<?> connection);
}