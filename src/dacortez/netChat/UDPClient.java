package dacortez.netChat;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

/**
 * Concretiza a classe Client utilizando o protocolo UDP para
 * comunicação com o servidor e com outros clientes (p2p).
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class UDPClient extends Client {

	public UDPClient(String host, int port, int pierPort) throws Exception {
		super(host, port, pierPort);
		serverPipe = new UDPPipe(host, port);
	}

	@Override
	protected void registerChannelsWithSelector() throws IOException {
		setDatagramChannel(pierPort);
		datagramChannel.register(selector, SelectionKey.OP_READ);
		setSelectableChannel();
		stdin.register(selector, SelectionKey.OP_READ);
		stdinPipe.start();
	}
	
	@Override
	protected void p2pInstantiation(String host, Integer port) throws IOException {
		p2pPipe = new UDPPipe(host, port);
	}
	
	// Bloco de dados (bytes) do arquivo sendo transferido --------------------
	
	@Override
	protected void saveData(Channel channel) throws IOException {
		int limit = buffer.limit();
		if (totalWritten + limit < totalSize) {
			writeNextBlock(limit);
			send(channel, new ProtocolData(ProtocolMessage.DATA_SAVED));
		}
		else {
			writeFinalBlock();
			send(channel, new ProtocolData(ProtocolMessage.DATA_SAVED));
			transferEndReceiver();
		}
	}		

	// TRANSFER_START -----------------------------------------------------------------------------
	
	@Override
	protected Integer sendNextBlock(Integer bytesRead) throws IOException {
		p2pPipe.send(fileBuffer);
		totalSent += bytesRead;
		//System.out.println("Total enviado = " + totalSent);
		p2pPipe.receive();
		return inFromFile.read(fileBuffer);
	}
}
