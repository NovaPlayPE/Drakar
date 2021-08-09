package net.novatech.drakar;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import lombok.Getter;
import lombok.Setter;
import net.novatech.drakar.administration.OwnerObject;
import net.novatech.drakar.listener.ServerListener;
import net.novatech.drakar.packet.Packet;
import net.novatech.drakar.utils.Context;
import net.novatech.drakar.utils.Task;
import net.novatech.drakar.utils.TaskNotCompletedException;

public abstract class AbstractServer implements OwnerObject {

	@Getter
	private InetSocketAddress address;
	protected List<Connection<AbstractServer>> clients;
	protected boolean verbose;
	@Getter
	@Setter
	private ServerListener serverListener;
	@Getter
	private Context context;

	private boolean isRunning = false;
	private int backlog;
	protected Thread serverThread;

	public ServerSocket serverSocket;

	public AbstractServer(InetSocketAddress address, boolean verbose, int backlog) {
		this.address = address;
		this.clients = new ArrayList<>();
		this.context = new Context();
		this.verbose = verbose;
		this.backlog = backlog;
		try {
			initSocket();
		} catch (IOException e) {
		}
	}

	public abstract void initSocket() throws IOException;

	public void bind() throws TaskNotCompletedException {
		getContext().start();
		isRunning = true;
	}

	public void waitForCOnnection() {
		for (int i = 0; i < ((this.backlog / 1000) == 0 ? 1 : this.backlog / 1000); i++) {
			Task serverTask = new Task(true, context) {
				@Override
				public void run() throws IOException {
					while (isRunning) {
						Socket socket = serverSocket.accept();
						Connection<AbstractServer> clientConnection = onClientConnect(socket);
						if (getServerListener().onClientConnected(clientConnection) && clientConnection != null) {
							if (verbose)
								System.out.println(
										"[Server] Client at " + clientConnection.getPort() + " successfully connected");
							clients.add(clientConnection);
						} else {
							if (verbose)
								System.out.println("[Server] Client rejected");
						}
					}
				}

				@Override
				public boolean onComplete(Exception exception) {
					return exception != null;
				}

				@Override
				public void onInitialise() {
					AbstractServer.this.serverThread = this.getTaskThread();
				}
			};
			context.addTask(serverTask);
		}
	}

	private Connection<AbstractServer> onClientConnect(Socket clientSocket) throws IOException {
		Connection<AbstractServer> connection = new Connection<>(context, Connection.Owner.SERVER, clientSocket, this);
		return connection.connectToClient() ? connection : null;
	}

	public void sendMessage(Packet<?, ?> message, Connection<TCPServer> client) {
		client.addPacket(message);
	}

	public void sendAll(Packet<?, ?> message) {
		for (Connection<AbstractServer> client : this.clients) {
			client.addPacket(message);
		}
	}

	public void sendAll(Packet<?, ?> message, Connection<?> excludeClient) {
		for (Connection<?> client : this.clients) {
			if (client.equals(excludeClient))
				continue;
			client.addPacket(message);
		}
	}

	@Override
	public void onMessageReceived(Packet<?, ?> receivedMessage, Connection<?> client) {
		getServerListener().onMessageReceived(receivedMessage, client);
	}

	@Override
	public boolean isVerbose() {
		return this.verbose;
	}

	@Override
	public void detachConnection(Connection<?> connection) {
		clients.remove(connection);
	}

	@Override
	public void clientConnectionClosed(Connection<?> connection) {
		this.detachConnection(connection);
		connection.close((Exception e) -> {
		});
		this.getServerListener().onClientDisConnected(connection);
	}

}