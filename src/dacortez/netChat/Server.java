/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

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
		if (received.getMessage() == ProtocolMessage.CONNECTION_OK)
			connectionOK(channel, received);
		else if (received.getMessage() == ProtocolMessage.LOGIN_REQUEST)
			loginRequest(channel, received);
		else if (received.getMessage() == ProtocolMessage.USERS_REQUEST)
			usersRequest(channel);
		else if (received.getMessage() == ProtocolMessage.CHAT_REQUEST)
			chatRequest(channel, received);
		else if (received.getMessage() == ProtocolMessage.CHAT_END)
			chatEnd(channel, received);
		else if (received.getMessage() == ProtocolMessage.TRANSFER_REQUEST)
			transferRequest(channel, received);
		else if (received.getMessage() == ProtocolMessage.LOGOUT_REQUEST)
			logoutRequest(channel, received);
		else if (received.getMessage() == ProtocolMessage.HEART_BEAT)
			heartBeat();
	}
	
	// CONNECTION_OK ------------------------------------------------------------------------------

	private void connectionOK(Channel channel, ProtocolData connectionOK) throws IOException {
		send(channel, connectionOK);
	}
	
	// LOGIN_REQUEST ------------------------------------------------------------------------------

	private void loginRequest(Channel channel, ProtocolData loginRequest) throws IOException {
		if (logInUser(channel, loginRequest))			
			send(channel, new ProtocolData(ProtocolMessage.LOGIN_OK));
		else
			send(channel, new ProtocolData(ProtocolMessage.LOGIN_FAIL));
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
		Integer pierPort = Integer.parseInt(loginRequest.getHeaderLine(2));
		user.setPierPort(pierPort);
		user.setLocked(false);
	}
	
	// USERS_REQUEST ------------------------------------------------------------------------------
	
	private void usersRequest(Channel channel) throws IOException {
		ProtocolData usersList = new ProtocolData(ProtocolMessage.USERS_LIST);
		for (User user: loggedInUsers)
			usersList.addToHeader(user.toString());
		send(channel, usersList);
	}
	
	// CHAT_REQUEST -------------------------------------------------------------------------------

	private void chatRequest(Channel channel, ProtocolData chatRequest) throws IOException {
		User requested = getLoggedUser(chatRequest.getHeaderLine(0));
		User sender = getLoggedUser(chatRequest.getHeaderLine(1));
		if (requested != null)
			checkConnectionAndLockForChat(channel, requested, sender);
		else {
			ProtocolData chatDenied = new ProtocolData(ProtocolMessage.CHAT_DENIED);
			chatDenied.addToHeader("usuario nao conectado");
			send(channel, chatDenied);
		}
	}

	private void checkConnectionAndLockForChat(Channel channel, User requested, User sender) throws IOException {
		ProtocolData chatDenied = new ProtocolData(ProtocolMessage.CHAT_DENIED);
		if (isConnectionRight(channel, requested.getType())) {
			if (!requested.isLocked()) {
				requested.setLocked(true); 
				sender.setLocked(true);
				send(channel, chatOK(requested, sender));
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

	private ProtocolData chatOK(User requested, User sender) throws IOException {
		ProtocolData chatOK = new ProtocolData(ProtocolMessage.CHAT_OK);
		chatOK.addToHeader(requested.getHost());
		chatOK.addToHeader(requested.getPierPort().toString());
		chatOK.addToHeader(sender.getHost());
		chatOK.addToHeader(sender.getPierPort().toString());
		return chatOK;
	}
	
	// CHAT_END -----------------------------------------------------------------------------------
	
	private void chatEnd(Channel channel, ProtocolData chatEnd) throws IOException {
		User user = getLoggedUser(chatEnd.getHeaderLine(0));
		user.setLocked(false);
		ProtocolData chatFinished = new ProtocolData(ProtocolMessage.CHAT_FINISHED);
		chatFinished.addToHeader(user.getUserName());
		send(channel, chatFinished);
	}
	
	// TRANSFER_REQUEST ---------------------------------------------------------------------------
	
	private void transferRequest(Channel channel, ProtocolData transferRequest) throws IOException {
		User receiver = getLoggedUser(transferRequest.getHeaderLine(0));
		User sender = getLoggedUser(transferRequest.getHeaderLine(1));
		if (receiver != null) {
			checkConnectionAndLockForTransfer(channel, receiver, sender);
		} 
		else {
			ProtocolData transferDenied = new ProtocolData(ProtocolMessage.TRANSFER_DENIED);
			transferDenied.addToHeader("usuario nao conectado");
			send(channel, transferDenied);
		}
	}
	
	private void checkConnectionAndLockForTransfer(Channel channel, User requested, User sender) throws IOException {
		ProtocolData transferDenied = new ProtocolData(ProtocolMessage.TRANSFER_DENIED);
		if (isConnectionRight(channel, requested.getType())) {
			if (!requested.isLocked()) {
				send(channel, transferOK(requested, sender));
			}
			else {
				transferDenied.addToHeader("usuario bloqueado");
				send(channel, transferDenied);
			}
		}
		else {
			transferDenied.addToHeader("outro tipo de conexao");
			send(channel, transferDenied);
		}
	}

	private ProtocolData transferOK(User receiver, User sender) {
		ProtocolData transferOK = new ProtocolData(ProtocolMessage.TRANSFER_OK);
		transferOK.addToHeader(receiver.getUserName());
		transferOK.addToHeader(receiver.getHost());
		transferOK.addToHeader(receiver.getPierPort().toString());
		transferOK.addToHeader(sender.getUserName());
		transferOK.addToHeader(sender.getHost());
		transferOK.addToHeader(sender.getPierPort().toString());
		return transferOK;
	}
	
	// LOGOUT_REQUEST -----------------------------------------------------------------------------
	
	private void logoutRequest(Channel channel, ProtocolData logoutRequest) throws IOException {
		User user = getLoggedUser(logoutRequest.getHeaderLine(0));
		if (user != null)
			loggedInUsers.remove(user);
		send(channel, new ProtocolData(ProtocolMessage.LOGOUT_OK));
	}
	
	// HEART_BEAT ---------------------------------------------------------------------------------
	
	private void heartBeat() {
		System.out.println("Heart beat received");
	}
}