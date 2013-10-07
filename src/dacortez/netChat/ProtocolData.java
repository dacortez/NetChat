package dacortez.netChat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProtocolData {
	private DataType type;
	private List<String> header;
	private byte[] data;
	
	public DataType getType() {
		return type;
	}
	
	public ProtocolData(DataType type) {
		header = new ArrayList<String>();
		this.type = type;
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
		type = selectDataType(new String(sb));
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
		type = selectDataType(new String(sb));
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
		int i = appendLine(type.toString(), 0, array);
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
		int i = appendLine(type.toString(), 0, buffer);
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
		int size = type.toString().length() + 2;
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
	
	private DataType selectDataType(String value) {
		if (value.contentEquals(DataType.TCP_OK.toString()))
			return DataType.TCP_OK;
		if (value.contentEquals(DataType.LOGIN_REQUEST.toString()))
			return DataType.LOGIN_REQUEST;
		if (value.contentEquals(DataType.LOGIN_OK.toString()))
			return DataType.LOGIN_OK;
		if (value.contentEquals(DataType.LOGIN_FAIL.toString()))
			return DataType.LOGIN_FAIL;
		if (value.contentEquals(DataType.USERS_REQUEST.toString()))
			return DataType.USERS_REQUEST;
		if (value.contentEquals(DataType.USERS_LIST.toString()))
			return DataType.USERS_LIST;
		if (value.contentEquals(DataType.LOGOUT_REQUEST.toString()))
			return DataType.LOGOUT_REQUEST;
		if (value.contentEquals(DataType.LOGOUT_OK.toString()))
			return DataType.LOGOUT_OK;
		if (value.contentEquals(DataType.CHAT_REQUEST.toString()))
			return DataType.CHAT_REQUEST;
		if (value.contentEquals(DataType.CHAT_OK.toString()))
			return DataType.CHAT_OK;
		if (value.contentEquals(DataType.CHAT_DENIED.toString()))
			return DataType.CHAT_DENIED;
		if (value.contentEquals(DataType.CHAT_MSG.toString()))
			return DataType.CHAT_MSG;
		if (value.contentEquals(DataType.CHAT_END.toString()))
			return DataType.CHAT_END;
		if (value.contentEquals(DataType.CHAT_FINISHED.toString()))
			return DataType.CHAT_FINISHED;
		if (value.contentEquals(DataType.HEART_BEAT.toString()))
			return DataType.HEART_BEAT;
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); 
		sb.append(type.toString()).append("\r\n");
		for (String line: header)
			sb.append(line).append("\r\n");
		sb.append("\r\n");
		sb.append(data);
		return sb.toString();
	}
}
