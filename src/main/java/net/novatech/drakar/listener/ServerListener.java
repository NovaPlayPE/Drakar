package net.novatech.drakar.listener;

import net.novatech.drakar.AbstractServer;
import net.novatech.drakar.Connection;
import net.novatech.drakar.packet.Packet;

public abstract class ServerListener {

	public abstract boolean onClientConnected(Connection<AbstractServer> clientConnection);

	public abstract void onMessageReceived(Packet<?, ?> receivedMessage, Connection<?> client);

	public abstract void onClientDisConnected(Connection<?> clientConnection);

}