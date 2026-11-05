import java.io.*;
import java.util.*;

public class Manager {

    static final int LOW_STOCK_LIMIT = 5;
    static final String FILE_NAME = "inventory.txt";

    static class Item {
        String id;
        String category;
        String name;
        double price; // PHP (₱)
        int qty;

        Item(String id, String category, String name, double price, int qty) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.price = price;
            this.qty = qty;
        }

        String toLine() {
            return id + "|" + category + "|" + name + "|" + String.format("%.2f", price) + "|" + qty;
        }
    }

    static List<Item> inventory = new ArrayList<>();
    static Scanner scanner; // Shared with Main

    // Create default inventory file if missing
    static void checkOrCreateInventoryFile() {
        File file = new File(FILE_NAME);
        if (file.exists()) return;

        String[] defaults = {
            "H101|Human_Drink|Caramel Macchiato|220.00|20",
            "H102|Human_Drink|Iced Matcha Latte|235.00|15",
            "H103|Human_Drink|Cold Brew Coffee|185.00|25",
            "H104|Human_Drink|Chocolate Frappucino|200.00|22",
            "H105|Human_Food|Blueberry Muffin|160.00|10",
            "H106|Human_Food|Butter Croissant|140.00|12",
            "H107|Human_Food|Tiramisu|200.00|9",
            "H108|Human_Food|Macaroons|190.00|7",
            "D201|Dog_Drink|Classic Puppuccino|175.00|30",
            "D202|Dog_Drink|Dog Chamomile Tea|195.00|4",
            "D203|Dog_Drink|Goat Milk Latte|180.00|15",
            "D204|Dog_Drink|Bone Broth Refresher|165.00|12",
            "D205|Dog_Treat|Peanut Butter Bites|125.00|3",
            "D206|Dog_Treat|Chicken Jerky Crunch|140.00|20",
            "D207|Dog_Treat|Pupsicle Berry Freeze|160.00|2",
            "D208|Dog_Treat|Salmon Chew Stick|150.00|18"
        };

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (String line : defaults) {
                writer.println(line);
            }
            System.out.println("[SYSTEM INFO] 'inventory.txt' not found. Created with 16 default items.");
        } catch (IOException e) {
            System.out.println("Could not create inventory.txt: " + e.getMessage());
        }
    }

    // Load inventory from inventory.txt
    static void loadInventory() {
        inventory.clear();
        File file = new File(FILE_NAME);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|");
                if (p.length != 5) continue;

                try {
                    inventory.add(new Item(
                        p.trim(), p[1].trim(), p[2].trim(),
                        Double.parseDouble(p[3].trim()),
                        Integer.parseInt(p[4].trim())));
                } catch (NumberFormatException e) {
                    System.out.println("Skipped bad line: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading inventory.txt: " + e.getMessage());
        }
    }

    // Save inventory to inventory.txt
    static void saveInventory() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Item item : inventory) {
                writer.println(item.toLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving inventory.txt: " + e.getMessage());
        }
    }

    static void printSection(String title, String categoryPrefix) {
        System.out.println("\n" + title);
        System.out.printf("%-6s %-12s %-24s %12s %6s%n", "ID", "Category", "Item", "Price (PHP)", "Qty");
        System.out.println("-".repeat(64));

        for (Item item : inventory) {
            if (item.category.startsWith(categoryPrefix)) {
                String flag = item.qty <= LOW_STOCK_LIMIT ? " (Low Stock)" : "";
                System.out.printf("%-6s %-12s %-24s %12.2f %6d%s%n",
                    item.id, item.category, item.name, item.price, item.qty, flag);
            }
        }
    }

    static void viewStock() {
        System.out.println("\n=== CURRENT STOCK ===");
        printSection("1. Human Drinks & Food", "Human");
        printSection("2. Dog Drinks & Treats", "Dog");
    }

    // Scans every item and warns when qty <= 5
    static int lowStockDetector() {
        System.out.println("\n=== LOW STOCK SCAN ===");
        int flagged = 0;
        for (Item item : inventory) {
            if (item.qty <= LOW_STOCK_LIMIT) {
                System.out.println("WARNING: " + item.name + " (" + item.id + ") - only " + item.qty + " left!");
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

    static Item findItemById(String id) {
        for (Item item : inventory) {
            if (item.id.equalsIgnoreCase(id)) return item;
        }
        return null;
    }

    static void restock() {
        System.out.println("\n=== RESTOCK ===");
        viewStock();

        Item item = null;
        while (item == null) {
            System.out.print("\nEnter item ID to restock, e.g. H101 (0 to cancel): ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                System.out.println("Restock cancelled.");
                return;
            }
            item = findItemById(input);
            if (item == null) {
                System.out.println("Error: no item with ID '" + input + "'.");
            }
        }

        int shipment = 0;
        boolean valid = false;
        while (!valid) {
            System.out.print("Enter shipment quantity for " + item.name + ": ");
            String input = scanner.nextLine().trim();
            try {
                shipment = Integer.parseInt(input);
                if (shipment > 0) {
                    valid = true;
                } else {
                    System.out.println("Error: quantity must be greater than 0.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: quantity must be a whole number.");
            }
        }

        item.qty += shipment;
        saveInventory();
        System.out.println("Restocked! " + item.name + " now has " + item.qty + " in stock.");
    }

    public static void managerMenu(Scanner sc) {
        scanner = sc;
        checkOrCreateInventoryFile();
        loadInventory();

        boolean running = true;
        while (running) {
            System.out.println("\n===== MANAGER MENU (Inventory) =====");
            System.out.println("1. View stock");
            System.out.println("2. Low stock scan");
            System.out.println("3. Restock");
            System.out.println("4. Save and exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    viewStock();
                    break;
                case "2":
                    lowStockDetector();
                    break;
                case "3":
                    restock();
                    break;
                case "4":
                    saveInventory();
                    System.out.println("Inventory saved. Returning to main menu.");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose 1-4.");
            }
        }
    }
}