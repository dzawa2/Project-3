package com.connectfour.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    // Define the port number to use (for example, 12345)
    public static final int PORT = 12345;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Connect Four Server started on port " + PORT);

            // Continuously listen for incoming connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection accepted from: " + clientSocket.getInetAddress());

                // Launch a new thread to handle the client connection
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server on port " + PORT);
            e.printStackTrace();
        }
    }
}
