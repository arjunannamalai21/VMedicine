package com.technathon.vmedicine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.technathon.vmedicine.data.UserProfile; // Ensure this import is correct

import java.util.ArrayList; // For initializing empty lists
import java.util.Arrays; // For pre-populating data

public class UserSelectionActivity extends AppCompatActivity {

    private static final String TAG = "UserSelectionActivity";
    private static final String PREFS_NAME = "VMedicinePrefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_selection);

        db = FirebaseFirestore.getInstance();

        Button btnUserA = findViewById(R.id.btnUserA);
        Button btnUserB = findViewById(R.id.btnUserB);
        Button btnNewUser = findViewById(R.id.btnNewUser);

        btnUserA.setOnClickListener(v -> selectUser("user_profile_a"));
        btnUserB.setOnClickListener(v -> selectUser("user_profile_b"));
        btnNewUser.setOnClickListener(v -> selectUser(java.util.UUID.randomUUID().toString())); // Generate unique ID for new user
    }

    /**
     * Selects a user, saves their ID, ensures their profile exists, and launches MainActivity.
     * @param userId The ID of the user to select.
     */
    private void selectUser(String userId) {
        // Save the selected user ID to SharedPreferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CURRENT_USER_ID, userId);
        editor.apply(); // Use apply() for async save

        Log.d(TAG, "Selected User ID: " + userId);
        Toast.makeText(this, "Logged in as: " + userId, Toast.LENGTH_SHORT).show();

        // Ensure the user's profile exists in Firestore
        ensureUserProfileExists(userId);
    }

    /**
     * Checks if a user profile exists in Firestore. If not, creates a basic one.
     * This is crucial for initial setup of "User A" and "User B" profiles.
     * @param userId The ID of the user to check/create.
     */
    private void ensureUserProfileExists(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG, "User profile for " + userId + " already exists.");
                                launchMainActivity();
                            } else {
                                Log.d(TAG, "User profile for " + userId + " does not exist. Creating new profile.");
                                UserProfile newUserProfile = new UserProfile();
                                newUserProfile.setUserId(userId);
                                newUserProfile.setAge(30); // Default age
                                newUserProfile.setMedicalConditions(new ArrayList<>(Arrays.asList("Hypertension"))); // Example
                                newUserProfile.setAllergies(new ArrayList<>(Arrays.asList("Penicillin"))); // Example

                                // Pre-populate some current medications for testing "Find Medicine" tab
                                if (userId.equals("user_profile_a")) {
                                    newUserProfile.setCurrentMedications(new ArrayList<>(Arrays.asList("Metformin 500mg", "Lisinopril 10mg", "Amoxicillin 250mg")));
                                } else if (userId.equals("user_profile_b")) {
                                    newUserProfile.setCurrentMedications(new ArrayList<>(Arrays.asList("Paracetamol 500mg", "Ibuprofen 400mg", "Rare Drug X 10mg")));
                                } else {
                                    newUserProfile.setCurrentMedications(new ArrayList<>()); // Empty for new users
                                }


                                db.collection("users").document(userId).set(newUserProfile)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.d(TAG, "User profile for " + userId + " created successfully.");
                                                    launchMainActivity();
                                                } else {
                                                    Log.e(TAG, "Error creating user profile for " + userId + ": " + task.getException().getMessage());
                                                    Toast.makeText(UserSelectionActivity.this, "Error creating profile.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Log.e(TAG, "Error checking user profile existence: " + task.getException().getMessage());
                            Toast.makeText(UserSelectionActivity.this, "Error checking profile.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Launches the MainActivity after user selection.
     */
    private void launchMainActivity() {
        Intent intent = new Intent(UserSelectionActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish this activity so user can't go back to selection screen
    }
}
