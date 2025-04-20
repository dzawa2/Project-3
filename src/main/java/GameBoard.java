// GameBoard.java
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import java.util.function.Consumer;

public class GameBoard {
    private static final int ROWS = 6, COLS = 7;
    private final StackPane root = new StackPane();
    private final GridPane grid = new GridPane();

    private final Circle[][] imageCircles   = new Circle[ROWS][COLS];
    private final Circle[][] overlayCircles = new Circle[ROWS][COLS];

    private boolean myTurn;
    private boolean gameOver = false;
    private final String playerId;
    private final Consumer<Integer> moveSender;

    private Image myImage, oppImage;

    // semi-transparent tints
    private static final Color TINT_P1 = Color.rgb(255,   0,   0, 0.4);
    private static final Color TINT_P2 = Color.rgb(  0,   0, 255, 0.4);

    public GameBoard(String playerId,
                     Consumer<Integer> moveSender,
                     Image myImage,
                     Image oppImage)
    {
        this.playerId   = playerId;
        this.moveSender = moveSender;
        this.myImage    = myImage;
        this.oppImage   = oppImage;
        this.myTurn     = playerId.equals("PLAYER1");

        root.setAlignment(Pos.CENTER);
        StackPane.setAlignment(grid, Pos.CENTER);
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(5);
        grid.setVgap(5);

        for (int c = 0; c < COLS; c++) {
            VBox colBox = new VBox();
            colBox.setAlignment(Pos.CENTER);
            final int col = c;
            colBox.setOnMouseClicked(e -> {
                if (myTurn && !gameOver) {
                    moveSender.accept(col);
                }
            });

            for (int r = 0; r < ROWS; r++) {
                Circle imgCirc = new Circle(40, Color.WHITE);
                imgCirc.setStroke(Color.BLACK);

                Circle overlay = new Circle(40, Color.TRANSPARENT);
                overlay.setMouseTransparent(true);

                imageCircles[r][c]   = imgCirc;
                overlayCircles[r][c] = overlay;

                StackPane cell = new StackPane(imgCirc, overlay);
                colBox.getChildren().add(cell);
            }

            grid.add(colBox, c, 0);
        }

        root.getChildren().add(grid);
    }

    public StackPane getRoot() {
        return root;
    }

    /** update when SELECT messages arrive */
    public void updateImages(Image myImg, Image oppImg) {
        this.myImage  = myImg;
        this.oppImage = oppImg;
    }

    /**
     * Place a piece: your planet for your moves, opponent's for theirs,
     * tinted red if PLAYER1, blue if PLAYER2.
     */
    public void placePiece(int col, String movingPlayer) {
        boolean isMyMove = movingPlayer.equals(playerId);
        ImagePattern pattern = new ImagePattern(isMyMove ? myImage : oppImage);
        Color tint = movingPlayer.equals("PLAYER1") ? TINT_P1 : TINT_P2;

        for (int r = ROWS - 1; r >= 0; r--) {
            if (imageCircles[r][col].getFill().equals(Color.WHITE)) {
                imageCircles[r][col].setFill(pattern);
                overlayCircles[r][col].setFill(tint);

                if (!gameOver) {
                    myTurn = !isMyMove;
                }
                break;
            }
        }
    }

    /** you can leave your existing win-popup or swap in an end-scene */
    public void showWinMessage(String message) {
        gameOver = true;
        // … your existing popup or scene swap …
    }
}
