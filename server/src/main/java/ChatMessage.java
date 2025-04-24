
import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String sender;
    private String message;
    private int playerId;

    public ChatMessage(String sender, String message, int playerId) {
        this.sender = sender;
        this.message = message;
        this.playerId = playerId;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public int getPlayerId(){return playerId;}

}
