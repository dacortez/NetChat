package dacortez.netChat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Security {
	
	public static String getHash(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(password.getBytes());
			byte byteData[] = md.digest();
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < byteData.length; i++)
				hexString.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
}
