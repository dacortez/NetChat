/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Concretiza a classe Client utilizando o protocolo TCP para
 * comunicação com o servidor e com outros clientes (p2p).
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class TCPClient extends Client {
	
	public TCPClient(String host, int port, int pierPort) throws Exception {
		super(host, port, pierPort);
		serverPipe = new TCPPipe(host, port);
	}
	
	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setServerSocketChannel(pierPort);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		setSelectableChannel();
		stdin.register(selector, SelectionKey.OP_READ);
		stdinPipe.start();
	}
	
	@Override
	protected void p2pInstantiation(String host, Integer port) throws IOException {
		p2pPipe = new TCPPipe(host, port);
	}
}
