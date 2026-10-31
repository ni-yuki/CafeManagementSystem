import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Admin {
    private static final Path NOTIFICATIONS_FILE = Paths.get("notifications.txt");
    private static final Path USER_FILE = Paths.get("users.txt");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        showMenu(scanner, "admin", "admin");
    }

    public static void showMenu(Scanner scanner, String currentUserId, String currentUsername) {
        ensureFileExists(NOTIFICATIONS_FILE);
        seedSampleData(); // Pre-loads sample promos and stamp reminders for customer1-4

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
                case "4" -> adminSession = false;
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
            String roleChoice = scanner.nextLine().trim();
            if (!roleChoice.equals("1") && !roleChoice.equals("2")) {
                System.out.println("Invalid role. Choose 1 for Manager or 2 for Cashier.");
                return;
            }
            String role = roleChoice.equals("1") ? "Manager" : "Cashier";

            if (username.isEmpty() || password.isEmpty() || username.contains(",") || password.contains(",")) {
                System.out.println("ERROR: Username and password cannot be empty or contain commas.");
                return;
            }

            try {
                if (userExists(username)) {
                    System.out.println("ERROR: That username already exists.");
                    return;
                }

                String userId = getNextUserId();
                if (appendToFile(USER_FILE, userId + "," + username + "," + password + "," + role)) {
                    System.out.println("SUCCESS: " + role + " account '" + username + "' created!");
                } else {
                    System.out.println("ERROR: The staff account could not be saved.");
                }
            } catch (IOException e) {
                System.err.println("ERROR reading user file: " + e.getMessage());
            }
        }
    }

    private static void broadcastAdminPromo(Scanner scanner) {
        System.out.println("\n--- Broadcast Shop Announcement ---");
        System.out.print("Enter Customer Username (customer1, customer2, customer3, or customer4): ");
        String targetCustomer = scanner.nextLine();

        if (!validateCustomer(targetCustomer)) {
            return;
        }

        System.out.print("Enter promo message: ");
        String promoMessage = scanner.nextLine().trim();

        if (promoMessage.isEmpty()) {
            System.out.println("Message cannot be empty.");
            return;
        }

        if (appendNotification(targetCustomer, "[PROMO ANNOUNCEMENT] " + promoMessage)) {
            System.out.println("Promo sent to '" + targetCustomer + "'.");
        } else {
            System.out.println("ERROR: Could not send the announcement to '" + targetCustomer + "'.");
        }
    }

    public static void sendPostPurchaseStampReminder(Scanner scanner) {
        System.out.println("\n--- Send Stamp Reminder ---");
        System.out.print("Enter Customer Username (customer1 to customer4): ");
        String exactCustomer = scanner.nextLine();

        if (!validateCustomer(exactCustomer)) {
            return;
        }

        System.out.print("Enter Current Stamp Count: ");

        try {
            int stamps = Integer.parseInt(scanner.nextLine().trim());
            String message = "[STAMP REMINDER] Thank you for your purchase! You currently have " 
                    + stamps + " stamp(s). Collect 10 to redeem a free drink!";
            if (appendNotification(exactCustomer, message)) {
                System.out.println("SUCCESS: Stamp reminder sent to '" + exactCustomer + "'.");
            } else {
                System.out.println("ERROR: The stamp reminder could not be saved.");
            }
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Stamp count must be a valid number.");
        }
    }

    // ================= SAMPLE DATA SEEDING =================

    private static void seedSampleData() {
        try {
            ensureFileExists(USER_FILE);

            ensureFileExists(NOTIFICATIONS_FILE);
            if (Files.size(NOTIFICATIONS_FILE) == 0) {
                seedDefaultNotifications();
            }
        } catch (IOException e) {
            System.err.println("ERROR seeding sample data: " + e.getMessage());
        }
    }

    private static void seedDefaultNotifications() {
        String promoMsg = "[PROMO ANNOUNCEMENT] Buy 1 Get 1 Free on all Milk Teas this weekend!";
        appendNotification("customer1", promoMsg);
        appendNotification("customer2", promoMsg);
        appendNotification("customer3", promoMsg);
        appendNotification("customer4", promoMsg);

        appendNotification("customer1", createStampMessage(3));
        appendNotification("customer2", createStampMessage(9));
        appendNotification("customer3", createStampMessage(10));
        appendNotification("customer4", createStampMessage(5));
    }

    private static String createStampMessage(int stampCount) {
        return "[STAMP REMINDER] Thank you for your purchase! You currently have " 
                + stampCount + " stamp(s). Collect 10 to redeem a free drink!";
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

    private static boolean appendNotification(String customerId, String message) {
        return appendToFile(NOTIFICATIONS_FILE, customerId + "|UNREAD|" + message);
    }

    private static boolean appendToFile(Path path, String content) {
        try {
            Files.writeString(path, content + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            System.err.println("ERROR writing to file: " + e.getMessage());
            return false;
        }
    }

    private static List<String> getUsersByRole(String role) {
        List<String> users = new ArrayList<>();
        if (Files.exists(USER_FILE)) {
            try {
                for (String line : Files.readAllLines(USER_FILE)) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 4 && parts[3].trim().equalsIgnoreCase(role)) {
                        users.add(parts[1].trim());
                    }
                }
            } catch (IOException e) {
                System.err.println("ERROR reading user file: " + e.getMessage());
            }
        }
        return users;
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isSupportedCustomer(String username) {
        return username.equals("customer1")
                || username.equals("customer2")
                || username.equals("customer3")
                || username.equals("customer4");
    }

    private static boolean validateCustomer(String username) {
        if (!isSupportedCustomer(username)) {
            System.out.println("ERROR: Customer username must be customer1, customer2, customer3, or customer4.");
            return false;
        }

        try {
            if (isCustomerAccount(username)) {
                return true;
            }
            System.out.println("ERROR: Customer username '" + username + "' is not registered.");
        } catch (IOException e) {
            System.err.println("ERROR checking customer username: " + e.getMessage());
        }
        return false;
    }

    private static boolean userExists(String username) throws IOException {
        String normalizedInput = normalizeUsername(username);
        for (String line : Files.readAllLines(USER_FILE)) {
            String[] parts = line.split(",", -1);
            if (parts.length >= 4 && normalizeUsername(parts[1]).equals(normalizedInput)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCustomerAccount(String username) throws IOException {
        String normalizedInput = normalizeUsername(username);
        for (String line : Files.readAllLines(USER_FILE)) {
            String[] parts = line.split(",", -1);
            if (parts.length >= 4
                    && normalizeUsername(parts[1]).equals(normalizedInput)
                    && parts[3].trim().equalsIgnoreCase("Customer")) {
                return true;
            }
        }
        return false;
    }

    private static String getNextUserId() throws IOException {
        int largestId = 0;
        for (String line : Files.readAllLines(USER_FILE)) {
            String[] parts = line.split(",", -1);
            if (parts.length >= 4) {
                try {
                    largestId = Math.max(largestId, Integer.parseInt(parts[0].trim()));
                } catch (NumberFormatException e) {
                    // Customer IDs can contain letters, so only numeric IDs are counted.
                }
            }
        }
        return String.valueOf(largestId + 1);
    }

    private static void displayUsersByRole(String role) {
        List<String> users = getUsersByRole(role);
        for (String user : users) {
            System.out.println("- " + user);
        }
    }
}
