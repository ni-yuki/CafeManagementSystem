import java.io.*;
import java.util.*;

public class Admin {
    private static final String NOTIFICATIONS_FILE = "notifications.txt";
    private static final String USER_FILE = "users.txt";

    /**
     * Display the Admin Menu options.
     * Called from Main.java when an Admin logs in.
     */
    public static void showMenu(Scanner scanner, String currentUserId, String currentUsername) {
        ensureNotificationFileExists();
        boolean adminSession = true;

        while (adminSession) {
            System.out.println("\n==========================================");
            System.out.println("            ADMIN DASHBOARD               ");
            System.out.println("==========================================");
            System.out.println("1. Manage Staff Accounts");
            System.out.println("2. Broadcast Shop Announcement (Promo)");
            System.out.println("3. Send Post-Purchase Stamp Reminder");
            System.out.println("4. View Customer Inbox (Unread Messages)");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    manageStaffAccounts(scanner);
                    break;
                case "2":
                    broadcastAdminPromo(scanner);
                    break;
                case "3":
                    sendPostPurchaseStampReminder(scanner);
                    break;
                case "4":
                    viewCustomerInbox(scanner);
                    break;
                case "5":
                    System.out.println("Logging out from Admin account...");
                    adminSession = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
   
   private static void manageStaffAccounts(Scanner scanner) {
    System.out.println("\n--- Manage Staff Accounts ---");
    System.out.println("1. View Staff Accounts");
    System.out.println("2. Add New Staff Account");
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
        
        // Ask for specific role instead of hardcoding "staff"
        System.out.println("Select Role: 1. Manager  2. Cashier");
        System.out.print("Choice: ");
        String roleChoice = scanner.nextLine().trim();
        String role = roleChoice.equals("1") ? "Manager" : "Cashier";

        if (!username.isEmpty() && !password.isEmpty()) {
            appendUserRecord(username, password, role); 
            System.out.println("SUCCESS: " + role + " account '" + username + "' created!");
        } else {
            System.out.println("ERROR: Username and password cannot be empty.");
        }
    }
}
    // 2. BROADCAST SHOP ANNOUNCEMENT (Admin Promo)

    private static void broadcastAdminPromo(Scanner scanner) {
        System.out.println("\n--- Broadcast Shop Announcement ---");
        System.out.print("Enter promo message to send to ALL customers: ");
        String promoMessage = scanner.nextLine().trim();

        if (promoMessage.isEmpty()) {
            System.out.println("Message cannot be empty.");
            return;
        }

        List<String> customers = getAllCustomerUsernames();
        if (customers.isEmpty()) {
            System.out.println("No customer accounts found to broadcast.");
            return;
        }

        for (String customer : customers) {
            appendNotification(customer, "[PROMO ANNOUNCEMENT] " + promoMessage);
        }

        System.out.println("SUCCESS: Promo broadcasted to " + customers.size() + " customer(s).");
    }

    
    // 3. POST-PURCHASE STAMP REMINDER (Notification Method)
    
    public static void sendPostPurchaseStampReminder(Scanner scanner) {
        System.out.println("\n--- Send Stamp Reminder ---");
        System.out.print("Enter Customer Username: ");
        String customerId = scanner.nextLine().trim();
        System.out.print("Enter Current Stamp Count: ");
        String stampsInput = scanner.nextLine().trim();

        try {
            int stamps = Integer.parseInt(stampsInput);
            String reminderMessage = "[STAMP REMINDER] Thank you for your purchase! You currently have " 
                    + stamps + " stamp(s). Collect 10 to redeem a free drink!";
            
            appendNotification(customerId, reminderMessage);
            System.out.println("SUCCESS: Stamp reminder sent to customer '" + customerId + "'.");
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Stamp count must be a valid number.");
        }
    }

   
    // 4. VIEW INBOX METHOD (Customer Unread Messages)
  
    public static void viewCustomerInbox(Scanner scanner) {
        System.out.println("\n--- View Unread Inbox Messages ---");
        System.out.print("Enter Customer Username to check inbox: ");
        String customerId = scanner.nextLine().trim();

        List<String> allLines = new ArrayList<>();
        boolean foundUnread = false;

        File file = new File(NOTIFICATIONS_FILE);
        if (!file.exists()) {
            System.out.println("No notifications recorded yet.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Format: customerId|READ/UNREAD|message
                String[] parts = line.split("\\|", 3);
                if (parts.length == 3) {
                    String recipient = parts[0];
                    String status = parts[1];
                    String message = parts[2];

                    if (recipient.equalsIgnoreCase(customerId) && "UNREAD".equalsIgnoreCase(status)) {
                        System.out.println("📩 " + message);
                        foundUnread = true;
                        line = recipient + "|READ|" + message; // Mark as read
                    }
                }
                allLines.add(line);
            }
        } catch (IOException e) {
            System.err.println("ERROR reading notification file: " + e.getMessage());
            return;
        }

        if (!foundUnread) {
            System.out.println("No new unread messages for '" + customerId + "'.");
        } else {
            updateNotificationFile(allLines);
            System.out.println("\n(Messages have been marked as READ.)");
        }
    }

   
    // FILE HELPER METHODS
   
    private static void ensureNotificationFileExists() {
        File file = new File(NOTIFICATIONS_FILE);
        try {
            if (file.createNewFile()) {
                System.out.println("[SYSTEM] Created " + NOTIFICATIONS_FILE);
            }
        } catch (IOException e) {
            System.err.println("ERROR creating " + NOTIFICATIONS_FILE + ": " + e.getMessage());
        }
    }

    private static void appendNotification(String customerId, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(NOTIFICATIONS_FILE, true))) {
            writer.write(customerId + "|UNREAD|" + message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("ERROR writing notification: " + e.getMessage());
        }
    }

    private static void updateNotificationFile(List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(NOTIFICATIONS_FILE, false))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("ERROR updating notification file: " + e.getMessage());
        }
    }

    private static void appendUserRecord(String username, String password, String role) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_FILE, true))) {
            writer.write(username + "," + password + "," + role);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("ERROR saving user: " + e.getMessage());
        }
    }

    private static void displayUsersByRole(String role) {
        File file = new File(USER_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[2].trim().equalsIgnoreCase(role)) {
                    System.out.println("- " + parts[0].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR reading users: " + e.getMessage());
        }
    }

    private static List<String> getAllCustomerUsernames() {
        List<String> customers = new ArrayList<>();
        File file = new File(USER_FILE);
        if (!file.exists()) return customers;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[2].trim().equalsIgnoreCase("customer")) {
                    customers.add(parts[0].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR reading customer accounts: " + e.getMessage());
        }
        return customers;
    }
}
