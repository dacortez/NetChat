/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat.examples;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Servidor UDP básico para fins de testes. Não faz parte do EP.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class UDPServer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		DatagramSocket serverSocket = new DatagramSocket(9800);
		
		byte[] receiveData = new byte[1024]; 
		byte[] sendData = new byte[1024];
		
		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			
			String sentence = new String(receivePacket.getData());
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort(); 
			
			String capitalizedSentence = sentence.toUpperCase();
			sendData = capitalizedSentence.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); 
			serverSocket.send(sendPacket);
		}
	}
}
