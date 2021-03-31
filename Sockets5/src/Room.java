import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Room implements AutoCloseable {
	private static SocketServer5 server;// used to refer to accessible server functions
	private String name;

	// Commands
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";

	public Room(String name) {
		this.name = name;
	}

	public static void setServer(SocketServer5 server) {
		Room.server = server;
	}

	public String getName() {
		return name;
	}

	private List<ServerThread5> clients = new ArrayList<ServerThread5>();

	protected synchronized void addClient(ServerThread5 client) {
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			System.out.println("Attempting to add a client that already exists");
		} else {
			clients.add(client);
			if (client.getClientName() != null) {
				sendMessage(client, "joined the room " + getName());
			}
		}
	}

	protected synchronized void removeClient(ServerThread5 client) {
		clients.remove(client);
		if (clients.size() > 0) {
			sendMessage(client, "left the room");
		} else {
			cleanupEmptyRoom();
		}
	}

	private void cleanupEmptyRoom() {
		// If name is null it's already been closed. And don't close the Lobby
		if (name == null || name.equalsIgnoreCase(SocketServer5.LOBBY)) {
			return;
		}
		try {
			System.out.println("Closing empty room: " + name);
			close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void joinRoom(String room, ServerThread5 client) {
		server.joinRoom(room, client);
	}

	protected void joinLobby(ServerThread5 client) {
		server.joinLobby(client);
	}

	/***
	 * Helper function to process messages to trigger different functionality.
	 * 
	 * @param message The original message being sent
	 * @param client  The sender of the message (since they'll be the ones
	 *                triggering the actions)
	 */
	private boolean processCommands(String message, ServerThread5 client) {
		boolean wasCommand = false;
		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
				String[] comm = message.split(COMMAND_TRIGGER);
				System.out.println(message);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				if (command != null) {
					command = command.toLowerCase();
				}
				String roomName;
				switch (command) {
				case CREATE_ROOM:
					roomName = comm2[1];
					if (server.createNewRoom(roomName)) {
						joinRoom(roomName, client);
					}
					wasCommand = true;
					break;
				case JOIN_ROOM:
					roomName = comm2[1];
					joinRoom(roomName, client);
					wasCommand = true;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wasCommand;
	}

	protected void sendConnectionStatus(String clientName, boolean isConnect) {
		Iterator<ServerThread5> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread5 client = iter.next();
			boolean messageSent = client.sendConnectionStatus(clientName, isConnect);
			if (!messageSent) {
				iter.remove();
				System.out.println("Removed client " + client.getId());
			}
		}
	}

	/***
	 * Takes a sender and a message and broadcasts the message to all clients in
	 * this room. Client is mostly passed for command purposes but we can also use
	 * it to extract other client info.
	 * 
	 * @param sender  The client sending the message
	 * @param message The message to broadcast inside the room
	 */
	protected void sendMessage(ServerThread5 sender, String message) {
		System.out.println(getName() + ": Sending message to " + clients.size() + " clients");
		if (processCommands(message, sender)) {
			// it was a command, don't broadcast
			return;
		}
		Iterator<ServerThread5> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread5 client = iter.next();
			boolean messageSent = client.send(sender.getClientName(), message);
			if (!messageSent) {
				iter.remove();
				System.out.println("Removed client " + client.getId());
			}
		}
	}

	/***
	 * Will attempt to migrate any remaining clients to the Lobby room. Will then
	 * set references to null and should be eligible for garbage collection
	 */
	@Override
	public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
			System.out.println("Migrating " + clients.size() + " to Lobby");
			Iterator<ServerThread5> iter = clients.iterator();
			Room lobby = server.getLobby();
			while (iter.hasNext()) {
				ServerThread5 client = iter.next();
				lobby.addClient(client);
				iter.remove();
			}
			System.out.println("Done Migrating " + clientCount + " to Lobby");
		}
		server.cleanupRoom(this);
		name = null;
		// should be eligible for garbage collection now
	}
}