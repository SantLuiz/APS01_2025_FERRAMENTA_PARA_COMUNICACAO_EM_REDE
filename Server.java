import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<String, PrintWriter> clientUsers = new HashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Servidor de Chat iniciado...");
        ServerSocket serverSocket = new ServerSocket(12345);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Novo cliente conectado: " + clientSocket);
            new ClientHandler(clientSocket).start();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String userName;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Primeiro, ler o nome do usuário
                userName = in.readLine();
                synchronized (clientWriters) {
                    clientWriters.add(out);
                    clientUsers.put(userName, out);
                }
                broadcastUserList();
                broadcastMessage("Servidor", userName + " entrou no chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(userName, message);
                }
            } catch (IOException e) {
                System.out.println("Erro: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Erro ao fechar socket: " + e.getMessage());
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                    clientUsers.remove(userName);
                }
                broadcastUserList();
                broadcastMessage("Servidor", userName + " saiu do chat.");
            }
        }

        private void broadcastMessage(String sender, String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(sender + ": " + message);
                }
            }
        }

        private void broadcastUserList() {
            synchronized (clientWriters) {
                StringBuilder userList = new StringBuilder();
                for (String user : clientUsers.keySet()) {
                    userList.append(user).append(",");
                }
                if (!userList.isEmpty()) {
                    userList.deleteCharAt(userList.length() - 1); // Remove última vírgula
                }
                for (PrintWriter writer : clientWriters) {
                    writer.println("USERLIST:" + userList.toString());
                }
            }
        }
    }
}
