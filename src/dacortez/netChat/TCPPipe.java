package dacortez.netChat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPPipe implements Pipe {
	private final byte[] buffer = new byte[10000];
	private Socket socket;
	
	public TCPPipe(String host, int port) throws IOException {
		socket = new Socket(host, port);
	}
	
	@Override
	public void send(ProtocolData data) throws IOException {
		byte[] array = data.toByteArray();
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(array);
		//System.out.println("Sent array.length = " + array.length + " / data.size = " + data.getSize());
		//System.out.println("Sent " + array.length + " from " + socket);
	}
	
	@Override
	public void send(byte[] data) throws IOException {
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(data);
	}
	
	@Override
	public ProtocolData receive() throws IOException {
		int length = socket.getInputStream().read(buffer);
		ProtocolData protocolData = new ProtocolData(buffer, length); 
		//System.out.println("Read " + length + " from " + socket);
		return protocolData;
	}
	
	@Override 
	public void close() throws IOException {
		socket.close();
	}
}
	

