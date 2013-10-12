/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * Implementa Pipe utilizando um socket UDP.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class UDPPipe implements Pipe {
	// Buffer para transferência da dados.
	private final byte[] buffer = new byte[10000];
	// Socket UDP resposável pela comunicação.
	private DatagramSocket socket;
	// Endereço para conexão UDP.
	private InetAddress inet;
	// Porta para conexão UDP.
	private int port;
	
	public UDPPipe(String host, int port) throws IOException {
		socket = new DatagramSocket(9001 + new Random().nextInt(2000));
		inet = InetAddress.getByName(host);
		this.port = port;
	}
	
	@Override
	public void send(ProtocolData protocolData) throws IOException {
		byte[]  array = protocolData.toByteArray();
		DatagramPacket packet = new DatagramPacket(array, array.length, inet, port);
		socket.send(packet); 
	}
	
	@Override
	public void send(byte[] data) throws IOException {
		DatagramPacket packet = new DatagramPacket(data, data.length, inet, port);
		socket.send(packet);
	}
	
	@Override
	public ProtocolData receive() throws IOException {
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length); 
		socket.receive(packet);		
		byte[] data = packet.getData();
		int length = data.length;
		return new ProtocolData(data, length); 
	}
	
	@Override 
	public void close() throws IOException {
		socket.close();
	}
}
