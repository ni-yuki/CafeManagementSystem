import java.util.Scanner;

/*
 * Admin.java - System Administration & Analytics
 * CRUD on staff accounts (users.txt), promo broadcasts and stamp reminders
 * (notifications.txt), and sales/revenue reports (sales.txt)
 */
public class Admin {

    // =========================================================================
    // ADMIN MENU
    // =========================================================================

    static void adminMenu(Scanner scanner, String adminUsername) {
        // Keep showing the Admin menu until the admin goes back
        boolean running = true;
        while (running) {
            Main.printHeader("ADMIN DASHBOARD");
            System.out.println("[1] Manage Staff Accounts");
            System.out.println("[2] Broadcast Promo to All Customers");
            System.out.println("[3] Send Stamp Reminder to a Customer");
            System.out.println("[4] Sales & Revenue Report");
            System.out.println("[5] Back to Main Menu");
            // Read the admin's choice
            String choice = Main.ask(scanner, "Choose an option: ");

            // Run the chosen option
            switch (choice) {
                case "1":
                    manageStaffAccounts(scanner, adminUsername);
                    break;
                case "2":
                    broadcastPromo(scanner);
                    break;
                case "3":
                    sendStampReminder(scanner);
                    break;
                case "4":
                    salesReport();
                    break;
                case "5":
                    // Stop the loop to go back to the main menu
                    running = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose 1-5.");
            }
        }
    }

    // =========================================================================
    // STAFF ACCOUNTS (Create, Get, Update, Delete)
    // users.txt line format: userId,username,password,role
    // =========================================================================

    static void manageStaffAccounts(Scanner scanner, String adminUsername) {
        // Keep showing the staff menu until the admin goes back
        boolean managing = true;
        while (managing) {
            System.out.println("\n--- MANAGE STAFF ACCOUNTS ---");
            System.out.println("[1] View Staff Accounts");
            System.out.println("[2] Add New Staff Account");
            System.out.println("[3] Update Staff Account");
            System.out.println("[4] Delete Staff Account");
            System.out.println("[5] Back");
            // Read which staff action to do
            String choice = Main.ask(scanner, "Select action: ");

            // Run the chosen option
            switch (choice) {
                case "1":
                    // Show all staff accounts
                    viewStaff();
                    break;
                case "2":
                    addStaff(scanner);
                    break;
                case "3":
                    updateStaff(scanner);
                    break;
                case "4":
                    deleteStaff(scanner, adminUsername);
                    break;
                case "5":
                    // Stop the loop to go back to the Admin menu
                    managing = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose 1-5.");
            }
        }
    }

    static boolean isStaffRole(String role) {
        // Only these three roles count as staff
        return role.equals("Admin") || role.equals("Manager") || role.equals("Cashier");
    }

    // Get
    static void viewStaff() {
        // Load all accounts from users.txt
        String[] users = Main.readLines(Main.USER_FILE);
        System.out.println("\n" + Main.pad("ID", 6) + Main.pad("Username", 16) + "Role");
        System.out.println("------------------------------");
        // Go through every account
        for (String line : users) {
            String[] parts = line.split(",");
            // Show only Admin, Manager and Cashier accounts (not customers)
            if (parts.length == 4 && isStaffRole(parts[3])) {
                System.out.println(Main.pad(parts[0], 6) + Main.pad(parts[1], 16) + parts[3]);
            }
        }
    }

    static String askRole(Scanner scanner) {
        // The admin picks the role by number
        String roleChoice = Main.ask(scanner, "Select Role ([1] Manager / [2] Cashier): ");
        if (roleChoice.equals("1")) {
            return "Manager";
        } else if (roleChoice.equals("2")) {
            return "Cashier";
        }
        System.out.println("Error: Invalid role. Choose 1 for Manager or 2 for Cashier.");
        return "";
    }

    // Create
    static void addStaff(Scanner scanner) {
        System.out.println("\n--- ADD NEW STAFF ACCOUNT ---");
        // Ask for the details of the new account
        String username = Main.ask(scanner, "Enter new staff username: ");
        // Username cannot be empty or contain spaces, commas or |
        if (!Main.isValidText(username) || username.contains(" ")) {
            System.out.println("Error: Username cannot be empty or contain spaces, commas, or |.");
            return;
        }
        // Do not allow a username that already exists
        if (Main.findUserIndex(Main.readLines(Main.USER_FILE), username) != -1) {
            System.out.println("Error: That username already exists.");
            return;
        }

        String password = Main.ask(scanner, "Enter password: ");
        if (!Main.isValidText(password)) {
            System.out.println("Error: Password cannot be empty or contain commas or |.");
            return;
        }

        String role = askRole(scanner);
        if (role.length() == 0) {
            return;
        }

        // Get a new unique user ID
        int userId = Main.getNextUserId();
        // Add the new account at the end of users.txt
        if (Main.appendLine(Main.USER_FILE, userId + "," + username + "," + password + "," + role)) {
            System.out.println("SUCCESS: " + role + " account '" + username + "' created with ID " + userId + ".");
        }
    }

