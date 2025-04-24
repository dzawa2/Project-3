import java.io.Serializable;

public class GameEvent implements Serializable {
    public enum Type { START, MOVE, WIN }

    private Type type;
    private int column;               // for MOVE
    private String movingPlayer;      // for MOVE
    private String winningPlayer;     // for WIN
    private String planetPath;        // for SELECT
    private int playerId;

    //START constructor
    public GameEvent (Type type, String opponent, String opponentPath ,int playerId) {
        this.type = type;
        this.movingPlayer = opponent;
        this.planetPath = opponentPath;
        this.playerId = playerId;
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
    public int getPlayerId() { return playerId; }
    public String getMovingPlayer() { return movingPlayer; }
    public String getWinningPlayer() { return winningPlayer; }
    public String getOpponent() { return movingPlayer; }
    public String getPlanetPath() { return planetPath; }
}
