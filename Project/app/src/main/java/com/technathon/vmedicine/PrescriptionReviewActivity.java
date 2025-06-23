package com.technathon.vmedicine;

import android.content.Intent; // Added import for Intent
import android.content.SharedPreferences; // Added import for SharedPreferences
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.technathon.vmedicine.data.UserProfile;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray; // Import JSONArray for parsing AI response

import java.io.IOException; // For OkHttp exceptions
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// OkHttp imports for network requests
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PrescriptionReviewActivity extends AppCompatActivity {

    private static final String TAG = "PrescriptionReview";
    private static final String PREFS_NAME = "VMedicinePrefs"; // Constant for SharedPreferences file name
    private static final String KEY_CURRENT_USER_ID = "current_user_id"; // Constant for the key

    // UI elements declarations
    private EditText etAllergies;
    private EditText etCurrentMedications;
    private EditText etMedicalConditions;
    private EditText etUserAge; // EditText for user's age
    private Button btnConfirmSave;
    private Button btnGoBackRescan;

    // Firebase and concurrency related declarations
    private FirebaseFirestore db;
    private ExecutorService executorService = Executors.newSingleThreadExecutor(); // For background tasks
    private String currentUserId; // Declared to store the retrieved user ID (no longer hardcoded)

    // Data passed from previous activity
    private String originalScannedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription_review);

        // --- Start: Retrieve currentUserId from SharedPreferences ---
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = preferences.getString(KEY_CURRENT_USER_ID, null);

        if (currentUserId == null) {
            Toast.makeText(this, "No user selected. Please select a profile.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, UserSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return; // IMPORTANT: Exit onCreate early as the user will be redirected
        }
        Log.d(TAG, "Current User ID in PrescriptionReviewActivity: " + currentUserId);
        // --- End: Retrieve currentUserId from SharedPreferences ---

        // Initialize Firebase Firestore instance
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements by finding their IDs from the layout file
        etAllergies = findViewById(R.id.etAllergies);
        etCurrentMedications = findViewById(R.id.etCurrentMedications);
        etMedicalConditions = findViewById(R.id.etMedicalConditions);
        etUserAge = findViewById(R.id.etUserAge);
        btnConfirmSave = findViewById(R.id.btnConfirmSave);
        btnGoBackRescan = findViewById(R.id.btnGoBackRescan);

        // Retrieve the scanned text from the intent that started this activity
        originalScannedText = getIntent().getStringExtra("scanned_text");

        // If scanned text is available, process it with the Gemini AI
        if (originalScannedText != null && !originalScannedText.isEmpty()) {
            Log.d(TAG, "Scanned text received: " + originalScannedText);
            processTextWithGeminiAPI(originalScannedText);
        } else {
            // Handle case where no text was passed (e.g., direct launch, error)
            Toast.makeText(this, "No prescription text received for review.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "No scanned text received in PrescriptionReviewActivity. Cannot process.");
            // Optionally, disable buttons or finish activity if critical data is missing
        }

        // Set up click listeners for the action buttons
        btnConfirmSave.setOnClickListener(v -> saveUserProfileToFirestore());
        btnGoBackRescan.setOnClickListener(v -> finish()); // Simply finishes the current activity
    }

    /**
     * Sends the raw prescription text to the Gemini API to extract structured medical data.
     * Expects a JSON response with allergies, current medications, and medical conditions.
     *
     * @param text The raw text extracted from the scanned prescription image.
     */
    private void processTextWithGeminiAPI(String text) {
        Toast.makeText(this, "Sending to AI for analysis...", Toast.LENGTH_SHORT).show(); // User feedback

        executorService.execute(() -> { // Execute network request on a background thread
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8"); // Define content type

            // Construct the prompt for Gemini API
            String prompt = "From the following prescription text, extract: " +
                    "1. Allergies (as a comma-separated list, e.g., Penicillin, Sulfa drugs)\n" +
                    "2. Current Medications (as a comma-separated list, e.g., Lisinopril 10mg, Metformin 500mg)\n" +
                    "3. Medical Conditions (as a comma-separated list, e.g., Hypertension, Type 2 Diabetes)\n" +
                    "Return the extracted data as a JSON object with keys: 'allergies', 'currentMedications', 'medicalConditions'. " +
                    "Each key should map to an array of strings. If a category is not found, its array should be empty. " +
                    "Do not include any other text or formatting. Here is the prescription text: " + text;

            // Prepare the JSON payload for the Gemini API request
            // Ensure quotes and newlines are properly escaped for JSON
            String jsonPayload = String.format(
                    "{\"contents\": [{\"role\": \"user\", \"parts\": [{\"text\": \"%s\"}]}], " +
                            "\"generationConfig\": {\"responseMimeType\": \"application/json\", \"responseSchema\": {" +
                            "\"type\": \"OBJECT\", \"properties\": {" +
                            "\"allergies\": {\"type\": \"ARRAY\", \"items\": {\"type\": \"STRING\"}}," +
                            "\"currentMedications\": {\"type\": \"ARRAY\", \"items\": {\"type\": \"STRING\"}}," +
                            "\"medicalConditions\": {\"type\": \"ARRAY\", \"items\": {\"type\": \"STRING\"}}" +
                            "}, \"propertyOrdering\": [\"allergies\", \"currentMedications\", \"medicalConditions\"]}}}",
                    prompt.replace("\"", "\\\"").replace("\n", "\\n")
            );

            // API key is left empty; Canvas will inject it for gemini-2.0-flash
            // IMPORTANT: If you are hardcoding an API key for Gemini here,
            // make sure it's correct. Otherwise, use BuildConfig or leave blank if Canvas injects.
            String apiKey = "AIzaSyBbFGrqfLMQMQTsOu8MOKbN97ybUlGSQgA"; // Use your actual Gemini API Key here if hardcoding, or leave blank if Canvas injects
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            RequestBody body = RequestBody.create(JSON, jsonPayload);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .build();

            // Enqueue the network call asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> { // Run on UI thread for Toast
                        Toast.makeText(PrescriptionReviewActivity.this, "AI processing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Gemini API call failed: " + e.getMessage(), e);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            Log.d(TAG, "Gemini API Raw Response: " + responseBody);

                            // Parse the main JSON response from the API
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            if (jsonResponse.has("candidates") && jsonResponse.getJSONArray("candidates").length() > 0) {
                                JSONObject candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
                                if (candidate.has("content") && candidate.getJSONObject("content").has("parts") && candidate.getJSONObject("content").getJSONArray("parts").length() > 0) {
                                    // Get the actual text content that contains the parsed JSON
                                    String textContent = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                                    Log.d(TAG, "Gemini API Parsed Text Content: " + textContent);

                                    // Parse this 'textContent' string as a JSON object
                                    final JSONObject parsedData = new JSONObject(textContent);

                                    runOnUiThread(() -> { // Update UI on the main thread
                                        etAllergies.setText(joinJSONArray(parsedData.optJSONArray("allergies")));
                                        etCurrentMedications.setText(joinJSONArray(parsedData.optJSONArray("currentMedications")));
                                        etMedicalConditions.setText(joinJSONArray(parsedData.optJSONArray("medicalConditions")));

                                        Toast.makeText(PrescriptionReviewActivity.this, "AI parsing complete! Review and save.", Toast.LENGTH_SHORT).show();
                                    });
                                } else {
                                    runOnUiThread(() -> Toast.makeText(PrescriptionReviewActivity.this, "AI response content is empty or malformed.", Toast.LENGTH_LONG).show());
                                    Log.e(TAG, "Gemini response missing content or parts.");
                                }
                            } else {
                                runOnUiThread(() -> Toast.makeText(PrescriptionReviewActivity.this, "AI response candidates missing.", Toast.LENGTH_LONG).show());
                                Log.e(TAG, "Gemini response candidates missing array.");
                            }
                        } catch (IOException | JSONException e) {
                            runOnUiThread(() -> {
                                Toast.makeText(PrescriptionReviewActivity.this, "Error processing AI response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Error reading/parsing Gemini API response: " + e.getMessage(), e);
                            });
                        }
                    } else {
                        // Handle unsuccessful HTTP responses (e.g., 400, 500)
                        runOnUiThread(() -> {
                            try {
                                String errorBody = response.body() != null ? response.body().string() : "No error body";
                                Toast.makeText(PrescriptionReviewActivity.this, "AI processing failed (HTTP " + response.code() + "): " + errorBody, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Gemini API HTTP Error: " + response.code() + " - " + errorBody);
                            } catch (IOException e) {
                                Toast.makeText(PrescriptionReviewActivity.this, "AI processing failed (HTTP " + response.code() + "): Could not read error body.", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Gemini API HTTP Error: " + response.code() + " (Error reading body: " + e.getMessage() + ")");
                            }
                        });
                    }
                }
            });
        });
    }

    /**
     * Saves the user's profile data, including allergies, medications, conditions, and age, to Firestore.
     * Performs basic validation on age.
     */
    private void saveUserProfileToFirestore() {
        // Retrieve parsed data from EditText fields
        List<String> allergies = parseCommaSeparatedText(etAllergies.getText().toString());
        List<String> currentMedications = parseCommaSeparatedText(etCurrentMedications.getText().toString());
        List<String> medicalConditions = parseCommaSeparatedText(etMedicalConditions.getText().toString());

        // Parse and validate user's age
        int age = 0; // Default if not valid
        String ageText = etUserAge.getText().toString().trim();
        if (ageText.isEmpty()) {
            Toast.makeText(this, "Please enter your age.", Toast.LENGTH_SHORT).show();
            return; // Exit if age is not provided
        }
        try {
            age = Integer.parseInt(ageText);
            if (age <= 0 || age > 150) { // Basic sanity check for age
                Toast.makeText(this, "Age must be a positive number and realistic.", Toast.LENGTH_SHORT).show();
                return; // Exit if age is invalid
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid age input: '" + ageText + "' - " + e.getMessage());
            Toast.makeText(this, "Please enter a valid age (numbers only).", Toast.LENGTH_SHORT).show();
            return; // Exit if age is not a number
        }

        // Create a new UserProfile object with all the collected data
        // Ensure UserProfile constructor or setters handle this
        UserProfile userProfile = new UserProfile(currentUserId, allergies, currentMedications, medicalConditions, age);

        // Save the UserProfile object to Firestore
        db.collection("users").document(currentUserId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PrescriptionReviewActivity.this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User profile data saved for user: " + currentUserId);
                    finish(); // Close this activity and return to the previous one
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PrescriptionReviewActivity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to save user profile for user: " + currentUserId, e);
                });
    }

    /**
     * Helper method to parse a comma-separated string from an EditText into a List of Strings.
     * Handles trimming whitespace and empty input.
     *
     * @param text The input string (e.g., from an EditText).
     * @return A List of strings, where each string is an item from the input. Returns an empty list if input is null or empty.
     */
    private List<String> parseCommaSeparatedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(text.split("\\s*,\\s*")));
    }

    /**
     * Helper method to convert a JSONArray into a single comma-separated String.
     * Handles potential JSONException internally when accessing elements.
     *
     * @param jsonArray The JSONArray to process.
     * @return A comma-separated string representation of the array, or an empty string if null/empty.
     */
    private String joinJSONArray(JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.length() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                builder.append(jsonArray.getString(i));
                if (i < jsonArray.length() - 1) {
                    builder.append(", ");
                }
            } catch (JSONException e) {
                // Log the exception but continue processing other elements
                Log.e(TAG, "JSONException in joinJSONArray at index " + i + ": " + e.getMessage());
            }
        }
        return builder.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}