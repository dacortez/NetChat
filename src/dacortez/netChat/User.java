package dacortez.netChat;

import java.nio.channels.Channel;

public class User {
	private String name;
	private String userName;
	private String passwordHash;
	private String host = null;
	private int port;
	private ConnectionType type;
	private Channel channel;
	private boolean locked;
	private Integer clientPort;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public String getPasswordHash() {
		return passwordHash;
	}
	
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public ConnectionType getType() {
		return type;
	}

	public void setType(ConnectionType type) {
		this.type = type;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public Integer getClientPort() {
		return clientPort;
	}

	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	public User(String name, String userName, String passwordHash) {
		this.name = name;
		this.userName = userName;
		this.passwordHash = passwordHash;
	}
	
	public boolean hasUserName(String userName) {
		return userName.toLowerCase().contentEquals(this.userName);
	}
	
	public boolean authenticate(String password) {
		return Security.getHash(password).contentEquals(passwordHash);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append(" (").append(userName).append(") @ ");
		sb.append(host).append(": ").append(type).append("/").append(clientPort);
		sb.append(" [").append(locked).append("]");
		return sb.toString();
	}
}
