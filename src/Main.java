import java.io.*;
import java.util.Scanner;

public class Main {

    // --- FILE LOCATION & SESSION TRACKING ---
    // The name of the text file where user account credentials are stored
    private static final String USER_FILE = "users.txt";

    // Variables to remember who is currently logged in (set to null when logged out)
    private static String currentUserId = null;     // Stores active user ID (e.g., "101", "103", "201")
    private static String currentUsername = null;   // Stores active username (e.g., "cashier")
    private static String currentRole = null;       // Stores active role ("Admin", "Manager", "Cashier", "Customer")

    public static void main(String[] args) {

        // Scanner object to read user input from the console
        Scanner scanner = new Scanner(System.in);

        // Ensure "users.txt" exists with default test accounts before running
        checkOrCreateUserFile();

        // Controls the main application execution loop
        boolean systemRunning = true;

        while (systemRunning) {
            System.out.println("\n=========================================");
            System.out.println("        BARKS & BREWS CAFE SYSTEM      ");
            System.out.println("=========================================");

            // --- CHECK IF A USER IS LOGGED IN ---
            if (currentRole == null) {
                // =============================================================
                // GUEST MENU (NOT LOGGED IN)
                // =============================================================
                System.out.println("1. Login");
                System.out.println("2. Exit System");
                System.out.print("Choose an option: ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        // Start login credential verification
                        login(scanner);
                        break;
                    case "2":
                        // Exit the program
                        System.out.println("\nThank you for using Barks & Brews System! Goodbye.");
                        systemRunning = false;
                        break;
                    default:
                        System.out.println("Invalid selection. Please choose 1 or 2.");
                }
            } else {
                // =============================================================
                // LOGGED-IN DASHBOARD (ROLE-BASED MENU)
                // =============================================================
                // Display user header showing Username, Role, and User ID
                System.out.println("Logged in as: " + currentUsername + " (" + currentRole + ", ID: " + currentUserId + ")");
                System.out.println("------------------------------------------");
                System.out.println("1. View My Role Permissions");

                // Displays Option 2 dynamically according to user's assigned role
                if (currentRole.equals("Admin")) {
                    System.out.println("2. Admin Dashboard");
                } else if (currentRole.equals("Manager")) {
                    System.out.println("2. Inventory Management");
                } else if (currentRole.equals("Cashier")) {
                    System.out.println("2. Cashier POS Counter");
                } else if (currentRole.equals("Customer")) {
                    System.out.println("2. My Loyalty Card & Inbox");
                }

                System.out.println("3. Logout");
                System.out.print("Choose an option: ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        // Show permission breakdown for the logged-in role
                        displayRolePermissions();
                        break;
                    case "2":
                        // Route user based on their specific role
                        if ("Admin".equals(currentRole)) {
                            Admin.showMenu(scanner, currentUserId, currentUsername);
                            logout();
                        } else if ("Manager".equals(currentRole)) {
                            openManagerMenu(scanner);
                        } else if ("Cashier".equals(currentRole)) {
                            openCashierMenu(scanner);
                        } else if ("Customer".equals(currentRole)) {
                            openCustomerMenu(scanner);
                        } else {
                            System.out.println("Invalid selection.");
                        }
                        break;
                    case "3":
                        // Log out current user and clear session
                        logout();
                        break;
                    default:
                        System.out.println("Invalid selection.");
                }
            }
        }
    }

    // =========================================================================
    // ROLE ACCESS GUARD METHODS
    // =========================================================================

    // Ensures only Cashier accounts can access the Cashier POS Counter
    private static void openCashierMenu(Scanner scanner) {
        if (currentRole != null && currentRole.equals("Cashier")) {
            Cashier.cashierMenu(scanner, currentUsername);
        } else {
            System.out.println("Access denied. Only Cashiers can open the POS Counter.");
        }
    }

    // Ensures only Manager accounts can access the Inventory Management screen
    private static void openManagerMenu(Scanner scanner) {
        if (currentRole != null && currentRole.equals("Manager")) {
            Manager.managerMenu(scanner);
        } else {
            System.out.println("Access denied. Only the Manager can open inventory.");
        }
    }

