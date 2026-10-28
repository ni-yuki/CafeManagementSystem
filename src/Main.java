import java.io.*;
import java.util.Scanner;

public class Main {

    // The name of the file where we save our user accounts
    private static final String USER_FILE = "users.txt";

    // Variables to remember who is currently logged in.
    // They are 'null' at the start because no one is logged in yet.
    private static String currentUserId = null;
    private static String currentUsername = null;
    private static String currentRole = null;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // Before starting the menu, make sure the user file exists so we can log in
        checkOrCreateUserFile();

        // This keeps our program running in a loop until the user chooses to exit
        boolean systemRunning = true;

        while (systemRunning) {
            System.out.println("\n=========================================");
            System.out.println("        BARKS & BREWS CAFE SYSTEM      ");
            System.out.println("=========================================");

            // Check if someone is logged in. If currentRole is null, no one is logged in.
            if (currentRole == null) {
                // --- MENU FOR GUESTS (NOT LOGGED IN) ---
                System.out.println("1. Login");
                System.out.println("2. Exit System");
                System.out.print("Choose an option: ");

                // Read the user's choice and remove any extra spaces
                String choice = scanner.nextLine().trim();

                // Decide what to do based on the choice
                switch (choice) {
                    case "1":
                        // Go to the login process
                        login(scanner);
                        break;
                    case "2":
                        // Stop the loop to exit the program
                        System.out.println("\nThank you for using Barks & Brews System! Goodbye.");
                        systemRunning = false;
                        break;
                    default:
                        System.out.println("Invalid selection. Please choose 1 or 2.");
                }
            } else {
                // --- MENU FOR LOGGED IN USERS ---
                // Show who is logged in and what their role is (Admin, Manager, etc.)
                System.out.println("Logged in as: " + currentUsername + " (" + currentRole + ", ID: " + currentUserId + ")"); // NEW: shows ID
                System.out.println("------------------------------------------");
                System.out.println("1. View My Role Permissions");
                // NEW: only managers see the inventory option
                if (currentRole.equals("Manager")) {
                    System.out.println("2. Inventory Management");
                }
                System.out.println("3. Logout");
                System.out.print("Choose an option: ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        // Show what this specific user is allowed to do
                        displayRolePermissions();
                        break;
                    case "2":                                  // NEW
                        // Open the manager inventory screen (managers only)
                        openManagerMenu(scanner);
                        break;
                    case "3":
                        // Log the user out
                        logout();
                        break;
                    default:
                        System.out.println("Invalid selection.");
                }
            }
        }
    }

    /*
    NEW: This method gives the Manager access to the inventory screen (Manager.java).
    It checks the logged-in role first, so Admins, Cashiers and Customers cannot open it.
     */
    private static void openManagerMenu(Scanner scanner) {
        if (currentRole != null && currentRole.equals("Manager")) {
            Manager.managerMenu(scanner);
        } else {
            System.out.println("Access denied. Only the Manager (Kitchen & Kennel Manager) can open inventory.");
        }
    }

    /*
    This method checks if the "users.txt" file exists on our computer.
    If it does not exist, it creates it and adds some default accounts so we can test the system.
     */
    public static void checkOrCreateUserFile() {
        try {
            File file = new File(USER_FILE);

            // createNewFile() returns true if the file was just created, false if it already exists
            if (file.createNewFile()) {
                System.out.println("[SYSTEM INFO] 'users.txt' not found. Creating default accounts...");

                // FileWriter writes text into our newly created file
                try (FileWriter writer = new FileWriter(file)) {
                    // Write 4 default accounts. Format is: UserID,Username,Password,Role
                    writer.write("101,admin,admin123,Admin\n");
                    writer.write("102,manager,mgr123,Manager\n");
                    writer.write("103,cashier,cashier123,Cashier\n");
                    writer.write("C101,sarah,user123,Customer\n");
                }
                System.out.println("[SYSTEM INFO] Default accounts successfully written to 'users.txt'.");
            }
        } catch (IOException e) {
            System.out.println("Error initializing user file: " + e.getMessage());
        }
    }

    /*
    This method asks for a username and password, then checks the text file to see if they match.
     */
    public static boolean login(Scanner scanner) {
        System.out.println("\n--- STAFF & CUSTOMER LOGIN ---");
        System.out.print("Enter Username: ");
        String inputUsername = scanner.nextLine().trim(); // Get username from keyboard

        System.out.print("Enter Password: ");
        String inputPassword = scanner.nextLine().trim(); // Get password from keyboard

        File file = new File(USER_FILE);

        try (Scanner fileScanner = new Scanner(file)) {
            // Read the file line by line until there are no more lines
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();

                // Skip empty lines so the program doesn't crash
                if (line.isEmpty()) {
                    continue;
                }

                // Split the line into pieces using the comma as a separator
                // For example: "101,admin,admin123,Admin" becomes an array of 4 strings
                String[] userDetails = line.split(",");

                // Make sure the line actually has all 4 pieces of information
                if (userDetails.length == 4) {
                    // Assign each piece of the array to a readable variable name
                    String storedId = userDetails[0].trim();       // 1st item: ID
                    String storedUsername = userDetails[1].trim(); // 2nd item: Username
                    String storedPassword = userDetails[2].trim(); // 3rd item: Password
                    String storedRole = userDetails[3].trim();     // 4th item: Role

                    // Check if what the user typed matches what is saved in the file
                    // .equals() is used to compare Strings in Java
                    if (storedUsername.equals(inputUsername) && storedPassword.equals(inputPassword)) {

                        // Match found! Save their info to the tracking variables
                        currentUserId = storedId;
                        currentUsername = storedUsername;
                        currentRole = storedRole;

                        System.out.println("\n Login Successful! Welcome, " + currentUsername + ".");
                        System.out.println("Role Assigned: [" + currentRole + "]");

                        return true; // Stop searching and return success
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user database: " + e.getMessage());
            return false;
        }

        // If the loop finishes and didn't return true, it means no match was found
        System.out.println("Invalid Username or Password. Please try again.");
        return false;
    }

    /*
    This method simply resets the tracking variables back to null,
    making the system forget who was logged in.
     */
    public static void logout() {
        if (currentUsername != null) {
            System.out.println("\n Logging out " + currentUsername + "...");

            // Erase the current user's session data
            currentUserId = null;
            currentUsername = null;
            currentRole = null;

            System.out.println("You have been successfully logged out.");
        } else {
            System.out.println("No user is currently logged in.");
        }
    }

    /*
    This method shows different messages depending on the role of the person logged in.
     */
    private static void displayRolePermissions() {
        System.out.println("\n--- ACCESS PERMISSIONS FOR ROLE: [" + currentRole.toUpperCase() + "] ---");

        // Checks the value of currentRole and runs the matching block of code
        switch (currentRole) {
            case "Admin":
                System.out.println("• Full Access: Manage Staff Accounts, Broadcast Promos, Review Sales Logs [1].");
                break;
            case "Manager":
                System.out.println("• Inventory Control: View Low Stock Alerts (<=5), Restock Items, Update Prices [2].");
                break;
            case "Cashier":
                System.out.println("• POS Operations: Process Orders, Calculate Pesos (₱) Totals, Issue Loyalty Stamps [2, 5].");
                break;
            case "Customer":
                System.out.println("• Customer View: Check 10-Visit Stamp Progress & Read Message Inbox [5].");
                break;
            default:
                System.out.println("Unknown Role.");
        }
    }
}