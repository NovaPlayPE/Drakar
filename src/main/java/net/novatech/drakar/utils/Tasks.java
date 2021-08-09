package net.novatech.drakar.utils;

import java.io.IOException;
import java.io.InputStream;

import net.novatech.drakar.packet.Packet;

public class Tasks {
	public static class ReadMessageTask extends Task {
		public interface Callback {
			void onMessageReceived(Packet<?, ?> message);
		}

		public interface DisconnectCallback {
			void onDisconnect();
		}

		final private InputStream inputStream;
		final private Callback readMessageCallback;
		final private DisconnectCallback disconnect;
		public boolean isAlive;

		public ReadMessageTask(Context context, InputStream inputStream, Callback callback,
				DisconnectCallback disconnect) {
			super(true, context);
			this.inputStream = inputStream;
			this.readMessageCallback = callback;
			this.disconnect = disconnect;
			this.isAlive = true;
		}

		@Override
		public void run() throws IOException {
			while (isAlive) {
				synchronized (this.inputStream) {
					Packet<?, ?> newMessage = Packet.readFrom(this.inputStream);
					readMessageCallback.onMessageReceived(newMessage);
				}
			}
		}

		@Override
		public boolean onComplete(Exception e) {
			if (e != null)
				disconnect.onDisconnect();
			return true;
		}
	}
}