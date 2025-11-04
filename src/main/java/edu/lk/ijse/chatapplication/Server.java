package edu.lk.ijse.chatapplication;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends Application {

    @FXML private TextArea chatArea;
    private static Server instance;

    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final List<String> names = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void start(Stage stage) throws IOException {
        instance = this;
        FXMLLoader loader = new FXMLLoader(Server.class.getResource("Server.fxml"));
        loader.setController(this);
        Scene scene = new Scene(loader.load(), 700, 500);
        stage.setTitle("Chat Server");
        stage.setScene(scene);
        stage.show();
        log("Server Started");
        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(3030)) {
                while (true) {
                    Socket client = server.accept();
                    new ClientHandler(client).start();
                }
            } catch (IOException e) {
                log("Server Stopped");
            }
        }).start();
    }

    public static void broadcast(String msg) {
        synchronized (clients) {
            clients.values().forEach(c -> c.send(msg));
        }
        Platform.runLater(() -> {
            if (instance != null) instance.log(msg);
        });
    }

    public static void addClient(String name, ClientHandler handler) {
        clients.put(name, handler);
        names.add(name);
        broadcast(name + " joined");
        updateOnline();
    }

    public static void removeClient(String name) {
        clients.remove(name);
        names.remove(name);
        broadcast(name + " left");
        updateOnline();
    }

    public static boolean nameExists(String name) {
        return names.contains(name);
    }

    private static void updateOnline() {
        broadcast("Online: " + names.size() + " → " + String.join(", ", names));
    }

    @FXML
    private void log(String msg) {
        chatArea.appendText(msg + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Send name: " + name);
                name = in.readLine();

                if (name == null || name.trim().isEmpty() || nameExists(name)) {
                    out.println("error");
                    out.println("Name taken!");
                    socket.close();
                    return;
                }

                out.println("Accept");
                addClient(name, this);

                String msg;
                while (true) {
                    msg = in.readLine();
                    if (msg == null || "Logout".equals(msg)) break;
                    if (!msg.trim().isEmpty()) {
                        broadcast(name + ": " + msg);
                    }
                }
            } catch (IOException e) {
            } finally {
                if (name != null) removeClient(name);
                try { socket.close(); } catch (IOException e) {}
            }
        }

        public void send(String msg) {
            out.println(msg);
        }
    }
}