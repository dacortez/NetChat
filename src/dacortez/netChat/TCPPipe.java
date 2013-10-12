/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Implementa Pipe utilizando um socket TCP.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class TCPPipe implements Pipe {
	// Buffer para transferência de dados.
	private final byte[] buffer = new byte[10000];
	// Socket TCP resposável pela comunicação.
	private Socket socket;
	
	public TCPPipe(String host, int port) throws IOException {
		socket = new Socket(host, port);
	}
	
	@Override
	public void send(ProtocolData protocolData) throws IOException {
		byte[] array = protocolData.toByteArray();
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(array);
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
		return protocolData;
	}
	
	@Override 
	public void close() throws IOException {
		socket.close();
	}
}
