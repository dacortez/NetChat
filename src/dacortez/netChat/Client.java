package dacortez.netChat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	private String host;
	private int port;
	private final byte[] buffer = new byte[16384];
	private final BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	private Socket socket;
	private String userName;
	
	public static void main(String args[]) throws Exception {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		new Client(host, port).start();
	}
	
	public Client(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void start() {
		try {
			socket = new Socket(host, port);
			ProtocolData tcpOK = new ProtocolData(DataType.TCP_OK);
			sendTCP(tcpOK);
			if (receiveTCP().getType() == DataType.TCP_OK)
				requestLogin();
			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	private void requestLogin() throws IOException {
		System.out.println("Servidor disponível!");
		System.out.println("Faça seu login...");
		
		System.out.print("Username: ");
		userName = inFromUser.readLine();
		System.out.print("Password: ");
		String password = inFromUser.readLine();
		
		ProtocolData loginInfo = new ProtocolData(DataType.LOGIN_REQUEST);
		loginInfo.addToHeader(userName);
		loginInfo.addToHeader(password);
		sendTCP(loginInfo);
		
		ProtocolData received = receiveTCP();
		if (received.getType() == DataType.LOGIN_OK) {
			System.out.println("Bem-vindo ao servidor de Chat!");
			mainMenu();
		}
		else if (received.getType() == DataType.LOGIN_FAIL)
			System.out.println("Login inválido! Até a próxima...");
	}
	
	private void mainMenu() throws IOException {
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
		sendTCP(new ProtocolData(DataType.USERS_REQUEST));
		ProtocolData received = receiveTCP();
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
		sendTCP(logoutRequest);
		ProtocolData received = receiveTCP();
		if (received.getType() == DataType.LOGOUT_OK) {
			System.out.println("Até a próxima...");
			return true;
		}
		return false;
	}

	private void sendTCP(ProtocolData data) throws IOException {
		byte[] array = data.toByteArray();
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(array);
		System.out.println("Sent " + array.length + " from " + socket);
	}
	
	private ProtocolData receiveTCP() throws IOException {
		int length = socket.getInputStream().read(buffer);
		ProtocolData protocolData = new ProtocolData(buffer, length); 
		System.out.println("Read " + length + " from " + socket);
		return protocolData;
	}
}
