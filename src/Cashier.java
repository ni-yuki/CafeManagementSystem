import java.util.Scanner;

/*
 * Cashier.java - Front-Desk POS Operations
 * Takes orders, adds the dog playground fee, applies loyalty rewards,
 * accepts cash, logs sales, adds stamps, and sends stamp messages to customers.
 */
public class Cashier {

    static final double PLAYGROUND_RATE = 100.00; // Dog Playground fee per hour
    static final int MAX_CART = 50;

    // =========================================================================
    // CASHIER MENU
    // =========================================================================

    static void cashierMenu(Scanner scanner, String cashierUsername) {
        // Load the stock so the cashier can sell items
        Manager.loadInventory();
        Manager.lowStockScan(); // shows low stock alerts when the POS opens

        // Keep showing the POS menu until the cashier goes back
        boolean inPOS = true;
        while (inPOS) {
            Main.printHeader("CASHIER POS COUNTER");
            System.out.println("[1] Process New Customer Order");
            System.out.println("[2] View Today's Menu & Stock");
            System.out.println("[3] Register New Loyalty Card Customer");
            System.out.println("[4] Back to Main Menu");
            // Read the cashier's choice
            String choice = Main.ask(scanner, "Choose an option: ");

            // Run the chosen option
            switch (choice) {
                case "1":
                    processOrder(scanner, cashierUsername);
                    break;
                case "2":
                    Manager.viewStock();
                    break;
                case "3":
                    registerCustomer(scanner);
                    break;
                case "4":
                    // Stop the loop to go back to the main menu
                    inPOS = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose 1-4.");
            }
        }
    }

    // =========================================================================
    // PROCESS ORDER
    // =========================================================================

