package dacortez.netChat;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class UDPClient extends Client {

	public UDPClient(String host, int port, int clientPort) throws IOException {
		super(host, port, clientPort);
		serverPipe = new UDPPipe(host, port);
	}

	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setDatagramChannel(clientPort);
		datagramChannel.register(selector, SelectionKey.OP_READ);
		setSelectableChannel();
		stdin.register(selector, SelectionKey.OP_READ);
		stdinPipe.start();
	}
	
	@Override
	protected void p2pInstantiation(String host, Integer port) throws IOException {
		p2pPipe = new UDPPipe(host, port);
	}
}
