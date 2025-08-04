package com.eatatruck;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SmsHandler {

    // As per the README, timeouts are 10, 5, 5 minutes.
    private static final int INITIAL_RESPONSE_TIMEOUT_SECONDS = 600; // 10 minutes
    private static final int CUSTOMIZATION_RESPONSE_TIMEOUT_SECONDS = 300; // 5 minutes
    private static final int SPECIAL_DETAILS_TIMEOUT_SECONDS = 300; // 5 minutes

    private final RedisManager redisManager;
    private final SocialMediaPoster socialMediaPoster;

    public enum ConversationState {
        WAITING_FOR_INITIAL_RESPONSE,
        WAITING_FOR_CUSTOMIZATION_RESPONSE,
        WAITING_FOR_MENU_ITEM_SELECTION,
        WAITING_FOR_SPECIAL_DETAILS,
        WAITING_FOR_SPECIAL_PICTURE,
        WAITING_FOR_SPECIAL_PRICE
    }

    public SmsHandler(RedisManager redisManager, SocialMediaPoster socialMediaPoster) {
        this.redisManager = redisManager;
        this.socialMediaPoster = socialMediaPoster;
    }

    public void handleInitialTrigger(String ownerPhone) {
        // Here we assume the location is correct and we are within opening hours.
        // The README mentions isAtPresetLocation() and isWithinOpenTime() checks.
        // For this simulation, we'll assume they pass.
        sendSms(ownerPhone, "Hello we see you are at a designated work zone. Would you like to Open Now? (YES/NO)");
        redisManager.setConversationState(ownerPhone, ConversationState.WAITING_FOR_INITIAL_RESPONSE.name(), INITIAL_RESPONSE_TIMEOUT_SECONDS);
    }

    public void processSms(String ownerPhone, String messageBody) {
        String currentStateStr = redisManager.getConversationState(ownerPhone);
        if (currentStateStr == null) {
            System.out.println("No active conversation for " + ownerPhone);
            // In a real app, you might handle unexpected messages here.
            return;
        }

        ConversationState currentState = ConversationState.valueOf(currentStateStr);

        switch (currentState) {
            case WAITING_FOR_INITIAL_RESPONSE:
                handleInitialResponse(ownerPhone, messageBody);
                break;
            case WAITING_FOR_CUSTOMIZATION_RESPONSE:
                handleCustomizationResponse(ownerPhone, messageBody);
                break;
            case WAITING_FOR_MENU_ITEM_SELECTION:
                handleMenuItemSelection(ownerPhone, messageBody);
                break;
            // The other states are handled within the flow, but we can add them to the switch for completeness.
            default:
                System.out.println("Unhandled conversation state: " + currentState);
                redisManager.deleteConversationState(ownerPhone);
        }
    }

    private void handleInitialResponse(String ownerPhone, String messageBody) {
        if ("YES".equalsIgnoreCase(messageBody)) {
            sendSms(ownerPhone, "Would you like to customize your open? (YES/NO)");
            redisManager.setConversationState(ownerPhone, ConversationState.WAITING_FOR_CUSTOMIZATION_RESPONSE.name(), CUSTOMIZATION_RESPONSE_TIMEOUT_SECONDS);
        } else if ("NO".equalsIgnoreCase(messageBody)) {
            handleFailScenario(ownerPhone, "Opening declined by owner.");
        } else {
            sendSms(ownerPhone, "Invalid response. Please reply with YES or NO.");
            // We can either extend the timeout or let it expire. For now, we do nothing.
        }
    }

    private void handleCustomizationResponse(String ownerPhone, String messageBody) {
        if ("YES".equalsIgnoreCase(messageBody)) {
            triggerCustomCampaignFlow(ownerPhone);
        } else if ("NO".equalsIgnoreCase(messageBody)) {
            triggerAutoCampaignFlow(ownerPhone);
        } else {
            sendSms(ownerPhone, "Invalid response. Please reply with YES or NO.");
        }
    }

    private void triggerAutoCampaignFlow(String ownerPhone) {
        // As per README: "Creates a post basic salutation and sends to all platforms"
        String campaignContent = "Hello everyone! We're open for business today. Come on down!";

        Campaign campaign = new Campaign(UUID.randomUUID().toString(), LocalDateTime.now(), campaignContent, List.of("all"), false);
        redisManager.saveCampaign(campaign);
        socialMediaPoster.post(campaignContent);

        sendSms(ownerPhone, "Successful Open! Your Auto Campaign has been posted to your social media.");
        redisManager.deleteConversationState(ownerPhone);
    }

    private void triggerCustomCampaignFlow(String ownerPhone) {
        List<MenuItem> menuItems = redisManager.getAllMenuItems();
        if (menuItems.isEmpty()) {
            sendSms(ownerPhone, "You have no menu items set up. Please add some items first. Aborting custom campaign.");
            redisManager.deleteConversationState(ownerPhone);
            return;
        }

        StringBuilder menuMessage = new StringBuilder("Here's your menu:\n");
        for (int i = 0; i < menuItems.size(); i++) {
            MenuItem item = menuItems.get(i);
            menuMessage.append(i + 1).append(". ").append(item.name()).append(" - $").append(item.price()).append("\n");
        }
        menuMessage.append("Please reply with the numbers of the items you'd like to feature in your campaign (e.g., 1, 3).");

        sendSms(ownerPhone, menuMessage.toString());
        redisManager.setConversationState(ownerPhone, ConversationState.WAITING_FOR_MENU_ITEM_SELECTION.name(), SPECIAL_DETAILS_TIMEOUT_SECONDS);
    }

    private void handleMenuItemSelection(String ownerPhone, String messageBody) {
        List<MenuItem> menuItems = redisManager.getAllMenuItems();
        List<Integer> selectedIndexes;
        try {
            selectedIndexes = Arrays.stream(messageBody.split(","))
                                    .map(s -> Integer.parseInt(s.trim()) - 1)
                                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            sendSms(ownerPhone, "Invalid format. Please reply with numbers separated by commas (e.g., 1, 3).");
            return;
        }

        List<MenuItem> selectedItems = selectedIndexes.stream()
            .filter(i -> i >= 0 && i < menuItems.size())
            .map(menuItems::get)
            .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            sendSms(ownerPhone, "No valid menu items selected. Please try again.");
            return;
        }

        // The README flow for custom campaigns is complex (details, picture, price).
        // For this simulation, we'll create a campaign directly from the selected menu items.
        StringBuilder campaignContent = new StringBuilder("Today's Specials:\n");
        for (MenuItem item : selectedItems) {
            campaignContent.append("- ").append(item.name()).append(": ").append(item.description())
                           .append(" for only $").append(item.price()).append("\n");
        }
        campaignContent.append("Come and get it!");

        Campaign campaign = new Campaign(UUID.randomUUID().toString(), LocalDateTime.now(), campaignContent.toString(), List.of("all"), false);
        redisManager.saveCampaign(campaign);
        socialMediaPoster.post(campaignContent.toString());

        sendSms(ownerPhone, "Successful Open! Your Custom Campaign has been posted to your social media.");
        redisManager.deleteConversationState(ownerPhone);
    }

    private void handleFailScenario(String ownerPhone, String reason) {
        System.out.println("FAIL Scenario triggered for " + ownerPhone + ". Reason: " + reason);
        sendSms(ownerPhone, "Store closed for today. " + reason);
        redisManager.deleteConversationState(ownerPhone);
    }

    private void sendSms(String phoneNumber, String message) {
        // This is a placeholder for a real SMS sending service.
        System.out.println("\n--- Sending SMS to " + phoneNumber + " ---");
        System.out.println(message);
        System.out.println("---------------------------------\n");
    }
}
