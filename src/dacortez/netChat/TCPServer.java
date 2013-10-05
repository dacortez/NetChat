/**
 * 
 */
package dacortez.netChat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author dacortez
 *
 */
public class TCPServer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String clientSentence; 
		String capitalizedSentence;
		
		ServerSocket welcomeSocket = new ServerSocket(6700);
		
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
