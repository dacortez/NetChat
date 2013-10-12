/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat.examples;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Cliente TCP básico para fins de testes. Não faz parte do EP.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class TCPClient {

	public static void main(String[] args) throws IOException { 
		String sentence;
		String modifiedSentence;
		
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		Socket clientSocket = new Socket("localhost", 6000);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		
		sentence = inFromUser.readLine(); 
		outToServer.writeBytes(sentence + '\n'); 
		modifiedSentence = inFromServer.readLine(); 
		System.out.println("FROM SERVER: " + modifiedSentence); 
		
		clientSocket.close();
	}
}
