import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;

public class Client extends Application {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerId;
    private GameBoard board;   // The game board UI component.

    // Chat UI components.
    private TextArea chatArea;
    private TextField chatInput;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Connect to the server.
        Socket socket = new Socket("localhost", 5555);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // Read assigned player ID from the server.
        playerId = (String) in.readObject();

        // Create the game board.
        board = new GameBoard(playerId, this::sendMove);

        // Create chat UI.
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(150);
        chatInput = new TextField();
        chatInput.setPromptText("Enter your message...");
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendChat());
        HBox chatInputBox = new HBox(10, chatInput, sendButton);
        chatInputBox.setHgrow(chatInput, Priority.ALWAYS);
        chatInputBox.setPadding(new Insets(10));

        VBox chatBox = new VBox(5, chatArea, chatInputBox);
        chatBox.setPrefHeight(200);

        // Combine game board and chat UI into one layout.
        BorderPane rootPane = new BorderPane();
        rootPane.setCenter(board.getRoot());
        rootPane.setBottom(chatBox);

        Scene scene = new Scene(rootPane, 700, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Connect Four - " + playerId);
        primaryStage.show();

        // Start a thread to listen for messages from the server.
        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    Platform.runLater(() -> processServerMessage(obj));
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processServerMessage(Object obj) {
        if (obj instanceof GameEvent) {
            GameEvent event = (GameEvent) obj;
            if (event.getType() == GameEvent.Type.MOVE) {
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
        } else if (obj instanceof ChatMessage) {
            ChatMessage chat = (ChatMessage) obj;
            chatArea.appendText(chat.getSender() + ": " + chat.getMessage() + "\n");
        }
    }

    // Sends a game move to the server.
    private void sendMove(int column) {
        try {
            out.writeObject(column);
            out.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // Sends a chat message to the server.
    private void sendChat() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            ChatMessage chatMsg = new ChatMessage(playerId, msg);
            try {
                out.writeObject(chatMsg);
                out.flush();
            } catch(IOException e) {
                e.printStackTrace();
            }
            chatInput.clear();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
