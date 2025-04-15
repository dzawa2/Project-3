import java.io.Serializable;

public class GameEvent implements Serializable {
    public enum Type { MOVE, WIN }

    private Type type;
    private int column;           // For MOVE events.
    private String movingPlayer;  // For MOVE events.
    private String winningPlayer; // For WIN events.

    // Constructor for MOVE event.
    public GameEvent(Type type, int column, String movingPlayer) {
        this.type = type;
        this.column = column;
        this.movingPlayer = movingPlayer;
    }

    // Constructor for WIN event.
    public GameEvent(Type type, String winningPlayer) {
        this.type = type;
        this.winningPlayer = winningPlayer;
    }

    public Type getType() {
        return type;
    }

    public int getColumn() {
        return column;
    }

    public String getMovingPlayer() {
        return movingPlayer;
    }

    public String getWinningPlayer() {
        return winningPlayer;
    }
}
