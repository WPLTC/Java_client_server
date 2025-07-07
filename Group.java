import java.util.HashSet;
import java.util.Set;

public class Group {
    private String name;
    private String moderator;
    private Set<String> members;

    public Group(String name, String moderator) {
        this.name = name;
        this.moderator = moderator;
        this.members = new HashSet<>();
        this.members.add(moderator);
    }

    public String getName() {
        return name;
    }

    public String getModerator() {
        return moderator;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void addMember(String username) {
        members.add(username);
        System.out.println("Membres du groupe " + name + " : " + members);
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    public boolean isMember(String username) {
        for (String member : members) {
            if (member.equalsIgnoreCase(username)) return true;
        }
        return false;
    }
} 