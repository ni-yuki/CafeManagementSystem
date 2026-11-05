import java.io.*;
import java.util.Scanner;

/*
 * BARKS & BREWS CAFE MANAGEMENT SYSTEM
 * Main.java - login / logout, role-based menu, and shared helper methods
 *
 * Data files (all plain text, one record per line):
 *   users.txt          -> userId,username,password,role
 *   inventory.txt      -> itemId,category,itemName,price,quantity
 *   loyalty_card.txt   -> cardId,username,stamps
 *   notifications.txt  -> username|status|message      (status = UNREAD or READ)
 *   sales.txt          -> receiptId,cashier,cardId,items,playgroundFee,discount,total
 *   restock_log.txt    -> itemId,itemName,addedQty,newQty,manager
 */
public class Main {

    // --- FILE NAMES (used by every role) ---
    static final String USER_FILE = "users.txt";
    static final String INVENTORY_FILE = "inventory.txt";
    static final String LOYALTY_FILE = "loyalty_card.txt";
    static final String NOTIFICATIONS_FILE = "notifications.txt";
    static final String SALES_FILE = "sales.txt";
    static final String RESTOCK_LOG_FILE = "restock_log.txt";

    static final String PESO = "₱";

    // --- CURRENT SESSION (null means nobody is logged in) ---
    static String currentUserId = null;
    static String currentUsername = null;
    static String currentRole = null;

    public static void main(String[] args) {
        // Scanner reads what the user types in the console
        Scanner scanner = new Scanner(System.in);
        // Create the data files with sample data if they do not exist yet
        setupFiles();

        // The program keeps looping until the user chooses Exit
        boolean systemRunning = true;
        while (systemRunning) {
            // Show the menu again and again
            printHeader("BARKS & BREWS CAFE SYSTEM");

            // No role yet means nobody is logged in
            if (currentRole == null) {
                // ---------- GUEST MENU ----------
                System.out.println("[1] Login");
                System.out.println("[2] Exit System");
                // Get the menu choice typed by the user
                String choice = ask(scanner, "Choose an option: ");

                // Do the action that matches the choice
                switch (choice) {
                    case "1":
                        login(scanner);
                        break;
                    case "2":
                        System.out.println("\nThank you for using Barks & Brews System! Goodbye.");
                        systemRunning = false;
                        break;
                    default:
                        System.out.println("Invalid selection. Please choose 1 or 2.");
                }
            } else {
                // ---------- LOGGED-IN MENU ----------
                System.out.println("Logged in as: " + currentUsername + " (" + currentRole + ", ID: " + currentUserId + ")");
                System.out.println("------------------------------------------");
                System.out.println("[1] View My Role Permissions");
                System.out.println("[2] " + getRoleMenuName());
                System.out.println("[3] Logout");
                // Get the menu choice typed by the user
                String choice = ask(scanner, "Choose an option: ");

                // Do the action that matches the choice
                switch (choice) {
                    case "1":
                        displayRolePermissions();
                        break;
                    case "2":
                        openRoleMenu(scanner);
                        break;
                    case "3":
                        logout();
                        break;
                    default:
                        System.out.println("Invalid selection. Please choose 1, 2, or 3.");
                }
            }
        }
        // Close the Scanner when the program ends
        scanner.close();
    }

    // =========================================================================
    // LOGIN / LOGOUT
    // =========================================================================

    static boolean login(Scanner scanner) {
        System.out.println("\n--- STAFF & CUSTOMER LOGIN ---");
        // Ask for the login details
        String inputUsername = ask(scanner, "Enter Username: ");
        String inputPassword = ask(scanner, "Enter Password: ");

        // Load all accounts from users.txt
        String[] users = readLines(USER_FILE);
        // Check each account line one by one
        for (String line : users) {
            // Split the line into its parts: id, username, password, role
            String[] parts = line.split(",");
            // Username and password both match, so the login is correct
            if (parts.length == 4 && parts[1].equals(inputUsername) && parts[2].equals(inputPassword)) {
                // Remember who is logged in
                currentUserId = parts[0];
                currentUsername = parts[1];
                currentRole = parts[3];
                System.out.println("\nLogin Successful! Welcome, " + currentUsername + ".");
                System.out.println("Role Assigned: [" + currentRole + "]");
                return true;
            }
        }
        // No account matched the username and password
        System.out.println("Invalid Username or Password. Please try again.");
        return false;
    }

    static void logout() {
        System.out.println("\nLogging out " + currentUsername + "...");
        // Clear the saved user so nobody is logged in
        currentUserId = null;
        currentUsername = null;
        currentRole = null;
        System.out.println("You have been successfully logged out.");
    }

    // =========================================================================
    // ROLE ROUTING
    // =========================================================================

    static String getRoleMenuName() {
        // Pick the menu name that fits the role
        switch (currentRole) {
            case "Admin":
                return "Admin Dashboard";
            case "Manager":
                return "Inventory Management";
            case "Cashier":
                return "Cashier POS Counter";
            case "Customer":
                return "My Loyalty Card & Inbox";
            default:
                return "Unknown Role";
        }
    }

