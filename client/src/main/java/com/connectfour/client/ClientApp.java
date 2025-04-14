package com.connectfour.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientApp {
    public static final String SERVER_ADDRESS = "localhost";  // Change if needed
    public static final int SERVER_PORT = 12345;

    public static void connectToServer() {
        // Run the networking code in a separate thread to avoid blocking the UI
        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // Read and print the welcome message from the server
                String welcomeMessage = in.readLine();
                System.out.println("Server: " + welcomeMessage);

                // Send a test message to the server
                out.println("Hello from the Connect Four client!");

                // Listen for further messages
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println("Server Echo: " + response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
