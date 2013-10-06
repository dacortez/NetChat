package dacortez.netChat;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

public abstract class Multiplex {
	protected final ByteBuffer buffer = ByteBuffer.allocate(16384);
	protected Selector selector;
	protected ServerSocketChannel serverSocketChannel;
	protected DatagramChannel datagramChannel;
	protected SelectableChannel stdin;
	
	public void run() {
		try {
			selector = Selector.open();
			registerChannelsWithSelector();
			while (true) {
				int readyChannels = selector.select();
				if (readyChannels == 0) continue;
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey selectionKey : keys)
					handleReadyChannel(selectionKey);
				keys.clear();
			}
		} catch (IOException ie) {
			System.err.println(ie);
		}
	}
	
	protected abstract void registerChannelsWithSelector() throws IOException;
	
	private void handleReadyChannel(SelectionKey selectionKey) {
		if (selectionKey.isAcceptable())
			handleAcceptable();
		else if (selectionKey.isReadable()) 
			handleReadable(selectionKey);
	}
	
	private void handleAcceptable() {
		Socket socket = null;
		try {
			socket = serverSocketChannel.socket().accept();
			SocketChannel socketChannel = socket.getChannel();
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_READ);
			System.out.println("Got TCP connection from " + socket);
		} catch (IOException e) {
			System.err.println("Error on accepting connection " + socket);
		}
	}
	
	private void handleReadable(SelectionKey key) {
		if (key.channel() instanceof SocketChannel)
			handleTCP(key);
		else if (key.channel() instanceof DatagramChannel)
			handleUDP(key);
		else
			handleStdin(key);
	}
	
	private void handleTCP(SelectionKey key) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = (SocketChannel) key.channel();
			if (readTCP(socketChannel))
				respondTCP(socketChannel);
			else
				closeSockect(key, socketChannel);
		} catch (IOException ie) {
			closeChannel(key, socketChannel);
		}
	}
	
	private void handleUDP(SelectionKey key) {
		DatagramChannel datagramChannel = null;
		try {
			datagramChannel = (DatagramChannel) key.channel();
			if (readUDP(datagramChannel))
				respondUDP(datagramChannel);
		} catch (IOException ie) {
			key.cancel();
		}
	}
	
	private void handleStdin(SelectionKey key) {
		ReadableByteChannel channel = null;
		try {
			channel = (ReadableByteChannel) key.channel();
			if (readStdin(channel))
				respondStdin(channel);
		} catch (IOException ie) {
			key.cancel();
		}
	}
	
	private boolean readTCP(SocketChannel channel) throws IOException {
		buffer.clear();
		channel.read(buffer);
		buffer.flip();
		if (buffer.limit() == 0) return false;
		System.out.println("Read " + buffer.limit() + " from TCP " + channel);
		return true;
	}
	
	private boolean readUDP(DatagramChannel channel) throws IOException {
		buffer.clear();
		channel.receive(buffer);
		buffer.flip();
		if (buffer.limit() == 0) return false;
		System.out.println("Processed " + buffer.limit() + " from UDP " + channel);
		return true;
	}

	protected boolean readStdin(ReadableByteChannel channel) throws IOException {
        buffer.clear();
        int count = channel.read(buffer);
        if (count < 0) return false;
        buffer.flip();
        System.out.println ("Processed " + count + " from Stdin " + channel);
		return true;
	}
	
	//-------------------------------------------------------------------------------
	
	protected abstract void respondTCP(SocketChannel channel) throws IOException;
	
	protected abstract void respondUDP(DatagramChannel channel) throws IOException;
	
	protected abstract void respondStdin(ReadableByteChannel channel) throws IOException;
	
	//-------------------------------------------------------------------------------
	
	protected void closeSockect(SelectionKey key, SocketChannel channel) {
		key.cancel();
		Socket socket = null;
		try {
			socket = channel.socket();
			socket.close();
		} catch (IOException ie) {
			System.err.println("Error closing socket " + socket + ": " + ie);
		}
		System.out.println("Closed " + socket);
	}
	
	protected void closeChannel(SelectionKey key, Channel channel) {
		key.cancel();
		try {
			channel.close();
		} catch (IOException ie) {
			System.err.println("Error closing channel " + channel + ": " + ie);
		}
		System.out.println("Closed " + channel);
	}
	
	protected void sendTCP(ProtocolData data, SocketChannel channel) throws IOException {
		ByteBuffer buffer = data.toByteBuffer();
		channel.write(buffer);
		System.out.println("Sent " + buffer.limit() + " from " + channel);
	}
}
