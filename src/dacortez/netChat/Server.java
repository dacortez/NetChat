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
	protected void registerChannelsWithSelector() throws IOException {
		setServerSocketChannel(port);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("Server listening TCP on port " + port);
		
		setDatagramChannel(port);
		datagramChannel.register(selector, SelectionKey.OP_READ);
		System.out.println("Server listening UDP on port " + port);
	}
	
	@Override
	protected void respondTCP(SocketChannel channel) throws IOException {
		ProtocolData received = new ProtocolData(buffer);
		if (received.getType() == DataType.TCP_OK) {
			sendTCP(received, channel);
		}
		else if (received.getType() == DataType.LOGIN_REQUEST) {
			if (logInUser(received, channel))			
				sendTCP(new ProtocolData(DataType.LOGIN_OK), channel);
			else
				sendTCP(new ProtocolData(DataType.LOGIN_FAIL), channel);
		}
		else if (received.getType() == DataType.HEART_BEAT) {
			System.out.println(received);
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

	private boolean logInUser(ProtocolData loginInfo, Channel channel) {
		String userName = loginInfo.getHeaderLine(0);
		String password = loginInfo.getHeaderLine(1);
		for (User user: allUsers)
			if (user.hasUserName(userName) && user.authenticate(password)) {
				// TODO Falta verificar se o usuário já está logado.
				SocketChannel socket = (SocketChannel) channel;
				user.setType(ConnectionType.TCP);
				user.setChannel(channel);
				user.setPort(socket.socket().getPort());
				user.setHost(socket.socket().getInetAddress().getHostName());
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