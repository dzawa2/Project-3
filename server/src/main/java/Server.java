
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 5555;


    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static ArrayList<ClientHandler> waitingPlayers = new ArrayList<>();
    private static ArrayList<GameSession> currentGames = new ArrayList<>();

    public static synchronized void addToWaitingList(ClientHandler handler) {
        if (!waitingPlayers.contains(handler)) {
            waitingPlayers.add(handler);
            System.out.println(handler.playerName + " wants to play!");
        }
    }

    private GameSession findSession(ClientHandler player) {
        synchronized (currentGames) {
            for (GameSession session : currentGames) {
                if (session.contains(player)) {
                    return session;
                }
            }
            return null;
        }
    }

    public void manageGames() {
        while (true) {
            try {
                Thread.sleep(100); // Small delay to avoid busy waiting
                synchronized (waitingPlayers) {
                    while (waitingPlayers.size() >= 2) {
                        ClientHandler p1 = waitingPlayers.remove(0);
                        ClientHandler p2 = waitingPlayers.remove(0);

                        System.out.println("Starting a game between " + p1.playerName + " and " + p2.playerName);

                        String Icon1 = p1.planetPath;
                        String Icon2 = p2.planetPath;

                        GameSession game = new GameSession(p1, p2, Icon1, Icon2);
                        currentGames.add(game);
                        System.out.println("Game started!");
                        p1.setOpponent(p2);
                        p2.setOpponent(p1);
                        p1.send(new GameEvent(GameEvent.Type.START, p2.playerName, Icon2, 1));
                        p2.send(new GameEvent(GameEvent.Type.START, p1.playerName, Icon1, 2));

                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void addNewUsers(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                String username = (String) in.readObject(); // Assuming the client sends username as first line
                // Now create the ClientHandler with the username
                ClientHandler handler = new ClientHandler(clientSocket, 0, username, out, in);
                if (clients.containsKey(username)) {
                    System.out.println("Client " + username + " is already in use!");
                    out.writeObject("INVALID");
                    clientSocket.close();
                } else {
                    System.out.println("Client " + username + " connected!");
                    out.writeObject("VALID");
                    clients.put(username, handler);
                    new Thread(handler).start(); // Run handler in new thread
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void start() {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started...");
            System.out.println("Waiting for clients...");
            Thread newUsersThread = new Thread(() -> addNewUsers(serverSocket));
            Thread mangageGamesThread = new Thread(this::manageGames);
            newUsersThread.start();
            mangageGamesThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendOpponent(ClientHandler opponent, Object msg) throws IOException {
        if (opponent != null) {
            try {
                opponent.send(msg);
            } catch (IOException e) {
                System.out.println("Opponent " + opponent.playerName + " unreachable.");
            }
        }
    }


    public class ClientHandler extends Thread {
        private final Socket sock;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;
        private final String playerName;
        private ClientHandler opponent;
        private String planetPath;

        ClientHandler(Socket sock, int num, String username, ObjectOutputStream out, ObjectInputStream in) throws IOException {
            this.sock = sock;
            this.playerName = username;
            this.out = out;
            this.in = in;
        }

        void setPlanetPath(String planetPath) {
            this.planetPath = planetPath;
        }

        void setOpponent(ClientHandler opponent) {
            this.opponent = opponent;
        }

        void send(Object o) throws IOException {
            out.writeObject(o);
            out.flush();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if ("Play".equals(obj)) {
                        this.setPlanetPath((String) in.readObject());
                        addToWaitingList(this);
                    }
                    if (obj instanceof GameEvent ge) {
                        switch (ge.getType()) {
                            case MOVE:
                                sendOpponent(this.opponent, ge);
                                break;
                            case WIN:
                                sendOpponent(this.opponent, ge);
                                sendOpponent(this, ge);
                                System.out.println("Player " + ge.getWinningPlayer() + " has won!");
                                currentGames.remove(findSession(this));
                                break;
                        }
                    } else if (obj instanceof ChatMessage cm) {
                        sendOpponent(this.opponent, cm);
                    }
                }
            } catch (SocketException e) {
                System.out.println("Client " + playerName + " disconnected (socket closed).");
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client " + playerName + " disconnected unexpectedly.");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    sock.close();
                    System.out.println("Disconnected and closed all resources for player " + playerName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class GameSession {
        private final ClientHandler player1;
        private final ClientHandler player2;
        private final String icon1;
        private final String icon2;

        public GameSession(ClientHandler p1, ClientHandler p2, String icon1, String icon2) {
            this.player1 = p1;
            this.player2 = p2;
            this.icon1 = icon1;
            this.icon2 = icon2;
        }

        public ClientHandler getOpponent(ClientHandler player) {
            return player == player1 ? player2 : player1;
        }

        public String getOpponentIcon(ClientHandler player) {
            return player == player1 ? icon2 : icon1;
        }

        public boolean contains(ClientHandler player) {
            return player == player1 || player == player2;
        }
    }

        public static void main(String[] args) {
            new Server().start();
        }
    }

