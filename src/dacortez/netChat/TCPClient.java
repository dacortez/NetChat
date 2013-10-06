package dacortez.netChat;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class TCPClient extends Client {
	
	public TCPClient(String host, int port, int clientPort) throws IOException {
		super(host, port, clientPort);
		serverPipe = new TCPPipe(host, port);
	}
	
	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setServerSocketChannel(clientPort);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		//System.out.println("TCPClient listening on port " + clientPort);
		setSelectableChannel();
		stdin.register(selector, SelectionKey.OP_READ);
		stdinPipe.start();
		//System.out.println("TCPClient listening stdin");
	}
	
	@Override
	protected void p2pInstantiation(String host, Integer port) throws IOException {
		p2pPipe = new TCPPipe(host, port);
	}

	@Override
	protected void respondTCP(SocketChannel channel) throws IOException {
		ProtocolData received = new ProtocolData(buffer);
		if (received.getType() == DataType.CHAT_OK) {
			chatOK(received);
		}
		else if (received.getType() == DataType.CHAT_MSG) {
			chatMsg(received);
		}
	}

	private void chatOK(ProtocolData chatOK) throws IOException {
		System.out.println("Bate-papo aceito:\n");
		String host = chatOK.getHeaderLine(2);
		Integer port = Integer.parseInt(chatOK.getHeaderLine(3));
		p2pInstantiation(host, port); 
		state = ClientState.CHATTING;	
	}
	
	private void chatMsg(ProtocolData chatMsg) {
		String sender = chatMsg.getHeaderLine(0);
		String msg = chatMsg.getHeaderLine(1);
		System.out.println("[" + sender + "]: " + msg);
	}

	@Override
	protected void respondUDP(DatagramChannel channel) throws IOException {
		// Nothing to do here.
	}
}
