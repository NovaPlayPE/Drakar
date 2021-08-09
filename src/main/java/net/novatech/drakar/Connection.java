package net.novatech.drakar;

import java.io.*;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.UUID;

import net.novatech.drakar.administration.OwnerObject;
import net.novatech.drakar.packet.Packet;
import net.novatech.drakar.utils.*;

public class Connection<T extends OwnerObject> {
	public enum Owner {
		CLIENT, SERVER;
	}

	public enum Type {
		TCP, UDP;
	}

	public enum DefaultMessages {
		CONNECTED, AUTHORISATION
	}

	public interface onCloseExceptions {
		void onCloseException(Exception e);
	}

	final private ArrayDeque<Packet<?, ?>> outMessageQueue;

	final private Context context;
	final private Owner owner;
	final public Socket ownerSocket;
	final private T ownerObject;
	final private UUID id;

	final private InputStream socketInputStream;
	final private OutputStream socketOutputStream;

	private Long tokenSent, tokenReceived;

	final private ObjectLock isWriting;

	public Connection(Context context, Owner owner, Socket socket, T ownerObject) throws IOException {
		this.context = context;
		this.outMessageQueue = new ArrayDeque<>();
		this.owner = owner;
		this.ownerSocket = socket;
		this.ownerObject = ownerObject;
		this.id = UUID.randomUUID();
		this.socketInputStream = socket.getInputStream();
		this.socketOutputStream = socket.getOutputStream();
		this.isWriting = new ObjectLock();
	}

	public boolean connectToServer() {
		if (owner == Owner.CLIENT) {
			try {
				Packet<DefaultMessages, Long> message = (Packet<DefaultMessages, Long>) Packet
						.readFrom(socketInputStream);
				long authenticationToken = encode(message.packetBody);

				Packet<DefaultMessages, Long> newMessage = new Packet<>(Connection.DefaultMessages.AUTHORISATION,
						authenticationToken);
				newMessage.writeTo(socketOutputStream);

				this.tokenReceived = this.encode(authenticationToken);
				this.tokenSent = encode(tokenReceived);

				Packet<DefaultMessages, Long> authenticationMessage = new Packet<>(DefaultMessages.AUTHORISATION,
						tokenSent);
				authenticationMessage.writeTo(socketOutputStream);
				Packet<DefaultMessages, ?> statusMessage = (Packet<DefaultMessages, ?>) Packet
						.readFrom(socketInputStream);

				if (statusMessage.getId() == DefaultMessages.CONNECTED) {
					Task readMessage = new Tasks.ReadMessageTask(context, socketInputStream,
							(Packet<?, ?> recievedMessage) -> ownerObject.onMessageReceived(recievedMessage, this),
							() -> Connection.this.ownerObject.clientConnectionClosed(Connection.this));

					context.addTask(readMessage);
					if (context.isRunning())
						context.start();
					return true;
				} else {
					return false;
				}
			} catch (IOException | TaskNotCompletedException exception) {
				return false;
			}
		}
		return false;
	}

	public boolean connectToClient() {
		if (owner == Owner.SERVER) {
			try {
				long max = Long.MAX_VALUE;
				long min = 0;
				long randomToken = (long) (Math.random() * max);

				tokenSent = encode(randomToken);
				tokenReceived = encode(tokenSent);

				Packet<DefaultMessages, Long> authenticationMessage = new Packet<>(DefaultMessages.AUTHORISATION,
						tokenSent);
				authenticationMessage.writeTo(socketOutputStream);

				Packet<DefaultMessages, Long> receivedTokenMessage = (Packet<DefaultMessages, Long>) Packet
						.readFrom(socketInputStream);

				if (receivedTokenMessage.getId() == DefaultMessages.AUTHORISATION
						&& receivedTokenMessage.packetBody.equals(tokenReceived)) {
					Packet<DefaultMessages, Serializable> statusMessage = new Packet<>(DefaultMessages.CONNECTED,
							null);
					statusMessage.writeTo(socketOutputStream);

					Task readMessage = new Tasks.ReadMessageTask(context, socketInputStream,
							(Packet<?, ?> recievedMessage) -> ownerObject.onMessageReceived(recievedMessage, this),
							() -> Connection.this.ownerObject.clientConnectionClosed(Connection.this));

					context.addTask(readMessage);
					if (context.isRunning())
						context.start();
					return true;
				} else {
					if (ownerObject.isVerbose())
						System.out.println("[Connection] Closing Connection - Authentication failed: ID = "
								+ receivedTokenMessage.getId() + ", Recieved Token = "
								+ receivedTokenMessage.packetBody + ", Expected Token = " + this.tokenReceived
								+ ", Authentication Status = "
								+ (receivedTokenMessage.packetBody.equals(tokenReceived)));
					ownerSocket.close();
				}
			} catch (IOException | TaskNotCompletedException exception) {
				return false;
			}
		}
		return false;
	}

	public void writePacket() {
		synchronized (socketOutputStream) {
			this.isWriting.setLocked(true);
			while (!outMessageQueue.isEmpty()) {
				Packet<?, ?> currentMessage = outMessageQueue.removeFirst();
				try {
					currentMessage.writeTo(socketOutputStream);
				} catch (IOException exception) {
					this.close((Exception ignored) -> {
					});
					ownerObject.detachConnection(this);
				}
			}
			this.isWriting.setLocked(false);
		}
	}

	public void addPacket(Packet<?, ?> packet) {
		outMessageQueue.add(packet);
		if (!this.isWriting())
			this.writePacket();
	}

	public void close(onCloseExceptions callback) {
		try {
			this.socketInputStream.close();
			this.socketOutputStream.close();
			this.ownerSocket.close();
		} catch (IOException exception) {
			callback.onCloseException(exception);
		}
	}

	public UUID getId() {
		return id;
	}

	public Context getContext() {
		return this.context;
	}

	public int getPort() {
		return ownerSocket.getPort();
	}

	public boolean isWriting() {
		return this.isWriting.locked;
	}

	private Long encode(Long token) {
		return ~(token ^ 0xC0DEBEEF & 231973274);
	}

}