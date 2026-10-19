import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AuthService {
    private static final String FILE_PATH = "users.txt";
    private User currentUser = null;

    public AuthService() {
        // Automatically creates users.txt with a default Admin if it doesn't exist
        createDefaultAdminFile();
    }

    private void createDefaultAdminFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Default Bootstrap Account: ID 101, username "admin", password "admin123", role "Admin"
                writer.write("101,admin,admin123,Admin");
                writer.newLine();
            } catch (IOException e) {
                System.out.println("Error creating default users file.");
            }
        }
    }

    // Validates credentials against users.txt
    public boolean login(String username, String password) {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String id = parts[0].trim();
                    String uName = parts[1].trim();
                    String pwd = parts[2].trim();
                    String role = parts[3].trim();

                    if (uName.equals(username) && pwd.equals(password)) {
                        currentUser = new User(id, uName, pwd, role);
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading users file.");
        }

        return false;
    }

    // Clears the current session
    public void logout() {
        if (currentUser != null) {
            System.out.println("\n[+] Successfully logged out: " + currentUser.getUsername());
            currentUser = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
