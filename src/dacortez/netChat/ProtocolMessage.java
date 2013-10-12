/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

/**
 * Representa o tipo de mensagem utilizada no protocolo de 
 * comunicação entre servido e cliente, ou entre cliente e 
 * cliente (p2p).
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public enum ProtocolMessage {
	
	CONNECTION_OK("EP2P/CONNECTION_OK"), 
	LOGIN_REQUEST("EP2P/LOGIN_REQUEST"), 
	LOGIN_OK("EP2P/LOGIN_OK"), 
	LOGIN_FAIL("EP2P/LOGIN_FAIL"),
	USERS_REQUEST("EP2P/USER_REQUEST"), 
	USERS_LIST("EP2P/USERS_LIST"),
	LOGOUT_REQUEST("EP2P/LOGOUT_REQUEST"),
	LOGOUT_OK("EP2P/LOGOUT_OK"),
	CHAT_REQUEST("EP2P/CHAT_REQUEST"),
	CHAT_OK("EP2P/CHAT_OK"),
	CHAT_DENIED("EP2P/CHAT_DENIED"),
	CHAT_MSG("EP2P/CHAT_MSG"),
	CHAT_END("EP2P/CHAT_END"),
	CHAT_FINISHED("EP2P/CHAT_FINISHED"),
	TRANSFER_REQUEST("EP2P/TRANSFER_REQUEST"),
	TRANSFER_OK("EP2P/TRANSFER_OK"),
	TRANSFER_DENIED("EP2P/TRANSFER_DENIED"),
	TRANSFER_START("EP2P/TRANSFER/START"),
	DATA_SAVED("EP2P/DATA_SAVED"),
	SEND_AGAIN("EP2P/SEND_AGAIN"),
	HEART_BEAT("EP2P/HEART_BEAT");
	
	// String associada ao tipo de mensagem.
	private final String value;
	
	private ProtocolMessage(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
