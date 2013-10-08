package dacortez.netChat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.TimerTask;

public abstract class Client extends Multiplex {
	protected String host;
	protected Integer port;
	protected Integer pierPort;
	protected final BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	protected String userName;
	protected ClientState state;
	protected Pipe serverPipe;
	protected Pipe p2pPipe;
	private static final long HEART_BEAT_TIME = 10 * 1000;
	private File file;
	
	public static void main(String args[]) throws Exception {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		if (args[2].charAt(0) == 'T') {
			System.out.println("TCP");
			new TCPClient(host, port, 5000 + new Random().nextInt(1000)).start();
		}
		else if (args[2].charAt(0) == 'U') {
			System.out.println("UDP");
			new UDPClient(host, port, 5000 + new Random().nextInt(1000)).start();
		}
	}
	
	public Client(String host, int port, int pierPort) {
		this.host = host;
		this.port = port;
		this.pierPort = pierPort;
	}
	
	protected void start() throws IOException {
		ProtocolData connectionOK = new ProtocolData(DataType.CONNECTION_OK);
		serverPipe.send(connectionOK);
		ProtocolData received = serverPipe.receive();
		if (received.getType() == DataType.CONNECTION_OK)
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
		serverPipe.send(loginRequest());
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

	private ProtocolData loginRequest() throws IOException {
		System.out.println("Servidor disponível!");
		System.out.println("Faça seu login...");
		System.out.print("Username: ");
		userName = inFromUser.readLine().toLowerCase();
		System.out.print("Password: ");
		String password = inFromUser.readLine();
		ProtocolData loginRequest = new ProtocolData(DataType.LOGIN_REQUEST);
		loginRequest.addToHeader(userName);
		loginRequest.addToHeader(password);
		loginRequest.addToHeader(pierPort.toString());
		return loginRequest;
	}
	
	protected void printMenu() {
		System.out.println("(L) Lista usuários logados");
		System.out.println("(B) Iniciar bate-papo com usuário");
		System.out.println("(A) Enviar arquivo para usuário");
		System.out.println("(Q) Fazer logout");
		System.out.println("Escolha uma das opções:");
		state = ClientState.SCANNING_MENU;
	}
	
	@Override
	protected void respondStdin(ReadableByteChannel channel) throws IOException {
		switch (state) {
		case TYPING_USER:
			typingUser();
			break;
		case CHATTING:
			chatting();
			break;
		case TYPING_FILE:
			typingFile();
			break;
		case TRANSFERING:
			System.out.println("Transferindo arquivo!");
			break;
		case SCANNING_MENU:	
			switchMenuOption((char) buffer.get());
		}		
	}

	private void typingUser() throws IOException {
		System.out.println("Buscando usuário no servidor...");
		ProtocolData chatRequest = chatRequest();
		if (chatRequest != null) {
			serverPipe.send(chatRequest);
			processResponseFromChatRequest();
		} 
		else
			printMenu();
	}
	
	private ProtocolData chatRequest() {
		String requested = bufferToString();
		if (requested.toLowerCase().contentEquals(this.userName)) {
			System.out.println("O usuário não deve ser você mesmo!");
			return null;
		}
		ProtocolData chatRequest = new ProtocolData(DataType.CHAT_REQUEST);
		chatRequest.addToHeader(requested); // requested
		chatRequest.addToHeader(this.userName); // sender
		return chatRequest;
	}

	private void processResponseFromChatRequest() throws IOException {
		ProtocolData received = serverPipe.receive();
		if (received.getType() == DataType.CHAT_DENIED) {
			System.out.println("Bate-papo negado: " + received.getHeaderLine(0));
			printMenu();
		}
		else if (received.getType() == DataType.CHAT_OK)
			chatOKFromServer(received);
	}

	private void chatOKFromServer(ProtocolData chatOK) throws IOException {
		System.out.println("Bate-papo aceito (digite 'q()' para finalizar):\n");
		String host = chatOK.getHeaderLine(0);
		Integer pierPort = Integer.parseInt(chatOK.getHeaderLine(1));
		p2pInstantiation(host, pierPort); 
		state = ClientState.CHATTING;
		p2pPipe.send(chatOK);	
	}
	
	protected abstract void p2pInstantiation(String host, Integer port) throws IOException;
	
	private void chatting() throws IOException {
		ProtocolData chatMsg = chatMsg();
		if (chatMsg != null)
			p2pPipe.send(chatMsg);
	}
	
	private void typingFile() throws IOException {
		System.out.println("Buscando usuário no servidor...");
		ProtocolData transferRequest = transferRequest();
		if (transferRequest != null) {
			serverPipe.send(transferRequest);
			processResponseFromTransferRequest();
		} 
		else
			printMenu();
	}
	
	private ProtocolData transferRequest() {
		String pathAndUserName = bufferToString();
		int index = pathAndUserName.indexOf(">>");
		if (index > 0)
			return transferRequest(pathAndUserName, index);
		else 
			System.out.println("Informação inválida!");
		return null;
	}

	private ProtocolData transferRequest(String pathAndUserName, int index) {
		String path = pathAndUserName.substring(0, index);
		String userName = pathAndUserName.substring(index + 2);		
		file = new File(path);
		if (file.exists()) {
			ProtocolData transferRequest = new ProtocolData(DataType.TRANSFER_REQUEST);
			transferRequest.addToHeader(userName);
			return transferRequest;
		}
		else {
			System.out.println("Arquivo não encontrado!");
			return null;
		}
	}
	
	private void processResponseFromTransferRequest() throws IOException {
		ProtocolData received = serverPipe.receive();
		if (received.getType() == DataType.TRANSFER_OK) {
			String userName = received.getHeaderLine(0);
			String host = received.getHeaderLine(1);
			Integer pierPort = Integer.parseInt(received.getHeaderLine(2));
			String path = file.getPath();
			System.out.println("Iniciando transferência de arquivo...");
			System.out.println(path + ">>" + userName + "@" + host + ":" + pierPort.toString());
			transferFile(file, userName, host, pierPort);
			printMenu();
		}
		else if (received.getType() == DataType.TRANSFER_DENIED) {
			System.out.println("Transferencia negada: " + received.getHeaderLine(0));
			printMenu();
		}
	}
	
	public void transferFile(File file, String userName, String host, Integer pierPort) throws IOException {
		state = ClientState.TRANSFERING;
		//p2pInstantiation(host, pierPort); 
		FileInputStream inFromFile = new FileInputStream(file);
		FileOutputStream outToFile = new FileOutputStream(new File("chegada.txt"));
		byte[] fileBuffer = new byte[10];
		Integer bytesRead = inFromFile.read(fileBuffer);
		while (bytesRead > 0) {
			ProtocolData fileData = new ProtocolData(DataType.FILE_DATA);
			fileData.addToHeader(file.getName());
			fileData.addToHeader(bytesRead.toString());
			fileData.allocateData(bytesRead);
			for (int i = 0; i < bytesRead; i++)
				fileData.putByte(i, fileBuffer[i]);
			//p2pPipe.send(fileData);
			System.out.println(fileData);
			outToFile.write(fileData.getData());
			bytesRead = inFromFile.read(fileBuffer);
		}
		inFromFile.close();
		outToFile.close();
	}

	// Melhorar ---------------------------------------------------------
	
	private ProtocolData chatMsg() throws IOException {
		String msg = bufferToString();
		if (msg.isEmpty()) 
			return null;
		if (msg.contentEquals("q()"))
			return chatFinished();
		ProtocolData chatMsg = new ProtocolData(DataType.CHAT_MSG);
		chatMsg.addToHeader(this.userName); 
		chatMsg.addToHeader(msg); 
		return chatMsg;
	}
	
	private ProtocolData chatFinished() throws IOException {
		ProtocolData chatEnd = new ProtocolData(DataType.CHAT_END);
		chatEnd.addToHeader(userName);
		serverPipe.send(chatEnd);
		return serverPipe.receive();
	}
	
	// Fim melhorar -----------------------------------------------------

	private void switchMenuOption(char option) {
		switch (option) {
		case 'L': case 'l':
			listUsers();
			printMenu();
			break;
		case 'B': case 'b':
			System.out.println("Informe o Username do usuário:");
			state = ClientState.TYPING_USER;
			break;
		case 'A': case 'a':
			System.out.println("Informe arquivo e usuário (path>>username):");
			state = ClientState.TYPING_FILE;
			break;
		case 'Q': case 'q':
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
			e.printStackTrace();
		}
	}
		
	//------------------------------------------------------------------
	
	@Override
	protected void respond(Channel channel) throws IOException {
		ProtocolData received = new ProtocolData(buffer);
		if (received.getType() == DataType.CHAT_OK) {
			chatOKFromPier(received);
		}
		else if (received.getType() == DataType.CHAT_MSG) {
			chatMsg(received);
		}
		else if (received.getType() == DataType.CHAT_FINISHED) {
			chatFinished(channel);
		}
		else if (received.getType() == DataType.CHAT_END) {
			chatEnd(channel);
		}
	}

	private void chatOKFromPier(ProtocolData chatOK) throws IOException {
		System.out.println("Bate-papo aceito (digite q() para finalizar):\n");
		String host = chatOK.getHeaderLine(2);
		Integer pierPort = Integer.parseInt(chatOK.getHeaderLine(3));
		p2pInstantiation(host, pierPort); 
		state = ClientState.CHATTING;	
	}
	
	private void chatMsg(ProtocolData chatMsg) {
		String sender = chatMsg.getHeaderLine(0);
		String msg = chatMsg.getHeaderLine(1);
		System.out.println("[" + sender + "]: " + msg);
	}
	
	private void chatFinished(Channel channel) throws IOException {
		closeIfSocketChannel(channel);
		ProtocolData chatEnd = new ProtocolData(DataType.CHAT_END);
		chatEnd.addToHeader(userName);
		serverPipe.send(chatEnd);
		if (serverPipe.receive().getType() == DataType.CHAT_FINISHED) {
			p2pPipe.send(chatEnd);
			p2pPipe.close();
			System.out.println("Bate-papo-finalizado!");
			printMenu();
		}
	}
	
	private void chatEnd(Channel channel) throws IOException {
		closeIfSocketChannel(channel);
		p2pPipe.close();
		System.out.println("Bate-papo-finalizado!");
		printMenu();
	}
	
	private void closeIfSocketChannel(Channel channel) throws IOException {
		if (channel instanceof SocketChannel) {
			SocketChannel sc = (SocketChannel) channel;
			sc.keyFor(selector).cancel();
			sc.close();
		}
	}
}