    // Ensures only Customer accounts can access the Customer Loyalty Portal
    private static void openCustomerMenu(Scanner scanner) {
        if (currentRole != null && currentRole.equals("Customer")) {
            Customer.customerMenu(scanner, currentUsername);
        } else {
            System.out.println("Access denied. Only Customers can access this dashboard.");
        }
    }

    // =========================================================================
    // FILE INITIALIZATION & AUTHENTICATION
    // =========================================================================

    // Checks if "users.txt" exists; creates default test accounts if missing
    public static void checkOrCreateUserFile() {
        try {
            File file = new File(USER_FILE);

            if (file.createNewFile()) {
                System.out.println("[SYSTEM INFO] 'users.txt' not found. Creating default accounts...");

                // Write default accounts using 4 columns: UserID, Username, Password, Role
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("101,admin,admin123,Admin\n");
                    writer.write("102,manager,mgr123,Manager\n");
                    writer.write("103,cashier,cashier123,Cashier\n");
                    writer.write("201,customer1,pass123,Customer\n");
                    writer.write("202,customer2,pass123,Customer\n");
                    writer.write("203,customer3,pass123,Customer\n");
                    writer.write("204,customer4,pass123,Customer\n");
                }
                System.out.println("[SYSTEM INFO] Default accounts successfully written to 'users.txt'.");
            }
        } catch (IOException e) {
            System.out.println("Error initializing user file: " + e.getMessage());
        }
    }

    // Reads users.txt line-by-line to verify username and password
    public static boolean login(Scanner scanner) {
        System.out.println("\n--- STAFF & CUSTOMER LOGIN ---");
        System.out.print("Enter Username: ");
        String inputUsername = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String inputPassword = scanner.nextLine().trim();

        File file = new File(USER_FILE);

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();

                // Skip blank lines
                if (line.isEmpty()) continue;

                // Split CSV record by comma into 4 columns: ID, Username, Password, Role
                String[] userDetails = line.split(",");

                if (userDetails.length == 4) {
                    // Fixed array indexing , [1], [2], [3]
                    String storedId = userDetails.trim();       // Column 1: User ID
                    String storedUsername = userDetails[1].trim(); // Column 2: Username
                    String storedPassword = userDetails[2].trim(); // Column 3: Password
                    String storedRole = userDetails[3].trim();     // Column 4: Role

                    // Verify entered username and password
                    if (storedUsername.equals(inputUsername) && storedPassword.equals(inputPassword)) {
                        // Save active user session details
                        currentUserId = storedId;
                        currentUsername = storedUsername;
                        currentRole = storedRole;

                        System.out.println("\nLogin Successful! Welcome, " + currentUsername + ".");
                        System.out.println("Role Assigned: [" + currentRole + "]");
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user database: " + e.getMessage());
            return false;
        }

        System.out.println("Invalid Username or Password. Please try again.");
        return false;
    }

    // Resets session tracking variables back to null
    public static void logout() {
        if (currentUsername != null) {
            System.out.println("\nLogging out " + currentUsername + "...");
            currentUserId = null;
            currentUsername = null;
            currentRole = null;
            System.out.println("You have been successfully logged out.");
        } else {
            System.out.println("No user is currently logged in.");
        }
    }

    // Displays permission details for the active role
    private static void displayRolePermissions() {
        System.out.println("\n--- ACCESS PERMISSIONS FOR ROLE: [" + currentRole.toUpperCase() + "] ---");

        switch (currentRole) {
            case "Admin":
                System.out.println("• Full Access: Manage Staff Accounts, Broadcast Promos, Review Sales Logs.");
                break;
            case "Manager":
                System.out.println("• Inventory Control: View Low Stock Alerts (<=5), Restock Items, Update Prices.");
                break;
            case "Cashier":
                System.out.println("• POS Operations: Process Orders, Calculate Pesos (₱) Totals, Issue Loyalty Stamps.");
                break;
            case "Customer":
                System.out.println("• Customer View: Check 10-Visit Stamp Progress & Read Message Inbox.");
                break;
            default:
                System.out.println("Unknown Role.");
        }
    }
}

