package com.technathon.vmedicine.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;

public class DrugInteractionDatabase {

    private static final String TAG = "DrugInteractionDB";

    // A simple mock database of drug-allergy interactions
    // Key: Medicine Name (LOWERCASE for consistent comparison)
    // Value: List of allergies that interact with this medicine (LOWERCASE)
    private static final Map<String, List<String>> MOCK_ALLERGY_DATA = new HashMap<>();

    static {
        // Initialize our mock allergy data with LOWERCASE names
        MOCK_ALLERGY_DATA.put("penicillin", Arrays.asList("penicillin", "cephalexin"));
        MOCK_ALLERGY_DATA.put("aspirin", Arrays.asList("aspirin", "nsaids"));
        MOCK_ALLERGY_DATA.put("ibuprofen", Arrays.asList("ibuprofen", "nsaids", "aspirin"));
        MOCK_ALLERGY_DATA.put("amoxicillin", Arrays.asList("penicillin")); // Amoxicillin is a type of penicillin
        MOCK_ALLERGY_DATA.put("sulfamethoxazole", Arrays.asList("sulfa drugs"));
        MOCK_ALLERGY_DATA.put("codeine", Arrays.asList("opioids"));
        MOCK_ALLERGY_DATA.put("morphine", Arrays.asList("opioids"));
        MOCK_ALLERGY_DATA.put("insulin", Arrays.asList("insulin allergy"));
        MOCK_ALLERGY_DATA.put("paracetamol", Arrays.asList("acetaminophen allergy"));
        // Add more mock data as needed for testing, always in lowercase
    }

    /**
     * Checks if a given medicine interacts with any of the user's known allergies.
     * This is a MOCK implementation for testing purposes.
     *
     * @param medicineName The name of the medicine to check (can be any case from scan).
     * @param userAllergies A list of allergies the user has (e.g., "Penicillin", "Sulfa drugs" from database).
     * @return The original name of the interacting allergy (from userAllergies) if found, otherwise null.
     */
    public static String checkForAllergyInteraction(String medicineName, List<String> userAllergies) {
        if (medicineName == null || medicineName.trim().isEmpty() || userAllergies == null || userAllergies.isEmpty()) {
            Log.d(TAG, "checkForAllergyInteraction: Invalid input (medicineName or userAllergies is empty/null)");
            return null;
        }

        // Convert incoming medicineName to LOWERCASE for lookup
        String lowerCaseMedicineName = medicineName.toLowerCase().trim();
        Log.d(TAG, "Checking allergy for medicine (lowercase): " + lowerCaseMedicineName + " against user allergies: " + userAllergies.toString());

        // Get potential interacting allergies for the medicine from our lowercase mock data
        List<String> medicineRelatedAllergiesFromDb = MOCK_ALLERGY_DATA.get(lowerCaseMedicineName);

        if (medicineRelatedAllergiesFromDb != null) {
            for (String medicineRelatedAllergyLower : medicineRelatedAllergiesFromDb) {
                // For each user allergy, convert it to lowercase and compare
                for (String userAllergyOriginalCase : userAllergies) {
                    if (userAllergyOriginalCase.toLowerCase().trim().equals(medicineRelatedAllergyLower.trim())) {
                        Log.d(TAG, "Interaction found! Medicine '" + medicineName + "' interacts with '" + userAllergyOriginalCase + "' allergy.");
                        return userAllergyOriginalCase; // Return the user's allergy in its original casing
                    }
                }
            }
        }
        Log.d(TAG, "No direct allergy interaction found for '" + medicineName + "'.");
        return null; // No interaction found
    }
}
