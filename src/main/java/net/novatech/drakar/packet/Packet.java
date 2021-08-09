package net.novatech.drakar.packet;

import java.io.*;
import java.util.Objects;

public class Packet<T extends Enum<T>, Q extends Serializable> implements Serializable {
	final private PacketHeader<T> packetHeader;
	public Q packetBody;

	public Packet(T id, Q packetBody) {
		packetHeader = new PacketHeader<>(id);
		this.packetBody = packetBody;
		packetHeader.setSize(this.getBodySize());
	}

	private Packet(PacketHeader<T> header, Q packetBody) {
		this.packetHeader = header;
		this.packetBody = packetBody;
	}

	public T getId() {
		return this.packetHeader.id;
	}

	public int getHeaderSize() {
		return this.packetHeader.toByteArray().length;
	}

	public void writeTo(OutputStream outputStream) throws IOException {
		packetHeader.writeTo(outputStream);
		outputStream.write(toByteArray());
	}

	public static Packet<?, ?> readFrom(InputStream inputStream) throws IOException {
		PacketHeader<?> header = PacketHeader.readFrom(inputStream);
		int size = (int) header.getSize();
		byte[] array = new byte[size];
		final int read = inputStream.read(array);
		Object obj = fromByteArray(array);
		return new Packet<>(header, (Serializable) obj);
	}

	public byte[] toByteArray() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = null;
		try {
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(this.packetBody);
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

	public int getBodySize() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = null;
		try {
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(this.packetBody);
			objectOutputStream.flush();
			byte[] bytes = byteArrayOutputStream.toByteArray();
			return bytes.length;
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
		return -1;
	}

	public static Object fromByteArray(byte[] bytes) {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		ObjectInput objectInput = null;

		try {
			objectInput = new ObjectInputStream(byteArrayInputStream);
			return objectInput.readObject();
		} catch (Exception exception) {
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