package dacortez.netChat;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Server implements Runnable {
	private static List<User> allUsers;
	private int port;
	// A pre-allocated buffer for encrypting data
	private final ByteBuffer buffer = ByteBuffer.allocate(16384);

	public Server(int port) {
		this.port = port;
		setAllUsers();
		new Thread(this).start();
	}

	private void setAllUsers() {
		allUsers = new ArrayList<User>();
		// senha "foobar"
		allUsers.add(new User("Daniel Augusto Cortez", "dacortez",
				"3858f62230ac3c915f300c664312c63f"));
		// senha "barfoo"
		allUsers.add(new User("Angela Pedroso Tonon", "aptonon",
				"96948aad3fcae80c08a35c9b5958cd89"));
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		new Server(port);
	}

	public void run() {
		try {
			// Instead of creating a ServerSocket, create a ServerSocketChannel.
			ServerSocketChannel ssc = ServerSocketChannel.open();

			// Set it to non-blocking, so we can use select.
			ssc.configureBlocking(false);

			// Get the Socket connected to this channel, and bind it
			// to the listening port.
			ServerSocket ss = ssc.socket();
			InetSocketAddress isa = new InetSocketAddress(port);
			ss.bind(isa);
			
			// Create a new Selector for selecting.
			Selector selector = Selector.open();

			// Register the ServerSocketChannel, so we can
			// listen for incoming connections.
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("Listening TCP on port " + port);
			
			
			DatagramChannel dc = DatagramChannel.open();
			dc.configureBlocking(false);
			DatagramSocket ds = dc.socket();
			ds.bind(isa);
			dc.register(selector, SelectionKey.OP_READ);
			System.out.println("Listening UDP on port " + port);
			
			
			while (true) {
				// See if we've had any activity -- either
				// an incoming connection, or incoming data on an
				// existing connection.
				int num = selector.select();

				// If we don't have any activity, loop around and wait again.
				if (num == 0)
					continue;

				// Get the keys corresponding to the activity that has been
				// detected, and process them one by one.
				// key is representing one of bits of I/O activity.
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey key : keys) {
					
					// What kind of activity is it?
					if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
						System.out.println("acc");

						// It's an incoming connection.
						// Register this socket with the Selector so we can
						// listen for input on it.
						Socket s = ss.accept();
						System.out.println("Got connection from " + s);

						// Make sure to make it non-blocking, so we can
						// use a selector on it.
						SocketChannel sc = s.getChannel();
						sc.configureBlocking(false);

						// Register it with the selector, for reading
						sc.register(selector, SelectionKey.OP_READ);
					} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

						
						if (key.channel() instanceof DatagramChannel) {
							DatagramChannel dtc = null;
							try {
								System.out.println("Chegou algo");
								dtc = (DatagramChannel) key.channel();
								buffer.clear();
								dtc.receive(buffer);
								buffer.flip();
								StringBuilder sb = new StringBuilder();
								for (int i = 0; i < buffer.limit(); i++)
									sb.append((char)buffer.get(i));
								System.out.println(sb);
								//key.cancel();
								
//								DatagramSocket dts = dtc.socket();
//								byte[] receiveData = new byte[1024]; 
//								DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//								dts.receive(receivePacket);
//								System.out.println(new String(receivePacket.getData()));
//								try {
//									dtc.close();
//								} catch (IOException ie) {
//									System.err.println("Error closing socket "
//											+ dtc + ": " + ie);
//								}
							} catch (IOException ie) {
								// On exception, remove this channel from the
								// selector.
								key.cancel();
								try {
									dtc.close();
								} catch (IOException ie2) {
									System.out.println(ie2);
								}
								System.out.println("Closed " + dtc);
							}
						} 
						else {
							SocketChannel sc = null;
							try {
								// It's incoming data on a connection, so process
								// it.
								sc = (SocketChannel) key.channel();
								boolean ok = processInput(sc);
	
								// If the connection is dead, then remove it from
								// the selector and close it.
								if (!ok) {
									key.cancel();
									Socket s = null;
									try {
										s = sc.socket();
										s.close();
									} catch (IOException ie) {
										System.err.println("Error closing socket "
												+ s + ": " + ie);
									}
								}
							} catch (IOException ie) {
								// On exception, remove this channel from the
								// selector.
								key.cancel();
								try {
									sc.close();
								} catch (IOException ie2) {
									System.out.println(ie2);
								}
								System.out.println("Closed " + sc);
							}
						}
					}
				}

				// We remove the selected keys, because we've dealt
				// with them.
				keys.clear();
			}
		} catch (IOException ie) {
			System.err.println(ie);
		}
	}

	// Do some cheesy encryption on the incoming data,
	// and send it back out
	private boolean processInput(SocketChannel sc) throws IOException {
		buffer.clear();
		sc.read(buffer);
		buffer.flip();

		// If no data, close the connection
		if (buffer.limit() == 0)
			return false;

		// Simple rot-13 encryption
		for (int i = 0; i < buffer.limit(); ++i) {
			byte b = buffer.get(i);
			if ((b >= 'a' && b <= 'm') || (b >= 'A' && b <= 'M'))
				b += 13;
			else if ((b >= 'n' && b <= 'z') || (b >= 'N' && b <= 'Z'))
				b -= 13;
			buffer.put(i, b);
		}
		sc.write(buffer);

		System.out.println("Processed " + buffer.limit() + " from " + sc);

		return true;
	}
}
