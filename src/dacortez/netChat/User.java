/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

/**
 * Modela um usuário para o programa de bate-papo, conténdo
 * informações sobre nome, username, senha, tipo de conexão,
 * porta para conexão p2p e status de travamento (usuário
 * batendo papo com outro usuario).
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class User {
	// Nome do usuário.
	private String name;
	// Username para efetuar login.
	private String userName;
	// Hash da senha do usuário.
	private String passwordHash;
	// Host do usuário para conexão.
	private String host = null;
	// Porta de conexão do usuário.
	private Integer pierPort;
	// Tipo de conexão que o usuário está utilzando.
	private ConnectionType type;
	// Status de travamento (usuário batendo papo com outro).
	private boolean locked;
	
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
	
	public Integer getPierPort() {
		return pierPort;
	}

	public void setPierPort(int pierPort) {
		this.pierPort = pierPort;
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