    // Asks for a Manager/Cashier username. Returns its index in users, or -1.
    static int askStaffIndex(Scanner scanner, String[] users) {
        // Show the staff list first so the admin can pick one
        viewStaff();
        String username = Main.ask(scanner, "\nEnter staff username: ");
        // Find the position of that username
        int index = Main.findUserIndex(users, username);
        if (index == -1) {
            System.out.println("Error: User '" + username + "' not found.");
            return -1;
        }
        String[] parts = users[index].split(",");
        // Admin accounts are protected and cannot be changed here
        if (parts[3].equals("Admin")) {
            System.out.println("Error: Admin accounts cannot be changed here.");
            return -1;
        }
        if (!isStaffRole(parts[3])) {
            System.out.println("Error: '" + username + "' is not a staff account.");
            return -1;
        }
        return index;
    }

    // Update
    static void updateStaff(Scanner scanner) {
        System.out.println("\n--- UPDATE STAFF ACCOUNT ---");
        // Load all accounts from users.txt
        String[] users = Main.readLines(Main.USER_FILE);
        // Ask which staff account to use
        int index = askStaffIndex(scanner, users);
        if (index == -1) {
            return;
        }

        String[] parts = users[index].split(",");
        System.out.println("[1] Change Password\n2. Change Role (currently " + parts[3] + ")");
        // Ask whether to change the password or the role
        String choice = Main.ask(scanner, "Select action: ");

        if (choice.equals("1")) {
            String newPassword = Main.ask(scanner, "Enter new password: ");
            if (!Main.isValidText(newPassword)) {
                System.out.println("Error: Password cannot be empty or contain commas or |.");
                return;
            }
            // Replace the old password
            parts[2] = newPassword;
        } else if (choice.equals("2")) {
            String newRole = askRole(scanner);
            if (newRole.length() == 0) {
                return;
            }
            // Replace the old role
            parts[3] = newRole;
        } else {
            System.out.println("Invalid selection.");
            return;
        }

        // Put the updated account back into the array
        users[index] = parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3];
        // Save all accounts back to the file
        if (Main.writeLines(Main.USER_FILE, users, users.length)) {
            System.out.println("SUCCESS: Account '" + parts[1] + "' updated.");
        }
    }

    // Delete
    static void deleteStaff(Scanner scanner, String adminUsername) {
        System.out.println("\n--- DELETE STAFF ACCOUNT ---");
        // Load all accounts from users.txt
        String[] users = Main.readLines(Main.USER_FILE);
        // Ask which staff account to use
        int index = askStaffIndex(scanner, users);
        if (index == -1) {
            return;
        }

        String[] parts = users[index].split(",");
        // Ask for Y or N before deleting
        String confirm = Main.ask(scanner, "Delete " + parts[3] + " account '" + parts[1] + "'? (Y/N): ");
        if (!confirm.toUpperCase().equals("Y")) {
            System.out.println("Delete cancelled.");
            return;
        }

        // Copy every account except the deleted one
        // A smaller array that will hold every account except the deleted one
        String[] remaining = new String[users.length - 1];
        int next = 0;
        for (int i = 0; i < users.length; i++) {
            if (i != index) {
                remaining[next] = users[i];
                next++;
            }
        }
        // Save only the remaining accounts
        if (Main.writeLines(Main.USER_FILE, remaining, remaining.length)) {
            System.out.println("SUCCESS: Account '" + parts[1] + "' deleted.");
        }
    }

    // =========================================================================
    // CUSTOMER LIST HELPER
    // =========================================================================

    static String[] getCustomerUsernames() {
        // Load all accounts from users.txt
        String[] users = Main.readLines(Main.USER_FILE);
        // First count the customers so we know the array size
        int count = 0;
        for (String line : users) {
            String[] parts = line.split(",");
            if (parts.length == 4 && parts[3].equals("Customer")) {
                count++;
            }
        }

        // Second pass: store the customer usernames
        String[] customers = new String[count];
        int index = 0;
        for (String line : users) {
            String[] parts = line.split(",");
            if (parts.length == 4 && parts[3].equals("Customer")) {
                customers[index] = parts[1];
                index++;
            }
        }
        return customers;
    }

    // =========================================================================
    // PROMO BROADCAST (to every customer inbox)
    // =========================================================================

    static void broadcastPromo(Scanner scanner) {
        System.out.println("\n--- BROADCAST SHOP ANNOUNCEMENT ---");
        // Type the announcement for all customers
        String promo = Main.ask(scanner, "Enter promo message: ");
        if (promo.length() == 0 || promo.contains("|")) {
            System.out.println("Error: Message cannot be empty or contain |.");
            return;
        }

        // Get the usernames of all customers
        String[] customers = getCustomerUsernames();
        // Count how many messages were sent
        int sent = 0;
        // Send the message to every customer's inbox
        for (String customer : customers) {
            if (Main.sendNotification(customer, "[PROMO ANNOUNCEMENT] " + promo)) {
                sent++;
            }
        }
        System.out.println("SUCCESS: Promo sent to " + sent + " customer inbox(es).");
    }

    // =========================================================================
    // STAMP REMINDER (stamp count is read from loyalty_card.txt)
    // =========================================================================

    static void sendStampReminder(Scanner scanner) {
        System.out.println("\n--- SEND STAMP REMINDER ---");
        // Get the usernames of all customers
        String[] customers = getCustomerUsernames();
        // Show each customer with their stamp count
        for (String customer : customers) {
            int stamps = Customer.getStamps(customer);
            String stampText = (stamps == -1) ? "no card" : stamps + "/" + Customer.STAMP_GOAL + " stamps";
            System.out.println("- " + customer + " (" + stampText + ")");
        }

        // Choose which customer gets the reminder
        String username = Main.ask(scanner, "Enter customer username: ");
        // It must be a real customer account
        if (!Main.getRoleOf(username).equals("Customer")) {
            System.out.println("Error: '" + username + "' is not a registered customer.");
            return;
        }
        // Read the real stamp count from loyalty_card.txt
        int stamps = Customer.getStamps(username);
        // -1 means the customer has no loyalty card
        if (stamps == -1) {
            System.out.println("Error: '" + username + "' does not have a loyalty card.");
            return;
        }

        // Find the next milestone reward
        // Build the reminder text
        String message = "[STAMP REMINDER] You currently have " + stamps + "/" + Customer.STAMP_GOAL + " stamps.";
        // Find the next reward the customer has not reached yet
        for (int i = 0; i < Customer.REWARD_STAMPS.length; i++) {
            if (Customer.REWARD_STAMPS[i] > stamps) {
                int visitsLeft = Customer.REWARD_STAMPS[i] - stamps;
                message += " Only " + visitsLeft + " more visit(s) to get: " + Customer.REWARD_NAMES[i] + "!";
                break;
            }
        }

        // Save the reminder in the customer's inbox
        if (Main.sendNotification(username, message)) {
            System.out.println("SUCCESS: Stamp reminder sent to '" + username + "'.");
        }
    }

    // =========================================================================
    // SALES & REVENUE REPORT
    // sales.txt line format: receiptId,cashier,cardId,items,playgroundFee,discount,total
    // =========================================================================

    static void salesReport() {
        // Load every saved sale from sales.txt
        String[] sales = Main.readLines(Main.SALES_FILE);
        System.out.println("\n--- SALES TRANSACTION LOG ---");
        // Nothing to show if no sale was made yet
        if (sales.length == 0) {
            System.out.println("No sales recorded yet.");
            return;
        }

        // Totals that will be added up below
        double totalRevenue = 0;
        double totalPlayground = 0;
        double totalDiscounts = 0;
        int transactions = 0;

        // Read each sale line
        for (String line : sales) {
            String[] parts = line.split(",");
            // Skip lines that are not complete
            if (parts.length != 7) {
                continue;
            }
            try {
                // Change the text amounts into numbers
                double playground = Double.parseDouble(parts[4]);
                double discount = Double.parseDouble(parts[5]);
                double total = Double.parseDouble(parts[6]);

                System.out.println(parts[0] + " | Cashier: " + parts[1] + " | Card: " + parts[2]);
                System.out.println("   Items: " + parts[3]);
                System.out.println("   Total: " + Main.peso(total));

                // Add this sale to the totals
                totalRevenue += total;
                totalPlayground += playground;
                totalDiscounts += discount;
                transactions++;
            } catch (NumberFormatException e) {
                System.out.println("Skipped bad sales line: " + line);
            }
        }

        System.out.println("\n========== REVENUE SUMMARY ==========");
        System.out.println("Transactions:          " + transactions);
        System.out.println("Total Revenue:         " + Main.peso(totalRevenue));
        System.out.println("Dog Playground Fees:   " + Main.peso(totalPlayground));
        System.out.println("Loyalty Discounts:     " + Main.peso(totalDiscounts));
        // Only compute the average when there is at least one sale
        if (transactions > 0) {
            System.out.println("Average per Order:     " + Main.peso(totalRevenue / transactions));
        }
    }
}
