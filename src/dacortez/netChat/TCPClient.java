package dacortez.netChat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
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
		send(tcpOK);
		if (receive().getType() == DataType.TCP_OK)
			doLogin();
		socket.close();		
	}
	
	@Override
	protected void send(ProtocolData data) throws IOException {
		byte[] array = data.toByteArray();
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(array);
		System.out.println("Sent " + array.length + " from " + socket);
	}
	
	@Override
	protected ProtocolData receive() throws IOException {
		int length = socket.getInputStream().read(buffer);
		ProtocolData protocolData = new ProtocolData(buffer, length); 
		System.out.println("Read " + length + " from " + socket);
		return protocolData;
	}
	
	@Override
	protected void registerChannelsWithSelector() throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	protected void respondTCP(SocketChannel channel) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	protected void respondUDP(DatagramChannel channel) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	protected void respondStdin(ReadableByteChannel channel) throws IOException {
		// TODO Auto-generated method stub
	}
}
