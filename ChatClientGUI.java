import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Random;
import javax.swing.*;
import javax.swing.text.*;

public class ChatClientGUI {
    private String userName;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private JFrame frame;
    private JTextPane chatPane;
    private StyledDocument doc;
    private JTextField inputField;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private HashMap<String, Color> userColors;

    public ChatClientGUI(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            userName = JOptionPane.showInputDialog("Digite seu nome:");
            writer.println(userName);

            createGUI();
            startMessageReader();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame("Chat - " + userName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(350, 300);
        frame.setLayout(new BorderLayout());

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();

        JScrollPane chatScroll = new JScrollPane(chatPane);
        frame.add(chatScroll, BorderLayout.CENTER);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFixedCellWidth(120);
        frame.add(new JScrollPane(userList), BorderLayout.EAST);

        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());
        frame.add(inputField, BorderLayout.SOUTH);

        frame.setVisible(true);

        userColors = new HashMap<>();
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.trim().isEmpty()) {
            writer.println(message);
            inputField.setText("");
        }
    }

    private void startMessageReader() {
        Thread readerThread = new Thread(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        readerThread.start();
    }

    private void handleMessage(String message) {
        if (message.startsWith("[USERS]")) {
            updateUsers(message.substring(7));
        } else {
            displayMessage(message);
        }
    }

    private void updateUsers(String usersString) {
        String[] users = usersString.split(",");
        userListModel.clear();
        for (String user : users) {
            user = user.trim();
            userListModel.addElement(user);
            if (!userColors.containsKey(user)) {
                userColors.put(user, randomColor());
            }
        }
    }

    private void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String[] parts = message.split(": ", 2);
                if (parts.length == 2) {
                    String user = parts[0].trim();
                    String text = parts[1].trim();
                    Color color = userColors.getOrDefault(user, Color.BLACK);

                    SimpleAttributeSet attr = new SimpleAttributeSet();
                    StyleConstants.setForeground(attr, color);
                    StyleConstants.setBold(attr, true);

                    doc.insertString(doc.getLength(), user + ": ", attr);

                    SimpleAttributeSet normalAttr = new SimpleAttributeSet();
                    doc.insertString(doc.getLength(), " (" + getCurrentTimestamp() + ") " + text + " \n" , normalAttr);
                } else {
                    // Mensagem de sistema
                    SimpleAttributeSet systemAttr = new SimpleAttributeSet();
                    StyleConstants.setItalic(systemAttr, true);
                    StyleConstants.setForeground(systemAttr, Color.GRAY);

                    doc.insertString(doc.getLength(), message + "\n", systemAttr);
                }
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private Color randomColor() {
        Random rand = new Random();
        float hue = rand.nextFloat();
        float saturation = 0.7f;
        float brightness = 0.9f;
        return Color.getHSBColor(hue, saturation, brightness);
    }

    private String getCurrentTimestamp() {
        return java.time.LocalTime.now().withNano(0).toString();
    }

    public static void main(String[] args) {
        String serverAddress = JOptionPane.showInputDialog(null, "Digite o endereço do servidor:", "Conectar ao Servidor", JOptionPane.QUESTION_MESSAGE);
        if (serverAddress == null || serverAddress.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Endereço do servidor não informado. Encerrando.");
            return;
        }
    
        String portInput = JOptionPane.showInputDialog(null, "Digite a porta do servidor (padrão: 12345):", "Porta do Servidor", JOptionPane.QUESTION_MESSAGE);
        int serverPort = 12345; // valor padrão
        try {
            if (portInput != null && !portInput.trim().isEmpty()) {
                serverPort = Integer.parseInt(portInput.trim());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Porta inválida. Usando 12345 como padrão.");
        }
    
        new ChatClientGUI(serverAddress.trim(), serverPort);
    }
    
    
}
