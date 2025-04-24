import java.sql.*;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:connect4.db";

    public static void connect() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                String sql = "CREATE TABLE IF NOT EXISTS users (" +
                             "username TEXT PRIMARY KEY," +
                             "password TEXT NOT NULL," +
                             "elo INTEGER DEFAULT 1000," +
                             "friends TEXT)"; // Store as comma-separated usernames
                stmt.execute(sql);
                System.out.println("Connected and ensured table exists.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static String insertUser(String username, String password) {
        String insertUserSql = "INSERT INTO users(username, password) VALUES(?, ?)";
        String findUserSql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(findUserSql)) {
            pstmt.setString(1, username);
            pstmt.executeQuery();
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                return "Username Taken";
            }
            else{
                try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, password);
                    insertStmt.executeUpdate();
                    System.out.println("User inserted.");
                    return "SUCCESS";
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("Database error: " + e.getMessage());
            return "Internal error.";
        }
    }

    public static String loginUser(String username, String password) {
        String findUserSql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(findUserSql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                return "Username does not exist.";
            }

            String storedPassword = rs.getString("password");
            if (!storedPassword.equals(password)) {
                return "Incorrect password.";
            }
            else{
                return "SUCCESS";
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return "Internal error.";
        }
    }
}

