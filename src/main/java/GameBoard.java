// GameBoard.java
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.util.function.Consumer;

public class GameBoard {
    private static final int ROWS = 6, COLS = 7;
    private final StackPane root = new StackPane();
    private final GridPane grid = new GridPane();
    private final Circle[][] circles = new Circle[ROWS][COLS];

    private boolean myTurn;
    private boolean gameOver = false;
    private final String playerId;
    private final Consumer<Integer> moveSender;

    private Image myImage, oppImage;

    public GameBoard(String playerId,
                     Consumer<Integer> moveSender,
                     Image myImage,
                     Image oppImage) {
        this.playerId   = playerId;
        this.moveSender = moveSender;
        this.myImage    = myImage;
        this.oppImage   = oppImage;
        this.myTurn     = playerId.equals("PLAYER1");

        root.setAlignment(Pos.CENTER);
        StackPane.setAlignment(grid, Pos.CENTER);
        grid.setAlignment(Pos.CENTER);

        for (int c = 0; c < COLS; c++) {
            final int col = c;
            VBox colBox = new VBox();
            colBox.setAlignment(Pos.CENTER);
            colBox.setOnMouseClicked(e -> {
                if (myTurn && !gameOver) moveSender.accept(col);
            });
            for (int r = 0; r < ROWS; r++) {
                Circle circ = new Circle(40, Color.WHITE);
                circ.setStroke(Color.BLACK);
                circles[r][c] = circ;
                colBox.getChildren().add(circ);
            }
            grid.add(colBox, c, 0);
        }
        root.getChildren().add(grid);
    }

    public StackPane getRoot() { return root; }

    /** Update both players’ images on a SELECT event */
    public void updateImages(Image myImg, Image oppImg) {
        this.myImage  = myImg;
        this.oppImage = oppImg;
    }

    public void placePiece(int col, boolean isMyMove) {
        ImagePattern pat = new ImagePattern(isMyMove ? myImage : oppImage);
        for (int r = ROWS - 1; r >= 0; r--) {
            if (circles[r][col].getFill().equals(Color.WHITE)) {
                circles[r][col].setFill(pat);
                if (!gameOver) myTurn = !isMyMove;
                break;
            }
        }
    }

    public void showWinMessage(String msg) {
        gameOver = true;
        Alert alert = new Alert(AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
