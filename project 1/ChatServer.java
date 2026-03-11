import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class ChatServer {

    private static final Set<PrintWriter> clientWriters =
            Collections.synchronizedSet(new HashSet<>());

    public static final int PORT = 59001;

    private static class ClientHandler extends Thread {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Handling client communication in thread: "
                    + Thread.currentThread().getName()
                    + " for client: " + socket.getRemoteSocketAddress());

            BufferedReader reader = null;
            PrintWriter writer = null;
            String username = null;

            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                clientWriters.add(writer);

                writer.println("SERVER: Enter your username:");
                username = reader.readLine();
                System.out.println("Client " + socket.getRemoteSocketAddress()
                        + " set username to: " + username);

                broadcastMessage("SERVER: " + username + " has joined the chat!");

                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Received message from " + username + ": " + message);
                    broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Error handling client communication: " + e.getMessage());
            } finally {
                if (writer != null) {
                    clientWriters.remove(writer);
                }

                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                    if (username != null) {
                        broadcastMessage("SERVER: " + username + " has left the chat!");
                    }
                } catch (IOException e) {
                    System.out.println("Error closing client resources: " + e.getMessage());
                }
            }
        }

        private void broadcastMessage(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("The chat server is running. Awaiting connections at port " + PORT + "...");

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = listener.accept();
                System.out.println("New client connected: " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket);
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("Error starting the server: " + e.getMessage());
        }
    }
}
