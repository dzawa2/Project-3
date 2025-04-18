// GameEvent.java
import java.io.Serializable;

public class GameEvent implements Serializable {
    public enum Type { SELECT, MOVE, WIN }

    private Type type;
    private int column;               // for MOVE
    private String movingPlayer;      // for MOVE
    private String winningPlayer;     // for WIN
    private String planetPath;        // for SELECT
    private String selectingPlayer;   // for SELECT

    // SELECT constructor
    public GameEvent(Type type, String planetPath, String selectingPlayer) {
        this.type = type;
        this.planetPath = planetPath;
        this.selectingPlayer = selectingPlayer;
    }

    // MOVE constructor
    public GameEvent(Type type, int column, String movingPlayer) {
        this.type = type;
        this.column = column;
        this.movingPlayer = movingPlayer;
    }

    // WIN constructor
    public GameEvent(Type type, String winningPlayer) {
        this.type = type;
        this.winningPlayer = winningPlayer;
    }

    public Type getType() { return type; }
    public int getColumn() { return column; }
    public String getMovingPlayer() { return movingPlayer; }
    public String getWinningPlayer() { return winningPlayer; }
    public String getPlanetPath() { return planetPath; }
    public String getSelectingPlayer() { return selectingPlayer; }
}
