import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class SimpleClient {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int PORT = 59001;

    private static class IncomingMessageHandler extends Thread {
        private final BufferedReader reader;

        public IncomingMessageHandler(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Error reading messages from the server: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        System.out.printf("Connecting to chat server at %s:%d...%n", SERVER_ADDRESS, PORT);

        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;
        Scanner scanner = null;

        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            scanner = new Scanner(System.in);

            Thread incomingMessageHandler = new IncomingMessageHandler(reader);
            incomingMessageHandler.setDaemon(true);
            incomingMessageHandler.start();

            System.out.println("Connected to the chat server");
            System.out.println("Type your message:");

            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                writer.println(message);
            }
        } catch (IOException e) {
            System.out.println("Error connecting to the server: " + e.getMessage());
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.out.println("Error closing the reader: " + e.getMessage());
                }
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing the socket: " + e.getMessage());
                }
            }
        }
    }
}
