package com.connectfour.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // Using try-with-resources to ensure streams and the socket are closed properly
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Send a welcome message to the client
            out.println("Welcome to the Connect Four Server!");

            String inputLine;
            // Read client messages and echo them back for now
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from client: " + inputLine);
                out.println("Echo: " + inputLine);
                // Here is where you'll later process game commands, chat, etc.
            }
        } catch (IOException e) {
            System.err.println("Error handling client connection");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Handle error closing the socket
            }
        }
    }
}
