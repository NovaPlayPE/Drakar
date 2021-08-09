package net.novatech.drakar;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import net.novatech.drakar.packet.Packet;

public class TCPServer extends AbstractServer {

	public TCPServer(InetSocketAddress address, boolean verbose, int backlog) {
		super(address, verbose, backlog);
	}

	@Override
	public void initSocket() throws IOException {
		this.serverSocket = new ServerSocket(getAddress().getPort());

	}

}
