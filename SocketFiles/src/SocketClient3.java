import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class SocketClient3 {
	private Socket server;
	private Thread inputThread;
	private Thread fromServerThread;

	public void connect(String address, int port) {
		try {
			server = new Socket(address, port);
			System.out.println("Client connected");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void listenForKeyboard(Scanner si, ObjectOutputStream out) {
		// Thread to listen for keyboard input so main thread isn't blocked
		inputThread = new Thread() {
			@Override
			public void run() {
				try {

					while (!server.isClosed()) {
						System.out.println("Waiting for input");
						String line = si.nextLine();// this line causes a problem due to blocking IO when the server
													// terminates
						if (!"quit".equalsIgnoreCase(line) && line != null) {
							// grab line and write it to the stream
							out.writeObject(line);// technically a String is an object so this works
						} else {
							System.out.println("Stopping input thread");
							// we're quitting so tell server we disconnected so it can broadcast
							out.writeObject("bye");
							break;
						}
						try {
							sleep(50);
						} catch (Exception e) {
							System.out.println("Problem sleeping thread");
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					close();
					System.out.println("Stopped listening to console input");
				}
			}
		};
		inputThread.start();// start the thread
	}

	private void listenForServerMessage(ObjectInputStream in) {
		// Thread to listen for responses from server so it doesn't block main thread
		fromServerThread = new Thread() {
			@Override
			public void run() {
				try {
					String fromServer;
					// while we're connected, listen for strings from server
					while (!server.isClosed() && (fromServer = (String) in.readObject()) != null) {
						// keep this one as sysout otherwise if we turn of System.out.println we'll not
						// see
						// messages
						System.out.println(fromServer);
					}
				} catch (Exception e) {
					if (!server.isClosed()) {
						e.printStackTrace();
						System.out.println("Server closed connection");
					} else {
						System.out.println("Connection closed");
					}
				} finally {
					close();
					System.out.println("Stopped listening to server input");
				}
			}
		};
		fromServerThread.start();// start the thread
	}

	public void start() throws IOException {
		if (server == null) {
			return;
		}
		System.out.println("Client Started");
		// listen to console, server in, and write to server out
		try (Scanner si = new Scanner(System.in);
				ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(server.getInputStream());) {

			// starts new thread
			listenForKeyboard(si, out);

			// starts new thread
			listenForServerMessage(in);

			// Keep main thread alive until the socket is closed
			// initialize/do everything before this line
			// (Without this line the program would stop after the first message
			while (!server.isClosed()) {
				Thread.sleep(50);
			}
			System.out.println("Exited loop");
			System.out.println("Press enter to stop the program");
			// alternatively in this case we could nuke the program with
			// System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}

	private void close() {
		if (server != null && !server.isClosed()) {
			try {
				server.close();
				System.out.println("Closed socket");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		SocketClient3 client = new SocketClient3();
		int port = -1;
		try {
			// not safe but try-catch will get it
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.out.println("Invalid port");
		}
		if (port == -1) {
			return;
		}
		client.connect("127.0.0.1", port);
		try {
			client.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
