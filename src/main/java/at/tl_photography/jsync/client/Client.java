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
package at.tl_photography.jsync.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.logging.Logger;

import at.tl_photography.jsync.common.FileChecker;
import at.tl_photography.jsync.common.FileInfo;

/**
 * The Class Client.
 */
public class Client {

	/** The logger. */
	private static Logger logger = Logger.getLogger(Client.class.getName());

	/** The host. */
	public static String host;

	/** The port. */
	public static Integer port;

	/** The directory. */
	public static String directory;

	/** The socket. */
	private static Socket socket;

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws NoSuchPaddingException
	 *             the no such padding exception
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException {
		try {

			// parse args
			directory = args[1];
			host = args[2];
			port = Integer.parseInt(args[3]);

			// open socket
			socket = new Socket(host, port);

			logger.info("trying to connect to " + host + ":" + port);

			// sender or receiver
			if (args[0].equals("s")) {
				sendFiles();
			} else {
				receiveFiles();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Recieve files.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
	private static void receiveFiles() throws IOException, ClassNotFoundException {

		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

		while (true) {

			FileInfo info = (FileInfo) ois.readObject();
			logger.info(info.toString());

			File file = new File(directory + info.getPath());
			if (file.exists()) {
				logger.info("file " + info + " exists");
				if (FileChecker.generateMD5(file.toPath()).equals(info.getMD5())) {
					logger.info("file " + info + " has the same MD5");
					oos.writeBoolean(true);
					oos.flush();
					continue;
				} else {
					logger.info("file " + info + " has not the same MD5, deleting");
					file.delete();
					oos.writeBoolean(false);
					oos.flush();
				}
			} else {
				logger.info("file " + info + " does not extist");
				oos.writeBoolean(false);
				oos.flush();
			}

			System.out.println(file.getParentFile());

			file.getParentFile().mkdirs();

			FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());

			byte[] buffer = new byte[1024];
			int bytesRead;

			while ((bytesRead = ois.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}
			fos.flush();
			fos.close();

		}
	}

	/**
	 * Send files.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static void sendFiles() throws IOException {

		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

		while (true) {
			Iterator<Path> iter = Files.walk(Paths.get(directory)).filter(Files::isRegularFile).iterator();

			while (iter.hasNext()) {
				Path file = (Path) iter.next();

				logger.info("found file " + file);

				String relative = new File(directory).toURI().relativize(file.toFile().toURI()).getPath();
				FileInfo info = new FileInfo(relative, FileChecker.generateMD5(file));
				oos.writeObject(info);
				oos.flush();

				if (ois.readBoolean()) {
					logger.info("file " + file + " exsist on the other side");
				} else {
					logger.info("file " + file + " does not exsist on the other side");
					FileInputStream fis = new FileInputStream(file.toFile());

					byte[] buffer = new byte[1024];
					int bytesRead;

					while ((bytesRead = fis.read(buffer)) != -1) {
						oos.write(buffer, 0, bytesRead);
					}
					oos.flush();
					fis.close();
				}
			}

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
