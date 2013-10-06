package dacortez.netChat;

public class Main {

	public static void main(String[] args) {
		System.out.println(Security.getHash("foobar"));
		System.out.println(Security.getHash("barfoo"));
	}
}
