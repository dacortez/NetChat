package dacortez.netChat;

import java.util.ArrayList;
import java.util.List;

public class Server {
	private static List<User> allUsers;
	
	public Server() {
		allUsers = new ArrayList<User>();
		// senha "foobar"
		allUsers.add(new User("Daniel Augusto Cortez", "dacortez", "3858f62230ac3c915f300c664312c63f"));
		// senha "barfoo"
		allUsers.add(new User("Angela Pedroso Tonon",  "aptonon",  "96948aad3fcae80c08a35c9b5958cd89"));
	}
}
