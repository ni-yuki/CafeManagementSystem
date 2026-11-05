import java.util.Scanner;

/*
 * Customer.java - Customer Portal (10-Visit Loyalty Card & Inbox)
 * Reads loyalty_card.txt and notifications.txt; can mark messages as read and delete them
 */
public class Customer {

    // A full loyalty card has 10 stamps
    static final int STAMP_GOAL = 10;

    // Milestone rewards: same index = same reward
    static final int[] REWARD_STAMPS = {2, 5, 8, 10};
    static final String[] REWARD_NAMES = {
        "10% Welcome Discount on your whole order",
        "1 FREE Dog Treat",
        "20% Discount on all beverages",
        "50% OFF your whole purchase"
    };

    // =========================================================================
    // CUSTOMER MENU
    // =========================================================================

    static void customerMenu(Scanner scanner, String username) {
        // Keep showing the Customer menu until the customer goes back
        boolean running = true;
        while (running) {
            Main.printHeader("MY LOYALTY CARD & INBOX");
            // Count the messages that the customer has not read yet
            int unread = countMessages(username, "UNREAD");
            System.out.println("You have " + unread + " unread message(s).");
            System.out.println("------------------------------------------");
            System.out.println("[1] View My Stamps");
            System.out.println("[2] Check My Rewards");
            System.out.println("[3] Read Inbox");
            System.out.println("[4] Delete an Inbox Message");
            System.out.println("[5] Back to Main Menu");
            String choice = Main.ask(scanner, "Choose an option: ");

            // Run the chosen option
            switch (choice) {
                case "1":
                    viewStamps(username);
                    break;
                case "2":
                    rewardCheck(username);
                    break;
                case "3":
                    readInbox(username);
                    break;
                case "4":
                    deleteMessage(scanner, username);
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
    // LOYALTY CARD HELPERS (also used by Cashier and Admin)
    // loyalty_card.txt line format: cardId,username,stamps
    // =========================================================================

    // Returns the card ID of a customer, or "" if they have no card
    static String getCardId(String username) {
        // Load all loyalty cards
        String[] cards = Main.readLines(Main.LOYALTY_FILE);
        for (String line : cards) {
            String[] parts = line.split(",");
            // This card belongs to the customer
            if (parts.length == 3 && parts[1].equals(username)) {
                return parts[0];
            }
        }
        // Empty text means no card was found
        return "";
    }

    // Returns the stamp count of a customer, or -1 if they have no card
    static int getStamps(String username) {
        // Load all loyalty cards
        String[] cards = Main.readLines(Main.LOYALTY_FILE);
        for (String line : cards) {
            String[] parts = line.split(",");
            // This card belongs to the customer
            if (parts.length == 3 && parts[1].equals(username)) {
                try {
                    // parts[2] is the stamp count
                    return Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        // -1 means the customer has no card
        return -1;
    }

    // Returns the text of the reward unlocked at this stamp count, or "" if none
    static String getRewardAt(int stamps) {
        for (int i = 0; i < REWARD_STAMPS.length; i++) {
            if (REWARD_STAMPS[i] == stamps) {
                return REWARD_NAMES[i];
            }
        }
        // Empty text means there is no reward at this stamp count
        return "";
    }

    // =========================================================================
    // VIEW STAMPS
    // =========================================================================

    static void viewStamps(String username) {
        // Get the customer's stamp count
        int stamps = getStamps(username);
        // The customer has no loyalty card yet
        if (stamps == -1) {
            System.out.println("\nYou don't have a loyalty card yet. Ask the cashier to register one!");
            return;
        }

        System.out.println("\n--- MY LOYALTY CARD (" + getCardId(username) + ") ---");
        // Draw the card: [X] is a stamp, [ ] is an empty box
        String card = "";
        for (int i = 1; i <= STAMP_GOAL; i++) {
            card += (i <= stamps) ? "[X]" : "[ ]";
        }
        System.out.println(card);
        System.out.println("Stamps collected: " + stamps + "/" + STAMP_GOAL);
        System.out.println("Visits left to complete the card: " + (STAMP_GOAL - stamps));
    }

    // =========================================================================
    // REWARD CHECK
    // =========================================================================

    static void rewardCheck(String username) {
        // Get the customer's stamp count
        int stamps = getStamps(username);
        // The customer has no loyalty card yet
        if (stamps == -1) {
            System.out.println("\nYou don't have a loyalty card yet. Ask the cashier to register one!");
            return;
        }

        System.out.println("\n--- MILESTONE REWARDS (you have " + stamps + " stamp(s)) ---");
        for (int i = 0; i < REWARD_STAMPS.length; i++) {
            String status;
            // Reward already reached
            if (stamps >= REWARD_STAMPS[i]) {
                status = "USED";
            } else if (stamps + 1 == REWARD_STAMPS[i]) {
                // Reward unlocks on the next visit
                status = "UNLOCKS ON YOUR NEXT VISIT!";
            } else {
                status = (REWARD_STAMPS[i] - stamps) + " more visit(s)";
            }
            System.out.println("Stamp " + REWARD_STAMPS[i] + " - " + REWARD_NAMES[i] + "  -> " + status);
        }
        System.out.println("Rewards are applied automatically by the cashier on the visit that earns the stamp.");
    }

    // =========================================================================
    // INBOX (notifications.txt line format: username|status|message)
    // The | character is special in split(), so we write "\\|" to split on it.
    // =========================================================================

    static int countMessages(String username, String status) {
        // Load all messages from notifications.txt
        String[] lines = Main.readLines(Main.NOTIFICATIONS_FILE);
        int count = 0;
        for (String line : lines) {
            // A message line looks like: username|status|message
            String[] parts = line.split("\\|");
            // Count the messages of this user that have this status
            if (parts.length == 3 && parts[0].equals(username) && parts[1].equals(status)) {
                count++;
            }
        }
        return count;
    }

    // Shows all messages, then marks them as READ
    static void readInbox(String username) {
        // Load all messages from notifications.txt
        String[] lines = Main.readLines(Main.NOTIFICATIONS_FILE);
        System.out.println("\n--- MY INBOX ---");

        int number = 0;
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split("\\|");
            if (parts.length == 3 && parts[0].equals(username)) {
                number++;
                // Show (NEW) beside messages that were not read before
                String tag = parts[1].equals("UNREAD") ? " (NEW)" : "";
                System.out.println(number + ". " + parts[2] + tag);
                lines[i] = parts[0] + "|READ|" + parts[2]; // mark as read
            }
        }

        // No messages were found
        if (number == 0) {
            System.out.println("Your inbox is empty.");
            return;
        }
        // Save the new READ status to the file
        Main.writeLines(Main.NOTIFICATIONS_FILE, lines, lines.length);
    }

    static void deleteMessage(Scanner scanner, String username) {
        // Load all messages from notifications.txt
        String[] lines = Main.readLines(Main.NOTIFICATIONS_FILE);
        int[] positions = new int[lines.length]; // which file line each numbered message is on
        int number = 0;

        System.out.println("\n--- DELETE A MESSAGE ---");
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split("\\|");
            if (parts.length == 3 && parts[0].equals(username)) {
                positions[number] = i;
                number++;
                System.out.println(number + ". " + parts[2]);
            }
        }

        // No messages were found
        if (number == 0) {
            System.out.println("Your inbox is empty.");
            return;
        }

        // Ask which message to delete
        int pick = Main.askInt(scanner, "Enter message number to delete (0 to cancel): ");
        if (pick == 0) {
            System.out.println("Delete cancelled.");
            return;
        }
        if (pick < 1 || pick > number) {
            System.out.println("Error: No message with that number.");
            return;
        }

        // Find the real line number of that message in the file
        int removeAt = positions[pick - 1];
        // Copy every line except the deleted one into a new array
        String[] remaining = new String[lines.length - 1];
        int index = 0;
        for (int i = 0; i < lines.length; i++) {
            if (i != removeAt) {
                remaining[index] = lines[i];
                index++;
            }
        }
        // Save the file without the deleted message
        Main.writeLines(Main.NOTIFICATIONS_FILE, remaining, remaining.length);
        System.out.println("Message deleted.");
    }
}
