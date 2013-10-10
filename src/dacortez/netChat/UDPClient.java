package dacortez.netChat;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.util.Timer;
import java.util.TimerTask;

public class UDPClient extends Client {
	private static final long RESEND_TIME = 2 * 1000;

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
	
	@Override
	protected void respond(Channel channel) throws IOException {
		super.respond(channel);
		if (received != null)
			if (received.getType() == DataType.DATA_SAVED)
				sendNext();
	}
	
	// saveData() ---------------------------------------------------------------------------------
	
	protected void saveData() throws IOException {
		FileOutputStream out = new FileOutputStream(receiveFile, true);
		int limit = buffer.limit();
		if (totalWritten + limit < totalSize) {
			out.write(buffer.array(), 0, limit);
			out.flush();
			totalWritten += limit;
			out.close();
			System.out.println("Total recebido = " + receiveFile.length());
			p2pPipe.send(new ProtocolData(DataType.DATA_SAVED));
		}
		else {
			out.write(buffer.array(), 0, totalSize.intValue() - totalWritten);
			out.flush();
			totalWritten += totalSize - totalWritten;
			out.close();
			p2pPipe.send(new ProtocolData(DataType.DATA_SAVED));
			System.out.println("Total recebido = " + receiveFile.length());
			transferEnd();
		}
	}		

	// TRANSFER_START -----------------------------------------------------------------------------
	
	private DataInputStream inFromFile;
	private byte[] fileBuffer = new byte[10000];
	private Timer dataTimer;
	
	@Override
	protected void transferStart() throws IOException {
		inFromFile = new DataInputStream(new FileInputStream(sendFile));
		setDataTimer();
		sendNext();
	}
	
	private void setDataTimer() {
		dataTimer = new Timer();
		dataTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (received == null)
					try {
						sendAgain();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}, RESEND_TIME, RESEND_TIME);
	}
	
	private byte[] lastSent;
	
	private void sendNext() throws IOException {
		received = null;
		Integer bytesRead = inFromFile.read(fileBuffer);
		if (bytesRead > 0) {
			lastSent = fileBuffer.clone();
			p2pPipe.send(fileBuffer);
			totalSent += bytesRead;
			System.out.println("Total enviado = " + totalSent);
		}
		else {
			cancelTimer(dataTimer);
			inFromFile.close();
			transferEnd();
		}
	}
	
	private void sendAgain() throws IOException {
		received = null;
		p2pPipe.send(lastSent);
		System.out.println("Dados reenviados.");
	}
}
