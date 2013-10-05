/**
 * 
 */
package dacortez.netChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author dacortez
 *
 */
public class UDPClient {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		DatagramSocket clientSocket = new DatagramSocket(); 
		//InetAddress IPAddress = InetAddress.getByName("192.168.56.101");
		InetAddress IPAddress = InetAddress.getByName("127.0.0.1");
		
		byte[] sendData = new byte[1024]; 
		//byte[] receiveData = new byte[1024];
		
		String sentence = inFromUser.readLine();
		sendData = sentence.getBytes();
		
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 7000); 
		clientSocket.send(sendPacket); 
		
		//DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
		//clientSocket.receive(receivePacket);
		
		//String modifiedSentence = new String(receivePacket.getData());
		//System.out.println("FROM SERVER: " + modifiedSentence); 
		
		clientSocket.close();
	}
}
