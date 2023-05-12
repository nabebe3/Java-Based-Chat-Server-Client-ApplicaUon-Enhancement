import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class Client extends Thread {

	Socket socketClient;

	ObjectOutputStream out;
	ObjectInputStream in;

	private Consumer<Serializable> callback;

	Client(Consumer<Serializable> call) {
		callback = call;
	}

	public void run() {
		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		while (true) {
			try {
				String messageType = in.readObject().toString();

				if (messageType.equals("UPDATE_CLIENT_LIST")) {
					List<String> clientNames = (List<String>) in.readObject();
					callback.accept("Client list: " + clientNames);
				} else if (messageType.equals("message")) {
					String message = in.readObject().toString();
					callback.accept(message);
				} else {
					callback.accept(messageType);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void send(String target, String[] recipients, String data) {
		try {
			out.writeObject(target);
			out.writeObject(recipients);
			out.writeObject(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

