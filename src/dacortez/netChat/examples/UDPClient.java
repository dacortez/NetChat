/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Cliente UDP básico para fins de testes. Não faz parte do EP.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class UDPClient {

	public static void main(String[] args) throws IOException {
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		DatagramSocket clientSocket = new DatagramSocket(); 
		InetAddress IPAddress = InetAddress.getByName("localhost");
		
		byte[] sendData = new byte[1024]; 
		byte[] receiveData = new byte[1024];
		
		String sentence = inFromUser.readLine();
		sendData = sentence.getBytes();
		
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 7000); 
		clientSocket.send(sendPacket); 
		
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
		clientSocket.receive(receivePacket);
		
		String modifiedSentence = new String(receivePacket.getData());
		System.out.println("FROM SERVER: " + modifiedSentence); 
		
		clientSocket.close();
	}
}
