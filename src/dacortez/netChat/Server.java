package dacortez.netChat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
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
		allUsers.add(new User("Angela Pedroso Tonon", "aptonon", "3858f62230ac3c915f300c664312c63f"));
		allUsers.add(new User("Caio Augusto Cortez", "cacortez", "3858f62230ac3c915f300c664312c63f"));
		allUsers.add(new User("Eliana Cortez", "ecortez", "3858f62230ac3c915f300c664312c63f"));
	}

	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		new Server(port).run();
	}
		
	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setServerSocketChannel(port);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		setDatagramChannel(port);
		datagramChannel.register(selector, SelectionKey.OP_READ);
		System.out.println("Server listening TCP on port " + port + ".");
		System.out.println("Server listening UDP on port " + port + ".");
		System.out.println("Press Ctrl+C to close.");
	}
	
	@Override
	protected void respond(Channel channel) throws IOException {
		ProtocolData received = new ProtocolData(buffer);
		if (received.getType() == DataType.CONNECTION_OK)
			connectionOK(channel, received);
		else if (received.getType() == DataType.LOGIN_REQUEST)
			loginRequest(channel, received);
		else if (received.getType() == DataType.USERS_REQUEST)
			usersRequest(channel);
		else if (received.getType() == DataType.LOGOUT_REQUEST)
			logoutRequest(channel, received);
		else if (received.getType() == DataType.CHAT_REQUEST)
			chatRequest(channel, received);
		else if (received.getType() == DataType.CHAT_END)
			chatEnd(channel, received);
		else if (received.getType() == DataType.HEART_BEAT)
			heartBeat();
	}

	private void connectionOK(Channel channel, ProtocolData connectionOK) throws IOException {
		send(channel, connectionOK);
	}

	private void loginRequest(Channel channel, ProtocolData loginRequest) throws IOException {
		if (logInUser(channel, loginRequest))			
			send(channel, new ProtocolData(DataType.LOGIN_OK));
		else
			send(channel, new ProtocolData(DataType.LOGIN_FAIL));
	}
	
	private boolean logInUser(Channel channel, ProtocolData loginRequest) {
		String userName = loginRequest.getHeaderLine(0);
		String password = loginRequest.getHeaderLine(1);
		for (User user: allUsers)
			if (user.hasUserName(userName) && user.authenticate(password) && !isLoggedIn(user)) {
				setLoggedInUser(channel, user, loginRequest);
				loggedInUsers.add(user);
				return true;
			}
		return false;
	}
	
	private boolean isLoggedIn(User user) {
		return (getLoggedUser(user.getUserName()) != null);
	}
	
	private User getLoggedUser(String userName) {
		for (User user: loggedInUsers)
			if (user.hasUserName(userName))
				return user;
		return null;
	}

	private void setLoggedInUser(Channel channel, User user, ProtocolData loginRequest) {
		if (channel instanceof SocketChannel) {
			user.setType(ConnectionType.TCP);
			InetAddress inet = ((SocketChannel) channel).socket().getInetAddress();
			user.setHost(inet.getHostAddress());
		}
		else if (channel instanceof DatagramChannel) {
			user.setType(ConnectionType.UDP);
			InetAddress inet = ((InetSocketAddress) address).getAddress(); 
			user.setHost(inet.getHostAddress());
		}
		Integer clientPort = Integer.parseInt(loginRequest.getHeaderLine(3));
		user.setClientPort(clientPort);
		user.setLocked(false);
	}
	
	private void usersRequest(Channel channel) throws IOException {
		ProtocolData usersList = new ProtocolData(DataType.USERS_LIST);
		for (User user: loggedInUsers)
			usersList.addToHeader(user.toString());
		send(channel, usersList);
	}

	private void logoutRequest(Channel channel, ProtocolData logoutRequest) throws IOException {
		User user = getLoggedUser(logoutRequest.getHeaderLine(0));
		if (user != null)
			loggedInUsers.remove(user);
		send(channel, new ProtocolData(DataType.LOGOUT_OK));
	}

	private void chatRequest(Channel channel, ProtocolData chatRequest) throws IOException {
		User requested = getLoggedUser(chatRequest.getHeaderLine(0));
		User sender = getLoggedUser(chatRequest.getHeaderLine(1));
		if (requested != null)
			checkConnectionAndLock(channel, requested, sender);
		else {
			ProtocolData chatDenied = new ProtocolData(DataType.CHAT_DENIED);
			chatDenied.addToHeader("usuario nao conectado");
			send(channel, chatDenied);
		}
	}

	private void checkConnectionAndLock(Channel channel, User requested, User sender) throws IOException {
		ProtocolData chatDenied = new ProtocolData(DataType.CHAT_DENIED);
		if (isConnectionRight(channel, requested.getType())) {
			if (!requested.isLocked()) {
				requested.setLocked(true); 
				sender.setLocked(true);
				sendChatOK(channel, requested, sender);
			}
			else {
				chatDenied.addToHeader("usuario bloqueado");
				send(channel, chatDenied);
			}
		}
		else {
			chatDenied.addToHeader("outro tipo de conexao");
			send(channel, chatDenied);
		}
	}
	
	private boolean isConnectionRight(Channel channel, ConnectionType type) {
		if (type == ConnectionType.TCP)
			return (channel instanceof SocketChannel);
		else if (type == ConnectionType.UDP)
			return (channel instanceof DatagramChannel);
		return false;
	}

	private void sendChatOK(Channel channel, User requested, User sender) throws IOException {
		ProtocolData chatOK = new ProtocolData(DataType.CHAT_OK);
		chatOK.addToHeader(requested.getHost());
		chatOK.addToHeader(requested.getClientPort().toString());
		chatOK.addToHeader(sender.getHost());
		chatOK.addToHeader(sender.getClientPort().toString());
		send(channel, chatOK);
	}
	
	private void chatEnd(Channel channel, ProtocolData chatEnd) throws IOException {
		User user = getLoggedUser(chatEnd.getHeaderLine(0));
		user.setLocked(false);
		ProtocolData chatFinished = new ProtocolData(DataType.CHAT_FINISHED);
		chatFinished.addToHeader(user.getUserName());
		send(channel, chatFinished);
	}
	
	private void heartBeat() {
		System.out.println("Heart beat received");
	}
}