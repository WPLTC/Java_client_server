import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MessageStore {
    private List<Message> messages = new ArrayList<>();
    private String filePath = "messages.txt";

    public MessageStore() {
        loadMessages();
    }

    public void addMessage(Message message) {
        messages.add(message);
        saveMessageToFile(message);
    }

    public List<Message> getMessages() {
        return messages;
    }

    private void saveMessageToFile(Message message) {
        try (FileWriter fw = new FileWriter(filePath, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(message.getTimestamp() + "|" + message.getSender() + "|" + message.getRecipient() + "|" + message.getContent());
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMessages() {
        File file = new File(filePath);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 4);
                if (parts.length == 4) {
                    Message msg = new Message(parts[1], parts[2], parts[3]);
                    messages.add(msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 