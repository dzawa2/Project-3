package com.example.common;

import java.io.Serializable;

public class GameEvent implements Serializable {
    public enum Type { SELECT, MOVE, WIN }

    private final Type type;
    private final int column;
    private final String movingPlayer;
    private final String winningPlayer;
    private final String planetPath;
    private final String selectingPlayer;

    // SELECT
    public GameEvent(Type type, String planetPath, String selectingPlayer) {
        this.type             = type;
        this.planetPath       = planetPath;
        this.selectingPlayer  = selectingPlayer;
        this.column           = -1;
        this.movingPlayer     = null;
        this.winningPlayer    = null;
    }

    // MOVE
    public GameEvent(Type type, int column, String movingPlayer) {
        this.type             = type;
        this.column           = column;
        this.movingPlayer     = movingPlayer;
        this.planetPath       = null;
        this.selectingPlayer  = null;
        this.winningPlayer    = null;
    }

    // WIN
    public GameEvent(Type type, String winningPlayer) {
        this.type             = type;
        this.winningPlayer    = winningPlayer;
        this.column           = -1;
        this.movingPlayer     = null;
        this.planetPath       = null;
        this.selectingPlayer  = null;
    }

    public Type   getType()             { return type; }
    public int    getColumn()           { return column; }
    public String getMovingPlayer()     { return movingPlayer; }
    public String getWinningPlayer()    { return winningPlayer; }
    public String getPlanetPath()       { return planetPath; }
    public String getSelectingPlayer()  { return selectingPlayer; }
}
