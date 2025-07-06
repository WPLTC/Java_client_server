import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;

public class EchoServerGUI extends JFrame {

    private JTextArea logArea;
    private ServerSocket serverSocket;
    private int clientCount = 0; // Compteur de clients
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // Liste des clients connectés
    private List<String> messageHistory = new ArrayList<>(); // Historique des messages

    public EchoServerGUI() {
        setTitle("Serveur Echo");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Zone de texte pour afficher les messages du client
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Ajout du bouton Afficher l'historique
        JButton historyButton = new JButton("Afficher l'historique");
        historyButton.addActionListener(e -> showHistory());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(historyButton);
        add(topPanel, BorderLayout.NORTH);

        setVisible(true);
        startServer();
    }

    // Méthode pour démarrer le serveur
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(12345); // Port d'écoute
                log("Serveur en écoute sur le port 12345...");

                while (true) {
                    Socket clientSocket = serverSocket.accept(); // Accepte un client
                    clientCount++;
                    int clientId = clientCount; // Identifiant unique pour ce client
                    log("Client " + clientId + " connecte : " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, clientId);
                    clients.add(handler);
                    new Thread(handler).start();
                }

            } catch (IOException e) {
                log("Erreur serveur : " + e.getMessage());
            }
        }).start();
    }

    // Classe interne pour gérer chaque client
    private class ClientHandler implements Runnable {
        private Socket socket;
        private int clientId;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName = "";

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
            try {
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                log("Erreur d'initialisation du client " + clientId + " : " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                // Lire le nom du client comme premier message
                clientName = in.readLine();
                if (clientName == null || clientName.trim().isEmpty()) {
                    clientName = "Client " + clientId;
                }
                log(clientName + " s'est connecte.");
                broadcast(clientName + " s'est connecte.", this);
                sendUserListToAll();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/mp ")) {
                        // Format: /mp destinataire message
                        int firstSpace = message.indexOf(' ', 4);
                        if (firstSpace > 4) {
                            String destName = message.substring(4, firstSpace);
                            String privateMsg = message.substring(firstSpace + 1);
                            boolean sent = sendPrivateMessage(destName, "[Prive de " + clientName + "] " + privateMsg);
                            if (!sent) {
                                sendMessage("[Serveur] Utilisateur introuvable ou non connecte.");
                            }
                        }
                    } else if (message.equalsIgnoreCase("exit")) {
                        log(clientName + " a quitte la session.");
                        broadcast(clientName + " a quitte la session.", this);
                        break;
                    } else {
                        log(clientName + " : " + message);
                        broadcast(clientName + " : " + message, this);
                    }
                }
            } catch (IOException e) {
                log("Erreur client " + clientId + " : " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(this);
                if (!clientName.isEmpty()) {
                    broadcast(clientName + " a quitté la session.", this);
                    sendUserListToAll();
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }

    // Diffuse un message à tous les clients sauf l'expéditeur
    private void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
        synchronized (messageHistory) {
            messageHistory.add(message);
        }
    }

    // Affichage dans l'interface
    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
        synchronized (messageHistory) {
            messageHistory.add(message);
        }
    }

    // Récupérer la liste des noms d'utilisateurs connectés
    private List<String> getConnectedUsernames() {
        List<String> names = new java.util.ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.clientName != null && !c.clientName.isEmpty()) {
                names.add(c.clientName);
            }
        }
        return names;
    }

    // Envoie la liste des utilisateurs connectés à tous les clients
    private void sendUserListToAll() {
        StringBuilder sb = new StringBuilder("/users ");
        for (String name : getConnectedUsernames()) {
            sb.append(name).append(",");
        }
        String userListMsg = sb.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(userListMsg);
        }
    }

    // Envoie un message privé à un utilisateur par son nom
    private boolean sendPrivateMessage(String destName, String message) {
        boolean found = false;
        for (ClientHandler client : clients) {
            if (client.clientName.equals(destName)) {
                client.sendMessage(message);
                found = true;
            }
        }
        if (found) {
            synchronized (messageHistory) {
                messageHistory.add("[Privé à " + destName + "] " + message);
            }
        }
        return found;
    }

    // Affiche l'historique dans une boîte de dialogue
    private void showHistory() {
        StringBuilder sb = new StringBuilder();
        synchronized (messageHistory) {
            for (String msg : messageHistory) {
                sb.append(msg).append("\n");
            }
        }
        JTextArea historyArea = new JTextArea(sb.toString());
        historyArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setPreferredSize(new Dimension(350, 200));
        JOptionPane.showMessageDialog(this, scrollPane, "Historique des messages", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        new EchoServerGUI(); // Lance le serveur
    }
}
