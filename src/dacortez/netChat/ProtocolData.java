package dacortez.netChat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProtocolData {
	private String title;
	private List<String> header;
	private byte[] data;
	
	public ProtocolData(String title) {
		header = new ArrayList<String>();
		this.title = title;
	}
	
	public ProtocolData(ByteBuffer buffer) {
		header = new ArrayList<String>();
		int i = setTitle(buffer);
		i = setHeader(buffer, i);
		setData(buffer, i); 
	}

	private int setTitle(ByteBuffer buffer) {
		StringBuilder sb = new StringBuilder();	
		int i = 0;
		while ((char) buffer.get(i) != '\r' && (char) buffer.get(i + 1) != '\n')
			sb.append((char) buffer.get(i++));
		title = new String(sb);
		return i + 2;
	}

	private int setHeader(ByteBuffer buffer, int i) {
		while (true) {
			StringBuilder sb = new StringBuilder();
			while ((char) buffer.get(i) != '\r' && (char) buffer.get(i + 1) != '\n')
				sb.append((char) buffer.get(i++));
			i += 2;
			if (sb.length() > 0)
				header.add(new String(sb));
			else
				return i;
		}
	}
	
	private void setData(ByteBuffer buffer, int i) {
		int j = 0;
		data = new byte[buffer.limit() - i];
		while (i < buffer.limit())
			data[j++] = buffer.get(i++);
	}
	
	public ProtocolData(byte[] array, int length) {
		header = new ArrayList<String>();
		int i = setTitle(array);
		i = setHeader(array, i);
		setData(array, i, length);
	}
	
	private int setTitle(byte[] array) {
		StringBuilder sb = new StringBuilder();	
		int i = 0;
		while ((char) array[i] != '\r' && (char) array[i + 1] != '\n')
			sb.append((char) array[i++]);
		title = new String(sb);
		return i + 2;
	}
	
	private int setHeader(byte[] array, int i) {
		while (true) {
			StringBuilder sb = new StringBuilder();
			while ((char) array[i] != '\r' && (char) array[i + 1] != '\n')
				sb.append((char) array[i++]);
			i += 2;
			if (sb.length() > 0)
				header.add(new String(sb));
			else 
				return i;
		}
	}
	
	private void setData(byte[] array, int i, int length) {
		int j = 0;
		data = new byte[length - i];
		while (i < length)
			data[j++] = array[i++];
	}
	
	public byte[] toByteArray() {
		byte[] array = new byte[getSize()];
		int i = appendLine(title, 0, array);
		for (String line: header)
			i = appendLine(line, i, array);
		array[i++] = '\r'; array[i++] = '\n';
		if (data != null)
			for (int j = 0; j < data.length; j++)
				array[i++] = data[j];
		return array;
	}

	private int appendLine(String line, int i, byte[] array) {
		for (int j = 0; j < line.length(); j++)
			array[i++] = (byte) line.charAt(j);
		array[i++] = '\r'; array[i++] = '\n';
		return i;
	}
	
	public ByteBuffer toByteBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(getSize());
		int i = appendLine(title, 0, buffer);
		for (String line: header)
			i = appendLine(line, i, buffer);
		buffer.put(i++, (byte) '\r'); buffer.put(i++, (byte) '\n');
		if (data != null)
			for (int j = 0; j < data.length; j++)
				buffer.put(i++, data[j]);
		return buffer;
	}
	
	private int appendLine(String line, int i, ByteBuffer buffer) {
		for (int j = 0; j < line.length(); j++)
			buffer.put(i++, (byte) line.charAt(j));
		buffer.put(i++, (byte) '\r'); 
		buffer.put(i++, (byte) '\n');
		return i;
	}
	
	public int getSize() {
		int size = title.length() + 2;
		for (String line: header)
			size += line.length() + 2;
		size += 2;
		if (data != null)
			size += data.length;
		return size;
	}
	
	public int getNumberOfHeaderLines() {
		return header.size();
	}
	
	public void addToHeader(String line) {
		header.add(line);
	}
	
	public String getHeaderLine(int index) {
		return header.get(index);
	}
	
	public boolean isTCPOK() {
		return title.contentEquals(Protocol.TCP_OK);
	}
	
	public boolean isLoginInfo() {
		return title.contentEquals(Protocol.LOGIN_INFO);
	}
	
	public boolean isLoginOK() {
		return title.contentEquals(Protocol.LOGIN_OK);
	}
	
	public boolean isLoginFail() {
		return title.contentEquals(Protocol.LOGIN_FAIL);
	}
	
	public boolean isUsersRequest() {
		return title.contentEquals(Protocol.USERS_REQUEST);
	}
	
	public boolean isUsersList() {
		return title.contentEquals(Protocol.USERS_LIST);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); 
		sb.append(title).append("\r\n");
		for (String line: header)
			sb.append(line).append("\r\n");
		sb.append("\r\n");
		sb.append(data);
		return sb.toString();
	}
}
