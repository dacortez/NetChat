/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

import java.io.IOException;

/**
 * Inteface que define os métodos de transferência de dados de
 * forma independente do tipo de conexão.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public interface Pipe {
	public void send(ProtocolData protocolData) throws IOException;
	public void send(byte[] data) throws IOException;
	public ProtocolData receive() throws IOException;
	public void close() throws IOException;
}
