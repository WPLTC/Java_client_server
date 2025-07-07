import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

public class EchoServerGUI extends JFrame {

    private JTextArea logArea;
    private ServerSocket serverSocket;
    private int clientCount = 0; // Compteur de clients
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // Liste des clients connectes
    private List<String> messageHistory = new ArrayList<>(); // Historique des messages
    private UserManager userManager = new UserManager();
    private MessageStore messageStore = new MessageStore();
    private Map<String, Group> groups = new HashMap<>();

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
                log("Serveur en ecoutesur le port 12345...");

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
                // Authentification : attendre une commande REGISTER ou LOGIN
                String authLine = in.readLine();
                String[] authParts = authLine.split(" ", 3);
                boolean authenticated = false;
                if (authParts[0].equals(Protocol.REGISTER) && authParts.length == 3) {
                    if (userManager.register(authParts[1], authParts[2])) {
                        clientName = authParts[1];
                        out.println(Protocol.SUCCESS + " Inscription reussie");
                        authenticated = true;
                    } else {
                        out.println(Protocol.ERROR + " Nom déjà utilisé");
                    }
                } else if (authParts[0].equals(Protocol.LOGIN) && authParts.length == 3) {
                    if (userManager.login(authParts[1], authParts[2])) {
                        clientName = authParts[1];
                        out.println(Protocol.SUCCESS + " Connexion reussie");
                        authenticated = true;
                    } else {
                        out.println(Protocol.ERROR + " Identifiants invalides");
                    }
                }
                if (!authenticated) {
                    socket.close();
                    return;
                }
                log(clientName + " s'est connecte.");
                broadcast(clientName + " s'est connecte.", this);
                sendUserListToAll();
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/mp ")) {
                        int firstSpace = message.indexOf(' ', 4);
                        if (firstSpace > 4) {
                            String destName = message.substring(4, firstSpace);
                            String privateMsg = message.substring(firstSpace + 1);
                            boolean sent = sendPrivateMessage(destName, "[Privé de " + clientName + "] " + privateMsg);
                            if (!sent) {
                                sendMessage("[Serveur] Utilisateur introuvable ou non connecte.");
                            } else {
                                String horodatage = java.time.LocalDateTime.now().toString();
                                messageStore.addMessage(new Message(clientName, destName, "[Privé] " + privateMsg));
                            }
                        }
                    } else if (message.startsWith("/group ")) {
                        String[] parts = message.split(" ", 4);
                        if (parts.length >= 3) {
                            String action = parts[1];
                            String groupNameRaw = parts[2].trim();
                            String groupName = groupNameRaw.contains(" [") ? groupNameRaw.split(" \\[")[0].trim() : groupNameRaw;
                            if (action.equals("create")) {
                                if (groups.containsKey(groupName)) {
                                    sendMessage("[Serveur] Groupe déjà existant.");
                                } else {
                                    Group group = new Group(groupName, clientName);
                                    groups.put(groupName, group);
                                    sendMessage("[Serveur] Groupe '" + groupName + "' cree. Vous en etes le moderateur.");
                                    broadcastGroupListToAll();
                                }
                            } else if (action.equals("add") && parts.length == 4) {
                                String userToAdd = parts[3].trim();
                                if (userToAdd.contains(" [")) {
                                    userToAdd = userToAdd.split(" \\[")[0].trim();
                                }
                                System.out.println("Tentative d'ajout de membre : '" + userToAdd + "' (utilisateurs connectes : " + getConnectedUsernames() + ")");
                                Group group = groups.get(groupName);
                                if (group != null && group.getModerator().equals(clientName)) {
                                    group.addMember(userToAdd);
                                    System.out.println("Ajout de " + userToAdd + " au groupe " + groupName + " (moderateur: " + group.getModerator() + ")");
                                    sendMessage("[Serveur] " + userToAdd + " ajouter au groupe '" + groupName + "'.");
                                    broadcastGroupListToAll();
                                } else {
                                    sendMessage("[Serveur] Vous n'etes pas le moderateur ou le groupe n'existe pas.");
                                }
                            } else if (action.equals("remove") && parts.length == 4) {
                                String userToRemove = parts[3].trim();
                                if (userToRemove.contains(" [")) {
                                    userToRemove = userToRemove.split(" \\[")[0].trim();
                                }
                                Group group = groups.get(groupName);
                                if (group != null && group.getModerator().equals(clientName)) {
                                    group.removeMember(userToRemove);
                                    sendMessage("[Serveur] " + userToRemove + " retire du groupe '" + groupName + "'.");
                                    broadcastGroupListToAll();
                                } else {
                                    sendMessage("[Serveur] Vous n'etes pas le moderateur ou le groupe n'existe pas.");
                                }
                            } else if (action.equals("send") && parts.length == 4) {
                                String groupMsg = parts[3];
                                Group group = groups.get(groupName);
                                if (group != null && group.isMember(clientName)) {
                                    String horodatage = java.time.LocalDateTime.now().toString();
                                    String fullMsg = "[Groupe " + groupName + "] [" + horodatage + "] " + clientName + " : " + groupMsg;
                                    for (ClientHandler client : clients) {
                                        if (group.isMember(client.clientName)) {
                                            client.sendMessage(fullMsg);
                                        }
                                    }
                                    messageStore.addMessage(new Message(clientName, groupName, groupMsg));
                                } else {
                                    sendMessage("[Serveur] Vous n'etes pas membre du groupe ou le groupe n'existe pas.");
                                }
                            }
                        }
                    } else if (message.equalsIgnoreCase("exit")) {
                        log(clientName + " a quitté la session.");
                        broadcast(clientName + " a quitté la session.", this);
                        break;
                    } else {
                        String horodatage = java.time.LocalDateTime.now().toString();
                        String fullMsg = "[" + horodatage + "] " + clientName + " : " + message;
                        log(fullMsg);
                        broadcast(fullMsg, this);
                        messageStore.addMessage(new Message(clientName, "ALL", message));
                    }
                }
            } catch (IOException e) {
                log("Erreur client " + clientId + " : " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(this);
                if (!clientName.isEmpty()) {
                    userManager.logout(clientName);
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

    // Récupérer la liste des noms d'utilisateurs connectes
    private List<String> getConnectedUsernames() {
        List<String> names = new java.util.ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.clientName != null && !c.clientName.isEmpty()) {
                String pureName = c.clientName.trim();
                if (pureName.contains(" [")) {
                    pureName = pureName.split(" \\[")[0].trim();
                }
                names.add(pureName);
            }
        }
        return names;
    }

    // Envoie la liste des utilisateurs connectes à tous les clients
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

    // Diffuse la liste des groupes à tous les clients
    private void broadcastGroupListToAll() {
        for (ClientHandler client : clients) {
            StringBuilder sb = new StringBuilder("/groups ");
            for (String groupName : groups.keySet()) {
                Group group = groups.get(groupName);
                if (group.isMember(client.clientName)) {
                    sb.append(groupName).append(" [").append(group.getModerator()).append("],");
                }
            }
            System.out.println("Envoi à " + client.clientName + " : " + sb.toString());
            client.sendMessage(sb.toString());
        }
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
