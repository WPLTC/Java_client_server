public class User {
    private String username;
    private String password;
    private boolean connected;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.connected = false;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
} 