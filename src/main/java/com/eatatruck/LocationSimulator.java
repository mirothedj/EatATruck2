package com.eatatruck;

public class LocationSimulator {

    private final SmsHandler smsHandler;

    public LocationSimulator(SmsHandler smsHandler) {
        this.smsHandler = smsHandler;
    }

    /**
     * Simulates the food truck arriving at a preset location.
     * This triggers the initial SMS check-in with the owner.
     * @param ownerPhone The phone number of the owner to contact.
     */
    public void simulateArrivalAtPresetLocation(String ownerPhone) {
        System.out.println("\n🚚 Food truck has arrived at a preset location. Triggering check-in for " + ownerPhone + "...");
        smsHandler.handleInitialTrigger(ownerPhone);
    }
}
