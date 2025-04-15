import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Label;
import java.util.function.Consumer;

public class GameBoard {
    private static final int ROWS = 6;
    private static final int COLS = 7;

    private Pane root = new Pane();
    private Circle[][] circles = new Circle[ROWS][COLS];
    private boolean myTurn;  // indicates if it's this client's turn
    private String playerId;
    private Consumer<Integer> moveSender;

    // Constructor – set initial turn based on player identity:
    // For example, you might let PLAYER1 start first.
    public GameBoard(String playerId, Consumer<Integer> moveSender) {
        this.playerId = playerId;
        this.moveSender = moveSender;
        // Assume PLAYER1 starts first.
        this.myTurn = playerId.equals("PLAYER1");

        GridPane grid = new GridPane();
        for (int col = 0; col < COLS; col++) {
            final int column = col;
            VBox columnBox = new VBox();
            columnBox.setOnMouseClicked(e -> handleMove(column));
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

    // When the user makes a move by clicking on a column:
    private void handleMove(int col) {
        if (!myTurn) return;
        // Instead of setting the circle fill directly, delegate to placePiece.
        for (int row = ROWS - 1; row >= 0; row--) {
            if (circles[row][col].getFill() == Color.WHITE) {
                placePiece(col, true);  // local move, isMyMove is true.
                moveSender.accept(col);
                break;
            }
        }
    }

    /**
     * Place a piece on the board.
     * @param col the column in which to place the piece.
     * @param isMyMove true if the move is made locally; false if received from opponent.
     */
    public void placePiece(int col, boolean isMyMove) {
        // Determine the piece color: local moves are red, remote moves are yellow.
        Color pieceColor = (isMyMove ? Color.RED : Color.YELLOW);
        for (int row = ROWS - 1; row >= 0; row--) {
            if (circles[row][col].getFill() == Color.WHITE) {
                circles[row][col].setFill(pieceColor);
                // Update turn: if you just made a move, it becomes false.
                // If an opponent's move was received, it's now your turn.
                myTurn = !isMyMove;
                break;
            }
        }
    }

    // Optional: For debugging, you can expose the current turn.
    public boolean isMyTurn() {
        return myTurn;
    }
}
