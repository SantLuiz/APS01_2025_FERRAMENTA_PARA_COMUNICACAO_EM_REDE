import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static Set<String> userNames = new HashSet<>();

    public static void main(String[] args) {
        int port = 12345;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor de chat rodando na porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler handler : clientHandlers) {
            handler.sendMessage(message);
        }
    }

    public static void updateUsersList() {
        String userList = "[USERS]" + String.join(",", userNames);
        for (ClientHandler handler : clientHandlers) {
            handler.sendMessage(userList);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String userName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                userName = reader.readLine();
                userNames.add(userName);
                System.out.println(userName + " conectado");
                broadcast("🔔 " + userName + " entrou no chat!");
                updateUsersList();

                String message;
                while ((message = reader.readLine()) != null) {
                    broadcast(userName + ": " + message);
                }
            } catch (IOException e) {
                System.out.println(userName + " desconectado.");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientHandlers.remove(this);
                userNames.remove(userName);
                broadcast("🔕 " + userName + " saiu do chat.");
                updateUsersList();
            }
        }

        public void sendMessage(String message) {
            writer.println(message);
        }
    }
}
