import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Admin {
    private static final Path NOTIFICATIONS_FILE = Paths.get("notifications.txt");
    private static final Path USER_FILE = Paths.get("users.txt");

    public static void showMenu(Scanner scanner, String currentUserId, String currentUsername) {
        ensureFileExists(NOTIFICATIONS_FILE);
        boolean adminSession = true;

        while (adminSession) {
            System.out.println("\n==========================================");
            System.out.println("            ADMIN DASHBOARD               ");
            System.out.println("==========================================");
            System.out.println("1. Manage Staff Accounts");
            System.out.println("2. Broadcast Shop Announcement (Promo)");
            System.out.println("3. Send Post-Purchase Stamp Reminder");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> manageStaffAccounts(scanner);
                case "2" -> broadcastAdminPromo(scanner);
                case "3" -> sendPostPurchaseStampReminder(scanner);
                case "4" -> {
                    System.out.println("Logging out from Admin account...");
                    adminSession = false;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void manageStaffAccounts(Scanner scanner) {
        System.out.println("\n--- Manage Staff Accounts ---");
        System.out.println("1. View Staff Accounts\n2. Add New Staff Account");
        System.out.print("Select action: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            System.out.println("\n--- Managers & Cashiers ---");
            displayUsersByRole("Manager");
            displayUsersByRole("Cashier");
        } else if (choice.equals("2")) {
            System.out.print("Enter new staff username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();

            System.out.print("Select Role (1. Manager / 2. Cashier): ");
            String role = scanner.nextLine().trim().equals("1") ? "Manager" : "Cashier";

            if (!username.isEmpty() && !password.isEmpty()) {
                appendToFile(USER_FILE, username + "," + password + "," + role);
                System.out.println("SUCCESS: " + role + " account '" + username + "' created!");
            } else {
                System.out.println("ERROR: Username and password cannot be empty.");
            }
        }
    }

    private static void broadcastAdminPromo(Scanner scanner) {
        System.out.println("\n--- Broadcast Shop Announcement ---");
        System.out.print("Enter promo message: ");
        String promoMessage = scanner.nextLine().trim();

        if (promoMessage.isEmpty()) {
            System.out.println("Message cannot be empty.");
            return;
        }

        List<String> customers = getUsersByRole("Customer");
        if (customers.isEmpty()) {
            System.out.println("No customer accounts found to broadcast.");
            return;
        }

        for (String customer : customers) {
            appendNotification(customer, "[PROMO ANNOUNCEMENT] " + promoMessage);
        }

        System.out.println("SUCCESS: Promo broadcasted to " + customers.size() + " customer(s).");
    }

    public static void sendPostPurchaseStampReminder(Scanner scanner) {
        System.out.println("\n--- Send Stamp Reminder ---");
        System.out.print("Enter Customer Username: ");
        String customerId = scanner.nextLine().trim();
        System.out.print("Enter Current Stamp Count: ");

        try {
            int stamps = Integer.parseInt(scanner.nextLine().trim());
            String message = "[STAMP REMINDER] Thank you for your purchase! You currently have " 
                    + stamps + " stamp(s). Collect 10 to redeem a free drink!";
            appendNotification(customerId, message);
            System.out.println("SUCCESS: Stamp reminder sent to '" + customerId + "'.");
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Stamp count must be a valid number.");
        }
    }

    // ================= FILE HELPER METHODS =================

    private static void ensureFileExists(Path file) {
        try {
            if (Files.notExists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            System.err.println("ERROR creating file: " + e.getMessage());
        }
    }

    private static void appendNotification(String customerId, String message) {
        appendToFile(NOTIFICATIONS_FILE, customerId + "|UNREAD|" + message);
    }

    private static void appendToFile(Path path, String content) {
        try {
            Files.writeString(path, content + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("ERROR writing to file: " + e.getMessage());
        }
    }

    private static List<String> getUsersByRole(String role) {
        List<String> users = new ArrayList<>();
        if (Files.exists(USER_FILE)) {
            try {
                for (String line : Files.readAllLines(USER_FILE)) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3 && parts[2].trim().equalsIgnoreCase(role)) {
                        users.add(parts[0].trim());
                    }
                }
            } catch (IOException e) {
                System.err.println("ERROR reading user file: " + e.getMessage());
            }
        }
        return users;
    }

    private static void displayUsersByRole(String role) {
        List<String> users = getUsersByRole(role);
        for (String user : users) {
            System.out.println("- " + user);
        }
    }
}
