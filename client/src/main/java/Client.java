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
    private boolean listenerStarted = false;

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

        Image eyeCross = new Image(getClass().getResourceAsStream("/buttons/eye-crossed.png"));
        Image eyeOpen = new Image(getClass().getResourceAsStream("/buttons/eye-open.png"));

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

        Label feedbackLabel = new Label();
        feedbackLabel.setTextFill(Color.RED);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(200);

        PasswordField passwordField = new PasswordField();
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setMaxWidth(200);
        passwordField.setMaxWidth(200);

        // Sync content both ways
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        // Eye button to toggle
        ImageView toggleEye = new ImageView(eyeCross);
        toggleEye.setCursor(Cursor.HAND);
        toggleEye.setFitWidth(24);
        toggleEye.setFitHeight(24);
        toggleEye.setPreserveRatio(true);

        toggleEye.setOnMouseClicked(e -> {
            boolean isVisible = visiblePasswordField.isVisible();
            visiblePasswordField.setVisible(!isVisible);
            visiblePasswordField.setManaged(!isVisible);
            passwordField.setVisible(isVisible);
            passwordField.setManaged(isVisible);
            toggleEye.setImage(isVisible ? eyeCross : eyeOpen);
        });


        HBox passwordBox = new HBox(passwordField, visiblePasswordField, toggleEye);
        passwordBox.setAlignment(Pos.CENTER);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.isEmpty()) {
                feedbackLabel.setText("Username cannot be empty.");
                return;
            } else if (password.isEmpty()) {
                feedbackLabel.setText("Password cannot be empty.");
                return;
            }
            try {
                Socket sock = new Socket("localhost", 5555);
                out = new ObjectOutputStream(sock.getOutputStream());
                in = new ObjectInputStream(sock.getInputStream());
                out.writeObject(login[0] ? "Login" : "Sign-up");
                out.writeObject(username);
                out.writeObject(password);
                out.flush();

                Object response = in.readObject();
                System.out.println("Received: " + response);
                if ("SUCCESS".equals(response)) {
                    playerName = username;
                    primaryStage.setScene(createMenuScene());
                    primaryStage.setTitle("Connect Four – " + playerName);
                    if (!listenerStarted) {
                        startListener();
                        listenerStarted = true;
                    }
                } else {
                    feedbackLabel.setText((String) response);
                    in.close();
                    out.close();
                    sock.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                feedbackLabel.setText("Server connection error.");
            }
        });

        root.getChildren().addAll(togglePane, usernameField, passwordBox, feedbackLabel, confirmButton);
        return new Scene(root, 600, 600);
    }

    private void startListener() {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Object msg = in.readObject();
                    Platform.runLater(() -> {
                        if (msg instanceof GameEvent ge) {
                            switch (ge.getType()) {
                                case START -> startGame(ge);
                                case MOVE  -> { board.placePiece(ge.getMovingPlayer(), ge.getColumn()); board.onYourTurnEnd(); }
                                case WIN   -> {
                                    boolean iWon = ge.getWinningPlayer().equals(playerName);
                                    primaryStage.setScene(createEndScene(iWon));
                                }
                            }
                        } else if (msg instanceof ChatMessage cm) {
                            writeToChat(cm.getSender(), cm.getMessage());
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();

    }

    private void startGame(GameEvent startInfo) {
        if (startInfo.getPlayerId() == 1) {
            board = new GameBoard(playerName, playerName, startInfo.getOpponent(), myPlanetPath, startInfo.getPlanetPath(), this::sendMove);
            playerId = 1;
        } else {
            board = new GameBoard(playerName, startInfo.getOpponent(), playerName, startInfo.getPlanetPath(), myPlanetPath, this::sendMove);
            playerId = 2;
        }
        primaryStage.setTitle("Connect Four: " + (playerId == 1 ? playerName : startInfo.getOpponent()) + " vs " + (playerId == 1 ? startInfo.getOpponent() : playerName));

        chatList = new ListView<>();
        chatList.setPrefHeight(150);
        chatList.setFocusTraversable(false);
        chatInput = new TextField();
        chatInput.setPromptText("Enter your message...");
        chatInput.setOnAction(e -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty()) {
                try {
                    writeToChat(playerName, text);
                    out.writeObject(new ChatMessage(playerName, text, playerId));
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
        chatRow.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-text-fill: white;");

        VBox chatBox = new VBox(5, chatList, chatRow);
        chatBox.setPrefHeight(225);

        BorderPane root = new BorderPane();
        root.setCenter(board.getRoot()); root.setBottom(chatBox);
        root.setBackground(new Background(new BackgroundImage(
                backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(1,1,true,true,false,true)
        )));
        primaryStage.setScene(new Scene(root, 700, 800));
    }


        private Scene createMenuScene() {
        ImageView titleView = new ImageView(titleImage);
        titleView.setPreserveRatio(true);
        titleView.setFitWidth(600);

        ImageView playBtn = new ImageView(playImg);
        playBtn.setCursor(Cursor.HAND);
        playBtn.setPreserveRatio(true);
        playBtn.setFitWidth(200);
        playBtn.setOnMouseClicked(e -> {
            try {
                out.writeObject("Play"); out.flush();
                out.writeObject(myPlanetPath); out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        ImageView selBtn = new ImageView(selectImg);
        selBtn.setCursor(Cursor.HAND);
        selBtn.setPreserveRatio(true);
        selBtn.setFitWidth(200);
        selBtn.setOnMouseClicked(e -> {
            primaryStage.setScene(createSelectPlayerScene());
            primaryStage.setTitle("Select Player");
        });

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
        grid.setHgap(20); grid.setVgap(20);
        grid.setPadding(new Insets(30)); grid.setAlignment(Pos.CENTER);

        ImageView currentView = new ImageView(new Image(
                getClass().getResourceAsStream(myPlanetPath)));
        currentView.setPreserveRatio(true); currentView.setFitWidth(100);

        int col=0, row=0;
        for (Map.Entry<String,String> entry : planets.entrySet()) {
            String name     = entry.getKey();
            String iconPath = entry.getValue();
            String titlePath= "/planets/" + name.toLowerCase() + "-title.png";

            ImageView iconView  = new ImageView(new Image(
                    getClass().getResourceAsStream(iconPath), 100,100,true,true));
            iconView.setCursor(Cursor.HAND);

            ImageView titleView = new ImageView(new Image(
                    getClass().getResourceAsStream(titlePath)));
            titleView.setFitWidth(100);
            titleView.setPreserveRatio(true);

            VBox cell = new VBox(5, iconView, titleView);
            cell.setAlignment(Pos.CENTER);
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(e -> {
                myPlanetPath = iconPath;
                currentView.setImage(new Image(
                        getClass().getResourceAsStream(iconPath)));
            });

            grid.add(cell, col, row);
            if (++col == 4) { col = 0; row++; }
        }

        ImageView backBtn = new ImageView(backImg);
        backBtn.setCursor(Cursor.HAND);
        backBtn.setPreserveRatio(true);
        backBtn.setFitWidth(200);
        backBtn.setOnMouseClicked(e -> primaryStage.setScene(createMenuScene()));

        VBox root = new VBox(20, currentView, grid, backBtn);
        root.setAlignment(Pos.CENTER); root.setPadding(new Insets(20));
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

        return new Scene(root, 700, 800);
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
