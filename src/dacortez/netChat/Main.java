/**
 * 
 */
package dacortez.netChat;

/**
 * @author dacortez
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(Security.getHash("foobar"));
		System.out.println(Security.getHash("barfoo"));
	}

}
