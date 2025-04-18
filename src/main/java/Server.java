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

    private int[][] board = new int[ROWS][COLS];
    private int currentTurn = 1;  // 1 = PLAYER1, 2 = PLAYER2
    private final List<ClientHandler> clients = new ArrayList<>();

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Waiting for players...");

        // Block until two clients connect
        Socket socket1 = serverSocket.accept();
        System.out.println("Player 1 connected.");
        Socket socket2 = serverSocket.accept();
        System.out.println("Player 2 connected.");

        // Create handlers & register
        ClientHandler handler1 = new ClientHandler(socket1, 1);
        ClientHandler handler2 = new ClientHandler(socket2, 2);
        clients.add(handler1);
        clients.add(handler2);

        // Initialize board
        for (int i = 0; i < ROWS; i++) {
            Arrays.fill(board[i], 0);
        }

        // Start listening threads
        handler1.start();
        handler2.start();

        System.out.println("Both players connected. Game starts!");
    }

    private synchronized void processMove(int playerNumber, int column) throws IOException {
        if (playerNumber != currentTurn) return;

        int row = getAvailableRow(column);
        if (row == -1) return;  // column full

        board[row][column] = playerNumber;
        String playerName = (playerNumber == 1 ? "PLAYER1" : "PLAYER2");
        boolean win = checkWin(row, column, playerNumber);

        GameEvent event;
        if (win) {
            event = new GameEvent(GameEvent.Type.WIN, playerName);
        } else {
            event = new GameEvent(GameEvent.Type.MOVE, column, playerName);
            currentTurn = (currentTurn == 1 ? 2 : 1);
        }
        broadcast(event);
    }

    private int getAvailableRow(int col) {
        for (int r = ROWS - 1; r >= 0; r--) {
            if (board[r][col] == 0) return r;
        }
        return -1;
    }

    private boolean checkWin(int row, int col, int player) {
        if (countConnected(row, col, 0, 1, player) + countConnected(row, col, 0, -1, player) - 1 >= 4) return true;
        if (countConnected(row, col, 1, 0, player) + countConnected(row, col, -1, 0, player) - 1 >= 4) return true;
        if (countConnected(row, col, 1, 1, player) + countConnected(row, col, -1, -1, player) - 1 >= 4) return true;
        if (countConnected(row, col, 1, -1, player) + countConnected(row, col, -1, 1, player) - 1 >= 4) return true;
        return false;
    }

    private int countConnected(int row, int col, int dRow, int dCol, int player) {
        int count = 0, r = row, c = col;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == player) {
            count++;
            r += dRow;
            c += dCol;
        }
        return count;
    }

    private void broadcast(Object message) throws IOException {
        for (ClientHandler c : clients) {
            c.send(message);
        }
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;
        private final int playerNumber;
        private final String playerName;

        public ClientHandler(Socket socket, int playerNumber) throws IOException {
            this.socket = socket;
            this.playerNumber = playerNumber;
            this.playerName = (playerNumber == 1 ? "PLAYER1" : "PLAYER2");
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in  = new ObjectInputStream(socket.getInputStream());
            // send identifier
            out.writeObject(playerName);
            out.flush();
        }

        public void send(Object obj) throws IOException {
            out.writeObject(obj);
            out.flush();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Object obj = in.readObject();
                    synchronized (Server.this) {
                        if (obj instanceof GameEvent) {
                            GameEvent ge = (GameEvent) obj;
                            if (ge.getType() == GameEvent.Type.SELECT) {
                                // broadcast selection to both
                                broadcast(ge);
                            } else if (ge.getType() == GameEvent.Type.MOVE) {
                                processMove(playerNumber, ge.getColumn());
                            } else if (ge.getType() == GameEvent.Type.WIN) {
                                broadcast(ge);
                            }
                        } else if (obj instanceof Integer) {
                            // legacy move
                            processMove(playerNumber, (Integer) obj);
                        } else if (obj instanceof ChatMessage) {
                            broadcast(obj);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
