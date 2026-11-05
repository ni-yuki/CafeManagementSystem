import java.util.Scanner;

/*
 * Manager.java - Inventory Control & Supply Management
 * CRUD on inventory.txt: view stock, add item, update price / restock, remove item
 */
public class Manager {

    // An item with this many units or less is low stock
    static final int LOW_STOCK_LIMIT = 5;
    static final int MAX_ITEMS = 100;

    // Inventory is stored in parallel arrays: the same index = the same item
    static String[] itemIds = new String[MAX_ITEMS];
    static String[] itemCategories = new String[MAX_ITEMS];
    static String[] itemNames = new String[MAX_ITEMS];
    static double[] itemPrices = new double[MAX_ITEMS];
    static int[] itemQty = new int[MAX_ITEMS];
    static int itemCount = 0;

    // =========================================================================
    // MANAGER MENU
    // =========================================================================

    static void managerMenu(Scanner scanner, String managerUsername) {
        // Read inventory.txt into the arrays
        loadInventory();
        lowStockScan(); // automatic scan every time the manager opens inventory

        // Keep showing the Manager menu until the manager goes back
        boolean running = true;
        while (running) {
            Main.printHeader("MANAGER MENU (Inventory)");
            System.out.println("[1] View Stock Catalog");
            System.out.println("[2] Run Low Stock Scan");
            System.out.println("[3] Restock Item");
            System.out.println("[4] Add New Item");
            System.out.println("[5] Update Item Price");
            System.out.println("[6] Remove Item");
            System.out.println("[7] View Restock Log");
            System.out.println("[8] Back to Main Menu");
            // Read the manager's choice
            String choice = Main.ask(scanner, "Choose an option: ");

            // Run the chosen option
            switch (choice) {
                case "1":
                    viewStock();
                    break;
                case "2":
                    lowStockScan();
                    break;
                case "3":
                    restock(scanner, managerUsername);
                    break;
                case "4":
                    addItem(scanner);
                    break;
                case "5":
                    updatePrice(scanner);
                    break;
                case "6":
                    removeItem(scanner);
                    break;
                case "7":
                    viewRestockLog();
                    break;
                case "8":
                    // Stop the loop to go back to the main menu
                    running = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose 1-8.");
            }
        }
    }

    // =========================================================================
    // LOAD / SAVE inventory.txt
    // =========================================================================

    static void loadInventory() {
        // Get every line of inventory.txt
        String[] lines = Main.readLines(Main.INVENTORY_FILE);
        // Start counting the items from zero
        itemCount = 0;

        // Read each item line
        for (String line : lines) {
            // Split into id, category, name, price and quantity
            String[] parts = line.split(",");
            if (parts.length != 5 || itemCount >= MAX_ITEMS) {
                continue; // skip broken lines
            }
            try {
                // Change the price and quantity text into numbers
                double price = Double.parseDouble(parts[3].trim());
                int qty = Integer.parseInt(parts[4].trim());

                // Store each part in its own array at the same position
                itemIds[itemCount] = parts[0].trim().toUpperCase();
                itemCategories[itemCount] = parts[1].trim();
                itemNames[itemCount] = parts[2].trim();
                itemPrices[itemCount] = price;
                itemQty[itemCount] = qty;
                // Now there is one more item stored
                itemCount++;
            } catch (NumberFormatException e) {
                System.out.println("Skipped bad inventory line: " + line);
            }
        }
    }

    static void saveInventory() {
        // Turn every item back into one text line
        String[] lines = new String[itemCount];
        for (int i = 0; i < itemCount; i++) {
            lines[i] = itemIds[i] + "," + itemCategories[i] + "," + itemNames[i] + ","
                    + String.format("%.2f", itemPrices[i]) + "," + itemQty[i];
        }
        // Overwrite inventory.txt with the updated list
        Main.writeLines(Main.INVENTORY_FILE, lines, itemCount);
    }

