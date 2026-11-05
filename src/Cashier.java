import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Cashier {

    // --- CONSTANTS & FILE NAMES ---
    static final String SALES_FILE = "sales.txt";              // File where order receipts are logged
    static final String LOYALTY_FILE = "loyalty_card.txt";      // File storing customer visit stamps
    static final String NOTIFICATIONS_FILE = "notifications.txt"; // File storing customer inbox messages
    static final double PLAYGROUND_RATE_PER_HOUR = 100.00;     // Dog Playground fee: ₱100.00 per hour

    // Represents a single item added to the customer's shopping cart
    static class CartItem {
        String id;       // Item ID (e.g., "H101" or "D201")
        String name;     // Name of the drink/food
        double price;    // Price in Philippine Pesos (₱)
        int qty;         // Quantity ordered

        CartItem(String id, String name, double price, int qty) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.qty = qty;
        }

        double getSubtotal() {
            return price * qty;
        }
    }

    // Represents a customer's loyalty profile from loyalty_card.txt
    static class CustomerRecord {
        String cardId; // Customer's card ID (e.g., "C101")
        String name;   // Customer's name (e.g., "customer1")
        int visits;    // Accumulated visit count (0 to 10)

        CustomerRecord(String cardId, String name, int visits) {
            this.cardId = cardId;
            this.name = name;
            this.visits = visits;
        }

        String toLine() {
            return cardId + "," + name + "," + visits;
        }
    }

    // =========================================================================
    // 1. MAIN CASHIER POS MENU ENTRY POINT
    // =========================================================================
    public static void cashierMenu(Scanner scanner, String cashierUsername) {
        Manager.checkOrCreateInventoryFile();
        Manager.loadInventory();

        boolean inPOS = true;

        while (inPOS) {
            System.out.println("\n==========================================");
            System.out.println("            CASHIER POS COUNTER           ");
            System.out.println("==========================================");

            // Step 1: Scan inventory and automatically display low stock alerts (stock <= 5)
            displayLowStockAlerts();

            System.out.println("\n1. Process New Customer Order");
            System.out.println("2. View Today's Menu & Stock");
            System.out.println("3. Return to Main Menu");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    processOrder(scanner, cashierUsername);
                    break;
                case "2":
                    Manager.viewStock();
                    break;
                case "3":
                    System.out.println("Exiting POS Counter...");
                    inPOS = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose 1, 2, or 3.");
            }
        }
    }

    // =========================================================================
    // 2. LOW STOCK ALERT SCANNER
    // =========================================================================
    private static void displayLowStockAlerts() {
        int lowStockCount = 0;
        for (Manager.Item item : Manager.inventory) {
            if (item.qty <= Manager.LOW_STOCK_LIMIT) {
                if (lowStockCount == 0) {
                    System.out.println("⚠️  LOW STOCK DETECTED:");
                }
                System.out.printf(" - %s [%s]: %d left%n", item.id, item.name, item.qty);
                lowStockCount++;
            }
        }
        if (lowStockCount > 0) {
            System.out.println("------------------------------------------");
        }
    }

    // =========================================================================
    // 3. ORDER PROCESSING & CHECKOUT LOGIC
    // =========================================================================
    private static void processOrder(Scanner scanner, String cashierUsername) {
        List<CartItem> cart = new ArrayList<>();
        double subtotal = 0.0;

        System.out.println("\n--- NEW ORDER ENTRY ---");
        Manager.viewStock();

        // ITEM SELECTION LOOP
        boolean addingItems = true;
        while (addingItems) {
            System.out.print("\nEnter Item ID to order (or 'DONE' to finish, '0' to cancel): ");
            String itemId = scanner.nextLine().trim();

            if (itemId.equalsIgnoreCase("0")) {
                System.out.println("Order cancelled.");
                return;
            }

            if (itemId.equalsIgnoreCase("DONE")) {
                if (cart.isEmpty()) {
                    System.out.println("Cart is empty. Add at least one item or type '0' to cancel.");
                    continue;
                }
                addingItems = false;
                break;
            }

            Manager.Item item = Manager.findItemById(itemId);
            if (item == null) {
                System.out.println("Error: Item ID '" + itemId + "' not found.");
                continue;
            }

            if (item.qty <= 0) {
                System.out.println("Error: " + item.name + " is OUT OF STOCK!");
                continue;
            }

            System.out.print("Enter quantity for " + item.name + " (Available: " + item.qty + "): ");
            String qtyInput = scanner.nextLine().trim();
            int orderQty = 0;

            try {
                orderQty = Integer.parseInt(qtyInput);
                if (orderQty <= 0) {
                    System.out.println("Error: Quantity must be greater than 0.");
                    continue;
                }
                if (orderQty > item.qty) {
                    System.out.println("Error: Cannot order " + orderQty + ". Only " + item.qty + " in stock.");
                    continue;
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid number format. Please enter a whole number.");
                continue;
            }

            cart.add(new CartItem(item.id, item.name, item.price, orderQty));
            subtotal += (item.price * orderQty);
            System.out.printf("Added %dx %s to cart (Subtotal: ₱%.2f)%n", orderQty, item.name, subtotal);
        }

        // DOG PLAYGROUND ACCESS FEE (₱100.00 / HOUR)
        double playgroundFee = 0.0;
        int playgroundHours = 0;

        System.out.print("\nAdd Dog Playground Access Fee (₱100.00/hr)? (Y/N): ");
        String addPlayground = scanner.nextLine().trim();

        if (addPlayground.equalsIgnoreCase("Y")) {
            boolean validHours = false;
            while (!validHours) {
                System.out.print("Enter number of play hours requested: ");
                String hoursInput = scanner.nextLine().trim();
                try {
                    playgroundHours = Integer.parseInt(hoursInput);
                    if (playgroundHours > 0) {
                        playgroundFee = playgroundHours * PLAYGROUND_RATE_PER_HOUR;
                        validHours = true;
                        System.out.printf("🐶 Added %d hr(s) Dog Playground Fee: ₱%.2f%n", playgroundHours, playgroundFee);
                    } else {
                        System.out.println("Error: Hours must be at least 1.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: Please enter a whole number for hours.");
                }
            }
        }

        double grossTotal = subtotal + playgroundFee;

        // LOYALTY CARD & STAMP EVALUATION
        System.out.print("\nEnter Customer Loyalty Card ID (e.g. C101, or press Enter to skip): ");
        String cardId = scanner.nextLine().trim();

        CustomerRecord customer = null;
        double discountAmount = 0.0;

        if (!cardId.isEmpty()) {
            customer = findOrCreateLoyaltyRecord(scanner, cardId);
            customer.visits += 1;

            System.out.println("\n------------------------------------------");
            System.out.println("Customer Name: " + customer.name + " | Visit Count: " + customer.visits);

            if (customer.visits >= 10) {
                discountAmount = grossTotal * 0.15; // 15% Grand Combo Milestone Discount
                System.out.println("🎉 REWARD ELIGIBLE: 10th Visit Grand Combo Reward Applied (15% Off Total)!");
                customer.visits = 0; // Reset visit counter
            } else {
                System.out.println("Stamp added! " + (10 - customer.visits) + " more visit(s) needed for 10th Visit Reward.");
            }
            saveLoyaltyRecord(customer);
        }

        double netTotal = grossTotal - discountAmount;

        // BILLING & PAYMENT PROCESSING
        System.out.println("\n==========================================");
        System.out.println("               ORDER BILLING              ");
        System.out.println("==========================================");
        System.out.printf("Items Subtotal:      ₱%.2f%n", subtotal);
        if (playgroundFee > 0) {
            System.out.printf("Dog Playground (%d hr): ₱%.2f%n", playgroundHours, playgroundFee);
        }
        if (discountAmount > 0) {
            System.out.printf("Milestone Discount: -₱%.2f%n", discountAmount);
        }
        System.out.printf("Total Amount Due:    ₱%.2f%n", netTotal);
        System.out.println("------------------------------------------");

        double cashPaid = 0.0;
        boolean paymentValid = false;

        while (!paymentValid) {
            System.out.print("Enter Payment Received (₱): ");
            String cashInput = scanner.nextLine().trim();
            try {
                cashPaid = Double.parseDouble(cashInput);
                if (cashPaid >= netTotal) {
                    paymentValid = true;
                } else {
                    System.out.printf("Insufficient cash! Needs at least ₱%.2f%n", netTotal);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid numerical payment amount.");
            }
        }

        double change = cashPaid - netTotal;
        System.out.printf("Payment Received: ₱%.2f | Change: ₱%.2f%n", cashPaid, change);

        // DEDUCT STOCK FROM INVENTORY
        for (CartItem ci : cart) {
            Manager.Item item = Manager.findItemById(ci.id);
            if (item != null) {
                item.qty -= ci.qty;
            }
        }
        Manager.saveInventory();

        // LOG TRANSACTION TO SALES.TXT
        String receiptId = "REC-" + (System.currentTimeMillis() / 1000);
        logSalesTransaction(receiptId, cart, playgroundFee, netTotal, cashierUsername);

        // SEND INBOX NOTIFICATION TO CUSTOMER
        if (customer != null) {
            String msg = "Thank you for visiting! You earned +1 stamp. Current visits: " + customer.visits + "/10.";
            sendCustomerNotification(customer.name, msg);
        }

        System.out.println("\nSUCCESS: Transaction logged to sales.txt and inventory updated!");
    }

    // =========================================================================
    // 4. HELPER METHODS: LOYALTY CARD PERSISTENCE
    // =========================================================================

    private static CustomerRecord findOrCreateLoyaltyRecord(Scanner scanner, String cardId) {
        File file = new File(LOYALTY_FILE);
        List<CustomerRecord> list = new ArrayList<>();

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        // Fixed array indexing , [1], [2]
                        list.add(new CustomerRecord(parts.trim(), parts[1].trim(), Integer.parseInt(parts[2].trim())));
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.out.println("Warning reading loyalty file: " + e.getMessage());
            }
        }

        for (CustomerRecord cr : list) {
            if (cr.cardId.equalsIgnoreCase(cardId)) {
                return cr;
            }
        }

        System.out.print("First-time card user! Enter Customer Name for Card " + cardId + ": ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) name = "Valued Customer";

        return new CustomerRecord(cardId, name, 0);
    }

    private static void saveLoyaltyRecord(CustomerRecord record) {
        File file = new File(LOYALTY_FILE);
        List<CustomerRecord> list = new ArrayList<>();
        boolean found = false;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        if (parts.trim().equalsIgnoreCase(record.cardId)) {
                            list.add(record);
                            found = true;
                        } else {
                            list.add(new CustomerRecord(parts.trim(), parts[1].trim(), Integer.parseInt(parts[2].trim())));
                        }
                    }
                }
            } catch (IOException | NumberFormatException e) {
                // Keep loaded list intact
            }
        }

        if (!found) {
            list.add(record);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (CustomerRecord cr : list) {
                writer.println(cr.toLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving loyalty_card.txt: " + e.getMessage());
        }
    }

    // =========================================================================
    // 5. HELPER METHODS: TRANSACTION LOGGING & INBOX NOTIFICATIONS
    // =========================================================================

    private static void logSalesTransaction(String receiptId, List<CartItem> cart, double playgroundFee, double total, String cashier) {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        StringBuilder itemsSummary = new StringBuilder();

        for (CartItem ci : cart) {
            itemsSummary.append(ci.name).append("(").append(ci.qty).append("); ");
        }
        if (playgroundFee > 0) {
            itemsSummary.append("Dog Playground Fee; ");
        }

        String record = String.format("%s,%s,%s,%.2f,%s", receiptId, dateStr, itemsSummary.toString(), total, cashier);

        try (PrintWriter writer = new PrintWriter(new FileWriter(SALES_FILE, true))) {
            writer.println(record);
        } catch (IOException e) {
            System.out.println("Error writing to sales.txt: " + e.getMessage());
        }
    }

    private static void sendCustomerNotification(String recipient, String message) {
        File file = new File(NOTIFICATIONS_FILE);
        String record = recipient + "|UNREAD|[POST-PURCHASE STAMP] " + message;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            writer.println(record);
        } catch (IOException e) {
            System.out.println("Error writing notification: " + e.getMessage());
        }
    }
}