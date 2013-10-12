/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Módulo de segurança utilizado na criptografia das senhas 
 * dos usuários.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public class Security {

	/**
	 * Obtém o valor do hash associado à senha informada utilizando
	 * o algoritmo MD5 de criptografia.
	 * @param password Senha do usuário.
	 * @return Valor do hash associado à senha.
	 */
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
