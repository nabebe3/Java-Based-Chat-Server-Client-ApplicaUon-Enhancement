import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.ChoiceBox;
import javafx.collections.FXCollections;



public class GuiServer extends Application {

	TextField s1, s2, s3, s4, c1;
	Button serverChoice, clientChoice, b1;
	HashMap<String, Scene> sceneMap;
	GridPane grid;
	HBox buttonBox;
	VBox clientBox;
	Scene startScene;
	BorderPane startPane;
	Server serverConnection;
	Client clientConnection;

	ListView<String> listItems, listItems2, clientList, selectedClients;

	ComboBox<String> targetClients;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("The Networked Client/Server GUI Example");

		this.serverChoice = new Button("Server");
		this.serverChoice.setStyle("-fx-pref-width: 300px");
		this.serverChoice.setStyle("-fx-pref-height: 300px");

		this.serverChoice.setOnAction(e -> {
			primaryStage.setScene(sceneMap.get("server"));
			primaryStage.setTitle("This is the Server");
			serverConnection = new Server(data -> {
				Platform.runLater(() -> {
					listItems.getItems().add(data.toString());
				});

			});

		});

		this.clientChoice = new Button("Client");
		this.clientChoice.setStyle("-fx-pref-width: 300px");
		this.clientChoice.setStyle("-fx-pref-height: 300px");

		this.clientChoice.setOnAction(e -> {
			primaryStage.setScene(sceneMap.get("client"));
			primaryStage.setTitle("This is a client");
			clientConnection = new Client(data -> {
				Platform.runLater(() -> {
					if (data.toString().startsWith("Client list:")) {
						ObservableList<String> items = clientList.getItems();
						items.clear();
						items.addAll(data.toString().substring(12).replace("[", "").replace("]", "").split(", "));
					} else {
						listItems2.getItems().add(data.toString());
					}
				});
			});

			clientConnection.start();
		});

		this.buttonBox = new HBox(400, serverChoice, clientChoice);
		startPane = new BorderPane();
		startPane.setPadding(new Insets(70));
		startPane.setCenter(buttonBox);

		startScene = new Scene(startPane, 800, 800);

		listItems = new ListView<String>();
		listItems2 = new ListView<String>();
		clientList = new ListView<String>();
		selectedClients = new ListView<String>();
		selectedClients.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		c1 = new TextField();
		targetClients = new ComboBox<>();
		targetClients.getItems().addAll("All", "Individual", "Group");
		targetClients.setValue("All");

		targetClients.setOnAction(e -> {
			if (targetClients.getValue().equals("Individual") || targetClients.getValue().equals("Group")) {
				selectedClients.setVisible(true);
			} else {
				selectedClients.setVisible(false);
			}
		});

		b1 = new Button("Send");
		b1.setOnAction(e -> {
			String target = targetClients.getValue();
			ObservableList<String> selected = selectedClients.getSelectionModel().getSelectedItems();
			String[] selectedArray = selected.toArray(new String[0]);

			if (target.equals("All")) {
				clientConnection.send("ALL", null, c1.getText());
			} else if (target.equals("Individual") || target.equals("Group")) {
				clientConnection.send(target, selectedArray, c1.getText());
			}
			c1.clear();
		});


		sceneMap = new HashMap<String, Scene>();

		sceneMap.put("server", createServerGui());
		sceneMap.put("client", createClientGui());

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(startScene);
		primaryStage.show();

	}

	public Scene createServerGui() {

		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(70));
		pane.setStyle("-fx-background-color: coral");

		pane.setCenter(listItems);

		return new Scene(pane, 500, 400);

	}

	public Scene createClientGui() {

		clientBox = new VBox(10);
		clientBox.setStyle("-fx-background-color: blue");

		// Add a ChoiceBox to select the target of the message
		ChoiceBox<String> targetChoice = new ChoiceBox<>(FXCollections.observableArrayList("All", "Individual", "Group"));
		targetChoice.setValue("All");
		clientBox.getChildren().add(targetChoice);

		c1 = new TextField();
		b1 = new Button("Send");

		// Modify the send() method to accept an additional String[] parameter for the targets
		b1.setOnAction(e -> {
			String target = targetChoice.getValue();
			String message = c1.getText().trim();
			if (!message.equals("")) {
				String[] targets = {};
				if (target.equals("Individual")) {
					String selected = (String) clientList.getSelectionModel().getSelectedItem();
					if (selected != null && !selected.equals("Client list:")) {
						targets = new String[]{selected};
					}
				} else if (target.equals("Group")) {
					ObservableList<String> selected = clientList.getSelectionModel().getSelectedItems();
					if (selected != null && !selected.contains("Client list:")) {
						targets = selected.toArray(new String[selected.size()]);
					}
				}
				clientConnection.send(target, targets, message);
				c1.clear();
			}
		});

		// Enable multiple selection in the clientList
		clientList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// Add the list views to the clientBox
		clientBox.getChildren().addAll(c1, b1, listItems2, clientList);

		// Clear the selection in the clientList
		clientList.getSelectionModel().clearSelection();

		return new Scene(clientBox, 400, 300);
	}



}
