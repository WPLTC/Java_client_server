import javax.swing.*;
import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EchoClientGUI extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String clientName;
    private JButton connectButton;
    private JButton disconnectButton;
    private boolean isConnected = false;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JButton privateMsgButton;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private JLabel userCountLabel;

    public EchoClientGUI() {
        setTitle("Client Echo");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        clientName = JOptionPane.showInputDialog(this, "Entrez votre nom :", "Nom du client", JOptionPane.PLAIN_MESSAGE);
        if (clientName == null || clientName.trim().isEmpty()) {
            clientName = "Moi";
        }
        setTitle(clientName);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        inputField = new JTextField();

        connectButton = new JButton("Connexion");
        disconnectButton = new JButton("Deconnexion");
        disconnectButton.setEnabled(false);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userListScroll = new JScrollPane(userList);
        userListScroll.setPreferredSize(new Dimension(120, 0));

        userCountLabel = new JLabel("Utilisateurs connectes : 0");
        userCountLabel.setHorizontalAlignment(SwingConstants.CENTER);

        privateMsgButton = new JButton("Message prive");
        privateMsgButton.setEnabled(false);
        privateMsgButton.addActionListener(e -> sendPrivateMessage());
        userList.addListSelectionListener(e -> privateMsgButton.setEnabled(userList.getSelectedIndex() != -1 && isConnected));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(userCountLabel, BorderLayout.NORTH);
        rightPanel.add(userListScroll, BorderLayout.CENTER);
        rightPanel.add(privateMsgButton, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);
        add(topPanel, BorderLayout.NORTH);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        inputField.addActionListener(e -> sendMessage());
        inputField.setEnabled(false);

        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());

        setVisible(true);
    }

    // Connexion au serveur
    private void connectToServer() {
        if (isConnected) return;
        try {
            socket = new Socket("localhost", 12345); // Adresse et port
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Envoie le nom au serveur dès la connexion
            out.println(clientName);

            chatArea.append("Connecte au serveur.\n");
            isConnected = true;
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            inputField.setEnabled(true);

            // Thread pour lire les messages entrants
            new Thread(() -> {
                String response;
                try {
                    while ((response = in.readLine()) != null) {
                        String time = timeFormat.format(new Date());
                        if (response.startsWith("/users ")) {
                            updateUserList(response.substring(7));
                        } else if (response.startsWith("[Prive de ")) {
                            chatArea.append("[" + time + "] " + response + "\n");
                        } else {
                            chatArea.append("[" + time + "] " + response + "\n");
                        }
                    }
                } catch (IOException ex) {
                    chatArea.append("Deconnecte du serveur.\n");
                } finally {
                    isConnected = false;
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);
                    inputField.setEnabled(false);
                }
            }).start();

        } catch (IOException e) {
            chatArea.append("Erreur de connexion : " + e.getMessage() + "\n");
        }
    }

    // Déconnexion du serveur
    private void disconnectFromServer() {
        if (!isConnected) return;
        try {
            out.println("exit");
            socket.close(); // Ferme la connexion
            chatArea.append("Deconnexion.\n");
        } catch (IOException e) {
            chatArea.append("Erreur de fermeture : " + e.getMessage() + "\n");
        } finally {
            isConnected = false;
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            inputField.setEnabled(false);
        }
    }

    // Envoi de message
    private void sendMessage() {
        if (!isConnected) return;
        String message = inputField.getText();
        if (message.isEmpty()) return;

        out.println(message); // Envoie au serveur
        String time = timeFormat.format(new Date());
        chatArea.append("[" + time + "] " + clientName + " : " + message + "\n");
        inputField.setText("");

        if (message.equalsIgnoreCase("exit")) {
            try {
                socket.close(); // Ferme la connexion
                chatArea.append("Déconnexion.\n");
            } catch (IOException e) {
                chatArea.append("Erreur de fermeture : " + e.getMessage() + "\n");
            } finally {
                isConnected = false;
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                inputField.setEnabled(false);
            }
        }
    }

    // Met à jour la liste des utilisateurs connectés
    private void updateUserList(String users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            String[] names = users.split(",");
            int count = 0;
            for (String name : names) {
                if (!name.trim().isEmpty() && !name.equals(clientName)) {
                    userListModel.addElement(name);
                    count++;
                }
            }
            userCountLabel.setText("Utilisateurs connectes : " + (count + 1)); // +1 pour soi-même
        });
    }

    // Envoie un message privé à l'utilisateur sélectionné
    private void sendPrivateMessage() {
        String dest = userList.getSelectedValue();
        if (dest == null || !isConnected) return;
        String msg = JOptionPane.showInputDialog(this, "Message prive a " + dest + " :", "Message prive", JOptionPane.PLAIN_MESSAGE);
        if (msg != null && !msg.trim().isEmpty()) {
            out.println("/mp " + dest + " " + msg);
            String time = timeFormat.format(new Date());
            chatArea.append("[" + time + "] [Prive a" + dest + "] " + msg + "\n");
        }
    }

    public static void main(String[] args) {
        new EchoClientGUI(); // Lance le client
    }
}
