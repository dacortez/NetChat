package dacortez.netChat;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
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
	protected void setTimer() {
		// Nothing to do here.
	}
	
	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setServerSocketChannel(port);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		setDatagramChannel(port);
		datagramChannel.register(selector, SelectionKey.OP_READ);
		System.out.println("Server listening TCP on port " + port);
		System.out.println("Server listening UDP on port " + port);
	}
	
	@Override
	protected void respondTCP(SocketChannel channel) throws IOException {
		ProtocolData received = new ProtocolData(buffer);
		if (received.getType() == DataType.TCP_OK)
			tcpOK(channel, received);
		else if (received.getType() == DataType.LOGIN_REQUEST)
			loginRequest(channel, received);
		else if (received.getType() == DataType.HEART_BEAT)
			heartBeat();
		else if (received.getType() == DataType.USERS_REQUEST)
			usersRequest(channel);
		else if (received.getType() == DataType.LOGOUT_REQUEST)
			logoutRequest(channel, received);
		else if (received.getType() == DataType.CHAT_REQUEST)
			chatRequest(channel, received);
	}

	private void tcpOK(SocketChannel channel, ProtocolData received) throws IOException {
		sendTCP(received, channel);
	}

	private void loginRequest(SocketChannel channel, ProtocolData received) throws IOException {
		if (logInUser(received, channel))			
			sendTCP(new ProtocolData(DataType.LOGIN_OK), channel);
		else
			sendTCP(new ProtocolData(DataType.LOGIN_FAIL), channel);
	}

	private void heartBeat() {
		System.out.println("Heart beat received");
	}

	private void usersRequest(SocketChannel channel) throws IOException {
		ProtocolData usersList = new ProtocolData(DataType.USERS_LIST);
		for (User user: loggedInUsers)
			usersList.addToHeader(user.toString());
		tcpOK(channel, usersList);
	}

	private void logoutRequest(SocketChannel channel, ProtocolData received) throws IOException {
		logOutUser(received.getHeaderLine(0));
		sendTCP(new ProtocolData(DataType.LOGOUT_OK), channel);
	}

	private void chatRequest(SocketChannel channel, ProtocolData received) throws IOException {
		User requested = getLoggedUser(received.getHeaderLine(0));
		User sender = getLoggedUser(received.getHeaderLine(1));
		if (requested != null)
			checkConnectionAndLock(channel, requested, sender);
		else {
			ProtocolData chatDenied = new ProtocolData(DataType.CHAT_DENIED);
			chatDenied.addToHeader("usuario nao conectado");
			sendTCP(chatDenied, channel);
		}
	}

	private void checkConnectionAndLock(SocketChannel channel, User requested, User sender) throws IOException {
		ProtocolData chatDenied = new ProtocolData(DataType.CHAT_DENIED);
		if (requested.getType() == ConnectionType.TCP) {
			if (!requested.isLocked()) {
				requested.setLocked(true); 
				sender.setLocked(true);
				sendChatOK(channel, requested, sender);
			}
			else {
				chatDenied.addToHeader("usuario bloqueado");
				sendTCP(chatDenied, channel);
			}
		}
		else {
			chatDenied.addToHeader("outro tipo de conexao");
			sendTCP(chatDenied, channel);
		}
	}

	private void sendChatOK(SocketChannel channel, User requested, User sender) throws IOException {
		ProtocolData chatOK = new ProtocolData(DataType.CHAT_OK);
		chatOK.addToHeader(requested.getHost());
		chatOK.addToHeader(requested.getClientPort().toString());
		chatOK.addToHeader(sender.getHost());
		chatOK.addToHeader(sender.getClientPort().toString());
		sendTCP(chatOK, channel);
	}
	
	private User getLoggedUser(String userName) {
		for (User user: loggedInUsers)
			if (user.hasUserName(userName))
				return user;
		return null;
	}
	
	@Override
	protected void respondUDP(DatagramChannel channel) throws IOException {
		// TODO
	}
	
	@Override
	protected void respondStdin(ReadableByteChannel channel) throws IOException {
		// Nothing to do here.
	}

	private boolean logInUser(ProtocolData loginInfo, Channel channel) {
		String userName = loginInfo.getHeaderLine(0);
		String password = loginInfo.getHeaderLine(1);
		String clientPort = loginInfo.getHeaderLine(2);
		for (User user: allUsers)
			if (user.hasUserName(userName) && user.authenticate(password)) {
				// TODO Falta verificar se o usuário já está logado.
				setLoggedInUser(channel, clientPort, user);
				loggedInUsers.add(user);
				return true;
			}
		return false;
	}

	private void setLoggedInUser(Channel channel, String clientPort, User user) {
		SocketChannel socket = (SocketChannel) channel;
		user.setType(ConnectionType.TCP);
		user.setPort(socket.socket().getPort());
		user.setHost(socket.socket().getInetAddress().getHostName());
		user.setClientPort(Integer.parseInt(clientPort));
		user.setLocked(false);
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