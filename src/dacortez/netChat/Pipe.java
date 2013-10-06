package dacortez.netChat;

import java.io.IOException;

public interface Pipe {
	public void send(ProtocolData data) throws IOException;
	public ProtocolData receive() throws IOException;
	public void close() throws IOException;
}
