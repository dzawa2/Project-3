// Client.java
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.function.Consumer;

public class Client extends Application {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerId;

    private GameBoard board;
    private TextArea chatArea;
    private TextField chatInput;

    private String myPlanetPath  = "/planets/earth.png";
    private String oppPlanetPath = "/planets/jupiter.png";

    private Image myPieceImage, oppPieceImage;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Connect Four");
        primaryStage.setScene(createMenuScene(primaryStage));
        primaryStage.show();
    }

    private Scene createMenuScene(Stage stage) {
        Button playBtn = new Button("Play");
        Button selBtn  = new Button("Select Player");

        playBtn.setOnAction(e -> {
            try {
                stage.setScene(createGameScene(stage));
                stage.setTitle("Connect Four - " + playerId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        selBtn.setOnAction(e -> {
            stage.setScene(createSelectPlayerScene(stage));
            stage.setTitle("Select Player");
        });

        VBox box = new VBox(20, playBtn, selBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));
        return new Scene(box, 700, 800);
    }

    private Scene createSelectPlayerScene(Stage stage) {
        Map<String,String> planets = Map.of(
                "Mercury","/planets/mercury.png",
                "Venus",  "/planets/venus.png",
                "Earth",  "/planets/earth.png",
                "Mars",   "/planets/mars.png",
                "Jupiter","/planets/jupiter.png",
                "Saturn", "/planets/saturn.png",
                "Uranus", "/planets/uranus.png",
                "Neptune","/planets/neptune.png"
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int col = 0, row = 0;
        for (var e : planets.entrySet()) {
            String path = e.getValue();
            Image img = new Image(getClass().getResourceAsStream(path), 100, 100, true, true);
            ImageView iv = new ImageView(img);
            iv.setCursor(Cursor.HAND);
            iv.setOnMouseClicked(ev -> {
                myPlanetPath = path;
                stage.setScene(createMenuScene(stage));
                stage.setTitle("Connect Four");
            });
            grid.add(iv, col, row);
            if (++col == 4) { col = 0; row++; }
        }

        Button back = new Button("← Back");
        back.setOnAction(e -> {
            stage.setScene(createMenuScene(stage));
            stage.setTitle("Connect Four");
        });

        VBox root = new VBox(10, grid, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        return new Scene(root, 700, 800);
    }

    private Scene createGameScene(Stage stage) throws Exception {
        Socket sock = new Socket("localhost", 5555);
        out = new ObjectOutputStream(sock.getOutputStream());
        in  = new ObjectInputStream(sock.getInputStream());
        playerId = (String) in.readObject();

        // load images & notify server
        myPieceImage  = new Image(getClass().getResourceAsStream(myPlanetPath));
        oppPieceImage = new Image(getClass().getResourceAsStream(oppPlanetPath));
        out.writeObject(new GameEvent(GameEvent.Type.SELECT, myPlanetPath, playerId));
        out.flush();

        board = new GameBoard(playerId, this::sendMove, myPieceImage, oppPieceImage);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(150);

        chatInput = new TextField();
        chatInput.setPromptText("Enter your message...");
        Button send = new Button("Send");
        send.setOnAction(e -> sendChat());

        HBox h = new HBox(10, chatInput, send);
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        h.setPadding(new Insets(10));

        VBox chatBox = new VBox(5, chatArea, h);
        chatBox.setPrefHeight(200);

        BorderPane root = new BorderPane();
        root.setCenter(board.getRoot());
        root.setBottom(chatBox);

        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    Platform.runLater(() -> {
                        if (obj instanceof GameEvent) {
                            GameEvent ge = (GameEvent) obj;
                            if (ge.getType() == GameEvent.Type.SELECT) {
                                if (ge.getSelectingPlayer().equals(playerId)) {
                                    myPieceImage = new Image(getClass().getResourceAsStream(ge.getPlanetPath()));
                                } else {
                                    oppPieceImage = new Image(getClass().getResourceAsStream(ge.getPlanetPath()));
                                }
                                board.updateImages(myPieceImage, oppPieceImage);
                            } else {
                                processServerMessage(ge);
                            }
                        } else {
                            processServerMessage(obj);
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        return new Scene(root, 700, 800);
    }

    private void processServerMessage(Object obj) {
        if (obj instanceof GameEvent) {
            GameEvent e = (GameEvent) obj;
            if (e.getType() == GameEvent.Type.MOVE) {
                boolean isMine = e.getMovingPlayer().equals(playerId);
                board.placePiece(e.getColumn(), isMine);
            } else if (e.getType() == GameEvent.Type.WIN) {
                board.showWinMessage(e.getWinningPlayer().equals(playerId) ? "You win " + playerId + "!" : "You lose " + playerId + ".");
            }
        } else if (obj instanceof ChatMessage) {
            ChatMessage cm = (ChatMessage) obj;
            chatArea.appendText(cm.getSender() + ": " + cm.getMessage() + "\n");
        }
    }

    private void sendMove(int c) {
        try { out.writeObject(c); out.flush(); }
        catch (IOException ex) { ex.printStackTrace(); }
    }

    private void sendChat() {
        String m = chatInput.getText().trim();
        if (!m.isEmpty()) {
            try {
                out.writeObject(new ChatMessage(playerId, m));
                out.flush();
            } catch (IOException ex) { ex.printStackTrace(); }
            chatInput.clear();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
