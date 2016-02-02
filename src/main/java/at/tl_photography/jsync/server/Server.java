/*
 *	jSync 
 *
 * @author Thomas Leber
 * @version 0.1
 * 
 *
 *                                  Apache License
 *                          Version 2.0, January 2004
 *                      http://www.apache.org/licenses/
 */
package at.tl_photography.jsync.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * The Class Server.
 */
public class Server {

	/** The logger. */
	private static Logger logger = Logger.getLogger(Server.class.getName());

	/** The runner1. */
	private static Runner runner1;

	/** The runner2. */
	private static Runner runner2;

	/** The port. */
	private static Integer port = 81;

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws InterruptedException
	 *             the interrupted exception
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidKeyException
	 */
	public static void main(String[] args) throws Exception {

		port = Integer.parseInt(args[0]);

		@SuppressWarnings("resource")
		ServerSocket serversocket = new ServerSocket(port);
		logger.info("server socket open, waiting for clients");

		while (true) {
			try {

				Socket s1 = serversocket.accept();
				logger.info("client 1 is here");
				Socket s2 = serversocket.accept();
				logger.info("client 2 is here");

				runner1 = new Runner(s1.getInputStream(), s2.getOutputStream());
				runner2 = new Runner(s2.getInputStream(), s1.getOutputStream());

				new Thread(runner1).start();
				new Thread(runner2).start();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			while (true) {

				if (runner1.isConnected() && runner2.isConnected()) {
					Thread.sleep(1000);
				} else {
					logger.info("reset");
					runner1.disconnect();
					runner2.disconnect();
					break;
				}
			}
		}
	}
}
