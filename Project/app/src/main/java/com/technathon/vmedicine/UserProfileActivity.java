package com.technathon.vmedicine;

import android.content.Intent;
import android.content.SharedPreferences; // Added import
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // Added import for tvCurrentUserId
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth; // Added import for FirebaseAuth
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.technathon.vmedicine.data.UserProfile;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private static final String PREFS_NAME = "VMedicinePrefs"; // Constant for SharedPreferences file name
    private static final String KEY_CURRENT_USER_ID = "current_user_id"; // Constant for the key

    private EditText etAllergies, etCurrentMedications, etMedicalConditions, etUserAge;
    private Button btnSaveProfile, btnLogout; // Declared btnLogout
    private TextView tvCurrentUserId; // TextView to display the current user ID

    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // Firebase Auth instance

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: UserProfileActivity started.");

        try {
            setContentView(R.layout.activity_user_profile);
            Log.d(TAG, "onCreate: Layout set successfully.");

            // --- Start: Retrieve currentUserId from SharedPreferences ---
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            currentUserId = preferences.getString(KEY_CURRENT_USER_ID, null);

            if (currentUserId == null) {
                Toast.makeText(this, "No user selected. Please select a profile.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoginActivity.class); // Redirect to LoginActivity
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return; // IMPORTANT: Exit onCreate early as the user will be redirected
            }
            Log.d(TAG, "Current User ID in UserProfileActivity: " + currentUserId);
            // --- End: Retrieve currentUserId from SharedPreferences ---


            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth
            Log.d(TAG, "onCreate: Firestore and FirebaseAuth initialized.");

            // Initialize UI elements with null checks
            etAllergies = findViewById(R.id.etAllergies);
            if (etAllergies == null) Log.e(TAG, "etAllergies is NULL. Check activity_user_profile.xml");
            etCurrentMedications = findViewById(R.id.etCurrentMedications);
            if (etCurrentMedications == null) Log.e(TAG, "etCurrentMedications is NULL. Check activity_user_profile.xml");
            etMedicalConditions = findViewById(R.id.etMedicalConditions);
            if (etMedicalConditions == null) Log.e(TAG, "etMedicalConditions is NULL. Check activity_user_profile.xml");
            etUserAge = findViewById(R.id.etUserAge);
            if (etUserAge == null) Log.e(TAG, "etUserAge is NULL. Check activity_user_profile.xml");
            btnSaveProfile = findViewById(R.id.btnSaveProfile);
            if (btnSaveProfile == null) Log.e(TAG, "btnSaveProfile is NULL. Check activity_user_profile.xml");
            btnLogout = findViewById(R.id.btnLogout); // Initialize btnLogout
            if (btnLogout == null) Log.e(TAG, "btnLogout is NULL. Check activity_user_profile.xml");
            tvCurrentUserId = findViewById(R.id.tvCurrentUserId); // Initialize tvCurrentUserId
            if (tvCurrentUserId == null) Log.e(TAG, "tvCurrentUserId is NULL. Check activity_user_profile.xml");

            // Display the current user ID
            tvCurrentUserId.setText("User ID: " + currentUserId);


            Log.d(TAG, "onCreate: UI elements initialized. Proceeding to loadUserProfile.");

            loadUserProfile(); // This method might cause issues if UI elements are null

            btnSaveProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveUserProfile();
                }
            });

            btnLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logoutUser(); // Implement logout method
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "onCreate: An unexpected error occurred!", e);
            Toast.makeText(this, "An app error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadUserProfile() {
        Log.d(TAG, "loadUserProfile: Attempting to load profile for " + currentUserId);
        DocumentReference docRef = db.collection("users").document(currentUserId);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        UserProfile userProfile = document.toObject(UserProfile.class);
                        if (userProfile != null) {
                            String allergiesText = userProfile.getAllergies() != null ? String.join(", ", userProfile.getAllergies()) : "";
                            String medicationsText = userProfile.getCurrentMedications() != null ? String.join(", ", userProfile.getCurrentMedications()) : "";
                            String conditionsText = userProfile.getMedicalConditions() != null ? String.join(", ", userProfile.getMedicalConditions()) : "";

                            etAllergies.setText(allergiesText);
                            etCurrentMedications.setText(medicationsText);
                            etMedicalConditions.setText(conditionsText);
                            if (userProfile.getAge() > 0) {
                                etUserAge.setText(String.valueOf(userProfile.getAge()));
                            } else {
                                etUserAge.setText("");
                            }

                            Toast.makeText(UserProfileActivity.this, "Profile loaded!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "loadUserProfile: Profile loaded successfully for " + currentUserId);
                        } else {
                            Log.e(TAG, "loadUserProfile: UserProfile object is null after conversion for " + currentUserId);
                            Toast.makeText(UserProfileActivity.this, "Error: Profile data malformed.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(UserProfileActivity.this, "No profile found. Please create one!", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "loadUserProfile: No profile document found for " + currentUserId + ". Initializing fields.");
                        etAllergies.setText("");
                        etCurrentMedications.setText("");
                        etMedicalConditions.setText("");
                        etUserAge.setText("");
                    }
                } else {
                    Log.e(TAG, "loadUserProfile: Error loading profile document for " + currentUserId + ": " + task.getException().getMessage(), task.getException());
                    Toast.makeText(UserProfileActivity.this, "Error loading profile: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveUserProfile() {
        List<String> allergies = parseCommaSeparatedString(etAllergies.getText().toString());
        List<String> currentMedications = parseCommaSeparatedString(etCurrentMedications.getText().toString());
        List<String> medicalConditions = parseCommaSeparatedString(etMedicalConditions.getText().toString());

        int age = 0;
        String ageText = etUserAge.getText().toString().trim();
        if (ageText.isEmpty()) {
            Toast.makeText(this, "Please enter your age.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            age = Integer.parseInt(ageText);
            if (age <= 0 || age > 150) {
                Toast.makeText(this, "Age must be a positive number and realistic.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid age input: " + ageText + " - " + e.getMessage());
            Toast.makeText(this, "Please enter a valid age (numbers only).", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfile userProfile = new UserProfile(currentUserId, allergies, currentMedications, medicalConditions, age);

        db.collection("users").document(currentUserId)
                .set(userProfile)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(UserProfileActivity.this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Profile saved for user: " + currentUserId);
                        } else {
                            Log.e(TAG, "Error saving profile for " + currentUserId + ": " + task.getException().getMessage(), task.getException());
                            Toast.makeText(UserProfileActivity.this, "Error saving profile: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private List<String> parseCommaSeparatedString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    /**
     * Handles user logout: signs out from Firebase, clears SharedPreferences, and redirects to LoginActivity.
     */
    private void logoutUser() {
        mAuth.signOut(); // Sign out from Firebase Authentication

        // Clear the stored user ID from SharedPreferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_CURRENT_USER_ID); // Remove the user ID
        editor.apply();

        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "User logged out. Redirecting to LoginActivity.");

        // Redirect to LoginActivity and clear the activity stack
        Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish current activity
    }
}
