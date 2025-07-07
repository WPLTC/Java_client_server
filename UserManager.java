import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.*;

public class UserManager {
    private Map<String, User> users = new HashMap<>();
    private static final String USERS_FILE = "users.txt";

    public UserManager() {
        loadUsers();
    }

    public boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, password));
        saveUsers();
        return true;
    }

    public boolean login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            user.setConnected(true);
            return true;
        }
        return false;
    }

    public void logout(String username) {
        User user = users.get(username);
        if (user != null) user.setConnected(false);
    }

    public Set<String> getConnectedUsers() {
        return users.entrySet().stream()
            .filter(e -> e.getValue().isConnected())
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toSet());
    }

    public boolean isUserConnected(String username) {
        User user = users.get(username);
        return user != null && user.isConnected();
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    private void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users.values()) {
                bw.write(user.getUsername() + ":" + user.getPassword());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    users.put(parts[0], new User(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 