package dacortez.netChat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class TCPClient extends Client {
	private Socket socket;
	
	public TCPClient(String host, int port) {
		super(host, port);
	}
	
	@Override
	protected void start() throws IOException {
		socket = new Socket(host, port);
		ProtocolData tcpOK = new ProtocolData(DataType.TCP_OK);
		sendToServer(tcpOK);
		if (receiveFromServer().getType() == DataType.TCP_OK)
			doLogin();
		socket.close();		
	}
	
	@Override
	protected void sendToServer(ProtocolData data) throws IOException {
		byte[] array = data.toByteArray();
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(array);
		//System.out.println("Sent " + array.length + " from " + socket);
	}
	
	@Override
	protected ProtocolData receiveFromServer() throws IOException {
		int length = socket.getInputStream().read(serverBuffer);
		ProtocolData protocolData = new ProtocolData(serverBuffer, length); 
		//System.out.println("Read " + length + " from " + socket);
		return protocolData;
	}
	
	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setServerSocketChannel(8000);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("TCPClient listening on port 8000 for connections");
		
		setSelectableChannel();
		stdin.register(selector, SelectionKey.OP_READ);
		stdinPipe.start();
		System.out.println("TCPClient listening stdin");
		
		
	}

	@Override
	protected void respondTCP(SocketChannel channel) throws IOException {
		System.out.println("****");
		ProtocolData received = new ProtocolData(buffer);
		if (received.getType() == DataType.HEART_BEAT) {
			System.out.println("**** RECEBIDO HEART_BEAT *******");
		}
	}

	@Override
	protected void respondUDP(DatagramChannel channel) throws IOException {
		// Nothing to do here.
	}
}
