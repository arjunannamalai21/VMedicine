package com.technathon.vmedicine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.technathon.vmedicine.data.DrugInteractionDatabase;
import com.technathon.vmedicine.data.UserProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScannedMedicineDetailActivity extends AppCompatActivity {

    private static final String TAG = "ScannedMedDetail";
    private static final String PREFS_NAME = "VMedicinePrefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";

    // UI Elements
    private TextView tvMedicineName;
    private TextView tvAllergyStatus;
    private TextView tvHowToUse;
    private Button btnBackToMain;

    // Data members
    private FirebaseFirestore db;
    private String currentUserId; // Now dynamically fetched
    private String scannedMedicineName;
    private UserProfile currentUserProfile;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanned_medicine_detail);

        // --- Start: Retrieve currentUserId from SharedPreferences ---
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = preferences.getString(KEY_CURRENT_USER_ID, null);

        if (currentUserId == null) {
            Toast.makeText(this, "No user logged in. Please log in first.", Toast.LENGTH_LONG).show();
            // Redirect to LoginActivity if no user ID is found
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish current activity to prevent going back to it
            return; // IMPORTANT: Exit onCreate early as the user will be redirected
        }
        Log.d(TAG, "Current User ID in ScannedMedicineDetailActivity: " + currentUserId);
        // --- End: Retrieve currentUserId from SharedPreferences ---

        // Initialize UI elements
        tvMedicineName = findViewById(R.id.tvMedicineName);
        tvAllergyStatus = findViewById(R.id.tvAllergyStatus);
        tvHowToUse = findViewById(R.id.tvHowToUse);
        btnBackToMain = findViewById(R.id.btnBackToMain);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get the scanned medicine name from the Intent
        scannedMedicineName = getIntent().getStringExtra("medicine_name");
        if (scannedMedicineName != null && !scannedMedicineName.isEmpty()) {
            tvMedicineName.setText(scannedMedicineName);
            Log.d(TAG, "Scanned medicine received: " + scannedMedicineName);
            // Start the process: fetch user profile, then check, then get how-to-use
            fetchUserProfileAndProcessMedicine();
        } else {
            tvMedicineName.setText("N/A - No medicine scanned");
            tvAllergyStatus.setText("Error: No medicine name found.");
            tvAllergyStatus.setBackgroundResource(R.drawable.rounded_box_error);
            tvAllergyStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            tvHowToUse.setText("Cannot provide details without medicine name.");
            Toast.makeText(this, "No medicine name found from QR scan.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "No medicine_name extra found in intent.");
        }

        btnBackToMain.setOnClickListener(v -> finish());
    }

    /**
     * Fetches the current user's profile from Firestore to get allergies, age, and medical conditions.
     * After fetching, it calls methods to check for allergy interactions and get personalized usage.
     */
    private void fetchUserProfileAndProcessMedicine() {
        Log.d(TAG, "Fetching user profile for " + currentUserId);
        DocumentReference userDocRef = db.collection("users").document(currentUserId);
        userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        currentUserProfile = document.toObject(UserProfile.class);
                        if (currentUserProfile != null) {
                            Log.d(TAG, "User profile fetched: " + currentUserProfile.getAllergies() + ", Age: " + currentUserProfile.getAge());
                            checkAllergyInteraction(scannedMedicineName, currentUserProfile.getAllergies());
                            getPersonalizedHowToUse(scannedMedicineName, currentUserProfile.getAge(), currentUserProfile.getMedicalConditions());
                        } else {
                            Log.e(TAG, "UserProfile object is null after conversion.");
                            Toast.makeText(ScannedMedicineDetailActivity.this, "Error: User profile data malformed.", Toast.LENGTH_LONG).show();
                            tvAllergyStatus.setText("Profile data error.");
                            tvAllergyStatus.setBackgroundResource(R.drawable.rounded_box_error);
                            tvAllergyStatus.setTextColor(ContextCompat.getColor(ScannedMedicineDetailActivity.this, android.R.color.holo_red_dark));
                            tvHowToUse.setText("Cannot get instructions due to profile error.");
                        }
                    } else {
                        Log.d(TAG, "No user profile document found for " + currentUserId);
                        Toast.makeText(ScannedMedicineDetailActivity.this, "User profile not found. Please create one in User Profile section.", Toast.LENGTH_LONG).show();
                        tvAllergyStatus.setText("No profile. Cannot check allergies.");
                        tvAllergyStatus.setBackgroundResource(R.drawable.rounded_box_warning);
                        tvAllergyStatus.setTextColor(ContextCompat.getColor(ScannedMedicineDetailActivity.this, android.R.color.holo_orange_dark));
                        getPersonalizedHowToUse(scannedMedicineName, 0, new ArrayList<>()); // Still try to get general how-to-use
                    }
                } else {
                    Log.e(TAG, "Error fetching user profile: " + task.getException().getMessage(), task.getException());
                    Toast.makeText(ScannedMedicineDetailActivity.this, "Error loading profile: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    tvAllergyStatus.setText("Error loading profile. Cannot check allergies.");
                    tvAllergyStatus.setBackgroundResource(R.drawable.rounded_box_error);
                    tvAllergyStatus.setTextColor(ContextCompat.getColor(ScannedMedicineDetailActivity.this, android.R.color.holo_red_dark));
                    tvHowToUse.setText("Cannot get instructions due to loading error.");
                }
            }
        });
    }

    /**
     * Performs an allergy interaction check for the given medicine against user's allergies.
     * Updates tvAllergyStatus accordingly.
     *
     * @param medicineName The name of the medicine to check.
     * @param userAllergies List of allergies from the user's profile.
     */
    private void checkAllergyInteraction(String medicineName, List<String> userAllergies) {
        if (userAllergies == null || userAllergies.isEmpty()) {
            tvAllergyStatus.setText("No allergies found in your profile. Assuming safe for allergies.");
            tvAllergyStatus.setBackgroundResource(R.drawable.rounded_box_success);
            tvAllergyStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            return;
        }

        String interactingAllergy = DrugInteractionDatabase.checkForAllergyInteraction(medicineName, userAllergies);

        if (interactingAllergy != null) {
            String warningMessage = "WARNING: This medicine (" + medicineName + ") is contraindicated due to your " + interactingAllergy + " allergy. Do not use it.";
            tvAllergyStatus.setText(warningMessage);
            tvAllergyStatus.setBackgroundResource(R.drawable.rounded_box_error);
            tvAllergyStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            Log.w(TAG, warningMessage);
        } else {
            tvAllergyStatus.setText("No known allergy detected for " + medicineName + ". It is safe for your known allergies.");
            tvAllergyStatus.setBackgroundResource(R.drawable.rounded_box_success);
            tvAllergyStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            Log.i(TAG, "No direct allergy interaction found for " + medicineName + " with user's allergies.");
        }
    }

    /**
     * Calls Gemini API to get personalized "How to Use" instructions based on medicine, age, and medical conditions.
     * Updates tvHowToUse accordingly.
     *
     * @param medicineName The medicine to get instructions for.
     * @param age The user's age.
     * @param medicalConditions List of user's medical conditions.
     */
    private void getPersonalizedHowToUse(String medicineName, int age, List<String> medicalConditions) {
        tvHowToUse.setText("Generating personalized instructions...");
        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");

            String conditionsString = medicalConditions != null && !medicalConditions.isEmpty() ?
                    "User has medical conditions: " + String.join(", ", medicalConditions) + ". " :
                    "";
            String prompt = String.format(
                    "For the medicine '%s', provide a very brief and concise explanation of its general use and how to administer it. " +
                            "If it's a cream, briefly explain how to apply. " +
                            "DO NOT provide specific dosage numbers or quantities. " +
                            "Also, include any common and important warnings or side effects relevant to daily activities (e.g., drowsiness, dizziness) if applicable. " +
                            "Keep the response to 2-4 sentences max. " +
                            "Consider the user's age (%d years old) and their medical conditions: %s. " +
                            "Start directly with the explanation. If information is not available, state that briefly.",
                    medicineName, age, conditionsString
            );
            String jsonPayload = String.format(
                    "{\"contents\": [{\"role\": \"user\", \"parts\": [{\"text\": \"%s\"}]}]}",
                    prompt.replace("\"", "\\\"").replace("\n", "\\n")
            );

            // FIX: Set API key to empty string for Canvas to inject it.
            String apiKey = "AIzaSyBbFGrqfLMQMQTsOu8MOKbN97ybUlGSQgA"; // Set to empty string for Canvas to inject automatically
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            RequestBody body = RequestBody.create(jsonPayload, JSON);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        tvHowToUse.setText("Error generating instructions: " + e.getMessage());
                        Log.e(TAG, "Gemini API call for how-to-use failed: " + e.getMessage(), e);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String generatedText = "Could not generate instructions.";

                            if (jsonResponse.has("candidates") && jsonResponse.getJSONArray("candidates").length() > 0) {
                                JSONObject candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
                                if (candidate.has("content") && candidate.getJSONObject("content").has("parts") && candidate.getJSONObject("content").getJSONArray("parts").length() > 0) {
                                    generatedText = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                                    Log.d(TAG, "Gemini generated how-to-use: " + generatedText);
                                }
                            }

                            final String finalGeneratedText = generatedText;
                            runOnUiThread(() -> tvHowToUse.setText(finalGeneratedText));

                        } catch (IOException | JSONException e) {
                            runOnUiThread(() -> {
                                tvHowToUse.setText("Error parsing instructions: " + e.getMessage());
                                Log.e(TAG, "Error parsing Gemini response for how-to-use: " + e.getMessage(), e);
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            try {
                                String errorBody = response.body() != null ? response.body().string() : "No error body";
                                tvHowToUse.setText("Failed to get instructions: HTTP " + response.code() + " - " + errorBody);
                                Log.e(TAG, "Gemini API HTTP Error for how-to-use: " + response.code() + " - " + errorBody);
                            } catch (IOException e) {
                                tvHowToUse.setText("Failed to get instructions: HTTP " + response.code());
                                Log.e(TAG, "Gemini API HTTP Error (no body) for how-to-use: " + response.code());
                            }
                        });
                    }
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}
