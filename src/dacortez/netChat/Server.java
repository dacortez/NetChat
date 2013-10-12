/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * O servidor concretiza a classe Multiplex recebendo conexões 
 * TCP e UDP dos clientes. Mantém a lista dos usuários do sistema
 * de bate-papo e usuários logados. Faz o tratamento dos heart beats 
 * recebidos e armazena informações no arquivo de log.  
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class Server extends Multiplex {
	// Lista de todos os usuários do sistema.
	private static final List<User> allUsers = new ArrayList<User>();
	// Lista dos usuários logados.
	private List<User> loggedInUsers = new ArrayList<User>();
	// Porta em que o servido está escutando as conexões.
	private int port;
	// Stream usado na impressão do log.
	private PrintStream ps;
	// Formatador de datas para ser usado na impressão do log.
	private DateFormat df;
	
	
	public Server(int port) {
		this.port = port;
		setAllUsers();
		df = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
		try {
			ps = new PrintStream(new File("server.log"));
		} catch (FileNotFoundException e) {
			ps = System.out;
		}
	}

	private void setAllUsers() {
		allUsers.add(new User("Joao Fulano", "joao", "3858f62230ac3c915f300c664312c63f"));
		allUsers.add(new User("Maria Ciclano", "maria", "3858f62230ac3c915f300c664312c63f"));
		allUsers.add(new User("Pedro Beltrano", "pedro", "3858f62230ac3c915f300c664312c63f"));
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Uso: java dacortez/netChat/Server <porta>");
			return;
		}
		int port = Integer.parseInt(args[0]);
		new Server(port).run();
	}
		
	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setServerSocketChannel(port);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		setDatagramChannel(port);
		datagramChannel.register(selector, SelectionKey.OP_READ);
		System.out.println("Servidor escutando conexões TCP na porta " + port + ".");
		System.out.println("Servidor escutando conexões UDP na porta " + port + ".");
		System.out.println("Pressione Ctrl+C para fechar.");
		ps.printf("[Servidor iniciado: %s]\n", df.format(new Date()));
	}
	
	private final List<User> heartBeats = new ArrayList<User>();
	
	/* O timer do servidor verifica a lista de heart beats recebidos
	 * a cada 3 * HEART_BEAT segundos e atualiza a lista de usuários
	 * logados.
	 * (non-Javadoc)
	 * @see dacortez.netChat.Multiplex#setTimer()
	 */
	@Override
	protected void setTimer() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				List<User> remove = new ArrayList<User>();
				for (User logged: loggedInUsers)
					if (!heartBeats.contains(logged))
						remove.add(logged);
				for (User user: remove)
					loggedInUsers.remove(user);
				heartBeats.clear();
			}
		}, Client.HEART_BEAT * 1000 * 3,  Client.HEART_BEAT * 1000 * 3);
	}
		
	@Override
	protected void respond(Channel channel) throws IOException {
		ProtocolData received = new ProtocolData(buffer);
		if (received.getMessage() == ProtocolMessage.CONNECTION_OK) {
			ps.printf("[Conexão: %s]\n", df.format(new Date()));
			connectionOK(channel, received);
		} 
		else if (received.getMessage() == ProtocolMessage.LOGIN_REQUEST) {
			String userName = received.getHeaderLine(0);
			ps.printf("[Solicitação de login (%s): %s]\n", userName, df.format(new Date()));
			loginRequest(channel, received);
		}
		else if (received.getMessage() == ProtocolMessage.USERS_REQUEST) {
			ps.printf("[Solicitação de usuários: %s]\n", df.format(new Date()));
			usersRequest(channel);
		}
		else if (received.getMessage() == ProtocolMessage.CHAT_REQUEST) {
			String requested = received.getHeaderLine(0);
			String sender = received.getHeaderLine(1);
			ps.printf("[Solicitação de bate-papo (%s -> %s): %s]\n", sender, requested, df.format(new Date()));
			chatRequest(channel, received);
		}
		else if (received.getMessage() == ProtocolMessage.CHAT_END) {
			String userName = received.getHeaderLine(0);
			ps.printf("[Solicitação de encerramento de bate-papo (%s): %s]\n", userName, df.format(new Date()));
			chatEnd(channel, received);
		}
		else if (received.getMessage() == ProtocolMessage.TRANSFER_REQUEST) {
			String receiver = received.getHeaderLine(0);
			String sender = received.getHeaderLine(1);
			ps.printf("[Solicitação de transferência (%s -> %s): %s]\n", sender, receiver, df.format(new Date()));
			transferRequest(channel, received);
		}
		else if (received.getMessage() == ProtocolMessage.LOGOUT_REQUEST) {
			String userName = received.getHeaderLine(0);
			ps.printf("[Solicitação de logout (%s): %s]\n", userName, df.format(new Date()));
			logoutRequest(channel, received);
		}
		else if (received.getMessage() == ProtocolMessage.HEART_BEAT) {
			String userName = received.getHeaderLine(0);
			ps.printf("[Hear beat (%s): %s]\n", userName, df.format(new Date()));
			heartBeat(received);
		}
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
				cancelTimer(timer);
				setLoggedInUser(channel, user, loginRequest);
				loggedInUsers.add(user);
				setTimer();
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
	
	private void heartBeat(ProtocolData heartBeat) {
		User user = getLoggedUser(heartBeat.getHeaderLine(0));
		if (user != null)
			if (!heartBeats.contains(user))
				heartBeats.add(user);
	}
}