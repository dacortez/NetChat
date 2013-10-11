package dacortez.netChat;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
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
import java.util.Timer;

public abstract class Multiplex {
	protected ByteBuffer buffer = ByteBuffer.allocate(10000);
	protected SystemInPipe stdinPipe;
	protected Selector selector;
	protected ServerSocketChannel serverSocketChannel;
	protected DatagramChannel datagramChannel;
	protected SelectableChannel stdin;
	protected Timer timer;
	protected SocketAddress address;
	
	public void run() {
		try {
			selector = Selector.open();
			registerChannelsWithSelector();
			setTimer();
			while (true) {
				if (!selector.isOpen()) break;
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
		closeChannels();
	}
	
	protected void setTimer() {
		// Por padrão não faz nada.
	}
	
	protected abstract void registerChannelsWithSelector() throws IOException;
	
	protected void setServerSocketChannel(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		ServerSocket serverSocket = serverSocketChannel.socket();
		InetSocketAddress isa = new InetSocketAddress(port);
		serverSocket.bind(isa);
	}
	
	protected void setDatagramChannel(int port) throws IOException {
		datagramChannel = DatagramChannel.open();
		datagramChannel.configureBlocking(false);
		DatagramSocket datagramSocket = datagramChannel.socket();
		InetSocketAddress isa = new InetSocketAddress(port);
		datagramSocket.bind(isa);
	}
	
	protected void setSelectableChannel() throws IOException {
		stdinPipe = new SystemInPipe();
		stdin = stdinPipe.getStdinChannel();
		stdin.configureBlocking(false);
	}
	
	private void closeChannels() {
		try {
			if (serverSocketChannel != null && serverSocketChannel.isOpen())
				serverSocketChannel.close();
			if (datagramChannel != null && datagramChannel.isOpen())
				datagramChannel.close();
			if (stdin != null && stdin.isOpen())
				stdin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
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
			SocketChannel channel = socket.getChannel();
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_READ);
			//System.out.println("Got TCP connection from " + socket);
		} catch (IOException e) {
			System.err.println("Error on accepting connection " + socket);
		}
	}
	
	private void handleReadable(SelectionKey key) {
		if (key.channel() instanceof SocketChannel)
			handleReadableTCP(key);
		else if (key.channel() instanceof DatagramChannel)
			handleReadableUDP(key);
		else
			handleReadableStdin(key);
	}
	
	private void handleReadableTCP(SelectionKey key) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = (SocketChannel) key.channel();
			if (readTCP(socketChannel))
				respond(socketChannel);
			else
				closeSockect(key, socketChannel);
		} catch (IOException ie) {
			closeChannel(key, socketChannel);
		}
	}
	
	private void handleReadableUDP(SelectionKey key) {
		DatagramChannel channel = null;
		try {
			channel = (DatagramChannel) key.channel();
			if (readUDPAndSetAddress(channel)) {
				DatagramChannel newChannel = DatagramChannel.open(); 
				newChannel.connect(address);
				respond(newChannel);
				newChannel.disconnect();
				newChannel.close();
			}
		} catch (IOException ie) {
			ie.printStackTrace();
			key.cancel();
		}
	}
	
	private void handleReadableStdin(SelectionKey key) {
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
		//System.out.println("Read " + buffer.limit() + " from TCP " + channel);
		return true;
	}
	
	private boolean readUDPAndSetAddress(DatagramChannel channel) throws IOException {
		buffer.clear();
		address = channel.receive(buffer);
		buffer.flip();
		if (buffer.limit() == 0) return false;
		//System.out.println("Processed " + buffer.limit() + " from UDP " + channel);
		return true;
	}

	protected boolean readStdin(ReadableByteChannel channel) throws IOException {
        buffer.clear();
        int count = channel.read(buffer);
        if (count <= 0) return false;
        buffer.flip();
        //System.out.println ("Processed " + count + " from Stdin " + channel);
		return true;
	}
	
	//-------------------------------------------------------------------------------
	
	protected abstract void respond(Channel channel) throws IOException;
	
	//-------------------------------------------------------------------------------
	
	protected void respondStdin(ReadableByteChannel channel) throws IOException {
		// Por padrão não faz nada.
	}
	
	protected void closeSockect(SelectionKey key, SocketChannel channel) {
		key.cancel();
		Socket socket = null;
		try {
			socket = channel.socket();
			socket.close();
		} catch (IOException ie) {
			System.err.println("Error closing socket " + socket + ": " + ie);
		}
		//System.out.println("Closed " + socket);
	}
	
	protected void closeChannel(SelectionKey key, Channel channel) {
		key.cancel();
		try {
			channel.close();
		} catch (IOException ie) {
			System.err.println("Error closing channel " + channel + ": " + ie);
		}
		//System.out.println("Closed " + channel);
	}
	
	protected void send(Channel channel, ProtocolData data) throws IOException {
		ByteBuffer buffer = data.toByteBuffer();
		if (channel instanceof SocketChannel)
			((SocketChannel) channel).write(buffer);
		else if (channel instanceof DatagramChannel)
			((DatagramChannel) channel).write(buffer);
		//System.out.println("Sent " + buffer.limit() + " from " + channel);
	}
	
	protected String bufferToString() {
		StringBuilder sb = new StringBuilder();
		while (buffer.hasRemaining())
		     sb.append((char) buffer.get());
		int length = sb.length();
		if (length > 0 && sb.charAt(length - 1) == '\n') sb.deleteCharAt(length - 1);
		return sb.toString();
	}
	
	protected boolean isProtocolData() {
		if (buffer.limit() >= 5) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 5; i++)
				sb.append((char) buffer.get(i));
			return sb.toString().contentEquals("EP2P/");
		}
		return false;
	}
}