package dacortez.netChat;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Server extends Multiplex {
	private static List<User> allUsers;
	private List<User> loggedInUsers = new ArrayList<User>();
	private int port;
	
	public Server(int port) {
		this.port = port;
		setAllUsers();
	}

	private void setAllUsers() {
		allUsers = new ArrayList<User>();
		allUsers.add(new User("Daniel Augusto Cortez", "dacortez", "3858f62230ac3c915f300c664312c63f"));
		allUsers.add(new User("Angela Pedroso Tonon", "aptonon", "96948aad3fcae80c08a35c9b5958cd89"));
	}

	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		new Server(port).run();
	}
	
	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setServerSocketChannel();
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("Listening TCP on port " + port);
		
		setDatagramChannel();
		datagramChannel.register(selector, SelectionKey.OP_READ);
		System.out.println("Listening UDP on port " + port);
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

	@Override
	protected void respondTCP(SocketChannel channel) throws IOException {
		ProtocolData received = new ProtocolData(buffer);
		if (received.getType() == DataType.TCP_OK) {
			sendTCP(received, channel);
		}
		else if (received.getType() == DataType.LOGIN_REQUEST) {
			InetAddress inet = channel.socket().getInetAddress();
			if (logInUser(received, inet.getHostName()))			
				sendTCP(new ProtocolData(DataType.LOGIN_OK), channel);
			else
				sendTCP(new ProtocolData(DataType.LOGIN_FAIL), channel);
		}
		else if (received.getType() == DataType.USERS_REQUEST) {
			ProtocolData usersList = new ProtocolData(DataType.USERS_LIST);
			for (User user: loggedInUsers)
				usersList.addToHeader(user.toString());
			sendTCP(usersList, channel);
		}
		else if (received.getType() == DataType.LOGOUT_REQUEST) {
			logOutUser(received.getHeaderLine(0));
			sendTCP(new ProtocolData(DataType.LOGOUT_OK), channel);
		}
	}
	
	@Override
	protected void respondUDP(DatagramChannel channel) throws IOException {
		// TODO
	}
	
	@Override
	protected void respondStdin(ReadableByteChannel channel) throws IOException {
		// Nothing to do here.
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
}