    static void processOrder(Scanner scanner, String cashierUsername) {
        Manager.loadInventory(); // always use the latest stock
        System.out.println("\n--- NEW ORDER ENTRY ---");

        // ---------- STEP 1: LOYALTY CARD ----------
        // These variables remember the customer's loyalty card
        String cardId = "";
        String customerUsername = "";
        int oldStamps = 0;

        // Ask for the card until a valid one is typed or skipped
        while (true) {
            String input = Main.ask(scanner, "Enter Loyalty Card ID (e.g. LC201) or press Enter to skip: ").toUpperCase();
            // Pressing Enter skips the card (walk-in customer)
            if (input.length() == 0) {
                System.out.println("No loyalty card. This is a walk-in order.");
                break;
            }
            // Find who owns this card
            customerUsername = findCardOwner(input);
            if (customerUsername.length() > 0) {
                cardId = input;
                oldStamps = Customer.getStamps(customerUsername);
                System.out.println("Card found: " + customerUsername + " | Stamps: " + oldStamps + "/" + Customer.STAMP_GOAL);

                // Tell the cashier which reward this visit will unlock
                String upcoming = Customer.getRewardAt(oldStamps + 1);
                if (upcoming.length() > 0) {
                    System.out.println("*** This visit unlocks: " + upcoming + " ***");
                    if (oldStamps + 1 == 5) {
                        System.out.println("    (Add one Dog Treat to the order so it can be given for free.)");
                    }
                }
                break;
            }
            System.out.println("Error: Card ID '" + input + "' not found. Try again or press Enter to skip.");
        }

        // ---------- STEP 2: ITEM SELECTION ----------
        // The cart uses two arrays: the item position and the quantity
        int[] cartItem = new int[MAX_CART]; // index of the item in Manager's arrays
        int[] cartQty = new int[MAX_CART];
        int cartCount = 0;
        double itemsSubtotal = 0;

        // Show the items the customer can order
        Manager.viewStock();
        // Keep adding items until the cashier types DONE
        boolean adding = true;
        while (adding) {
            String input = Main.ask(scanner, "\nEnter Item ID to order (DONE to finish, 0 to cancel): ").toUpperCase();

            // 0 cancels the whole order
            if (input.equals("0")) {
                System.out.println("Order cancelled.");
                return;
            }
            // DONE means the customer finished ordering
            if (input.equals("DONE")) {
                if (cartCount == 0) {
                    System.out.println("Cart is empty. Add at least one item or type 0 to cancel.");
                } else {
                    adding = false;
                }
                continue;
            }

            // Find the item in the inventory
            int index = Manager.findItemIndex(input);
            if (index == -1) {
                System.out.println("Error: Item ID '" + input + "' not found.");
                continue;
            }

            // Check if this item is already in the cart
            int cartPos = -1;
            for (int c = 0; c < cartCount; c++) {
                if (cartItem[c] == index) {
                    cartPos = c;
                }
            }
            int alreadyInCart = (cartPos == -1) ? 0 : cartQty[cartPos];
            // Stock left after counting what is already in the cart
            int available = Manager.itemQty[index] - alreadyInCart;

            // Cannot sell an item that is out of stock
            if (available <= 0) {
                System.out.println("Error: " + Manager.itemNames[index] + " is OUT OF STOCK!");
                continue;
            }
            if (cartPos == -1 && cartCount >= MAX_CART) {
                System.out.println("Error: Cart is full.");
                continue;
            }

            // Ask how many the customer wants
            int qty = Main.askInt(scanner, "Enter quantity for " + Manager.itemNames[index] + " (Available: " + available + "): ");
            if (qty <= 0) {
                System.out.println("Error: Quantity must be greater than 0.");
                continue;
            }
            if (qty > available) {
                System.out.println("Error: Only " + available + " in stock.");
                continue;
            }

            // A new item goes to the cart; an item already in the cart just gets more quantity
            if (cartPos == -1) {
                cartItem[cartCount] = index;
                cartQty[cartCount] = qty;
                cartCount++;
            } else {
                cartQty[cartPos] += qty;
            }
            // Add price x quantity to the running subtotal
            itemsSubtotal += Manager.itemPrices[index] * qty;
            System.out.println("Added " + qty + "x " + Manager.itemNames[index] + " (Subtotal: " + Main.peso(itemsSubtotal) + ")");
        }

        // ---------- STEP 3: DOG PLAYGROUND FEE ----------
        int playgroundHours = 0;
        double playgroundFee = 0;
        // Ask if the dog will use the playground
        String addPlayground = Main.ask(scanner, "\nAdd Dog Playground Access (" + Main.peso(PLAYGROUND_RATE) + "/hr)? (Y/N): ");
        if (addPlayground.toUpperCase().equals("Y")) {
            while (playgroundHours <= 0) {
                playgroundHours = Main.askInt(scanner, "Enter number of play hours: ");
                if (playgroundHours <= 0) {
                    System.out.println("Error: Hours must be at least 1.");
                }
            }
            // Fee = hours x rate per hour
            playgroundFee = playgroundHours * PLAYGROUND_RATE;
            System.out.println("Added " + playgroundHours + " hr(s) Dog Playground Fee: " + Main.peso(playgroundFee));
        }

        // Total before any discount
        double grossTotal = itemsSubtotal + playgroundFee;

        // ---------- STEP 4: LOYALTY STAMP & REWARD ----------
        int newStamps = oldStamps;
        double discount = 0;
        String rewardText = "";

        // Only loyalty card customers earn stamps and rewards
        if (cardId.length() > 0) {
            // This visit adds one more stamp
            newStamps = oldStamps + 1;

            // Stamp 2: 10% off the whole order
            if (newStamps == 2) {
                discount = grossTotal * 0.10;
                rewardText = "Stamp 2 Welcome Reward (10% off)";
            } else if (newStamps == 5) {
                // Stamp 5: a free dog treat (its price becomes the discount)
                int treat = findFirstDogTreat(cartItem, cartCount);
                if (treat != -1) {
                    discount = Manager.itemPrices[treat];
                    rewardText = "Stamp 5 Reward (Free " + Manager.itemNames[treat] + ")";
                } else {
                    System.out.println("Note: No Dog Treat in this order, so the free treat reward was not used.");
                }
            } else if (newStamps == 8) {
                // Stamp 8: 20% off the drinks only
                double beverages = 0;
                for (int c = 0; c < cartCount; c++) {
                    // Add up only the drink items
                    if (Manager.itemCategories[cartItem[c]].endsWith("Drink")) {
                        beverages += Manager.itemPrices[cartItem[c]] * cartQty[c];
                    }
                }
                discount = beverages * 0.20;
                rewardText = "Stamp 8 Reward (20% off beverages)";
            } else if (newStamps >= Customer.STAMP_GOAL) {
                // Stamp 10: 50% off the whole order
                discount = grossTotal * 0.50;
                rewardText = "Stamp 10 Grand Reward (50% off)";
            }
        }

        // Round the money to 2 decimal places
        discount = Main.roundMoney(discount);
        double netTotal = Main.roundMoney(grossTotal - discount);

        // ---------- STEP 5: BILL ----------
        Main.printHeader("ORDER BILLING");
        for (int c = 0; c < cartCount; c++) {
            int i = cartItem[c];
            System.out.println(Main.pad(cartQty[c] + "x " + Manager.itemNames[i], 40) + Main.peso(Manager.itemPrices[i] * cartQty[c]));
        }
        if (playgroundFee > 0) {
            System.out.println(Main.pad("Dog Playground (" + playgroundHours + " hr)", 40) + Main.peso(playgroundFee));
        }
        System.out.println("----------------------------------------------------");
        System.out.println(Main.pad("Gross Total:", 40) + Main.peso(grossTotal));
        if (discount > 0) {
            System.out.println(Main.pad(rewardText + ":", 40) + "-" + Main.peso(discount));
        }
        System.out.println(Main.pad("TOTAL AMOUNT DUE:", 40) + Main.peso(netTotal));
        System.out.println("----------------------------------------------------");

        // ---------- STEP 6: CASH PAYMENT ----------
        // Keep asking until the customer pays enough
        double cashPaid = 0;
        boolean paymentValid = false;
        while (!paymentValid) {
            cashPaid = Main.askDouble(scanner, "Enter Payment Received (PHP): ");
            // The payment must cover the total
            if (cashPaid >= netTotal) {
                paymentValid = true;
            } else {
                System.out.println("Insufficient cash! Needs at least " + Main.peso(netTotal));
            }
        }
        // Change = money received - total due
        double change = Main.roundMoney(cashPaid - netTotal);
        System.out.println("Payment Received: " + Main.peso(cashPaid) + " | Change: " + Main.peso(change));

        // ---------- STEP 7: DEDUCT STOCK ----------
        for (int c = 0; c < cartCount; c++) {
            // Subtract the sold quantity from the stock
            Manager.itemQty[cartItem[c]] -= cartQty[c];
        }
        Manager.saveInventory();

        // ---------- STEP 8: LOG SALE ----------
        // New receipt number: R1001, R1002, and so on
        String receiptId = "R" + (1001 + Main.readLines(Main.SALES_FILE).length);
        // Build one text with all items, like: Latte x2; Muffin x1
        String items = "";
        for (int c = 0; c < cartCount; c++) {
            items += Manager.itemNames[cartItem[c]] + " x" + cartQty[c];
            if (c < cartCount - 1) {
                items += "; ";
            }
        }
        // Orders without a card are saved as WALK-IN
        String cardForLog = (cardId.length() > 0) ? cardId : "WALK-IN";
        // Save the sale as one new line in sales.txt
        Main.appendLine(Main.SALES_FILE, receiptId + "," + cashierUsername + "," + cardForLog + "," + items + ","
                + String.format("%.2f", playgroundFee) + "," + String.format("%.2f", discount) + ","
                + String.format("%.2f", netTotal));

        // ---------- STEP 9: SAVE STAMP & NOTIFY CUSTOMER ----------
        if (cardId.length() > 0) {
            // This is the message the customer will see in the inbox
            String message = "[POST-PURCHASE STAMP] Thank you for visiting! Receipt " + receiptId
                    + ". You earned +1 stamp. Current stamps: " + newStamps + "/" + Customer.STAMP_GOAL + ".";
            if (rewardText.length() > 0) {
                message += " Reward used: " + rewardText + ".";
            }
            if (newStamps >= Customer.STAMP_GOAL) {
                newStamps = 0; // card completed, start a new card
                message += " Your card is complete and has been reset to 0/10.";
            }
            // Save the new stamp count in loyalty_card.txt
            updateStamps(cardId, newStamps);
            // Send the message to the customer's inbox
            Main.sendNotification(customerUsername, message);
            System.out.println("Stamp added! " + customerUsername + " now has " + newStamps + "/" + Customer.STAMP_GOAL + " stamps.");
        }

        System.out.println("\nSUCCESS: Receipt " + receiptId + " saved to sales.txt and inventory updated!");
    }

