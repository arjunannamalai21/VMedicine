// MainActivity.java - Updated to retrieve currentUserId from SharedPreferences

package com.technathon.vmedicine;

import android.content.Intent;
import android.content.SharedPreferences; // Import added
import android.os.Bundle;
import android.util.Log; // Import added for logging
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // Added TAG for logging
    private static final String PREFS_NAME = "VMedicinePrefs"; // Constant for SharedPreferences file name
    private static final String KEY_CURRENT_USER_ID = "current_user_id"; // Constant for the key

    private String currentUserId; // Declared to store the retrieved user ID

    private Button btnScanQrCode;
    private Button btnScanDoctorPrescription;
    private Button btnAiChatBot;
    private Button btnFindMedicine;
    private Button btnUserProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Start: Retrieve currentUserId from SharedPreferences ---
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = preferences.getString(KEY_CURRENT_USER_ID, null); // Get the saved ID

        if (currentUserId == null) {
            // If no user ID is found in SharedPreferences, it means no user was selected
            // Redirect to UserSelectionActivity
            Toast.makeText(this, "No user selected. Please select a profile.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, UserSelectionActivity.class);
            // Clear the back stack so pressing back from UserSelection doesn't return here
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish MainActivity
            return; // IMPORTANT: Exit onCreate early as the user will be redirected
        }
        Log.d(TAG, "Current User ID in MainActivity: " + currentUserId);
        // --- End: Retrieve currentUserId from SharedPreferences ---


        // Initialize buttons
        btnScanQrCode = findViewById(R.id.btnScanQrCode);
        btnScanDoctorPrescription = findViewById(R.id.btnScanDoctorPrescription);
        btnAiChatBot = findViewById(R.id.btnAiChatBot);
        btnFindMedicine = findViewById(R.id.btnFindMedicine);
        btnUserProfile = findViewById(R.id.btnUserProfile);


        btnScanQrCode.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
            startActivity(intent);
        });

        btnScanDoctorPrescription.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PrescriptionScanActivity.class);
            startActivity(intent);
        });

        btnAiChatBot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatBotActivity.class);
            startActivity(intent);
        });

        btnFindMedicine.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FindMedicineActivity.class);
            startActivity(intent);
        });

        btnUserProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });
    }
}