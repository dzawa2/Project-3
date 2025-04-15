import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Server {
    private static final int PORT = 5555;
    private static final int ROWS = 6;
    private static final int COLS = 7;

    // Board state: 0 = empty, 1 = PLAYER1, 2 = PLAYER2.
    private int[][] board = new int[ROWS][COLS];

    private Socket player1Socket, player2Socket;
    private ObjectOutputStream out1, out2;
    private ObjectInputStream in1, in2;

    public void startServer() throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Waiting for players...");

        // Accept PLAYER1 connection.
        player1Socket = serverSocket.accept();
        out1 = new ObjectOutputStream(player1Socket.getOutputStream());
        in1 = new ObjectInputStream(player1Socket.getInputStream());
        out1.writeObject("PLAYER1");
        out1.flush();

        // Accept PLAYER2 connection.
        player2Socket = serverSocket.accept();
        out2 = new ObjectOutputStream(player2Socket.getOutputStream());
        in2 = new ObjectInputStream(player2Socket.getInputStream());
        out2.writeObject("PLAYER2");
        out2.flush();

        // Initialize board to empty.
        for (int i = 0; i < ROWS; i++) {
            Arrays.fill(board[i], 0);
        }

        runGame();
    }

    private void runGame() throws IOException, ClassNotFoundException {
        boolean gameRunning = true;
        int currentPlayer = 1; // 1 for PLAYER1; 2 for PLAYER2.

        while (gameRunning) {
            int column = -1;
            // Read move from the current player's input stream.
            if (currentPlayer == 1) {
                Object obj = in1.readObject();
                if (obj instanceof Integer) {
                    column = (Integer) obj;
                }
            } else {
                Object obj = in2.readObject();
                if (obj instanceof Integer) {
                    column = (Integer) obj;
                }
            }

            // Place the piece in the board if possible.
            int row = getAvailableRow(column);
            if (row == -1) {
                // Column is full; you might notify the player.
                continue;
            }
            board[row][column] = currentPlayer;

            // Identify the player string.
            String playerStr = (currentPlayer == 1 ? "PLAYER1" : "PLAYER2");
            // Check for win.
            boolean win = checkWin(row, column, currentPlayer);
            GameEvent event;
            if (win) {
                event = new GameEvent(GameEvent.Type.WIN, playerStr);
                gameRunning = false;
            } else {
                event = new GameEvent(GameEvent.Type.MOVE, column, playerStr);
            }

            // Broadcast event to both clients.
            sendToBoth(event);

            // Swap turn if game still running.
            if (gameRunning) {
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
            }
        }
        System.out.println("Game over.");
    }

    // Returns the lowest available row in the given column, or -1 if the column is full.
    private int getAvailableRow(int col) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == 0) {
                return row;
            }
        }
        return -1;
    }

    // Checks for a connect four in all directions.
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

    // Sends the GameEvent to both players.
    private void sendToBoth(GameEvent event) throws IOException {
        out1.writeObject(event);
        out1.flush();
        out2.writeObject(event);
        out2.flush();
    }

    public static void main(String[] args) {
        try {
            new Server().startServer();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
