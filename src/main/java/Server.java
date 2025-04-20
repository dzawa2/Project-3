// Server.java
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    private static final int PORT = 5555;
    private static final int ROWS = 6;
    private static final int COLS = 7;

    // 0 = empty, 1 = PLAYER1, 2 = PLAYER2
    private final int[][] board = new int[ROWS][COLS];
    private int currentTurn = 1;

    private final List<ClientHandler> clients = new ArrayList<>();

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Waiting for players...");

        // block until two players connect
        Socket s1 = serverSocket.accept();
        System.out.println("Player 1 connected.");
        Socket s2 = serverSocket.accept();
        System.out.println("Player 2 connected.");

        // create handlers
        ClientHandler h1 = new ClientHandler(s1, 1);
        ClientHandler h2 = new ClientHandler(s2, 2);
        clients.add(h1);
        clients.add(h2);

        // init board
        for (int r = 0; r < ROWS; r++) Arrays.fill(board[r], 0);

        // start threads
        h1.start();
        h2.start();

        System.out.println("Both players connected. Game starts!");
    }

    private synchronized void processMove(int playerNum, int column) throws IOException {
        if (playerNum != currentTurn) return;
        int row = getAvailableRow(column);
        if (row < 0) return; // full

        board[row][column] = playerNum;
        String name = playerNum == 1 ? "PLAYER1" : "PLAYER2";
        boolean win = checkWin(row, column, playerNum);

        GameEvent ev;
        if (win) {
            ev = new GameEvent(GameEvent.Type.WIN, name);
        } else {
            ev = new GameEvent(GameEvent.Type.MOVE, column, name);
            currentTurn = (currentTurn == 1 ? 2 : 1);
        }
        broadcast(ev);
    }

    private int getAvailableRow(int col) {
        for (int r = ROWS - 1; r >= 0; r--) {
            if (board[r][col] == 0) return r;
        }
        return -1;
    }

    private boolean checkWin(int row, int col, int player) {
        return count(row, col, 0,1, player) + count(row, col, 0,-1, player) - 1 >= 4
                || count(row, col, 1,0, player) + count(row, col, -1,0, player) - 1 >= 4
                || count(row, col, 1,1, player) + count(row, col, -1,-1, player) - 1 >= 4
                || count(row, col, 1,-1, player)+ count(row, col, -1,1, player) - 1 >= 4;
    }

    private int count(int r, int c, int dr, int dc, int p) {
        int cnt = 0;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == p) {
            cnt++; r += dr; c += dc;
        }
        return cnt;
    }

    private void broadcast(Object msg) throws IOException {
        for (ClientHandler ch : clients) {
            ch.send(msg);
        }
    }

    private class ClientHandler extends Thread {
        private final Socket sock;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;
        private final int playerNum;
        private final String playerName;

        ClientHandler(Socket sock, int num) throws IOException {
            this.sock = sock;
            this.playerNum = num;
            this.playerName = num == 1 ? "PLAYER1" : "PLAYER2";
            this.out = new ObjectOutputStream(sock.getOutputStream());
            this.in  = new ObjectInputStream(sock.getInputStream());
            // send identifier
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
                                case SELECT:
                                    // broadcast selection to both
                                    broadcast(ge);
                                    break;
                                case MOVE:
                                    processMove(playerNum, ge.getColumn());
                                    break;
                                case WIN:
                                    broadcast(ge);
                                    break;
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
