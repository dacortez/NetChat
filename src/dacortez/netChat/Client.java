/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * O cliente estende a classe Multiplex para poder receber conexões 
 * TCP e UDP dos outros clientes (p2p), bem como tratar a entrada 
 * padrão. A conexão com o servidor também pode ser feita usando 
 * tanto TCP quanto UDP, dependendo das classes concretas TCPClient 
 * e UDPClient. Envia periodicamente heart beats para o servidor.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public abstract class Client extends Multiplex {
	// O endereço do servidor para conexão.
	protected String host;
	// A porta do servidor para conexão.
	protected Integer serverPort;
	// A porta do cliente para poder receber conexão de outros clientes.
	protected Integer pierPort;
	// Entrada padrão do usuário.
	protected final BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	// Username do usuário rodando o proceeso cliente.
	protected String userName;
	// Variável de estada para o processo cliente.
	protected ClientState state;
	// Permite transferência de dados com o servidor.
	protected Pipe serverPipe;
	// Permite transferência de dados com outros clientes.
	protected Pipe p2pPipe;
	// Tempo em segundos entre envio de heart beats ao servidor.
	public static final long HEART_BEAT = 5;
	
	public static void main(String args[]) throws Exception {
		String host = args[0];
		int serverPort = Integer.parseInt(args[1]);
		if (args[2].charAt(0) == 'T') {
			System.out.println("Cliente TCP");
			new TCPClient(host, serverPort, 7001 + new Random().nextInt(2000)).start();
		}
		else if (args[2].charAt(0) == 'U') {
			System.out.println("Cliente UDP");
			new UDPClient(host, serverPort, 7001 + new Random().nextInt(2000)).start();
		}
	}
	
	public Client(String host, int serverPort, int pierPort) {
		this.host = host;
		this.serverPort = serverPort;
		this.pierPort = pierPort;
	}
	
	public void start() throws IOException {
		ProtocolData connectionOK = new ProtocolData(ProtocolMessage.CONNECTION_OK);
		serverPipe.send(connectionOK);
		ProtocolData received = serverPipe.receive();
		if (received.getMessage() == ProtocolMessage.CONNECTION_OK)
			doLogin();
		serverPipe.close();		
	}
	
	/* O timer do cliente envia informação de hear beat a cada
	 * HEART_BEAT segundos.
	 * (non-Javadoc)
	 * @see dacortez.netChat.Multiplex#setTimer()
	 */
	@Override
	protected void setTimer() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					ProtocolData heartBeat = new ProtocolData(ProtocolMessage.HEART_BEAT);
					heartBeat.addToHeader(userName);
					serverPipe.send(heartBeat);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, HEART_BEAT * 1000,  HEART_BEAT * 1000);
	}
	
	protected void doLogin() throws IOException {
		serverPipe.send(loginRequest());
		ProtocolData received = serverPipe.receive();
		if (received.getMessage() == ProtocolMessage.LOGIN_OK) {
			System.out.println("Bem-vindo ao sistema de bate-papo!");
			printMenu();
			run();
		}
		else if (received.getMessage() == ProtocolMessage.LOGIN_FAIL) {
			System.out.println("Login inválido! Até a próxima...");
			cancelTimer(timer);
		}
	}

	private ProtocolData loginRequest() throws IOException {
		System.out.println("O servidor está disponível!");
		System.out.println("Faça seu login...");
		System.out.print("Username: ");
		userName = inFromUser.readLine().toLowerCase();
		System.out.print("Password: ");
		String password = inFromUser.readLine();
		ProtocolData loginRequest = new ProtocolData(ProtocolMessage.LOGIN_REQUEST);
		loginRequest.addToHeader(userName);
		loginRequest.addToHeader(password);
		loginRequest.addToHeader(pierPort.toString());
		return loginRequest;
	}
	
	private void printMenu() {
		System.out.println("(L) Lista usuários logados");
		System.out.println("(B) Iniciar bate-papo com usuário");
		System.out.println("(A) Enviar arquivo para usuário");
		System.out.println("(E) Fazer experimento do EP");
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
		case TRANSFERING_FILE:
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
		ProtocolData chatRequest = new ProtocolData(ProtocolMessage.CHAT_REQUEST);
		chatRequest.addToHeader(requested); // requested
		chatRequest.addToHeader(this.userName); // sender
		return chatRequest;
	}

	private void serverResponseFromChatRequest() throws IOException {
		ProtocolData received = serverPipe.receive();
		if (received.getMessage() == ProtocolMessage.CHAT_DENIED) {
			System.out.println("Bate-papo negado: " + received.getHeaderLine(0));
			printMenu();
		}
		else if (received.getMessage() == ProtocolMessage.CHAT_OK)
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
		ProtocolData chatMsg = new ProtocolData(ProtocolMessage.CHAT_MSG);
		chatMsg.addToHeader(this.userName); 
		chatMsg.addToHeader(msg); 
		return chatMsg;
	}
	
	private ProtocolData chatFinished() throws IOException {
		ProtocolData chatEnd = new ProtocolData(ProtocolMessage.CHAT_END);
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
	
	// Arquivo que será enviado ao outro cliente.
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
			ProtocolData transferRequest = new ProtocolData(ProtocolMessage.TRANSFER_REQUEST);
			transferRequest.addToHeader(receiver);
			transferRequest.addToHeader(this.userName);
			return transferRequest;
		}
	}
	
	private void serverResponseFromTransferRequest() throws IOException {
		ProtocolData received = serverPipe.receive();
		if (received.getMessage() == ProtocolMessage.TRANSFER_OK) {
			transferOKFromServer(received);
		}
		else if (received.getMessage() == ProtocolMessage.TRANSFER_DENIED) {
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
		if (doExperiment)
			transferOK.addToHeader("T");
		else
			transferOK.addToHeader("F");
		state = ClientState.TRANSFERING_FILE;
		System.out.println("Iniciando transferência de arquivo...");
		System.out.println(sendFile.getName() + ">>" + receiver + "@" + host + ":" + receiverPierPort.toString());		
		p2pInstantiation(receiverHost, receiverPierPort);
		p2pPipe.send(transferOK);
		if (p2pPipe.receive().getMessage() == ProtocolMessage.TRANSFER_START)
			transferStart();
	}
	
	// Total de bytes do arquivo enviados.
	protected int totalSent;
	// Buffer para leitura do arquivo.
	protected final byte[] fileBuffer = new byte[10000];
	// InputStream do arquivo sendo transferido.
	protected DataInputStream inFromFile;
	// Número de medidadas de tempo para o experimento do EP.
	protected static final int MAX_EXP = 30;
	
	private void transferStart() throws IOException {
		if (doExperiment) {
			List<Double> times = new ArrayList<Double>();
			for (int i = 1; i <= MAX_EXP; i++) {
				times.add(sendOneFile());
				System.out.println("Mediada de tempo " + i + " realizada!");
			}
			transferEndSender();
			System.out.println("Média = " + mean(times) + " s");
			System.out.println("Desvio-padrão = " + stdev(times) + " s");
		}
		else {
			sendOneFile();
			transferEndSender();
		}
	}

	private Double sendOneFile() throws FileNotFoundException, IOException {
		inFromFile = new DataInputStream(new FileInputStream(sendFile));
		totalSent = 0;
		Long startTime = System.currentTimeMillis();
		Integer bytesRead = inFromFile.read(fileBuffer);
		while (bytesRead > 0)
			bytesRead = sendNextBlock(bytesRead);
		Long endTime = System.currentTimeMillis();
		inFromFile.close();
		return (endTime - startTime) / 1000.0;
	}

	protected Integer sendNextBlock(Integer bytesRead) throws IOException {
		p2pPipe.send(fileBuffer);
		totalSent += bytesRead;
		return inFromFile.read(fileBuffer);
	}
	
	private void transferEndSender() throws IOException {
		p2pPipe.close();
		System.out.println("Finalizada transferência de arquivo!");
		printMenu();
	}
	
	private Double mean(List<Double> values) {
		Double sum = 0.0;
		for (Double value: values)
			sum += value;
		return sum / values.size();
	}
	
	private Double stdev(List<Double> values) {
		Double mean = mean(values);
		Double sum = 0.0;
		for (Double value: values)
			sum += (value - mean) * (value - mean) ;
		return Math.sqrt(sum / values.size());
	}
	
	// TRANSFERING --------------------------------------------------------------------------------
	
	private void transfering() {
		System.out.println("Em transferência de arquivo!");
	}
	
	// SCANNING_MENU --------------------------------------------------------------------------------

	protected boolean doExperiment;
	
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
			doExperiment = false;
			System.out.println("Informe arquivo e usuário (path>>username):");
			state = ClientState.TYPING_FILE;
			break;
		case 'E': case 'e':
			doExperiment = true;
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
			serverPipe.send(new ProtocolData(ProtocolMessage.USERS_REQUEST));
			ProtocolData received = serverPipe.receive();
			if (received.getMessage() == ProtocolMessage.USERS_LIST) {
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
			ProtocolData logoutRequest = new ProtocolData(ProtocolMessage.LOGOUT_REQUEST);
			logoutRequest.addToHeader(userName);
			serverPipe.send(logoutRequest);
			ProtocolData received = serverPipe.receive();
			if (received.getMessage() == ProtocolMessage.LOGOUT_OK) {
				System.out.println("Até a próxima...");
				cancelTimer(timer);
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
		if (received.getMessage() == ProtocolMessage.CHAT_OK) {
			chatOKFromPier(received);
		}
		else if (received.getMessage() == ProtocolMessage.CHAT_MSG) {
			chatMsg(received);
		}
		else if (received.getMessage() == ProtocolMessage.CHAT_FINISHED) {
			chatFinished(channel);
		}
		else if (received.getMessage() == ProtocolMessage.CHAT_END) {
			chatEnd(channel);
		}
		else if (received.getMessage() == ProtocolMessage.TRANSFER_OK) {
			transferOKFromPier(channel, received);
		}
	}
	
	// Bloco de dados (bytes) do arquivo sendo transferido --------------------
	
	protected void saveData(Channel channel) throws IOException {
		int limit = buffer.limit();
		if (totalWritten + limit < totalSize) {
			writeNextBlock(limit);
		}
		else {
			writeFinalBlock();
			transferEndReceiver();
		}
	}

	protected void writeNextBlock(int limit) throws IOException {
		fileOut.write(buffer.array(), 0, limit);
		totalWritten += limit;
	}
	
	protected void writeFinalBlock() throws IOException {
		fileOut.write(buffer.array(), 0, totalSize.intValue() - totalWritten);
		totalWritten += totalSize - totalWritten;
		fileOut.flush();
		fileOut.close();
	}

	protected void transferEndReceiver() throws FileNotFoundException {
		System.out.println("Arquivo recebido com sucesso!");
		if (doExperiment) 
			if (++filesReceived >= MAX_EXP) {
				doExperiment = false;
				printMenu();
			}
			else {
				System.out.println("Recebendo " + (filesReceived + 1) + "...");
				receiveFile.delete();
				fileOut = new FileOutputStream(receiveFile, true);
				totalWritten = 0;
			}
		else {
			printMenu();
		}
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
		ProtocolData chatEnd = new ProtocolData(ProtocolMessage.CHAT_END);
		chatEnd.addToHeader(userName);
		serverPipe.send(chatEnd);
		if (serverPipe.receive().getMessage() == ProtocolMessage.CHAT_FINISHED) {
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
	
	protected Long totalSize;
	private File receiveFile;
	private FileOutputStream fileOut;
	protected int totalWritten;
	protected int filesReceived;
	
	private void transferOKFromPier(Channel channel, ProtocolData transferOK) throws IOException {
		String sender = transferOK.getHeaderLine(3);
		String senderHost = transferOK.getHeaderLine(4);
		Integer senderPierPort = Integer.parseInt(transferOK.getHeaderLine(5));
		String fileName = transferOK.getHeaderLine(6);
		totalSize = Long.parseLong(transferOK.getHeaderLine(7));
		doExperiment = transferOK.getHeaderLine(8).contentEquals("T");
		receiveFile = new File("r_" + fileName);
		if (receiveFile.exists()) {
			receiveFile.delete();
			receiveFile.createNewFile();
		}
		fileOut = new FileOutputStream(receiveFile, true);
		totalWritten = 0;
		filesReceived = 0;
		state = ClientState.TRANSFERING_FILE;
		System.out.println("Recebendo arquivo de " + sender + "@" + senderHost + ":" + senderPierPort);
		ProtocolData transferStart = new ProtocolData(ProtocolMessage.TRANSFER_START);
		send(channel, transferStart);	
	}
}
