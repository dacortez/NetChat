package dacortez.netChat;

public enum DataType {
	
	TCP_OK("TCP/OK"), 
	LOGIN_REQUEST("LOGIN/REQUEST"), 
	LOGIN_OK("LOGIN/OK"), 
	LOGIN_FAIL("LOGIN/FAIL"),
	USERS_REQUEST("USER/REQUEST"), 
	USERS_LIST("USERS/LIST"),
	LOGOUT_REQUEST("LOGOUT/REQUEST"),
	LOGOUT_OK("LOGOUT/OK"),
	CHAT_REQUEST("CHAT/REQUEST"),
	CHAT_OK("CHAT/OK"),
	CHAT_DENIED("CHAT/DENIED"),
	CHAT_MSG("CHAT/MSG"),
	HEART_BEAT("HEART/BEAT");
	
	private final String value;
	
	private DataType(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
