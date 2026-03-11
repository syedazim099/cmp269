import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatApp extends Application {
    private static final int PORT = 59001;

    private final TextArea chatArea = new TextArea();
    private final TextField inputField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField hostField = new TextField("localhost");
    private final Button sendButton = new Button("Send");
    private final Button connectButton = new Button("Connect");

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    @Override
    public void start(Stage stage) {
        chatArea.setEditable(false);

        inputField.setPromptText("Message...");
        inputField.setDisable(true);
        sendButton.setDisable(true);

        connectButton.setOnAction(e -> connect());
        sendButton.setOnAction(e -> sendMessage());

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        stage.setOnCloseRequest(e -> closeConnection());

        HBox connectBox = new HBox(
                5,
                new Label("Name:"), nameField,
                new Label("Host:"), hostField,
                connectButton
        );
        connectBox.setPadding(new Insets(10));

        HBox inputBox = new HBox(5, inputField, sendButton);
        inputBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(connectBox);
        root.setCenter(chatArea);
        root.setBottom(inputBox);

        stage.setScene(new Scene(root, 500, 400));
        stage.setTitle("Lehman Chat Client");
        stage.show();
    }

    private void connect() {
        String name = nameField.getText().trim();
        String host = hostField.getText().trim();

        if (name.isEmpty() || host.isEmpty()) {
            return;
        }

        try {
            socket = new Socket(host, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String serverPrompt = in.readLine();
            if (serverPrompt != null) {
                chatArea.appendText(serverPrompt + "\n");
            }

            out.println(name);

            Thread listener = new Thread(this::listenForMessages);
            listener.setDaemon(true);
            listener.start();

            inputField.setDisable(false);
            sendButton.setDisable(false);
            connectButton.setDisable(true);

            chatArea.appendText("Connected as " + name + "\n");
        } catch (IOException e) {
            chatArea.appendText("Connect failed: " + e.getMessage() + "\n");
        }
    }

    private void listenForMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String message = line;
                Platform.runLater(() -> chatArea.appendText(message + "\n"));
            }
        } catch (IOException e) {
            String errorMessage = "Disconnected: " + e.getMessage();
            Platform.runLater(() -> chatArea.appendText(errorMessage + "\n"));
        } finally {
            Platform.runLater(this::closeConnection);
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();

        if (text.isEmpty() || out == null) {
            return;
        }

        out.println(text);
        inputField.clear();
    }

    private void closeConnection() {
        inputField.setDisable(true);
        sendButton.setDisable(true);
        connectButton.setDisable(false);

        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore close errors
        }

        in = null;
        out = null;
        socket = null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