    // Returns the index of the first Dog_Treat in the cart, or -1
    static int findFirstDogTreat(int[] cartItem, int cartCount) {
        for (int c = 0; c < cartCount; c++) {
            // Return the first dog treat found in the cart
            if (Manager.itemCategories[cartItem[c]].equals("Dog_Treat")) {
                return cartItem[c];
            }
        }
        return -1;
    }

    // =========================================================================
    // LOYALTY CARD FILE HELPERS
    // =========================================================================

    // Returns the username that owns a card, or "" if the card does not exist
    static String findCardOwner(String cardId) {
        String[] cards = Main.readLines(Main.LOYALTY_FILE);
        for (String line : cards) {
            String[] parts = line.split(",");
            // The card ID matches, so return the owner's username
            if (parts.length == 3 && parts[0].equals(cardId)) {
                return parts[1];
            }
        }
        return "";
    }

    // Rewrites loyalty_card.txt with the new stamp count for one card
    static void updateStamps(String cardId, int stamps) {
        String[] cards = Main.readLines(Main.LOYALTY_FILE);
        for (int i = 0; i < cards.length; i++) {
            String[] parts = cards[i].split(",");
            if (parts.length == 3 && parts[0].equals(cardId)) {
                // Change only the stamp count of this card
                cards[i] = parts[0] + "," + parts[1] + "," + stamps;
            }
        }
        Main.writeLines(Main.LOYALTY_FILE, cards, cards.length);
    }

