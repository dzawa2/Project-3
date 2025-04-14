package com.connectfour.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Connect Four Client");

        // Create a simple button to trigger connection to the server
        Button connectButton = new Button("Connect to Server");
        connectButton.setOnAction(event -> ClientApp.connectToServer());

        StackPane root = new StackPane();
        root.getChildren().add(connectButton);

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
