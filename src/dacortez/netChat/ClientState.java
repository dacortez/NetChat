/**
 * MAC0448 - Programação para Redes - EP2
 * Daniel Augusto Cortez - 2960291
 * 
 */

package dacortez.netChat;

/**
 * Representa o estado do usuário durante as diversas fases em
 * que ele está interagindo com o sistema de bate-papo.
 * 
 * @author dacortez (dacortez79@gmail.com)
 * @version 2013.10.12
 */
public enum ClientState {
	SCANNING_MENU, TYPING_USER, CHATTING, TYPING_FILE, TRANSFERING_FILE;
}
