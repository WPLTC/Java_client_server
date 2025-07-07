import javax.swing.*;
import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.file.*;

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
    private String password;
    private boolean isRegisterMode = false;
    private DefaultListModel<String> groupListModel;
    private JList<String> groupList;
    private JButton createGroupButton;
    private String selectedGroup = null;
    private JButton addMemberButton;
    private JButton removeMemberButton;
    private String lastCreatedGroup = null;
    private JButton groupMsgButton;
    private JButton createConferenceButton;
    private java.util.Map<String, String> savedCredentials = new java.util.HashMap<>();
    private JButton sendButton;

    public EchoClientGUI() {
        setTitle("Client Echo");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        showAuthDialog();
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

        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane groupListScroll = new JScrollPane(groupList);
        groupListScroll.setPreferredSize(new Dimension(120, 60));
        createGroupButton = new JButton("Creer un groupe");
        createGroupButton.setEnabled(true);
        createGroupButton.addActionListener(e -> createGroup());
        addMemberButton = new JButton("Ajouter un membre");
        removeMemberButton = new JButton("Retirer un membre");
        addMemberButton.setEnabled(false);
        removeMemberButton.setEnabled(false);
        addMemberButton.addActionListener(e -> showAddMemberDialog());
        removeMemberButton.addActionListener(e -> showRemoveMemberDialog());
        groupMsgButton = new JButton("Message groupe");
        groupMsgButton.setEnabled(false);
        groupMsgButton.addActionListener(e -> sendGroupMessageDialog());
        createConferenceButton = new JButton("Creer une conference");
        createConferenceButton.addActionListener(e -> showCreateConferenceDialog());
        JPanel groupButtonPanel = new JPanel();
        groupButtonPanel.setLayout(new BoxLayout(groupButtonPanel, BoxLayout.X_AXIS));
        groupButtonPanel.add(addMemberButton);
        groupButtonPanel.add(Box.createHorizontalStrut(10));
        groupButtonPanel.add(removeMemberButton);
        JPanel groupPanel = new JPanel(new BorderLayout());
        groupPanel.add(new JLabel("Groupes/Conferences"), BorderLayout.NORTH);
        groupPanel.add(groupListScroll, BorderLayout.CENTER);

        JPanel groupSouthPanel = new JPanel();
        groupSouthPanel.setLayout(new BoxLayout(groupSouthPanel, BoxLayout.Y_AXIS));
        groupSouthPanel.add(createGroupButton);
        groupSouthPanel.add(Box.createVerticalStrut(5));
        groupSouthPanel.add(groupButtonPanel);
        groupSouthPanel.add(Box.createVerticalStrut(5));
        groupSouthPanel.add(createConferenceButton);
        groupSouthPanel.add(Box.createVerticalStrut(5));
        groupSouthPanel.add(groupMsgButton);

        groupPanel.add(groupSouthPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(userCountLabel, BorderLayout.NORTH);
        rightPanel.add(userListScroll, BorderLayout.CENTER);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(privateMsgButton);
        southPanel.add(Box.createVerticalStrut(10));
        southPanel.add(groupPanel);

        rightPanel.add(southPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);
        add(topPanel, BorderLayout.NORTH);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        sendButton = new JButton("Envoyer");
        sendButton.setEnabled(true);
        sendButton.addActionListener(e -> sendMessage());
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        inputField.addActionListener(e -> sendMessage());
        inputField.setEnabled(false);

        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());

        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedGroup = groupList.getSelectedValue();
                boolean groupSelected = selectedGroup != null;
                boolean isModerator = false;
                if (groupSelected && selectedGroup.contains(" [")) {
                    String[] parts = selectedGroup.split(" \\[");
                    if (parts.length == 2) {
                        String mod = parts[1].replace("]", "").trim();
                        isModerator = mod.equalsIgnoreCase(clientName.trim());
                    }
                }
                addMemberButton.setEnabled(groupSelected && isModerator);
                removeMemberButton.setEnabled(groupSelected && isModerator);
                groupMsgButton.setEnabled(groupSelected);
            }
        });

        setVisible(true);
    }

    private void showAuthDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2));
        JComboBox<String> nameCombo = new JComboBox<>();
        JPasswordField passField = new JPasswordField();
        JRadioButton loginBtn = new JRadioButton("Connexion", true);
        JRadioButton registerBtn = new JRadioButton("Inscription");
        ButtonGroup group = new ButtonGroup();
        group.add(loginBtn);
        group.add(registerBtn);
        panel.add(new JLabel("Nom d'utilisateur :"));
        panel.add(nameCombo);
        panel.add(new JLabel("Mot de passe :"));
        panel.add(passField);
        panel.add(loginBtn);
        panel.add(registerBtn);
        // Charger tous les comptes
        savedCredentials.clear();
        try {
            Path credPath = Paths.get("credentials.txt");
            if (Files.exists(credPath)) {
                java.util.List<String> lines = Files.readAllLines(credPath);
                for (String line : lines) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        savedCredentials.put(parts[0], parts[1]);
                        nameCombo.addItem(parts[0]);
                    }
                }
            }
        } catch (Exception ex) { /* ignore */ }
        nameCombo.setEditable(true);
        nameCombo.addActionListener(e -> {
            String selected = (String) nameCombo.getSelectedItem();
            if (selected != null && savedCredentials.containsKey(selected)) {
                passField.setText(savedCredentials.get(selected));
            }
        });
        int result = JOptionPane.showConfirmDialog(this, panel, "Authentification", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            clientName = ((String) nameCombo.getSelectedItem()).trim();
            password = new String(passField.getPassword());
            isRegisterMode = registerBtn.isSelected();
        } else {
            System.exit(0);
        }
    }

    // Connexion au serveur
    private void connectToServer() {
        if (isConnected) return;
        try {
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Envoie REGISTER ou LOGIN selon le choix
            if (isRegisterMode) {
                out.println(Protocol.REGISTER + " " + clientName + " " + password);
            } else {
                out.println(Protocol.LOGIN + " " + clientName + " " + password);
            }
            String serverResponse = in.readLine();
            if (serverResponse.startsWith(Protocol.ERROR)) {
                chatArea.append(serverResponse + "\n");
                socket.close();
                isConnected = false;
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                inputField.setEnabled(false);
                showAuthDialog();
                return;
            } else if (serverResponse.startsWith(Protocol.SUCCESS)) {
                chatArea.append(serverResponse + "\n");
                // Si inscription réussie, enregistrer les identifiants (sans doublon)
                if (isRegisterMode) {
                    try {
                        Path credPath = Paths.get("credentials.txt");
                        java.util.List<String> lines = Files.exists(credPath) ? Files.readAllLines(credPath) : new java.util.ArrayList<>();
                        boolean found = false;
                        for (String line : lines) {
                            if (line.startsWith(clientName + ":")) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            lines.add(clientName + ":" + password);
                            Files.write(credPath, lines);
                        }
                    } catch (Exception ex) { /* ignore */ }
                }
            }

            isConnected = true;
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            inputField.setEnabled(true);

            new Thread(() -> {
                String response;
                try {
                    while ((response = in.readLine()) != null) {
                        String time = timeFormat.format(new Date());
                        if (response.startsWith("/users ")) {
                            updateUserList(response.substring(7));
                        } else if (response.startsWith("/groups ")) {
                            updateGroupList(response.substring(8));
                        } else if (response.startsWith("[Prive de ")) {
                            chatArea.append("[" + time + "] " + response + "\n");
                        } else if (response.startsWith("[Groupe ")) {
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
            showAuthDialog(); // Réaffiche la fenêtre d'authentification
        }
    }

    // Envoi de message
    private void sendMessage() {
        if (!isConnected) return;
        String message = inputField.getText();
        if (message.isEmpty()) return;
        String groupName = selectedGroup;
        if (selectedGroup != null && selectedGroup.contains(" [")) {
            groupName = selectedGroup.split(" \\[")[0].trim();
        }
        if (selectedGroup != null) {
            out.println("/group send " + groupName + " " + message);
            String time = timeFormat.format(new Date());
            chatArea.append("[" + time + "] [Groupe " + groupName + "] " + clientName + " : " + message + "\n");
        } else {
            out.println(message);
            String time = timeFormat.format(new Date());
            chatArea.append("[" + time + "] " + clientName + " : " + message + "\n");
        }
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
                showAuthDialog();
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
                String pureName = name.trim();
                // Supprimer tout préfixe entre crochets s'il y en a
                if (pureName.contains(" [")) {
                    pureName = pureName.split(" \\[")[0].trim();
                }
                if (!pureName.isEmpty() && !pureName.equals(clientName)) {
                    userListModel.addElement(pureName);
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

    private void createGroup() {
        String groupName = JOptionPane.showInputDialog(this, "Nom du groupe :", "Creer un groupe", JOptionPane.PLAIN_MESSAGE);
        if (groupName != null && !groupName.trim().isEmpty()) {
            out.println("/group create " + groupName.trim());
            lastCreatedGroup = groupName.trim();
        }
    }

    private void updateGroupList(String groups) {
        SwingUtilities.invokeLater(() -> {
            groupListModel.clear();
            String[] names = groups.split(",");
            int selectIndex = -1;
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                if (!name.trim().isEmpty()) {
                    groupListModel.addElement(name);
                    if (lastCreatedGroup != null && name.startsWith(lastCreatedGroup + " [")) {
                        selectIndex = groupListModel.size() - 1;
                    }
                }
            }
            if (selectIndex != -1) {
                groupList.setSelectedIndex(selectIndex);
                lastCreatedGroup = null;
            }
        });
    }

    private void showAddMemberDialog() {
        if (selectedGroup == null) return;
        String groupName = selectedGroup;
        if (groupName.contains(" [")) {
            groupName = groupName.split(" \\[")[0].trim();
        }
        java.util.List<String> candidates = new ArrayList<>();
        for (int i = 0; i < userListModel.size(); i++) {
            String user = userListModel.get(i);
            if (!user.equals(clientName)) {
                candidates.add(user);
            }
        }
        String userToAdd = (String) JOptionPane.showInputDialog(this, "Sélectionner un utilisateur à ajouter :", "Ajouter un membre", JOptionPane.PLAIN_MESSAGE, null, candidates.toArray(), null);
        if (userToAdd != null && !userToAdd.trim().isEmpty()) {
            out.println("/group add " + groupName + " " + userToAdd.trim());
        }
    }

    private void showRemoveMemberDialog() {
        if (selectedGroup == null) return;
        String groupName = selectedGroup;
        if (groupName.contains(" [")) {
            groupName = groupName.split(" \\[")[0].trim();
        }
        String userToRemove = JOptionPane.showInputDialog(this, "Nom du membre à retirer :", "Retirer un membre", JOptionPane.PLAIN_MESSAGE);
        if (userToRemove != null && !userToRemove.trim().isEmpty()) {
            out.println("/group remove " + groupName + " " + userToRemove.trim());
        }
    }

    private void sendGroupMessageDialog() {
        if (selectedGroup == null) return;
        String groupName = selectedGroup;
        if (groupName.contains(" [")) {
            groupName = groupName.split(" \\[")[0].trim();
        }
        String msg = JOptionPane.showInputDialog(this, "Message au groupe '" + groupName + "' :", "Message groupe", JOptionPane.PLAIN_MESSAGE);
        if (msg != null && !msg.trim().isEmpty()) {
            out.println("/group send " + groupName + " " + msg.trim());
            String time = timeFormat.format(new Date());
            chatArea.append("[" + time + "] [Groupe " + groupName + "] " + clientName + " : " + msg + "\n");
        }
    }

    private void showCreateConferenceDialog() {
        // Liste des utilisateurs connectés (hors soi-même)
        java.util.List<String> candidates = new ArrayList<>();
        for (int i = 0; i < userListModel.size(); i++) {
            String user = userListModel.get(i);
            if (!user.equals(clientName)) {
                candidates.add(user);
            }
        }
        JCheckBox[] checkBoxes = new JCheckBox[candidates.size()];
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < candidates.size(); i++) {
            checkBoxes[i] = new JCheckBox(candidates.get(i));
            panel.add(checkBoxes[i]);
        }
        String groupName = JOptionPane.showInputDialog(this, "Nom de la conference :", "Creer une conference", JOptionPane.PLAIN_MESSAGE);
        if (groupName == null || groupName.trim().isEmpty()) return;
        int result = JOptionPane.showConfirmDialog(this, panel, "Sélectionner les membres", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            out.println("/group create " + groupName.trim());
            // Ajoute tous les membres sélectionnés
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    out.println("/group add " + groupName.trim() + " " + cb.getText());
                }
            }
            // Ouvre la fenêtre de chat pour la conference
            openConferenceWindow(groupName.trim());
        }
    }

    private void openConferenceWindow(String groupName) {
        JFrame confFrame = new JFrame("conference : " + groupName);
        confFrame.setSize(400, 300);
        JTextArea confArea = new JTextArea();
        confArea.setEditable(false);
        JTextField confInput = new JTextField();
        confFrame.add(new JScrollPane(confArea), BorderLayout.CENTER);
        confFrame.add(confInput, BorderLayout.SOUTH);
        confFrame.setVisible(true);
        confInput.addActionListener(e -> {
            String msg = confInput.getText();
            if (!msg.trim().isEmpty()) {
                out.println("/group send " + groupName + " " + msg.trim());
                String time = timeFormat.format(new java.util.Date());
                confArea.append("[" + time + "] " + clientName + " : " + msg + "\n");
                confInput.setText("");
            }
        });
        // Optionnel : tu peux ajouter ici la réception des messages du groupe dans cette fenêtre
    }

    public static void main(String[] args) {
        new EchoClientGUI(); // Lance le client
    }
}
