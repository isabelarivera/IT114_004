import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread5 extends Thread {
	private Socket client;
	private ObjectInputStream in;// from client
	private ObjectOutputStream out;// to client
	private boolean isRunning = false;
	private Room currentRoom;// what room we are in, should be lobby by default
	private String clientName;

	public String getClientName() {
		return clientName;
	}

	protected synchronized Room getCurrentRoom() {
		return currentRoom;
	}

	protected synchronized void setCurrentRoom(Room room) {
		if (room != null) {
			currentRoom = room;
		} else {
			System.out.println("Passed in room was null, this shouldn't happen");
		}
	}

	public ServerThread5(Socket myClient, Room room) throws IOException {
		this.client = myClient;
		this.currentRoom = room;
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
	}

	/***
	 * Sends the message to the client represented by this ServerThread5
	 * 
	 * @param message
	 * @return
	 */
	@Deprecated
	protected boolean send(String message) {
		// added a boolean so we can see if the send was successful
		try {
			out.writeObject(message);
			return true;
		} catch (IOException e) {
			System.out.println("Error sending message to client (most likely disconnected)");
			e.printStackTrace();
			cleanup();
			return false;
		}
	}

	/***
	 * Replacement for send(message) that takes the client name and message and
	 * converts it into a payload
	 * 
	 * @param clientName
	 * @param message
	 * @return
	 */
	protected boolean send(String clientName, String message) {
		Payload payload = new Payload();
		payload.setPayloadType(PayloadType.MESSAGE);
		payload.setClientName(clientName);
		payload.setMessage(message);

		return sendPayload(payload);
	}

	protected boolean sendConnectionStatus(String clientName, boolean isConnect) {
		Payload payload = new Payload();
		if (isConnect) {
			payload.setPayloadType(PayloadType.CONNECT);
		} else {
			payload.setPayloadType(PayloadType.DISCONNECT);
		}
		payload.setClientName(clientName);
		return sendPayload(payload);
	}

	private boolean sendPayload(Payload p) {
		try {
			out.writeObject(p);
			return true;
		} catch (IOException e) {
			System.out.println("Error sending message to client (most likely disconnected)");
			e.printStackTrace();
			cleanup();
			return false;
		}
	}

	/***
	 * Process payloads we receive from our client
	 * 
	 * @param p
	 */
	private void processPayload(Payload p) {
		switch (p.getPayloadType()) {
		case CONNECT:
			// here we'll fetch a clientName from our client
			String n = p.getClientName();
			if (n != null) {
				clientName = n;
				System.out.println("Set our name to " + clientName);
				if (currentRoom != null) {
					currentRoom.joinLobby(this);
				}
			}
			break;
		case DISCONNECT:
			isRunning = false;// this will break the while loop in run() and clean everything up
			break;
		case MESSAGE:
			currentRoom.sendMessage(this, p.getMessage());
			break;
		default:
			System.out.println("Unhandled payload on server: " + p);
			break;
		}
	}

	@Override
	public void run() {
		try {
			isRunning = true;
			Payload fromClient;
			while (isRunning && // flag to let us easily control the loop
					!client.isClosed() // breaks the loop if our connection closes
					&& (fromClient = (Payload) in.readObject()) != null // reads an object from inputStream (null would
			// likely mean a disconnect)
			) {
				System.out.println("Received from client: " + fromClient);
				processPayload(fromClient);
			} // close while loop
		} catch (Exception e) {
			// happens when client disconnects
			e.printStackTrace();
			System.out.println("Client Disconnected");
		} finally {
			isRunning = false;
			System.out.println("Cleaning up connection for ServerThread");
			cleanup();
		}
	}

	private void cleanup() {
		if (currentRoom != null) {
			System.out.println(getName() + " removing self from room " + currentRoom.getName());
			currentRoom.removeClient(this);
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				System.out.println("Input already closed");
			}
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				System.out.println("Client already closed");
			}
		}
		if (client != null && !client.isClosed()) {
			try {
				client.shutdownInput();
			} catch (IOException e) {
				System.out.println("Socket/Input already closed");
			}
			try {
				client.shutdownOutput();
			} catch (IOException e) {
				System.out.println("Socket/Output already closed");
			}
			try {
				client.close();
			} catch (IOException e) {
				System.out.println("Client already closed");
			}
		}
	}
}