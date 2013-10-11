package dacortez.netChat;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

public class UDPClient extends Client {

	public UDPClient(String host, int port, int pierPort) throws IOException {
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
	
	// saveData() ---------------------------------------------------------------------------------
	
	@Override
	protected void saveData(Channel channel) throws IOException {
		FileOutputStream out = new FileOutputStream(receiveFile, true);
		int limit = buffer.limit();
		if (totalWritten + limit < totalSize) {
			out.write(buffer.array(), 0, limit);
			totalWritten += limit;
			out.flush();
			out.close();
			System.out.println("Total recebido = " + receiveFile.length());
			send(channel, new ProtocolData(DataType.DATA_SAVED));
		}
		else {
			out.write(buffer.array(), 0, totalSize.intValue() - totalWritten);
			totalWritten += totalSize - totalWritten;
			out.flush();
			out.close();
			System.out.println("Total recebido = " + receiveFile.length());
			send(channel, new ProtocolData(DataType.DATA_SAVED));
			transferEndReceiver();
		}
	}		

	// TRANSFER_START -----------------------------------------------------------------------------
	
	private DataInputStream inFromFile;
	private byte[] fileBuffer = new byte[10000];
	
	@Override
	protected void transferStart() throws IOException {
		totalSent = 0;
		inFromFile = new DataInputStream(new FileInputStream(sendFile));
		sendNext();
	}
		
	private void sendNext() throws IOException {
		Integer bytesRead = inFromFile.read(fileBuffer);
		while (bytesRead > 0) {
			p2pPipe.send(fileBuffer);
			totalSent += bytesRead;
			System.out.println("Total enviado = " + totalSent);
			if (p2pPipe.receive().getType() == DataType.DATA_SAVED) {
				System.out.println("SALVOU");
			}
			bytesRead = inFromFile.read(fileBuffer);
		}
		inFromFile.close();
		transferEndSender();
	}
}
