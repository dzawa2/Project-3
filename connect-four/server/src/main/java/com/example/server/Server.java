package com.example.server;

import com.example.common.ChatMessage;
import com.example.common.GameEvent;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5555;
    private static final int ROWS = 6, COLS = 7;

    private final int[][] board = new int[ROWS][COLS];
    private int currentTurn = 1;
    private final List<ClientHandler> clients = new ArrayList<>();

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Waiting for players...");

        Socket s1 = serverSocket.accept();
        System.out.println("Player 1 connected.");
        Socket s2 = serverSocket.accept();
        System.out.println("Player 2 connected.");

        ClientHandler h1 = new ClientHandler(s1, 1);
        ClientHandler h2 = new ClientHandler(s2, 2);
        clients.add(h1);
        clients.add(h2);

        for (int r = 0; r < ROWS; r++) Arrays.fill(board[r], 0);

        h1.start();
        h2.start();
        System.out.println("Both players connected. Game starts!");
    }

    private synchronized void processMove(int playerNum, int column) throws IOException {
        if (playerNum != currentTurn) return;
        int row = getAvailableRow(column);
        if (row < 0) return;

        board[row][column] = playerNum;
        String name = playerNum == 1 ? "PLAYER1" : "PLAYER2";
        boolean win = checkWin(row, column, playerNum);

        GameEvent ev = win
                ? new GameEvent(GameEvent.Type.WIN, name)
                : new GameEvent(GameEvent.Type.MOVE, column, name);

        if (!win) currentTurn = (currentTurn == 1 ? 2 : 1);
        broadcast(ev);
    }

    private int getAvailableRow(int col) {
        for (int r = ROWS - 1; r >= 0; r--)
            if (board[r][col] == 0) return r;
        return -1;
    }

    private boolean checkWin(int row, int col, int p) {
        return count(row,col,0,1,p) + count(row,col,0,-1,p) - 1 >= 4
                || count(row,col,1,0,p) + count(row,col,-1,0,p) - 1 >= 4
                || count(row,col,1,1,p) + count(row,col,-1,-1,p) - 1 >= 4
                || count(row,col,1,-1,p) + count(row,col,-1,1,p) - 1 >= 4;
    }

    private int count(int r, int c, int dr, int dc, int p) {
        int cnt = 0;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == p) {
            cnt++; r += dr; c += dc;
        }
        return cnt;
    }

    private void broadcast(Object msg) throws IOException {
        for (ClientHandler ch : clients) ch.send(msg);
    }

    private class ClientHandler extends Thread {
        private final ObjectOutputStream out;
        private final ObjectInputStream  in;
        private final Socket             sock;
        private final int                playerNum;
        private final String             playerName;

        ClientHandler(Socket sock, int num) throws IOException {
            this.sock       = sock;
            this.playerNum  = num;
            this.playerName = (num == 1 ? "PLAYER1" : "PLAYER2");
            this.out        = new ObjectOutputStream(sock.getOutputStream());
            this.in         = new ObjectInputStream(sock.getInputStream());
            out.writeObject(playerName);
            out.flush();
        }

        void send(Object o) throws IOException {
            out.writeObject(o);
            out.flush();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Object obj = in.readObject();
                    synchronized (Server.this) {
                        if (obj instanceof GameEvent ge) {
                            switch (ge.getType()) {
                                case SELECT -> broadcast(ge);
                                case MOVE   -> processMove(playerNum, ge.getColumn());
                                case WIN    -> broadcast(ge);
                            }
                        } else if (obj instanceof Integer col) {
                            processMove(playerNum, col);
                        } else if (obj instanceof ChatMessage cm) {
                            broadcast(cm);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            new Server().startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
