import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;

public class Client extends Application {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerId;
    private GameBoard board;  // The JavaFX board UI.

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Connect to the server.
        Socket socket = new Socket("localhost", 5555);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // Read the assigned player ID.
        playerId = (String) in.readObject();

        // Initialize the board. The board uses a callback (moveSender) to send moves.
        board = new GameBoard(playerId, this::sendMove);

        Scene scene = new Scene(board.getRoot(), 700, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Connect Four - " + playerId);
        primaryStage.show();

        // Start a thread to listen for GameEvents from the server.
        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof GameEvent) {
                        GameEvent event = (GameEvent) obj;
                        Platform.runLater(() -> processGameEvent(event));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Processes incoming GameEvents (either MOVE or WIN).
     */
    private void processGameEvent(GameEvent event) {
        if (event.getType() == GameEvent.Type.MOVE) {
            // Determine if the move came from the local player.
            boolean isMyMove = event.getMovingPlayer().equals(playerId);
            board.placePiece(event.getColumn(), isMyMove);
        } else if (event.getType() == GameEvent.Type.WIN) {
            String winningPlayer = event.getWinningPlayer();
            String message;
            if (playerId.equals(winningPlayer)) {
                message = "You win! (" + winningPlayer + " wins)";
            } else {
                message = "You lose! (" + winningPlayer + " wins)";
            }
            board.showWinMessage(message);
        }
    }

    /**
     * Sends a move (the chosen column) to the server.
     */
    private void sendMove(int column) {
        try {
            out.writeObject(column);
            out.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
