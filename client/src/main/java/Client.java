// Client.java
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class Client extends Application {
    private Stage primaryStage;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private int playerId;
    private String playerName;

    private GameBoard board;
    private ListView<Label> chatList;
    private TextField chatInput;

    private String myPlanetPath  = "/planets/earth.png";

    private Image myPieceImage;

    private final Image backgroundImage =
            new Image(getClass().getResourceAsStream("/planets/galaxy.jpg"));
    private final Image titleImage =
            new Image(getClass().getResourceAsStream("/planets/MAIN-TITLE.png"));

    // button art now in /buttons
    private final Image playImg   = new Image(getClass().getResourceAsStream("/buttons/play.png"));
    private final Image selectImg = new Image(getClass().getResourceAsStream("/buttons/select-player.png"));
    private final Image backImg   = new Image(getClass().getResourceAsStream("/buttons/back.png"));
    private final Image quitImg   = new Image(getClass().getResourceAsStream("/buttons/quit.png"));
    private final Image loginImg = new Image(getClass().getResourceAsStream("/buttons/log-in.png"));
    private final Image signupImg = new Image(getClass().getResourceAsStream("/buttons/sign-in.png"));


    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Connect Four");
        stage.setScene(createLoginScene());
        stage.show();
    }

    private Scene createLoginScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setBackground(new Background(new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1, 1, true, true, false, true)
        )));

        final boolean[] login = {false};

        ImageView signupView = new ImageView(signupImg);
        signupView.setPreserveRatio(true);
        signupView.setFitHeight(60);
        signupView.setCursor(Cursor.HAND);

        ImageView loginView = new ImageView(loginImg);
        loginView.setPreserveRatio(true);
        loginView.setFitHeight(60);
        loginView.setCursor(Cursor.HAND);

        // Container that holds the currently visible image
        StackPane togglePane = new StackPane(signupView); // Start with signup

        signupView.setOnMouseClicked(e -> {
            togglePane.getChildren().setAll(loginView); // Swap to login
            login[0] = true;

        });

        loginView.setOnMouseClicked(e -> {
            togglePane.getChildren().setAll(signupView);
            login[0] = false;// Swap back to signup
        });

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(200);

        Label feedbackLabel = new Label();
        feedbackLabel.setTextFill(Color.RED);

        usernameField.setOnAction(e -> {
            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                feedbackLabel.setText("Username cannot be empty.");
                return;
            }
            try {
                // Connect only once during login
                Socket sock = new Socket("localhost", 5555);
                out = new ObjectOutputStream(sock.getOutputStream());
                in = new ObjectInputStream(sock.getInputStream());

                out.writeObject(username);  // Send the username
                out.flush();

                Object response = in.readObject();
                if ("VALID".equals(response)) {
                    playerName = username;
                    primaryStage.setScene(createMenuScene());
                    primaryStage.setTitle("Connect Four – " + playerName);
                } else {
                    feedbackLabel.setText("Username already in use. Try again.");
                    in.close();
                    out.close();
                    sock.close(); // Close and retry
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                feedbackLabel.setText("Server connection error.");
            }
        });

        root.getChildren().addAll(togglePane, usernameField, feedbackLabel);
        return new Scene(root, 600, 600);
    }

    private Scene createMenuScene() {
        ImageView titleView = new ImageView(titleImage);
        titleView.setPreserveRatio(true);
        titleView.setFitWidth(600);

        // PLAY button
        ImageView playBtn = new ImageView(playImg);
        playBtn.setCursor(Cursor.HAND);
        playBtn.setFitWidth(200);
        playBtn.setPreserveRatio(true);
        playBtn.setOnMouseClicked(e -> {
            try {
                primaryStage.setScene(createGameScene());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // SELECT PLAYER button
        ImageView selBtn = new ImageView(selectImg);
        selBtn.setCursor(Cursor.HAND);
        selBtn.setFitWidth(200);
        selBtn.setPreserveRatio(true);
        selBtn.setOnMouseClicked(e -> {
            primaryStage.setScene(createSelectPlayerScene());
            primaryStage.setTitle("Select Player");
        });

        // Center both buttons side by side
        HBox buttonBox = new HBox(30, playBtn, selBtn);
        buttonBox.setAlignment(Pos.CENTER);

        VBox menu = new VBox(40, titleView, buttonBox);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(40));
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
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(30));
        grid.setAlignment(Pos.CENTER);
        ImageView currentPlanetView = new ImageView(new Image(getClass().getResourceAsStream(myPlanetPath)));
        currentPlanetView.setFitWidth(100);
        currentPlanetView.setPreserveRatio(true);

        int col = 0, row = 0;
        for (var entry : planets.entrySet()) {
            String name      = entry.getKey();
            String iconPath  = entry.getValue();
            String titlePath = "/planets/" + name.toLowerCase() + "-title.png";

            ImageView iconView = new ImageView(
                    new Image(getClass().getResourceAsStream(iconPath), 100, 100, true, true)
            );
            iconView.setCursor(Cursor.HAND);

            ImageView titleView = new ImageView(
                    new Image(getClass().getResourceAsStream(titlePath))
            );
            titleView.setFitWidth(100);
            titleView.setPreserveRatio(true);

            VBox cell = new VBox(5, iconView, titleView);
            cell.setAlignment(Pos.CENTER);
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(e -> {
                myPlanetPath = iconPath;
                currentPlanetView.setImage(new Image(getClass().getResourceAsStream(myPlanetPath)));
            });

            grid.add(cell, col, row);
            if (++col == 4) {
                col = 0;
                row++;
            }
        }

        ImageView backBtn = new ImageView(backImg);
        backBtn.setCursor(Cursor.HAND);
        backBtn.setFitWidth(200);
        backBtn.setPreserveRatio(true);
        backBtn.setOnMouseClicked(e -> {
            primaryStage.setScene(createMenuScene());
            primaryStage.setTitle("Connect Four");
        });

        VBox root = new VBox(20, currentPlanetView,grid, backBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1,1,true,true,false,true)
        )));

        return new Scene(root, 700, 700);
    }

    private Scene createGameScene() throws Exception {

        out.writeObject("Play");
        out.flush();

        out.writeObject(myPlanetPath);
        out.flush();

        GameEvent startInfo = (GameEvent) in.readObject();
        playerId = startInfo.getPlayerId();
        if(startInfo.getPlayerId() == 1){ //Your player1
            board = new GameBoard(playerName,playerName,startInfo.getOpponent(),myPlanetPath,startInfo.getPlanetPath(),this::sendMove);
            playerId = 1;
            primaryStage.setTitle("Connect Four: " + playerName + " vs " + startInfo.getOpponent());

        }
        else { //Your player2
            board = new GameBoard(playerName,startInfo.getOpponent(),playerName, startInfo.getPlanetPath(), myPlanetPath,this::sendMove);
            playerId = 2;
            primaryStage.setTitle("Connect Four: " + startInfo.getOpponent() + " vs " + playerName);

        }

        chatList = new ListView<>();
        chatList.setPrefHeight(150);
        chatList.setFocusTraversable(false);

        chatInput = new TextField();
        chatInput.setPromptText("Enter your message...");
        chatInput.setOnAction(e -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty()) {
                try {
                    writeToChat(playerName,text);
                    out.writeObject(new ChatMessage(playerName, text,playerId));
                    out.flush();
                    chatInput.clear();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        HBox chatRow = new HBox(10, chatInput);
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        chatRow.setPadding(new Insets(10));
        chatRow.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-text-fill: white;");

        VBox chatBox = new VBox(5, chatList, chatRow);
        chatBox.setPrefHeight(225);

        BorderPane root = new BorderPane();
        root.setCenter(board.getRoot());
        root.setBottom(chatBox);
        root.setBackground(new Background(new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1,1,true,true,false,true)
        )));

        new Thread(() -> {
            try {
                while (true) {
                    Object msg = in.readObject();
                    Platform.runLater(() -> handleServerMessage(msg));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        return new Scene(root, 700, 800);
    }

    private void handleServerMessage(Object obj) {

            if (obj instanceof GameEvent ge) {
                switch (ge.getType()) {

                    case MOVE -> {
                        board.placePiece(ge.getMovingPlayer(), ge.getColumn());
                        board.onYourTurnEnd();
                    }
                    case WIN -> {
                        boolean iWon = ge.getWinningPlayer().equals(playerName);
                        primaryStage.setScene(createEndScene(iWon));
                    }
                }
            } else if (obj instanceof ChatMessage cm) {
                writeToChat(cm.getSender(), cm.getMessage());
            }
        }


    private Scene createEndScene(boolean won) {
        String path = won ? "/win-lose/win.png" : "/win-lose/lose.png";
        ImageView iv = new ImageView(new Image(getClass().getResourceAsStream(path)));
        iv.setPreserveRatio(true);
        iv.setFitWidth(primaryStage.getWidth() * 0.8);

        ImageView quitBtn = new ImageView(quitImg);
        quitBtn.setCursor(Cursor.HAND);
        quitBtn.setFitWidth(200);
        quitBtn.setPreserveRatio(true);
        quitBtn.setOnMouseClicked(e -> primaryStage.setScene(createMenuScene()));

        VBox root = new VBox(20, iv, quitBtn);
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

    private void sendMove(GameEvent ge) {
        try {
            out.writeObject(ge);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToChat(String sender, String message) {
        Label lbl = new Label(sender+ ": " + message);
        lbl.setTextFill(sender.equals(playerName) && playerId == 0? Color.RED : Color.BLUE);
        chatList.getItems().add(lbl);
        chatList.scrollTo(chatList.getItems().size() - 1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
