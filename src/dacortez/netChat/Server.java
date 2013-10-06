package dacortez.netChat;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Server {
	private static List<User> allUsers;
	private List<User> loggedInUsers = new ArrayList<User>();
	private int port;
	private final ByteBuffer buffer = ByteBuffer.allocate(16384);
	private ServerSocketChannel serverSocketChannel;
	private DatagramChannel datagramChannel;

	public Server(int port) {
		this.port = port;
		setAllUsers();
	}

	private void setAllUsers() {
		allUsers = new ArrayList<User>();
		allUsers.add(new User("Daniel Augusto Cortez", "dacortez", "3858f62230ac3c915f300c664312c63f"));
		allUsers.add(new User("Angela Pedroso Tonon", "aptonon", "96948aad3fcae80c08a35c9b5958cd89"));
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		new Server(port).run();
	}

	public void run() {
		try {
			Selector selector = getSelectorWithChannelsRegistered();
			while (true) {
				int readyChannels = selector.select();
				if (readyChannels == 0) continue;
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey selectionKey : keys)
					handleReadyChannel(selector, selectionKey);
				keys.clear();
			}
		} catch (IOException ie) {
			System.err.println(ie);
		}
	}
	
	private Selector getSelectorWithChannelsRegistered() throws IOException {
		Selector selector = Selector.open();
		setServerSocketChannel();
		setDatagramChannel();
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("Listening TCP on port " + port);
		datagramChannel.register(selector, SelectionKey.OP_READ);
		System.out.println("Listening UDP on port " + port);		
		return selector;
	}
	
	private void setServerSocketChannel() throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		ServerSocket serverSocket = serverSocketChannel.socket();
		InetSocketAddress isa = new InetSocketAddress(port);
		serverSocket.bind(isa);
	}
	
	private void setDatagramChannel() throws IOException {
		datagramChannel = DatagramChannel.open();
		datagramChannel.configureBlocking(false);
		DatagramSocket datagramSocket = datagramChannel.socket();
		InetSocketAddress isa = new InetSocketAddress(port);
		datagramSocket.bind(isa);
	}
	
	private void handleReadyChannel(Selector selector, SelectionKey selectionKey) throws IOException {
		if (selectionKey.isAcceptable())
			registerNewConnection(selector);
		else if (selectionKey.isReadable()) 
			handleReadable(selectionKey);
	}
	
	private void registerNewConnection(Selector selector) throws IOException {
		Socket socket = serverSocketChannel.socket().accept();
		SocketChannel socketChannel = socket.getChannel();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ);
		System.out.println("Got connection from " + socket);
	}

	private void handleReadable(SelectionKey selectionKey) {
		if (selectionKey.channel() instanceof DatagramChannel)
			handleUDP(selectionKey);
		else
			handleTCP(selectionKey);
	}

	private void handleUDP(SelectionKey selectionKey) {
		DatagramChannel datagramChannel = null;
		try {
			datagramChannel = (DatagramChannel) selectionKey.channel();
			if (readUDP(datagramChannel))
				respondUDP(datagramChannel);
		} catch (IOException ie) {
			selectionKey.cancel();
		}
	}
	
	private void handleTCP(SelectionKey selectionKey) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = (SocketChannel) selectionKey.channel();
			if (readTCP(socketChannel))
				respondTCP(socketChannel);
			else
				closeSockect(selectionKey, socketChannel);
				
		} catch (IOException ie) {
			closeChannel(socketChannel, selectionKey);
		}
	}

	private boolean readUDP(DatagramChannel datagramChannel) throws IOException {
		buffer.clear();
		datagramChannel.receive(buffer);
		buffer.flip();
		if (buffer.limit() == 0) return false;
		System.out.println("Processed " + buffer.limit() + " from " + datagramChannel);
		return true;
	}

	private boolean readTCP(SocketChannel socketChannel) throws IOException {
		buffer.clear();
		socketChannel.read(buffer);
		buffer.flip();
		if (buffer.limit() == 0) return false;
		System.out.println("Read " + buffer.limit() + " from " + socketChannel);
		return true;
	}

	private void respondTCP(SocketChannel socketChannel) throws IOException {
		ProtocolData received = new ProtocolData(buffer);
		if (received.getType() == DataType.TCP_OK) {
			sendTCP(received, socketChannel);
		}
		else if (received.getType() == DataType.LOGIN_REQUEST) {
			InetAddress inet = socketChannel.socket().getInetAddress();
			if (logInUser(received, inet.getHostName()))			
				sendTCP(new ProtocolData(DataType.LOGIN_OK), socketChannel);
			else
				sendTCP(new ProtocolData(DataType.LOGIN_FAIL), socketChannel);
		}
		else if (received.getType() == DataType.USERS_REQUEST) {
			ProtocolData usersList = new ProtocolData(DataType.USERS_LIST);
			for (User user: loggedInUsers)
				usersList.addToHeader(user.toString());
			sendTCP(usersList, socketChannel);
		}
		else if (received.getType() == DataType.LOGOUT_REQUEST) {
			logOutUser(received.getHeaderLine(0));
			sendTCP(new ProtocolData(DataType.LOGOUT_OK), socketChannel);
		}
	}
	
	private void respondUDP(DatagramChannel datagramChannel) throws IOException {
		
	}

	private boolean logInUser(ProtocolData loginInfo, String host) {
		String userName = loginInfo.getHeaderLine(0);
		String password = loginInfo.getHeaderLine(1);
		for (User user: allUsers)
			if (user.hasUserName(userName) && user.authenticate(password)) {
				// TODO Falta verificar se o usuário já está logado.
				user.setHost(host);
				loggedInUsers.add(user);
				return true;
			}
		return false;
	}
	
	private void logOutUser(String userName) {
		User found = null;
		for (User user: loggedInUsers)
			if (user.hasUserName(userName)) {
				found = user;
				break;
			}
		if (found != null) {
			loggedInUsers.remove(found);
		}
	}
	
	private void closeSockect(SelectionKey selectionKey, SocketChannel socketChannel) {
		selectionKey.cancel();
		Socket socket = null;
		try {
			socket = socketChannel.socket();
			socket.close();
		} catch (IOException ie) {
			System.err.println("Error closing socket " + socket + ": " + ie);
		}
		System.out.println("Closed " + socket);
	}
	
	private void closeChannel(Channel channel, SelectionKey selectionKey) {
		selectionKey.cancel();
		try {
			channel.close();
		} catch (IOException ie) {
			System.err.println("Error closing channel " + channel + ": " + ie);
		}
		System.out.println("Closed " + channel);
	}
	
	private void sendTCP(ProtocolData data, SocketChannel socketChannel) throws IOException {
		ByteBuffer buffer = data.toByteBuffer();
		socketChannel.write(buffer);
		System.out.println("Sent " + buffer.limit() + " from " + socketChannel);
	}
}