package net.novatech.drakar.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class PacketHeader<T extends Enum<T>> implements Serializable {
	public T id;
	private long size;

	public PacketHeader(T id) {
		this.id = id;
		this.size = 0;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getSize() {
		return size;
	}

	public T getId() {
		return id;
	}

	public void writeTo(OutputStream out) throws IOException {
		new DataOutputStream(out).writeInt(this.toByteArray().length);
		out.write(this.toByteArray(), 0, this.toByteArray().length);
	}

	public static PacketHeader<?> readFrom(InputStream inputStream) throws IOException {
		int headerSize = new DataInputStream(inputStream).readInt();
		byte[] array = new byte[headerSize];
		final int read = inputStream.read(array, 0, headerSize);
		return PacketHeader.fromByteArray(array);
	}

	public byte[] toByteArray() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = null;
		try {
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(this);
			objectOutputStream.flush();
			return byteArrayOutputStream.toByteArray();
		} catch (IOException exception) {
			exception.printStackTrace();
		} finally {
			try {
				byteArrayOutputStream.close();
				if (objectOutputStream != null)
					objectOutputStream.close();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}
		return null;
	}

	public static PacketHeader<?> fromByteArray(byte[] bytes) {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		ObjectInput objectInput = null;

		try {
			objectInput = new ObjectInputStream(byteArrayInputStream);
			return (PacketHeader<?>) objectInput.readObject();
		} catch (IOException | ClassNotFoundException exception) {
			exception.printStackTrace();
		} finally {
			try {
				byteArrayInputStream.close();
				if (objectInput != null)
					objectInput.close();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}
		return null;
	}
}