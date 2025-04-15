import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.function.Consumer;

public class GameBoard {
    private static final int ROWS = 6;
    private static final int COLS = 7;

    // We'll use a StackPane so we can center the GridPane
    private StackPane root = new StackPane();
    private GridPane grid = new GridPane();

    private Circle[][] circles = new Circle[ROWS][COLS];
    private boolean myTurn;
    private String playerId;
    private Consumer<Integer> moveSender;
    private boolean gameOver = false;

    public GameBoard(String playerId, Consumer<Integer> moveSender) {
        this.playerId = playerId;
        this.moveSender = moveSender;
        this.myTurn = playerId.equals("PLAYER1");

        // Make the StackPane itself center its child
        root.setAlignment(Pos.CENTER);

        // Ensure the GridPane is centered within the StackPane
        StackPane.setAlignment(grid, Pos.CENTER);

        // Have the GridPane center its children as well
        grid.setAlignment(Pos.CENTER);

        // (Optional) Add spacing if you like
        // grid.setHgap(5);
        // grid.setVgap(5);

        // Populate the GridPane
        for (int col = 0; col < COLS; col++) {
            final int column = col;
            VBox columnBox = new VBox();
            // Center each column of circles
            columnBox.setAlignment(Pos.CENTER);

            // On click, only send the move to the server (don't place piece locally)
            columnBox.setOnMouseClicked(e -> {
                if (myTurn && !gameOver) {
                    moveSender.accept(column);
                }
            });

            // Fill this column with circle "cells"
            for (int row = 0; row < ROWS; row++) {
                Circle circle = new Circle(40);
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);
                circles[row][col] = circle;
                columnBox.getChildren().add(circle);
            }
            grid.add(columnBox, col, 0);
        }

        // Add the GridPane to the StackPane
        root.getChildren().add(grid);
    }

    public StackPane getRoot() {
        return root;
    }

    /**
     * Places a piece in the given column when notified by the server.
     * @param col the column index
     * @param isMyMove true if local player’s move, false if opponent’s
     */
    public void placePiece(int col, boolean isMyMove) {
        Color pieceColor = isMyMove ? Color.RED : Color.YELLOW;
        for (int row = ROWS - 1; row >= 0; row--) {
            if (circles[row][col].getFill().equals(Color.WHITE)) {
                circles[row][col].setFill(pieceColor);
                if (!gameOver) {
                    myTurn = !isMyMove;
                }
                break;
            }
        }
    }

    /**
     * Displays a dialog with a win/lose message.
     */
    public void showWinMessage(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
