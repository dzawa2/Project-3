
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.Consumer;

public class GameBoard {
    private static final int ROWS = 6, COLS = 7;
    private final StackPane root = new StackPane();
    private final GridPane grid = new GridPane();
    private final BorderPane layoutPane = new BorderPane();

    // 0 = empty, 1 = PLAYER1, 2 = PLAYER2
    private final int[][] board = new int[ROWS][COLS];

    VBox leftBox;
    private final Circle[][] imageCircles = new Circle[ROWS][COLS];
    private final Circle[][] overlayCircles = new Circle[ROWS][COLS];
    private final Circle turnIndicator;
    private Label timerLabel;
    private Timeline timer;
    private int timeLeft = 30;
    private boolean myTurn;
    private boolean gameOver = false;

    private String player1, player2,currentPlayer, myUsername;

    private Image player1Img, player2Img;
    Consumer<GameEvent> moveSender;

    // semi-transparent tints
    private static final Color TINT_P1 = Color.rgb(255, 0, 0, 0.4);
    private static final Color TINT_P2 = Color.rgb(0, 0, 255, 0.4);

    public GameBoard(String myUsername, String player1, String player2, String player1ImgPath,String player2ImgPath,Consumer<GameEvent> moveSender) {
        this.myUsername = myUsername;
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        this.myTurn = myUsername.equals(player1);

        turnIndicator = new Circle(40); // radius

        this.player1Img = new Image(getClass().getResourceAsStream(player1ImgPath));
        this.player2Img = new Image(getClass().getResourceAsStream(player2ImgPath));

        updateTurnIndicator();

        timerLabel = new Label("Time left: 0s");
        timerLabel.setStyle("-fx-background-color: white; -fx-font-weight: bold");
        setupTimer();

        this.moveSender = moveSender;

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
                if (!gameOver) {
                    try {
                        processMove(myUsername,col,myUsername.equals(player1) ? 1 : 2);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            for (int r = 0; r < ROWS; r++) {
                Circle imgCirc = new Circle(40, Color.WHITE);
                imgCirc.setStroke(Color.BLACK);

                Circle overlay = new Circle(40, Color.TRANSPARENT);
                overlay.setMouseTransparent(true);

                imageCircles[r][c] = imgCirc;
                overlayCircles[r][c] = overlay;

                StackPane cell = new StackPane(imgCirc, overlay);
                colBox.getChildren().add(cell);
            }

            grid.add(colBox, c, 0);
        }

        leftBox = new VBox(10,timerLabel,turnIndicator);
        leftBox.setAlignment(Pos.CENTER);

        layoutPane.setLeft(leftBox);
        layoutPane.setCenter(grid);

        root.getChildren().add(layoutPane);
    }

    public StackPane getRoot () {
        return root;
    }
    public VBox getLeftBox () {
        return leftBox;
    }

    private void updateTurnIndicator() {
        ImagePattern pattern = currentPlayer.equals(player1) ? new ImagePattern(player1Img) : new ImagePattern(player2Img);
        Color tint = currentPlayer.equals(player1) ? TINT_P1 : TINT_P2;

        turnIndicator.setFill(pattern);
        turnIndicator.setStroke(tint);
        turnIndicator.setStrokeWidth(4);
    }
    /**
     * Place a piece: your planet for your moves, opponent's for theirs,
     * tinted red if PLAYER1, blue if PLAYER2.
     */
    public void placePiece ( String movingPlayer, int col){
        ImagePattern pattern = new ImagePattern(movingPlayer.equals(player1) ? player1Img : player2Img);
        Color tint = currentPlayer.equals(player1) ? TINT_P1 : TINT_P2;

        for (int r = ROWS - 1; r >= 0; r--) {
            if (imageCircles[r][col].getFill().equals(Color.WHITE)) {
                imageCircles[r][col].setFill(pattern);
                overlayCircles[r][col].setFill(tint);

                if (!gameOver) {
                    currentPlayer = movingPlayer.equals(player1) ? player2 : player1;
                }
                updateTurnIndicator();
                onYourTurnStart();
                break;
            }
        }
    }

    public void processMove(String playerName, int column, int playerNum) throws IOException {

        if (!playerName.equals(currentPlayer)) return;
        int row = getAvailableRow(column);
        if (row < 0) return; // full

        board[row][column] = playerNum;
        boolean win = checkWin(row, column, playerNum);

        GameEvent ev;
        if (win) {
            ev = new GameEvent(GameEvent.Type.WIN, playerName);
        } else {
            ev = new GameEvent(GameEvent.Type.MOVE, column, playerName);
            placePiece(playerName, column);
        }
        moveSender.accept(ev);
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
    /** you can leave your existing win-popup or swap in an end-scene */
    public void showWinMessage (String message){
        gameOver = true;
        // … your existing popup or scene swap …
    }

    private void setupTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (timeLeft > 0) {
                timeLeft--;
                timerLabel.setText("Time left: " + timeLeft + "s");
                if (timeLeft <= 5) {
                    timerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    timerLabel.setStyle("-fx-text-fill: black;");
                }
            } else {
                timer.stop();
                timerLabel.setText("Time's up!");
                GameEvent forfeit = new GameEvent(GameEvent.Type.WIN, myUsername.equals(player1) ? player2 : player1);
                moveSender.accept(forfeit);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
    }
    public void onYourTurnStart() {
        timerLabel.setText("Time left: " + timeLeft + "s");
        timer.play();
    }

    public void onYourTurnEnd() {
        timeLeft += 30;

        timer.pause();
    }
}

