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
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor TCP básico para fins de testes. Não faz parte do EP.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class TCPServer {

	public static void main(String[] args) throws IOException {
		String clientSentence; 
		String capitalizedSentence;
		
		ServerSocket welcomeSocket = new ServerSocket(6000);
		
		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			clientSentence = inFromClient.readLine(); 
			capitalizedSentence = clientSentence.toUpperCase() + '\n'; 
			outToClient.writeBytes(capitalizedSentence);
		}
	}
}
