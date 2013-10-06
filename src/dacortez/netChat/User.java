package dacortez.netChat;

public class User {
	private String name;
	private String userName;
	private String passwordHash;
	private String hostName = null;
	
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
	
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
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
		return userName + " [" + hostName + "]";
	}
}
