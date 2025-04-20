// Client.java
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.function.Consumer;

public class Client extends Application {
    private Stage primaryStage;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerId;

    private GameBoard board;
    private TextArea chatArea;
    private TextField chatInput;

    private String myPlanetPath  = "/planets/earth.png";
    private String oppPlanetPath = "/planets/jupiter.png";

    private Image myPieceImage, oppPieceImage;

    // galaxy background
    private final Image backgroundImage =
            new Image(getClass().getResourceAsStream("/planets/galaxy.jpg"));

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Connect Four");
        stage.setScene(createMenuScene());
        stage.show();
    }

    private Scene createMenuScene() {
        Button playBtn = new Button("Play");
        Button selBtn  = new Button("Select Player");

        playBtn.setOnAction(e -> {
            try {
                primaryStage.setScene(createGameScene());
                primaryStage.setTitle("Connect Four – " + playerId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        selBtn.setOnAction(e -> {
            primaryStage.setScene(createSelectPlayerScene());
            primaryStage.setTitle("Select Player");
        });

        VBox menu = new VBox(20, playBtn, selBtn);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(50));
        menu.setBackground(new Background(new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1,1,true,true,false,true)
        )));

        return new Scene(menu, 700, 800);
    }

    private Scene createSelectPlayerScene() {
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

        int col=0, row=0;
        for (var e : planets.entrySet()) {
            String path = e.getValue();
            Image thumbImg = new Image(
                    getClass().getResourceAsStream(path),
                    100,100,true,true
            );
            ImageView thumb = new ImageView(thumbImg);
            thumb.setCursor(Cursor.HAND);
            thumb.setOnMouseClicked(ev -> {
                myPlanetPath = path;
                primaryStage.setScene(createMenuScene());
                primaryStage.setTitle("Connect Four");
            });
            grid.add(thumb, col, row);
            if (++col==4) { col=0; row++; }
        }

        Button back = new Button("← Back");
        back.setOnAction(e -> {
            primaryStage.setScene(createMenuScene());
            primaryStage.setTitle("Connect Four");
        });

        VBox root = new VBox(10, grid, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1,1,true,true,false,true)
        )));

        return new Scene(root, 700, 800);
    }

    private Scene createGameScene() throws Exception {
        Socket sock = new Socket("localhost", 5555);
        out = new ObjectOutputStream(sock.getOutputStream());
        in  = new ObjectInputStream(sock.getInputStream());
        playerId = (String)in.readObject();

        // load planet images + notify server
        myPieceImage  = new Image(getClass().getResourceAsStream(myPlanetPath));
        oppPieceImage = new Image(getClass().getResourceAsStream(oppPlanetPath));
        out.writeObject(new GameEvent(
                GameEvent.Type.SELECT,
                myPlanetPath,
                playerId
        ));
        out.flush();

        board = new GameBoard(
                playerId, this::sendMove,
                myPieceImage, oppPieceImage
        );

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(150);

        chatInput = new TextField();
        chatInput.setPromptText("Enter your message...");
        Button send = new Button("Send");
        send.setOnAction(e -> sendChat());

        HBox chatRow = new HBox(10, chatInput, send);
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        chatRow.setPadding(new Insets(10));

        VBox chatBox = new VBox(5, chatArea, chatRow);
        chatBox.setPrefHeight(200);

        BorderPane root = new BorderPane();
        root.setCenter(board.getRoot());
        root.setBottom(chatBox);
        root.setBackground(new Background(new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1,1,true,true,false,true)
        )));

        // listen for server
        new Thread(() -> {
            try {
                while (true) {
                    Object o = in.readObject();
                    Platform.runLater(() -> handleServerMessage(o));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return new Scene(root, 700, 800);
    }

    private void handleServerMessage(Object obj) {
        if (obj instanceof GameEvent ge) {
            switch (ge.getType()) {
                case SELECT -> {
                    String path = ge.getPlanetPath();
                    Image img = new Image(getClass().getResourceAsStream(path));
                    if (ge.getSelectingPlayer().equals(playerId)) {
                        myPieceImage = img;
                    } else {
                        oppPieceImage = img;
                    }
                    board.updateImages(myPieceImage, oppPieceImage);
                }
                case MOVE -> {
                    boolean mine = ge.getMovingPlayer().equals(playerId);
                    board.placePiece(ge.getColumn(), mine);
                }
                case WIN -> {
                    boolean iWon = ge.getWinningPlayer().equals(playerId);
                    primaryStage.setScene(createEndScene(iWon));
                }
            }
        } else if (obj instanceof ChatMessage cm) {
            chatArea.appendText(cm.getSender() + ": " + cm.getMessage() + "\n");
        }
    }

    /** End‐scene uses scaled ImageView plus a Quit button */
    private Scene createEndScene(boolean won) {
        String path = won ? "/win-lose/win.png" : "/win-lose/lose.png";
        Image endImg = new Image(getClass().getResourceAsStream(path));
        ImageView iv = new ImageView(endImg);
        // reduce size to 80% of window width, preserve ratio
        iv.setFitWidth(primaryStage.getWidth() * 0.8);
        iv.setPreserveRatio(true);

        Button quit = new Button("Quit");
        quit.setOnAction(e -> Platform.exit());

        VBox root = new VBox(20, iv, quit);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setBackground(new Background(new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1,1,true,true,false,true)
        )));

        return new Scene(root, 700, 800);
    }

    private void sendMove(int col) {
        try {
            out.writeObject(col);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendChat() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            try {
                out.writeObject(new ChatMessage(playerId, msg));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            chatInput.clear();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
