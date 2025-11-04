package edu.lk.ijse.chatapplication;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.*;
import java.net.Socket;

public class Client2Controller {

    @FXML private TextArea chatArea;
    @FXML private TextField messageField, nameField;
    @FXML private Button connectBtn, logoutBtn;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    @FXML
    public void initialize() {
        logoutBtn.setDisable(true);
        messageField.setDisable(true);
    }

    @FXML
    private void connect() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            alert("Enter name!");
            return;
        }

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 3030);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println(name);

                String response;
                while ((response = in.readLine()) != null) {
                    if ("Accept".equals(response)) {
                        Platform.runLater(() -> {
                            connectBtn.setDisable(true);
                            nameField.setDisable(true);
                            logoutBtn.setDisable(false);
                            messageField.setDisable(false);
                            append( name +" is Connected" );
                        });
                        startReader();
                        return;
                    } else if ("Reject".equals(response)) {
                        String reason = in.readLine();
                        Platform.runLater(() -> alert(reason));
                        close();
                        return;
                    } else {
                        append(response);
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> alert("Server offline!"));
            }
        }).start();
    }

    private void startReader() {
        new Thread(() -> {
            try {
                String msg;
                while (true) {
                    msg = in.readLine();
                    if (msg == null) break;
                    append(msg);
                }
            } catch (IOException e) {
                append("Disconnected");
            }
        }).start();
    }

    @FXML
    private void send() {
        String msg = messageField.getText().trim();
        if (!msg.isEmpty() && out != null) {
            out.println(msg);
            messageField.clear();
        }
    }

    @FXML
    private void logout() {
        if (out != null) out.println("Logout");
        close();
        Platform.runLater(() -> {
            connectBtn.setDisable(false);
            nameField.setDisable(false);
            logoutBtn.setDisable(true);
            messageField.setDisable(true);
            append("You left");
        });
    }

    private void append(String text) {
        Platform.runLater(() -> chatArea.appendText(text + "\n"));
    }

    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {}
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg);
        a.setHeaderText(null);
        a.show();
    }
}