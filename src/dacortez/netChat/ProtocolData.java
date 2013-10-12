package dacortez.netChat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProtocolData {
	private ProtocolMessage message;
	private List<String> header;
	private byte[] data;
	
	public ProtocolMessage getMessage() {
		return message;
	}
	
	public byte[] getData() {
		return data;
	}
		
	public ProtocolData(ProtocolMessage type) {
		header = new ArrayList<String>();
		this.message = type;
	}
	
	public ProtocolData(ByteBuffer buffer) {
		header = new ArrayList<String>();
		int i = setType(buffer);
		i = setHeader(buffer, i);
		setData(buffer, i);
	}

	private int setType(ByteBuffer buffer) {
		StringBuilder sb = new StringBuilder();	
		int i = 0;
		while ((char) buffer.get(i) != '\r' && (char) buffer.get(i + 1) != '\n')
			sb.append((char) buffer.get(i++));
		message = selectMessage(new String(sb));
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
		if (buffer.limit() > i) {
			data = new byte[buffer.limit() - i];
			while (i < buffer.limit())
				data[j++] = buffer.get(i++);
		}
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
		message = selectMessage(new String(sb));
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
		if (length > i) {
			data = new byte[length - i];
			while (i < length)
				data[j++] = array[i++];
		}
	}
	
	public byte[] toByteArray() {
		byte[] array = new byte[getSize()];
		int i = appendLine(message.toString(), 0, array);
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
		int i = appendLine(message.toString(), 0, buffer);
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
		int size = message.toString().length() + 2;
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
	
	public void copyData(byte[] buffer, int len) {
		data = new byte[len];
		for (int i = 0; i < len; i++)
			data[i] = buffer[i];
	}

	public void appendData(byte[] buffer) {
		byte[] newData = new byte[data.length + buffer.length];
		int j = 0;
		for (int i = 0; i < data.length; i++)
			newData[j++] = data[i];
		for (int i = 0; i < buffer.length; i++)
			newData[j++] = buffer[i];
		data = newData;
	}
	
	private ProtocolMessage selectMessage(String value) {
		if (value.contentEquals(ProtocolMessage.CONNECTION_OK.toString()))
			return ProtocolMessage.CONNECTION_OK;
		if (value.contentEquals(ProtocolMessage.LOGIN_REQUEST.toString()))
			return ProtocolMessage.LOGIN_REQUEST;
		if (value.contentEquals(ProtocolMessage.LOGIN_OK.toString()))
			return ProtocolMessage.LOGIN_OK;
		if (value.contentEquals(ProtocolMessage.LOGIN_FAIL.toString()))
			return ProtocolMessage.LOGIN_FAIL;
		if (value.contentEquals(ProtocolMessage.USERS_REQUEST.toString()))
			return ProtocolMessage.USERS_REQUEST;
		if (value.contentEquals(ProtocolMessage.USERS_LIST.toString()))
			return ProtocolMessage.USERS_LIST;
		if (value.contentEquals(ProtocolMessage.LOGOUT_REQUEST.toString()))
			return ProtocolMessage.LOGOUT_REQUEST;
		if (value.contentEquals(ProtocolMessage.LOGOUT_OK.toString()))
			return ProtocolMessage.LOGOUT_OK;
		if (value.contentEquals(ProtocolMessage.CHAT_REQUEST.toString()))
			return ProtocolMessage.CHAT_REQUEST;
		if (value.contentEquals(ProtocolMessage.CHAT_OK.toString()))
			return ProtocolMessage.CHAT_OK;
		if (value.contentEquals(ProtocolMessage.CHAT_DENIED.toString()))
			return ProtocolMessage.CHAT_DENIED;
		if (value.contentEquals(ProtocolMessage.CHAT_MSG.toString()))
			return ProtocolMessage.CHAT_MSG;
		if (value.contentEquals(ProtocolMessage.CHAT_END.toString()))
			return ProtocolMessage.CHAT_END;
		if (value.contentEquals(ProtocolMessage.CHAT_FINISHED.toString()))
			return ProtocolMessage.CHAT_FINISHED;
		if (value.contentEquals(ProtocolMessage.TRANSFER_REQUEST.toString()))
			return ProtocolMessage.TRANSFER_REQUEST;
		if (value.contentEquals(ProtocolMessage.TRANSFER_OK.toString()))
			return ProtocolMessage.TRANSFER_OK;
		if (value.contentEquals(ProtocolMessage.TRANSFER_DENIED.toString()))
			return ProtocolMessage.TRANSFER_DENIED;
		if (value.contentEquals(ProtocolMessage.TRANSFER_START.toString()))
			return ProtocolMessage.TRANSFER_START;
		if (value.contentEquals(ProtocolMessage.FILE_DATA.toString()))
			return ProtocolMessage.FILE_DATA;
		if (value.contentEquals(ProtocolMessage.DATA_SAVED.toString()))
			return ProtocolMessage.DATA_SAVED;
		if (value.contentEquals(ProtocolMessage.SEND_AGAIN.toString()))
			return ProtocolMessage.SEND_AGAIN;
		if (value.contentEquals(ProtocolMessage.HEART_BEAT.toString()))
			return ProtocolMessage.HEART_BEAT;
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); 
		sb.append(message.toString()).append("\r\n");
		for (String line: header)
			sb.append(line).append("\r\n");
		sb.append("\r\n");
		if (message == ProtocolMessage.FILE_DATA)
			sb.append(dataToString());
		return sb.toString();
	}
	
	private String dataToString() {
		StringBuilder sb = new StringBuilder();
		int length = Integer.parseInt(header.get(0));
		for (int i = 0; i < length; i++)
			sb.append((char) data[i]);
		return sb.toString();
	}
}
