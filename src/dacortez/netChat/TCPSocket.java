package dacortez.netChat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPSocket {
	private final byte[] buffer = new byte[16384];
	private Socket socket;
	
	public TCPSocket(String host, int port) throws IOException {
		socket = new Socket(host, port);
	}
	
	public void send(ProtocolData data) throws IOException {
		byte[] array = data.toByteArray();
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(array);
		//System.out.println("Sent " + array.length + " from " + socket);
	}
	
	public ProtocolData receive() throws IOException {
		int length = socket.getInputStream().read(buffer);
		ProtocolData protocolData = new ProtocolData(buffer, length); 
		//System.out.println("Read " + length + " from " + socket);
		return protocolData;
	}
}
	

