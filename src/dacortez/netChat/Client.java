package dacortez.netChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class Client extends Multiplex {
	protected String host;
	protected int port;
	protected final byte[] buffer = new byte[16384];
	protected final BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	protected String userName;
	
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
	
	protected abstract void send(ProtocolData data) throws IOException;
	
	protected abstract ProtocolData receive() throws IOException;

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
		send(loginInfo);
		
		ProtocolData received = receive();
		if (received.getType() == DataType.LOGIN_OK) {
			System.out.println("Bem-vindo ao servidor de Chat!");
			mainMenu();
		}
		else if (received.getType() == DataType.LOGIN_FAIL)
			System.out.println("Login inválido! Até a próxima...");
	}
	
	private void mainMenu() throws IOException {
		// Implementa aqui a multiplexação pois login ok.
		while (true) {
			printMenu();
			String option = inFromUser.readLine();
			if (option.matches("U"))
				listUsers();
			else if (option.matches("L"))
				if (logout()) break;
			else 
				System.out.println("Opção inválida!");
		} 
	}

	private void printMenu() throws IOException {
		System.out.println("(U) Lista usuários logados");
		System.out.println("(B) Iniciar bate-papo com usuário logado");
		System.out.println("(E) Enviar arquivo para usuário logado");
		System.out.println("(L) Fazer logout");
		System.out.print("Escolha uma das opções: ");
	}
	
	private void listUsers() throws IOException {
		send(new ProtocolData(DataType.USERS_REQUEST));
		ProtocolData received = receive();
		if (received.getType() == DataType.USERS_LIST) {
			System.out.println("Lista de usuários logados:");
			int num = received.getNumberOfHeaderLines();
			for (int i = 0; i < num; i++)
				System.out.println(received.getHeaderLine(i));
			System.out.print("Pressione <ENTER> para continuar...");
			inFromUser.readLine();
		}
	}
	
	private boolean logout() throws IOException {
		ProtocolData logoutRequest = new ProtocolData(DataType.LOGOUT_REQUEST);
		logoutRequest.addToHeader(userName);
		send(logoutRequest);
		ProtocolData received = receive();
		if (received.getType() == DataType.LOGOUT_OK) {
			System.out.println("Até a próxima...");
			return true;
		}
		return false;
	}
}
