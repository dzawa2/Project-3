import java.net.*;
import java.io.*;

public class Server {
    private static final int PORT = 5555;
    private Socket player1, player2;
    private ObjectOutputStream out1, out2;
    private ObjectInputStream in1, in2;

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Waiting for players...");

        player1 = serverSocket.accept();
        out1 = new ObjectOutputStream(player1.getOutputStream());
        in1 = new ObjectInputStream(player1.getInputStream());
        out1.writeObject("PLAYER1");

        player2 = serverSocket.accept();
        out2 = new ObjectOutputStream(player2.getOutputStream());
        in2 = new ObjectInputStream(player2.getInputStream());
        out2.writeObject("PLAYER2");

        runGame();
    }

    private void runGame() throws IOException {
        boolean running = true;
        ObjectInputStream currentIn = in1;
        ObjectOutputStream currentOut = out2;

        while (running) {
            try {
                // FIX: Use Integer casting before unboxing
                int column = ((Integer) currentIn.readObject());

                // Forward the move to the other player
                currentOut.writeObject(column);

                // Swap players
                ObjectInputStream tempIn = currentIn;
                ObjectOutputStream tempOut = currentOut;
                currentIn = (currentIn == in1) ? in2 : in1;
                currentOut = (currentOut == out1) ? out2 : out1;

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server().startServer();
    }
}
