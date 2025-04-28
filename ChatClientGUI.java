import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ChatClientGUI {

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> listModel;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String username;

    public ChatClientGUI(String serverAddress, int serverPort) {
        frame = new JFrame("Chat - Cliente");
        chatArea = new JTextArea();
        inputField = new JTextField();
        sendButton = new JButton("Enviar");
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);

        frame.setLayout(new BorderLayout());

        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.add(new JScrollPane(userList), BorderLayout.EAST);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        try {
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            username = JOptionPane.showInputDialog(
                frame,
                "Digite seu nome de usuário:",
                "Login",
                JOptionPane.PLAIN_MESSAGE
            );

            if (username != null && !username.trim().isEmpty()) {
                out.println(username);
            } else {
                username = "Anônimo";
                out.println(username);
            }

            // Thread para receber mensagens
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("USERLIST:")) {
                            updateUserList(line.substring(9));
                        } else {
                            chatArea.append(line + "\n");
                        }
                    }
                } catch (IOException e) {
                    chatArea.append("Erro ao ler do servidor.\n");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Não foi possível conectar ao servidor.", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.trim().isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private void updateUserList(String users) {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            String[] userArray = users.split(",");
            Arrays.stream(userArray).forEach(listModel::addElement);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClientGUI("127.0.0.1", 12345));
    }
}
