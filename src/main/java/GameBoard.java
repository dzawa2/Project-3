import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.util.function.Consumer;

public class GameBoard {
    private static final int ROWS = 6;
    private static final int COLS = 7;

    private Pane root = new Pane();
    private Circle[][] circles = new Circle[ROWS][COLS];
    private boolean myTurn;  // true if it is this client's turn.
    private String playerId;
    private Consumer<Integer> moveSender;
    private boolean gameOver = false;

    public GameBoard(String playerId, Consumer<Integer> moveSender) {
        this.playerId = playerId;
        this.moveSender = moveSender;
        // Assume PLAYER1 starts first.
        this.myTurn = playerId.equals("PLAYER1");

        GridPane grid = new GridPane();
        for (int col = 0; col < COLS; col++) {
            final int column = col;
            VBox columnBox = new VBox();
            // --- UPDATED: Do not update board immediately on click ---
            columnBox.setOnMouseClicked(e -> {
                if (myTurn && !gameOver) {
                    // Instead of placing the piece locally, simply send the move.
                    moveSender.accept(column);
                }
            });
            for (int row = 0; row < ROWS; row++) {
                Circle circle = new Circle(40);
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);
                circles[row][col] = circle;
                columnBox.getChildren().add(circle);
            }
            grid.add(columnBox, col, 0);
        }
        root.getChildren().add(grid);
    }

    public Pane getRoot() {
        return root;
    }

    /**
     * Updates the board UI by placing a piece in the specified column.
     * This method is called when processing a GameEvent from the server.
     * @param col The column where the piece should be placed.
     * @param isMyMove true if this move is from the local player; false if from opponent.
     */
    public void placePiece(int col, boolean isMyMove) {
        Color pieceColor = (isMyMove ? Color.RED : Color.YELLOW);
        // Place the piece in the lowest available row.
        for (int row = ROWS - 1; row >= 0; row--) {
            if (circles[row][col].getFill().equals(Color.WHITE)) {
                circles[row][col].setFill(pieceColor);
                // Toggle turn only if the game is not over.
                if (!gameOver) {
                    myTurn = !isMyMove;
                }
                break;
            }
        }
    }

    /**
     * Displays a win/lose message.
     */
    public void showWinMessage(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Optional: Provide current turn status for external use.
    public boolean isMyTurn() {
        return myTurn;
    }
}