    // Each role can only open its own menu
    static void openRoleMenu(Scanner scanner) {
        // Open the menu of the logged-in role only
        switch (currentRole) {
            case "Admin":
                Admin.adminMenu(scanner, currentUsername);
                break;
            case "Manager":
                Manager.managerMenu(scanner, currentUsername);
                break;
            case "Cashier":
                Cashier.cashierMenu(scanner, currentUsername);
                break;
            case "Customer":
                Customer.customerMenu(scanner, currentUsername);
                break;
            default:
                System.out.println("Access denied. Unknown role.");
        }
    }

    static void displayRolePermissions() {
        System.out.println("\n--- ACCESS PERMISSIONS FOR ROLE: [" + currentRole.toUpperCase() + "] ---");
        // Show what the current role is allowed to do
        switch (currentRole) {
            case "Admin":
                System.out.println("- Create, view, update, and delete staff accounts (Manager / Cashier)");
                System.out.println("- Broadcast promo announcements to all customer inboxes");
                System.out.println("- Send stamp reminders to customers");
                System.out.println("- Review sales logs and total store revenue");
                break;
            case "Manager":
                System.out.println("- View the stock catalog with peso prices");
                System.out.println("- Run low stock scans (5 or fewer units left)");
                System.out.println("- Restock items (logged in restock_log.txt)");
                System.out.println("- Add new items, update prices, and remove items");
                break;
            case "Cashier":
                System.out.println("- Process orders and check stock in real time");
                System.out.println("- Add Dog Playground fee (" + peso(Cashier.PLAYGROUND_RATE) + " per hour)");
                System.out.println("- Accept cash, compute change, log sales, and add loyalty stamps");
                System.out.println("- Register new loyalty card customers");
                break;
            case "Customer":
                System.out.println("- View your 10-visit stamp progress");
                System.out.println("- Check which milestone rewards you can get");
                System.out.println("- Read and delete messages in your inbox");
                break;
            default:
                System.out.println("Unknown Role.");
        }
    }

    // =========================================================================
    // FILE SETUP (creates sample data the first time the program runs)
    // =========================================================================

    static void setupFiles() {
        // Create the accounts file only if it is missing
        if (!fileExists(USER_FILE)) {
            // Default accounts (userId,username,password,role)
            String[] users = {
                "101,admin,admin123,Admin",
                "102,manager,mgr123,Manager",
                "103,cashier,cashier123,Cashier",
                "201,customer1,pass123,Customer",
                "202,customer2,pass123,Customer",
                "203,customer3,pass123,Customer",
                "204,customer4,pass123,Customer"
            };
            // Save the default accounts into the file
            writeLines(USER_FILE, users, users.length);
            System.out.println("[SYSTEM INFO] users.txt created with default accounts.");
        }

        // Create the inventory file with the default menu items
        if (!fileExists(INVENTORY_FILE)) {
            String[] items = {
                "H101,Human_Drink,Caramel Macchiato,220.00,20",
                "H102,Human_Drink,Iced Matcha Latte,235.00,15",
                "H103,Human_Drink,Cold Brew Coffee,185.00,25",
                "H104,Human_Drink,Chocolate Frappuccino,200.00,22",
                "H105,Human_Food,Blueberry Muffin,160.00,10",
                "H106,Human_Food,Butter Croissant,140.00,12",
                "H107,Human_Food,Tiramisu,200.00,9",
                "H108,Human_Food,Macarons,190.00,7",
                "D201,Dog_Drink,Classic Puppuccino,175.00,30",
                "D202,Dog_Drink,Dog Chamomile Tea,195.00,4",
                "D203,Dog_Drink,Goat Milk Latte,180.00,15",
                "D204,Dog_Drink,Bone Broth Refresher,165.00,12",
                "D205,Dog_Treat,Peanut Butter Bites,125.00,3",
                "D206,Dog_Treat,Chicken Jerky Crunch,140.00,20",
                "D207,Dog_Treat,Pupsicle Berry Freeze,160.00,2",
                "D208,Dog_Treat,Salmon Chew Stick,150.00,18"
            };
            writeLines(INVENTORY_FILE, items, items.length);
            System.out.println("[SYSTEM INFO] inventory.txt created with 16 default items.");
        }

        // Create the loyalty cards file with sample cards
        if (!fileExists(LOYALTY_FILE)) {
            String[] cards = {
                "LC201,customer1,1",
                "LC202,customer2,4",
                "LC203,customer3,7",
                "LC204,customer4,9"
            };
            writeLines(LOYALTY_FILE, cards, cards.length);
            System.out.println("[SYSTEM INFO] loyalty_card.txt created with sample cards.");
        }

        // Create the inbox file with a sample promo
        if (!fileExists(NOTIFICATIONS_FILE)) {
            String promo = "[PROMO ANNOUNCEMENT] Buy 1 Get 1 Free on all Puppuccinos this weekend!";
            String[] messages = {
                "customer1|UNREAD|" + promo,
                "customer2|UNREAD|" + promo,
                "customer3|UNREAD|" + promo,
                "customer4|UNREAD|" + promo
            };
            writeLines(NOTIFICATIONS_FILE, messages, messages.length);
            System.out.println("[SYSTEM INFO] notifications.txt created with a sample promo.");
        }

        // These two files start empty and fill up as the system is used
        createEmptyFile(SALES_FILE);
        createEmptyFile(RESTOCK_LOG_FILE);
    }

