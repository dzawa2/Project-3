import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    private static final int PORT = 5555;
    private static final int ROWS = 6;
    private static final int COLS = 7;

    // Board state: 0 means empty; 1 means PLAYER1; 2 means PLAYER2.
    private int[][] board = new int[ROWS][COLS];

    // Indicates whose turn it is: 1 for PLAYER1, 2 for PLAYER2.
    private int currentTurn = 1;

    // List of client handlers.
    private List<ClientHandler> clients = new ArrayList<>();

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Waiting for players...");

        // Initialize board to empty.
        for (int i = 0; i < ROWS; i++) {
            Arrays.fill(board[i], 0);
        }

        // Accept two clients.
        while (clients.size() < 2) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket, clients.size() + 1);
            clients.add(handler);
            handler.start();
        }

        System.out.println("Both players connected. Game starts!");
    }

    // Synchronized method to process a move if it is that player's turn.
    private synchronized void processMove(int playerNumber, int column) throws IOException {
        // Only process move if it is that player's turn.
        if (playerNumber != currentTurn) return;
        int row = getAvailableRow(column);
        if (row == -1) {
            // Column is full; could notify the player (omitted for brevity).
            return;
        }
        board[row][column] = playerNumber;
        String playerName = (playerNumber == 1 ? "PLAYER1" : "PLAYER2");
        boolean win = checkWin(row, column, playerNumber);
        GameEvent event;
        if (win) {
            event = new GameEvent(GameEvent.Type.WIN, playerName);
        } else {
            event = new GameEvent(GameEvent.Type.MOVE, column, playerName);
            // Switch the turn.
            currentTurn = (currentTurn == 1) ? 2 : 1;
        }
        broadcast(event);
    }

    // Returns the lowest available row for the specified column or -1 if full.
    private int getAvailableRow(int col) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == 0) return row;
        }
        return -1;
    }

    // Checks win in all four directions.
    private boolean checkWin(int row, int col, int player) {
        if (countConnected(row, col, 0, 1, player) + countConnected(row, col, 0, -1, player) - 1 >= 4)
            return true;
        if (countConnected(row, col, 1, 0, player) + countConnected(row, col, -1, 0, player) - 1 >= 4)
            return true;
        if (countConnected(row, col, 1, 1, player) + countConnected(row, col, -1, -1, player) - 1 >= 4)
            return true;
        if (countConnected(row, col, 1, -1, player) + countConnected(row, col, -1, 1, player) - 1 >= 4)
            return true;
        return false;
    }

    // Helper method to count consecutive pieces of the same player.
    private int countConnected(int row, int col, int deltaRow, int deltaCol, int player) {
        int count = 0;
        int r = row, c = col;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == player) {
            count++;
            r += deltaRow;
            c += deltaCol;
        }
        return count;
    }

    // Broadcasts a message (GameEvent or ChatMessage) to all clients.
    private void broadcast(Object message) throws IOException {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    // Inner class to handle each client connection.
    private class ClientHandler extends Thread {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private int playerNumber;  // 1 or 2.
        private String playerName; // "PLAYER1" or "PLAYER2".

        public ClientHandler(Socket socket, int playerNumber) throws IOException {
            this.socket = socket;
            this.playerNumber = playerNumber;
            this.playerName = (playerNumber == 1 ? "PLAYER1" : "PLAYER2");
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            // Send the player identifier to the client.
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
                    synchronized(Server.this) {
                        if (obj instanceof Integer) {
                            // A move message.
                            int column = (Integer) obj;
                            if (playerNumber == currentTurn) {
                                processMove(playerNumber, column);
                            }
                        } else if (obj instanceof ChatMessage) {
                            // A chat message; broadcast it immediately.
                            ChatMessage chat = (ChatMessage) obj;
                            broadcast(chat);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Main method to start the server.
    public static void main(String[] args) {
        try {
            new Server().startServer();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
