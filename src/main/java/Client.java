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
    private GameBoard board;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Socket socket = new Socket("localhost", 5555);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        playerId = (String) in.readObject();

        board = new GameBoard(playerId, this::sendMove);

        // Thread to listen for opponent's move
        new Thread(() -> {
            try {
                while (true) {
                    int column = ((Integer) in.readObject());
                    Platform.runLater(() -> board.placePiece(column, false));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Scene scene = new Scene(board.  getRoot(), 700, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Connect Four - " + playerId);
        primaryStage.show();
    }

    private void sendMove(int column) {
        try {
            out.writeObject(column); // int auto-boxed to Integer
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
