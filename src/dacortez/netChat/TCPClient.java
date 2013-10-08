package dacortez.netChat;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class TCPClient extends Client {
	
	public TCPClient(String host, int port, int pierPort) throws IOException {
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