    // =========================================================================
    // REGISTER NEW CUSTOMER (creates a customer account + loyalty card)
    // =========================================================================

    static void registerCustomer(Scanner scanner) {
        System.out.println("\n--- REGISTER NEW LOYALTY CARD CUSTOMER ---");
        // Ask for the new customer's login details
        String username = Main.ask(scanner, "Enter new customer username: ");
        if (!Main.isValidText(username) || username.contains(" ")) {
            System.out.println("Error: Username cannot be empty or contain spaces, commas, or |.");
            return;
        }
        // The username must be new
        if (Main.findUserIndex(Main.readLines(Main.USER_FILE), username) != -1) {
            System.out.println("Error: That username is already taken.");
            return;
        }

        String password = Main.ask(scanner, "Enter password for the customer: ");
        if (!Main.isValidText(password)) {
            System.out.println("Error: Password cannot be empty or contain commas or |.");
            return;
        }

        // Get a new unique user ID
        int userId = Main.getNextUserId();
        // The loyalty card ID is LC + the user ID
        String cardId = "LC" + userId;

        // Create the account and the loyalty card (starts at 0 stamps)
        Main.appendLine(Main.USER_FILE, userId + "," + username + "," + password + ",Customer");
        Main.appendLine(Main.LOYALTY_FILE, cardId + "," + username + ",0");
        // Send a welcome message to the new customer
        Main.sendNotification(username, "[WELCOME] Welcome to Barks & Brews! Your loyalty card is " + cardId
                + ". Get a 10% discount on your 2nd visit!");

        System.out.println("SUCCESS: Customer '" + username + "' registered with Loyalty Card " + cardId + ".");
    }
}
