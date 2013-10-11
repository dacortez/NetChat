package dacortez.netChat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public abstract class Client extends Multiplex {
	protected String host;
	protected Integer serverPort;
	protected Integer pierPort;
	protected final BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	protected String userName;
	protected ClientState state;
	protected Pipe serverPipe;
	protected Pipe p2pPipe;
	private static final long HEART_BEAT_TIME = 10 * 1000;
	
	public static void main(String args[]) throws Exception {
		String host = args[0];
		int serverPort = Integer.parseInt(args[1]);
		if (args[2].charAt(0) == 'T') {
			System.out.println("TCP");
			new TCPClient(host, serverPort, 5000 + new Random().nextInt(1000)).start();
		}
		else if (args[2].charAt(0) == 'U') {
			System.out.println("UDP");
			new UDPClient(host, serverPort, 5000 + new Random().nextInt(1000)).start();
		}
	}
	
	public Client(String host, int serverPort, int pierPort) {
		this.host = host;
		this.serverPort = serverPort;
		this.pierPort = pierPort;
	}
	
	public void start() throws IOException {
		ProtocolData connectionOK = new ProtocolData(DataType.CONNECTION_OK);
		serverPipe.send(connectionOK);
		ProtocolData received = serverPipe.receive();
		if (received.getType() == DataType.CONNECTION_OK)
			doLogin();
		serverPipe.close();		
	}
	
	@Override
	protected void setTimer() {
		timer = new Timer();
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
			cancelTimer(timer);
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
	
	private void printMenu() {
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
			transfering();
			break;
		case SCANNING_MENU:	
			scanningMenu((char) buffer.get());
		}		
	}
	
	// TYPING_USER --------------------------------------------------------------------------------

	private void typingUser() throws IOException {
		System.out.println("Buscando usuário no servidor...");
		ProtocolData chatRequest = chatRequest();
		if (chatRequest != null) {
			serverPipe.send(chatRequest);
			serverResponseFromChatRequest();
		} 
		else
			printMenu();
	}
	
	private ProtocolData chatRequest() {
		String requested = bufferToString().toLowerCase();
		if (requested.contentEquals(this.userName)) {
			System.out.println("O usuário não deve ser você mesmo!");
			return null;
		}
		ProtocolData chatRequest = new ProtocolData(DataType.CHAT_REQUEST);
		chatRequest.addToHeader(requested); // requested
		chatRequest.addToHeader(this.userName); // sender
		return chatRequest;
	}

	private void serverResponseFromChatRequest() throws IOException {
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
	
	// CHATTING -----------------------------------------------------------------------------------
	
	private void chatting() throws IOException {
		ProtocolData chatMsg = chatMsg();
		if (chatMsg != null)
			p2pPipe.send(chatMsg);
	}
	
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
	
	// TYPING_FILE --------------------------------------------------------------------------------
	
	private void typingFile() throws IOException {
		System.out.println("Buscando usuário no servidor...");
		ProtocolData transferRequest = transferRequest();
		if (transferRequest != null) {
			serverPipe.send(transferRequest);
			serverResponseFromTransferRequest();
		} 
		else
			printMenu();
	}
	
	private ProtocolData transferRequest() {
		String pathAndUserName = bufferToString().toLowerCase();
		int index = pathAndUserName.indexOf(">>");
		if (index > 0)
			return transferRequest(pathAndUserName, index);
		else 
			System.out.println("Informação inválida!");
		return null;
	}
	
	protected File sendFile;

	private ProtocolData transferRequest(String pathAndUserName, int index) {
		String path = pathAndUserName.substring(0, index);
		String receiver = pathAndUserName.substring(index + 2);		
		sendFile = new File(path);
		if (sendFile.exists()) {
			return transferRequest(receiver);
		}
		else {
			System.out.println("Arquivo não encontrado!");
			return null;
		}
	}

	private ProtocolData transferRequest(String receiver) {
		if (receiver.contentEquals(this.userName)) {
			System.out.println("Você não pode enviar um arquivo para você mesmo!");
			return null;
		}
		else {
			ProtocolData transferRequest = new ProtocolData(DataType.TRANSFER_REQUEST);
			transferRequest.addToHeader(receiver);
			transferRequest.addToHeader(this.userName);
			return transferRequest;
		}
	}
	
	private void serverResponseFromTransferRequest() throws IOException {
		ProtocolData received = serverPipe.receive();
		if (received.getType() == DataType.TRANSFER_OK) {
			transferOKFromServer(received);
		}
		else if (received.getType() == DataType.TRANSFER_DENIED) {
			System.out.println("Transferencia negada: " + received.getHeaderLine(0));
			printMenu();
		}
	}
		
	private void transferOKFromServer(ProtocolData transferOK) throws IOException {
		String receiver = transferOK.getHeaderLine(0);
		String receiverHost = transferOK.getHeaderLine(1);
		Integer receiverPierPort = Integer.parseInt(transferOK.getHeaderLine(2));
		Long fileSize = sendFile.length();
		transferOK.addToHeader(sendFile.getName());
		transferOK.addToHeader(fileSize.toString());
		state = ClientState.TRANSFERING;
		System.out.println("Iniciando transferência de arquivo...");
		System.out.println(sendFile.getName() + ">>" + receiver + "@" + host + ":" + receiverPierPort.toString());		
		p2pInstantiation(receiverHost, receiverPierPort);
		p2pPipe.send(transferOK);
		
		ProtocolData received = p2pPipe.receive();
		if (received.getType() == DataType.TRANSFER_START) {
			transferStart();
		}
	}
	
	protected int totalSent;
	
	protected void transferStart() throws IOException {
		totalSent = 0;
		DataInputStream inFromFile = new DataInputStream(new FileInputStream(sendFile));
		byte[] fileBuffer = new byte[10000];
		Integer bytesRead = inFromFile.read(fileBuffer);
		while (bytesRead > 0) {
			p2pPipe.send(fileBuffer);
			totalSent += bytesRead;
			System.out.println("Total enviado = " + totalSent);
			bytesRead = inFromFile.read(fileBuffer);
		}
		inFromFile.close();
		transferEndSender();
	}
	
	protected void transferEndSender() throws IOException {
		p2pPipe.close();
		System.out.println("Finalizada transferência de arquivo!");
		printMenu();
	}
	
	// TRANSFERING --------------------------------------------------------------------------------
	
	private void transfering() {
		System.out.println("Em transferência de arquivo!");
	}
	
	// SCANNING_MENU --------------------------------------------------------------------------------

	private void scanningMenu(char option) {
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

	private void listUsers() {
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
	
	private boolean logout() {
		try {
			ProtocolData logoutRequest = new ProtocolData(DataType.LOGOUT_REQUEST);
			logoutRequest.addToHeader(userName);
			serverPipe.send(logoutRequest);
			ProtocolData received = serverPipe.receive();
			if (received.getType() == DataType.LOGOUT_OK) {
				System.out.println("Até a próxima...");
				cancelTimer(timer);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected void cancelTimer(Timer timer) {
		if (timer != null) {
			timer.cancel();
			timer.purge();
			timer = null;
		}
	}
	
	private void closeSelector() {
		try {
			selector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	// --------------------------------------------------------------------------------------------
	// Pier to pier communication (RECEIVER).
	// --------------------------------------------------------------------------------------------
	
	protected ProtocolData received;
	
	@Override
	protected void respond(Channel channel) throws IOException {		
		if (!isProtocolData()) {
			saveData(channel);
			return;
		}
		received = new ProtocolData(buffer);
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
		else if (received.getType() == DataType.TRANSFER_OK) {
			transferOKFromPier(channel, received);
		}
	}
	
	// saveData() ---------------------------------------------------------------------------------
	
	protected void saveData(Channel channel) throws IOException {
		FileOutputStream out = new FileOutputStream(receiveFile, true);
		int limit = buffer.limit();
		if (totalWritten + limit < totalSize) {
			out.write(buffer.array(), 0, limit);
			totalWritten += limit;
			out.flush();
			out.close();
			System.out.println("Total recebido = " + receiveFile.length());
		}
		else {
			out.write(buffer.array(), 0, totalSize.intValue() - totalWritten);
			totalWritten += totalSize - totalWritten;
			out.flush();
			out.close();
			System.out.println("Total recebido = " + receiveFile.length());
			transferEndReceiver();
		}
	}

	protected void transferEndReceiver() {
		System.out.println("Arquivo recebido com sucesso!");
		printMenu();
	}
	
	// CHAT_OK ------------------------------------------------------------------------------------

	protected void chatOKFromPier(ProtocolData chatOK) throws IOException {
		System.out.println("Bate-papo aceito (digite q() para finalizar):\n");
		String host = chatOK.getHeaderLine(2);
		Integer pierPort = Integer.parseInt(chatOK.getHeaderLine(3));
		p2pInstantiation(host, pierPort); 
		state = ClientState.CHATTING;	
	}
	
	// CHAT_MSG -----------------------------------------------------------------------------------
	
	protected void chatMsg(ProtocolData chatMsg) {
		String sender = chatMsg.getHeaderLine(0);
		String msg = chatMsg.getHeaderLine(1);
		System.out.println("[" + sender + "]: " + msg);
	}
	
	// CHAT_FINISHED ------------------------------------------------------------------------------
	
	protected void chatFinished(Channel channel) throws IOException {
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
	
	// CHAT_END -----------------------------------------------------------------------------------
	
	protected void chatEnd(Channel channel) throws IOException {
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
	
	// TRANSFER_OK -----------------------------------------------------------------------------
	
	protected File receiveFile;
	protected Long totalSize;
	protected int totalWritten;
	
	private void transferOKFromPier(Channel channel, ProtocolData transferOK) throws IOException {
		String sender = transferOK.getHeaderLine(3);
		String senderHost = transferOK.getHeaderLine(4);
		Integer senderPierPort = Integer.parseInt(transferOK.getHeaderLine(5));
		String fileName = transferOK.getHeaderLine(6);
		totalSize = Long.parseLong(transferOK.getHeaderLine(7));
		receiveFile = new File("r_" + fileName);
		if (receiveFile.exists()) {
			receiveFile.delete();
			receiveFile.createNewFile();
		} 
		totalWritten = 0;
		state = ClientState.TRANSFERING;
		System.out.println("Recebendo arquivo de " + sender + "@" + senderHost + ":" + senderPierPort);
		ProtocolData transferStart = new ProtocolData(DataType.TRANSFER_START);
		send(channel, transferStart);	
	}
}
