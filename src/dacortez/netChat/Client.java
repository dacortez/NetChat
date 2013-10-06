package dacortez.netChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.ReadableByteChannel;
import java.util.TimerTask;

public abstract class Client extends Multiplex {
	protected String host;
	protected int port;
	protected final byte[] serverBuffer = new byte[16384];
	protected final BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	protected String userName;
	protected ClientState state;
	private static final long HEART_BEAT_TIME = 10 * 1000;
	
	public static void main(String args[]) throws Exception {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		if (args[2].charAt(0) == 'T')
			new TCPClient(host, port).start();
	}
	
	public Client(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	protected abstract void start() throws IOException;
	
	@Override
	protected void setTimer() {
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					ProtocolData heartBeat = new ProtocolData(DataType.HEART_BEAT);
					System.out.println("Sending heart beat to server... " + heartBeat);
					sendToServer(heartBeat);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, HEART_BEAT_TIME,  HEART_BEAT_TIME);
	}
	
	protected abstract void sendToServer(ProtocolData data) throws IOException;
	
	protected abstract ProtocolData receiveFromServer() throws IOException;

	protected void doLogin() throws IOException {
		System.out.println("Servidor disponível!");
		System.out.println("Faça seu login...");
		
		System.out.print("Username: ");
		userName = inFromUser.readLine();
		System.out.print("Password: ");
		String password = inFromUser.readLine();
		
		ProtocolData loginInfo = new ProtocolData(DataType.LOGIN_REQUEST);
		loginInfo.addToHeader(userName);
		loginInfo.addToHeader(password);
		sendToServer(loginInfo);
		
		ProtocolData received = receiveFromServer();
		if (received.getType() == DataType.LOGIN_OK) {
			System.out.println("Bem-vindo ao servidor de Chat!");
			printMenu();
			run();
		}
		else if (received.getType() == DataType.LOGIN_FAIL)
			System.out.println("Login inválido! Até a próxima...");
	}
	
	private void printMenu() {
		System.out.println("(U) Lista usuários logados");
		System.out.println("(B) Iniciar bate-papo com usuário logado");
		System.out.println("(E) Enviar arquivo para usuário logado");
		System.out.println("(L) Fazer logout");
		System.out.println("Escolha uma das opções: ");
		state = ClientState.SCANNING_MENU;
	}
	
	@Override
	protected void respondStdin(ReadableByteChannel channel) throws IOException {
		// Buffer está com o conteúdo do teclado. Responde a entrada em função do estado do cliente.
		switch (state) {
		case SCANNING_MENU:	
			switchMenuOption((char) buffer.get());
			break;
		case CHOOSING_USER:
			break;
		case TYPING_MSG:
			break;
		case TYPING_FILE:
			break;
		case WAITING:
			printMenu();
		}		
	}

	private void switchMenuOption(char option) {
		switch (option) {
		case 'U':
			listUsers();
			break;
		case 'B':
			System.out.println("Opção não implementada!");
			printMenu();
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
			sendToServer(new ProtocolData(DataType.USERS_REQUEST));
			ProtocolData received = receiveFromServer();
			if (received.getType() == DataType.USERS_LIST) {
				System.out.println("Lista de usuários logados:");
				int num = received.getNumberOfHeaderLines();
				for (int i = 0; i < num; i++)
					System.out.println(received.getHeaderLine(i));
				System.out.print("Pressione <ENTER> para continuar...");
				state = ClientState.WAITING;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected boolean logout() {
		try {
			ProtocolData logoutRequest = new ProtocolData(DataType.LOGOUT_REQUEST);
			logoutRequest.addToHeader(userName);
			sendToServer(logoutRequest);
			ProtocolData received = receiveFromServer();
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
