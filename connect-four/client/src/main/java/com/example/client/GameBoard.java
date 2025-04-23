package com.example.client;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.util.function.Consumer;

public class GameBoard {
    private static final int ROWS = 6, COLS = 7;

    private final StackPane  root           = new StackPane();
    private final GridPane   grid           = new GridPane();
    private final Circle[][] imageCircles   = new Circle[ROWS][COLS];
    private final Circle[][] overlayCircles = new Circle[ROWS][COLS];

    private boolean myTurn;
    private boolean gameOver = false;
    private final String playerId;
    private final Consumer<Integer> moveSender;
    private Image myImage, oppImage;

    private static final Color TINT_P1 = Color.rgb(255, 0, 0, 0.4);
    private static final Color TINT_P2 = Color.rgb(  0, 0,255, 0.4);

    public GameBoard(String playerId,
                     Consumer<Integer> moveSender,
                     Image myImage,
                     Image oppImage) {
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
                if (myTurn && !gameOver) moveSender.accept(col);
            });

            for (int r = 0; r < ROWS; r++) {
                Circle cell = new Circle(40, Color.WHITE);
                cell.setStroke(Color.BLACK);

                Circle overlay = new Circle(40, Color.TRANSPARENT);
                overlay.setMouseTransparent(true);

                imageCircles[r][c]   = cell;
                overlayCircles[r][c] = overlay;

                colBox.getChildren().add(new StackPane(cell, overlay));
            }

            grid.add(colBox, c, 0);
        }

        root.getChildren().add(grid);
    }

    public StackPane getRoot() {
        return root;
    }

    public void updateImages(Image myImg, Image oppImg) {
        this.myImage  = myImg;
        this.oppImage = oppImg;
    }

    public void placePiece(int col, String movingPlayer) {
        boolean isMine = movingPlayer.equals(playerId);
        ImagePattern pattern = new ImagePattern(isMine ? myImage : oppImage);
        Color tint = movingPlayer.equals("PLAYER1") ? TINT_P1 : TINT_P2;

        for (int r = ROWS - 1; r >= 0; r--) {
            if (imageCircles[r][col].getFill().equals(Color.WHITE)) {
                imageCircles[r][col].setFill(pattern);
                overlayCircles[r][col].setFill(tint);
                myTurn = !isMine;
                break;
            }
        }
    }
}
