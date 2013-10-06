package dacortez.netChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.ReadableByteChannel;
import java.util.Random;
import java.util.TimerTask;

public abstract class Client extends Multiplex {
	protected String host;
	protected int port;
	protected Integer clientPort;
	protected final BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	protected String userName;
	protected ClientState state;
	protected Pipe serverPipe;
	protected Pipe p2pPipe;
	private static final long HEART_BEAT_TIME = 10 * 1000;
	
	public static void main(String args[]) throws Exception {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		if (args[2].charAt(0) == 'T')
			new TCPClient(host, port, 5000 + new Random().nextInt(1000)).start();
	}
	
	public Client(String host, int port, int clientPort) {
		this.host = host;
		this.port = port;
		this.clientPort = clientPort;
	}
	
	protected void start() throws IOException {
		ProtocolData tcpOK = new ProtocolData(DataType.TCP_OK);
		serverPipe.send(tcpOK);
		if (serverPipe.receive().getType() == DataType.TCP_OK)
			doLogin();
		serverPipe.close();		
	}
	
	@Override
	protected void setTimer() {
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					ProtocolData heartBeat = new ProtocolData(DataType.HEART_BEAT);
					serverPipe.send(heartBeat);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, HEART_BEAT_TIME,  HEART_BEAT_TIME);
	}
	
	protected void doLogin() throws IOException {
		serverPipe.send(getLoginRequest());
		ProtocolData received = serverPipe.receive();
		if (received.getType() == DataType.LOGIN_OK) {
			System.out.println("Bem-vindo ao servidor de Chat!");
			printMenu();
			run();
		}
		else if (received.getType() == DataType.LOGIN_FAIL) {
			System.out.println("Login inválido! Até a próxima...");
			timer.cancel();
		}
	}

	private ProtocolData getLoginRequest() throws IOException {
		System.out.println("Servidor disponível!");
		System.out.println("Faça seu login...");
		System.out.print("Username: ");
		userName = inFromUser.readLine().toLowerCase();
		System.out.print("Password: ");
		String password = inFromUser.readLine();
		ProtocolData loginRequest = new ProtocolData(DataType.LOGIN_REQUEST);
		loginRequest.addToHeader(userName);
		loginRequest.addToHeader(password);
		loginRequest.addToHeader(clientPort.toString());
		return loginRequest;
	}
	
	private void printMenu() {
		System.out.println("(U) Lista usuários logados");
		System.out.println("(B) Iniciar bate-papo com usuário");
		System.out.println("(E) Enviar arquivo para usuário");
		System.out.println("(L) Fazer logout");
		System.out.println("Escolha uma das opções:");
		state = ClientState.SCANNING_MENU;
	}
	
	@Override
	protected void respondStdin(ReadableByteChannel channel) throws IOException {
		// Buffer está com o conteúdo do teclado. Responde a entrada em função do estado do cliente.
		switch (state) {
		case TYPING_USER:
			userTyped();
			break;
		case CHATTING:
			chatting();
			break;
		case TYPING_FILE:
			break;
		case SCANNING_MENU:	
			switchMenuOption((char) buffer.get());
		}		
	}

	private void userTyped() throws IOException {
		System.out.println("Buscando usuário no servidor...");
		ProtocolData chatRequest = getChatRequest();
		if (chatRequest != null) {
			serverPipe.send(chatRequest);
			processResponseFromChatRequest();
		} 
		else {
			System.out.println("O usuário não deve ser você mesmo.");
			printMenu();
		}
	}

	private void processResponseFromChatRequest() throws IOException {
		ProtocolData received = serverPipe.receive();
		if (received.getType() == DataType.CHAT_DENIED) {
			System.out.print("Bate-papo negado: ");
			System.out.println(received.getHeaderLine(0));
			printMenu();
		}
		else if (received.getType() == DataType.CHAT_OK)
			chatOK(received);
	}

	private void chatOK(ProtocolData chatOK) throws IOException {
		System.out.println("Bate-papo aceito:\n");
		String host = chatOK.getHeaderLine(0);
		Integer port = Integer.parseInt(chatOK.getHeaderLine(1));
		p2pInstantiation(host, port); 
		state = ClientState.CHATTING;
		p2pPipe.send(chatOK);	
	}
	
	private void chatting() throws IOException {
		p2pPipe.send(getChatMsg());
	}

	private ProtocolData getChatMsg() {
		StringBuilder sb = new StringBuilder();
		while (buffer.hasRemaining())
		     sb.append((char) buffer.get());
		sb.deleteCharAt(sb.length() - 1);
		String msg = new String(sb.toString());
		ProtocolData chatMsg = new ProtocolData(DataType.CHAT_MSG);
		chatMsg.addToHeader(this.userName);
		chatMsg.addToHeader(msg); 
		return chatMsg;
	}

	protected abstract void p2pInstantiation(String host, Integer port) throws IOException;
	
	private ProtocolData getChatRequest() {
		StringBuilder sb = new StringBuilder();
		while (buffer.hasRemaining())
		     sb.append((char) buffer.get());
		sb.deleteCharAt(sb.length() - 1);
		String requested = new String(sb.toString());
		if (requested.toLowerCase().contentEquals(this.userName))
			return null;
		ProtocolData chatRequest = new ProtocolData(DataType.CHAT_REQUEST);
		chatRequest.addToHeader(new String(sb.toString())); // requested
		chatRequest.addToHeader(this.userName); // sender
		return chatRequest;
	}

	private void switchMenuOption(char option) {
		switch (option) {
		case 'U':
			listUsers();
			break;
		case 'B':
			System.out.println("Informe o Username do usuário:");
			state = ClientState.TYPING_USER;
			break;
		case 'E':
			System.out.println("Opção não implementada!");
			printMenu();
			break;
		case 'L':
			if (logout()) {
				stdinPipe.finalize();
				closeSelector();
			}
			break;
		default:
			System.out.println("Opção inválida!");
			printMenu();
		}
	}

	protected void listUsers() {
		try {
			serverPipe.send(new ProtocolData(DataType.USERS_REQUEST));
			ProtocolData received = serverPipe.receive();
			if (received.getType() == DataType.USERS_LIST) {
				System.out.println("Lista de usuários logados:");
				int num = received.getNumberOfHeaderLines();
				for (int i = 0; i < num; i++)
					System.out.println(received.getHeaderLine(i));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected boolean logout() {
		try {
			ProtocolData logoutRequest = new ProtocolData(DataType.LOGOUT_REQUEST);
			logoutRequest.addToHeader(userName);
			serverPipe.send(logoutRequest);
			ProtocolData received = serverPipe.receive();
			if (received.getType() == DataType.LOGOUT_OK) {
				System.out.println("Até a próxima...");
				timer.cancel();
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private void closeSelector() {
		try {
			selector.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
