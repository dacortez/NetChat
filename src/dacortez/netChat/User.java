package dacortez.netChat;

public class User {
	private String name;
	private String userName;
	private String passwordHash;
	private String host = null;
	private ConnectionType type;
	private boolean locked;
	private Integer pierPort;
	
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
	
	public ConnectionType getType() {
		return type;
	}

	public void setType(ConnectionType type) {
		this.type = type;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public Integer getPierPort() {
		return pierPort;
	}

	public void setPierPort(int pierPort) {
		this.pierPort = pierPort;
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
		sb.append(host).append(": ").append(type).append("/").append(pierPort);
		sb.append(" [").append(locked).append("]");
		return sb.toString();
	}
}