    // =========================================================================
    // SHARED FILE HELPERS
    // =========================================================================

    static boolean fileExists(String fileName) {
        // A File object points to the file on the computer
        File file = new File(fileName);
        // true if the file is already there
        return file.exists();
    }

    static void createEmptyFile(String fileName) {
        try {
            File file = new File(fileName);
            file.createNewFile(); // does nothing if the file already exists
        } catch (IOException e) {
            System.out.println("Error creating " + fileName + ": " + e.getMessage());
        }
    }

    // Reads every non-blank line of a file into an array
    static String[] readLines(String fileName) {
        // First pass: count the lines so we know the array size
        int count = 0;
        // BufferedReader reads the file line by line
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            // Keep reading until there are no more lines
            while ((line = reader.readLine()) != null) {
                // Skip blank lines
                if (line.trim().length() > 0) {
                    count++;
                }
            }
        } catch (IOException e) {
            return new String[0]; // file missing or unreadable -> empty array
        }

        // Second pass: store the lines in the array
        String[] lines = new String[count];
        int index = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null && index < count) {
                if (line.trim().length() > 0) {
                    lines[index] = line.trim();
                    index++;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading " + fileName + ": " + e.getMessage());
        }
        return lines;
    }

    // Overwrites a file with the first "count" lines of the array
    static boolean writeLines(String fileName, String[] lines, int count) {
        // FileWriter without "true" replaces the old content of the file
        try (FileWriter writer = new FileWriter(fileName)) {
            for (int i = 0; i < count; i++) {
                // Write each line followed by a new line
                writer.write(lines[i] + "\n");
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error writing " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    // Adds one line to the end of a file (true = append mode, does not overwrite)
    static boolean appendLine(String fileName, String line) {
        try (FileWriter writer = new FileWriter(fileName, true)) {
            writer.write(line + "\n");
            return true;
        } catch (IOException e) {
            System.out.println("Error writing " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // SHARED USER HELPERS
    // =========================================================================

    // Returns the position of a username in the users array, or -1 if not found
    static int findUserIndex(String[] users, String username) {
        for (int i = 0; i < users.length; i++) {
            String[] parts = users[i].split(",");
            // Username matches, so return its position in the array
            if (parts.length == 4 && parts[1].equals(username)) {
                return i;
            }
        }
        return -1;
    }

    static String getRoleOf(String username) {
        String[] users = readLines(USER_FILE);
        int index = findUserIndex(users, username);
        if (index == -1) {
            return "";
        }
        String[] parts = users[index].split(",");
        return parts[3];
    }

    // Next free numeric user ID (largest ID + 1)
    static int getNextUserId() {
        String[] users = readLines(USER_FILE);
        // Find the biggest user ID that is already used
        int largest = 100;
        for (String line : users) {
            String[] parts = line.split(",");
            try {
                int id = Integer.parseInt(parts[0]);
                // Keep the biggest ID found so far
                if (id > largest) {
                    largest = id;
                }
            } catch (NumberFormatException e) {
                // skip IDs that are not numbers
            }
        }
        // The next ID is one more than the biggest
        return largest + 1;
    }

    // Puts a message in one customer's inbox
    static boolean sendNotification(String username, String message) {
        return appendLine(NOTIFICATIONS_FILE, username + "|UNREAD|" + message);
    }

    // =========================================================================
    // SHARED INPUT & DISPLAY HELPERS
    // =========================================================================

    static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        // Read the whole line and remove extra spaces
        return scanner.nextLine().trim();
    }

    // Keeps asking until the user types a whole number
    static int askInt(Scanner scanner, String prompt) {
        // Repeat until the user types a valid number
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                // Change the typed text into a whole number
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a whole number.");
            }
        }
    }

    // Keeps asking until the user types a valid number (decimals allowed)
    static double askDouble(Scanner scanner, String prompt) {
        // Repeat until the user types a valid number
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                // Change the typed text into a decimal number
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid amount.");
            }
        }
    }

    // Text input is saved in comma/pipe separated files, so these characters are not allowed
    static boolean isValidText(String text) {
        return text.length() > 0 && !text.contains(",") && !text.contains("|");
    }

    // Rounds money to 2 decimal places (avoids values like 198.00000000000003)
    static double roundMoney(double amount) {
        // Round to 2 decimal places (centavos)
        return Math.round(amount * 100) / 100.0;
    }

    // Formats money as ₱220.00
    static String peso(double amount) {
        return PESO + String.format("%.2f", amount);
    }

    // Adds spaces after the text so table columns line up
    static String pad(String text, int width) {
        String result = text;
        // Add spaces until the text reaches the wanted width
        while (result.length() < width) {
            result += " ";
        }
        return result;
    }

    static void printHeader(String title) {
        // Print the title between two lines
        System.out.println("\n==========================================");
        System.out.println("  " + title);
        System.out.println("==========================================");
    }
}
