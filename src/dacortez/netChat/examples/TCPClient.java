/**
 * 
 */
package dacortez.netChat.examples;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * @author dacortez
 *
 */
public class TCPClient {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException { 
		String sentence;
		String modifiedSentence;
		
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		Socket clientSocket = new Socket("localhost", 6700);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		
		sentence = inFromUser.readLine(); 
		outToServer.writeBytes(sentence + '\n'); 
		modifiedSentence = inFromServer.readLine(); 
		System.out.println("FROM SERVER: " + modifiedSentence); 
		
		clientSocket.close();
	}
}