    // Returns the index of the item, or -1 if the ID does not exist
    static int findItemIndex(String id) {
        // Ignore upper and lower case when comparing
        String target = id.toUpperCase();
        for (int i = 0; i < itemCount; i++) {
            // Found it, so return its position
            if (itemIds[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }

    // Asks for an item ID until a valid one is typed. Returns -1 if cancelled with 0.
    static int askItemIndex(Scanner scanner, String prompt) {
        // Repeat until a real item ID is typed
        while (true) {
            String input = Main.ask(scanner, prompt);
            // 0 means cancel
            if (input.equals("0")) {
                return -1;
            }
            int index = findItemIndex(input);
            if (index != -1) {
                return index;
            }
            System.out.println("Error: No item with ID '" + input + "'.");
        }
    }

    // =========================================================================
    // VIEW STOCK (Get)
    // =========================================================================

    static void viewStock() {
        System.out.println("\n=== CURRENT STOCK ===");
        printSection("[1] Human Drinks & Food", "Human");
        printSection("[2] Dog Drinks & Treats", "Dog");
    }

    static void printSection(String title, String categoryPrefix) {
        System.out.println("\n" + title);
        System.out.println(Main.pad("ID", 7) + Main.pad("Category", 13) + Main.pad("Item", 25)
                + Main.pad("Price", 12) + "Qty");
        System.out.println("----------------------------------------------------------------");
        for (int i = 0; i < itemCount; i++) {
            // Show only the items of this section (Human or Dog)
            if (itemCategories[i].startsWith(categoryPrefix)) {
                // Add a warning next to items that are almost out of stock
                String flag = (itemQty[i] <= LOW_STOCK_LIMIT) ? "  (Low Stock)" : "";
                System.out.println(Main.pad(itemIds[i], 7) + Main.pad(itemCategories[i], 13)
                        + Main.pad(itemNames[i], 25) + Main.pad(Main.peso(itemPrices[i]), 12)
                        + itemQty[i] + flag);
            }
        }
    }

    // =========================================================================
    // LOW STOCK DETECTOR
    // =========================================================================

    // Warns about every item with 5 or fewer units. Returns how many were flagged.
    static int lowStockScan() {
        System.out.println("\n=== LOW STOCK SCAN ===");
        // Count how many items are low
        int flagged = 0;
        for (int i = 0; i < itemCount; i++) {
            // 5 or fewer units left means low stock
            if (itemQty[i] <= LOW_STOCK_LIMIT) {
                System.out.println("WARNING: " + itemNames[i] + " (" + itemIds[i] + ") - only "
                        + itemQty[i] + " left!");
                flagged++;
            }
        }
        if (flagged == 0) {
            System.out.println("All items are well stocked.");
        } else {
            System.out.println(flagged + " item(s) need restocking.");
        }
        return flagged;
    }

    // =========================================================================
    // RESTOCK (Update quantity + log)
    // =========================================================================

    static void restock(Scanner scanner, String managerUsername) {
        System.out.println("\n=== RESTOCK ===");
        viewStock();

        // Choose the item to restock
        int index = askItemIndex(scanner, "\nEnter item ID to restock, e.g. H101 (0 to cancel): ");
        if (index == -1) {
            System.out.println("Restock cancelled.");
            return;
        }

        // Ask for the shipment size (it must be more than 0)
        int shipment = 0;
        while (shipment <= 0) {
            shipment = Main.askInt(scanner, "Enter shipment quantity for " + itemNames[index] + ": ");
            if (shipment <= 0) {
                System.out.println("Error: Quantity must be greater than 0.");
            }
        }

        // Add the shipment to the current stock
        itemQty[index] += shipment;
        // Save the change to inventory.txt
        saveInventory();

        // Restock notice saved in restock_log.txt
        Main.appendLine(Main.RESTOCK_LOG_FILE, itemIds[index] + "," + itemNames[index] + ","
                + shipment + "," + itemQty[index] + "," + managerUsername);

        System.out.println("Restocked! " + itemNames[index] + " now has " + itemQty[index] + " in stock.");
    }

    static void viewRestockLog() {
        // Read the restock history
        String[] logs = Main.readLines(Main.RESTOCK_LOG_FILE);
        System.out.println("\n=== RESTOCK LOG ===");
        // Nothing is recorded yet
        if (logs.length == 0) {
            System.out.println("No restocks recorded yet.");
            return;
        }
        for (String line : logs) {
            String[] parts = line.split(",");
            if (parts.length == 5) {
                System.out.println("- " + parts[1] + " (" + parts[0] + "): +" + parts[2]
                        + " units, new stock " + parts[3] + " [by " + parts[4] + "]");
            }
        }
    }

    // =========================================================================
    // ADD NEW ITEM (Create)
    // =========================================================================

    static void addItem(Scanner scanner) {
        System.out.println("\n=== ADD NEW ITEM ===");
        // The arrays can only hold 100 items
        if (itemCount >= MAX_ITEMS) {
            System.out.println("Error: Inventory is full (" + MAX_ITEMS + " items).");
            return;
        }

        // Ask for the details of the new item
        String id = Main.ask(scanner, "Enter new Item ID (e.g. H109 or D209): ").toUpperCase();
        if (!Main.isValidText(id) || id.contains(" ")) {
            System.out.println("Error: Item ID cannot be empty or contain spaces, commas, or |.");
            return;
        }
        // Every item ID must be different
        if (findItemIndex(id) != -1) {
            System.out.println("Error: Item ID " + id + " already exists.");
            return;
        }

        System.out.println("Category: [1] Human_Drink  [2] Human_Food  [3] Dog_Drink  [4] Dog_Treat");
        String categoryChoice = Main.ask(scanner, "Choose category: ");
        String category;
        // Change the number into a category name
        switch (categoryChoice) {
            case "1":
                category = "Human_Drink";
                break;
            case "2":
                category = "Human_Food";
                break;
            case "3":
                category = "Dog_Drink";
                break;
            case "4":
                category = "Dog_Treat";
                break;
            default:
                System.out.println("Error: Invalid category.");
                return;
        }

        String name = Main.ask(scanner, "Enter item name: ");
        if (!Main.isValidText(name)) {
            System.out.println("Error: Name cannot be empty or contain commas or |.");
            return;
        }

        double price = Main.askDouble(scanner, "Enter price (PHP): ");
        if (price <= 0) {
            System.out.println("Error: Price must be greater than 0.");
            return;
        }

        int qty = Main.askInt(scanner, "Enter starting quantity: ");
        if (qty < 0) {
            System.out.println("Error: Quantity cannot be negative.");
            return;
        }

        // Put the new item at the end of the arrays
        itemIds[itemCount] = id;
        itemCategories[itemCount] = category;
        itemNames[itemCount] = name;
        itemPrices[itemCount] = Main.roundMoney(price);
        itemQty[itemCount] = qty;
        // Now there is one more item stored
        itemCount++;
        // Save the change to inventory.txt
        saveInventory();
        System.out.println("SUCCESS: " + name + " (" + id + ") added to the menu.");
    }

    // =========================================================================
    // UPDATE PRICE (Update)
    // =========================================================================

    static void updatePrice(Scanner scanner) {
        System.out.println("\n=== UPDATE ITEM PRICE ===");
        viewStock();

        int index = askItemIndex(scanner, "\nEnter item ID to update (0 to cancel): ");
        if (index == -1) {
            System.out.println("Update cancelled.");
            return;
        }

        System.out.println("Current price of " + itemNames[index] + ": " + Main.peso(itemPrices[index]));
        // Ask for the new price
        double newPrice = Main.askDouble(scanner, "Enter new price (PHP): ");
        if (newPrice <= 0) {
            System.out.println("Error: Price must be greater than 0.");
            return;
        }

        // Save the new price (rounded to centavos)
        itemPrices[index] = Main.roundMoney(newPrice);
        // Save the change to inventory.txt
        saveInventory();
        System.out.println("SUCCESS: " + itemNames[index] + " now costs " + Main.peso(itemPrices[index]) + ".");
    }

    // =========================================================================
    // REMOVE ITEM (Delete)
    // =========================================================================

    static void removeItem(Scanner scanner) {
        System.out.println("\n=== REMOVE ITEM ===");
        viewStock();

        int index = askItemIndex(scanner, "\nEnter item ID to remove (0 to cancel): ");
        if (index == -1) {
            System.out.println("Remove cancelled.");
            return;
        }

        // Ask for Y or N before removing
        String confirm = Main.ask(scanner, "Remove " + itemNames[index] + " from the menu? (Y/N): ");
        if (!confirm.toUpperCase().equals("Y")) {
            System.out.println("Remove cancelled.");
            return;
        }

        // Keep the name to show in the message later
        String removedName = itemNames[index];

        // Shift every item after it one place to the left
        for (int i = index; i < itemCount - 1; i++) {
            itemIds[i] = itemIds[i + 1];
            itemCategories[i] = itemCategories[i + 1];
            itemNames[i] = itemNames[i + 1];
            itemPrices[i] = itemPrices[i + 1];
            itemQty[i] = itemQty[i + 1];
        }
        // There is now one less item
        itemCount--;
        // Save the change to inventory.txt
        saveInventory();
        System.out.println("SUCCESS: " + removedName + " was removed.");
    }
}
