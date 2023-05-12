import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Server {

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	List<String> clientNames = new ArrayList<>();
	TheServer server;
	private Consumer<Serializable> callback;

	Server(Consumer<Serializable> call) {
		callback = call;
		server = new TheServer();
		server.start();
	}

	public void sendMessage(String message, List<String> targetClients) {
		for (ClientThread client : clients) {
			if (targetClients.contains(client.clientName)) {
				try {
					client.out.writeObject(message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public class TheServer extends Thread {

		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555);) {
				System.out.println("Server is waiting for a client!");

				while (true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("client has connected to server: " + "client #" + count);
					clients.add(c);
					clientNames.add(c.clientName);
					c.updateClientList();
					c.start();

					count++;
				}
			} catch (Exception e) {
				callback.accept("Server socket did not launch");
			}
		}
	}

	class ClientThread extends Thread {

		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;
		String clientName;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
			this.clientName = "Client #" + count;
		}

		public void updateClientList() {
			List<String> currentClientNames = new ArrayList<>(clientNames);
			for (ClientThread client : clients) {
				try {
					client.out.writeObject("UPDATE_CLIENT_LIST");
					client.out.writeObject(currentClientNames);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public void run() {

			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open");
			}

			updateClientList();

			while (true) {
				try {
					String target = in.readObject().toString();
					String[] data = (String[]) in.readObject();
					String message = in.readObject().toString();

					if (target.equals("All")) {
						for (ClientThread client : clients) {
							client.out.writeObject("message");
							client.out.writeObject(clientName + ": " + message);
						}
						callback.accept(clientName + " (ALL): " + message);
					} else if (target.equals("Individual") || target.equals("Group")) {
						for (String recipient : data) {
							for (ClientThread client : clients) {
								if (client.clientName.equals(recipient)) {
									client.out.writeObject("message");
									client.out.writeObject(clientName + ": " + message);
								}
							}
						}
						callback.accept(clientName + " (" + target.toUpperCase() + "): " + message);
					}
				} catch (Exception e) {
					callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					clients.remove(this);
					clientNames.remove(clientName);
					updateClientList();
					break;
				}
			}
		}
	}

}
