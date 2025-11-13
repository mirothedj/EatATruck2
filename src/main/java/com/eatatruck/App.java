package com.eatatruck;

import java.util.Scanner;

public class App {

    private static final String OWNER_PHONE_NUMBER = "+15551234567";
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    public static void main(String[] args) {
        RedisManager redisManager = null;
        Scanner scanner = new Scanner(System.in);

        try {
            redisManager = new RedisManager(REDIS_HOST, REDIS_PORT);
            // Check if Redis is available
            redisManager.getAllMenuItems();
        } catch (Exception e) {
            System.err.println("❌ Could not connect to Redis at " + REDIS_HOST + ":" + REDIS_PORT);
            System.err.println("Please ensure Redis is running and accessible.");
            return;
        }

        SocialMediaPoster socialMediaPoster = new SocialMediaPoster();
        SmsHandler smsHandler = new SmsHandler(redisManager, socialMediaPoster);
        LocationSimulator locationSimulator = new LocationSimulator(smsHandler);

        setupInitialData(redisManager);

        System.out.println("🚚 Eat-A-Truck Simulation Started!");
        System.out.println("Owner's phone number is: " + OWNER_PHONE_NUMBER);
        printHelp();

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();
            String[] parts = line.split(" ", 2);
            String command = parts[0].toLowerCase();

            if (command.isEmpty()) {
                continue;
            }

            switch (command) {
                case "arrive":
                    locationSimulator.simulateArrivalAtPresetLocation(OWNER_PHONE_NUMBER);
                    break;
                case "sms":
                    if (parts.length > 1) {
                        smsHandler.processSms(OWNER_PHONE_NUMBER, parts[1]);
                    } else {
                        System.out.println("Usage: sms <message>");
                    }
                    break;
                case "addmenu":
                    addMenuItem(redisManager, line);
                    break;
                case "showmenu":
                    showMenu(redisManager);
                    break;
                case "help":
                    printHelp();
                    break;
                case "exit":
                    System.out.println("Exiting simulation. Goodbye!");
                    redisManager.close();
                    scanner.close();
                    return;
                default:
                    System.out.println("Unknown command. Type 'help' for a list of commands.");
            }
        }
    }

    private static void setupInitialData(RedisManager redisManager) {
        if (redisManager.getAllMenuItems().isEmpty()) {
            System.out.println("No menu items found. Adding some initial data...");
            redisManager.saveMenuItem(new MenuItem("1", "Classic Burger", "A juicy beef patty with lettuce, tomato, and our special sauce.", 8.99));
            redisManager.saveMenuItem(new MenuItem("2", "Veggie Burger", "A delicious plant-based patty with avocado and sprouts.", 9.99));
            redisManager.saveMenuItem(new MenuItem("3", "Loaded Fries", "Crispy fries topped with cheese, bacon, and chives.", 6.50));
        }
        // Clean up any stale conversation from a previous run
        redisManager.deleteConversationState(OWNER_PHONE_NUMBER);
    }

    private static void addMenuItem(RedisManager redisManager, String line) {
        String[] parts = line.split(" ", 5);
        if (parts.length < 5) {
            System.out.println("Usage: addmenu <id> <price> <name> <description>");
            return;
        }
        try {
            String id = parts[1];
            double price = Double.parseDouble(parts[2]);
            String name = parts[3];
            String description = parts[4];
            MenuItem item = new MenuItem(id, name, description, price);
            redisManager.saveMenuItem(item);
            System.out.println("Menu item added: " + name);
        } catch (NumberFormatException e) {
            System.out.println("Invalid price. Please enter a valid number.");
        }
    }

    private static void showMenu(RedisManager redisManager) {
        System.out.println("\n--- Current Menu ---");
        redisManager.getAllMenuItems().forEach(item ->
            System.out.printf("ID: %s, Name: %s, Price: $%.2f, Desc: %s\n",
                item.id(), item.name(), item.price(), item.description())
        );
        System.out.println("--------------------\n");
    }

    private static void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  arrive              - Simulates the truck arriving at a preset location.");
        System.out.println("  sms <message>       - Simulates an incoming SMS from the owner (e.g., 'sms YES').");
        System.out.println("  addmenu <id> <price> <name> <desc> - Adds a new item to the menu.");
        System.out.println("  showmenu            - Displays the current menu.");
        System.out.println("  help                - Shows this help message.");
        System.out.println("  exit                - Quits the simulation.");
        System.out.println();
    }
}
