package com.technathon.vmedicine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.technathon.vmedicine.data.UserProfile; // Ensure this path is correct

import java.util.ArrayList; // For initializing empty lists

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "VMedicinePrefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // This layout will be created next

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, go to MainActivity
            Log.d(TAG, "User already logged in: " + currentUser.getUid());
            // Save UID to SharedPreferences just to ensure it's there
            saveUserIdToSharedPreferences(currentUser.getUid());
            launchMainActivity();
            return; // Exit onCreate
        }

        // Initialize UI elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Set up click listeners
        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
    }

    /**
     * Attempts to log in a user with the provided email and password.
     */
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(LoginActivity.this, "Authentication successful.", Toast.LENGTH_SHORT).show();
                                saveUserIdToSharedPreferences(user.getUid());
                                launchMainActivity();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Attempts to register a new user with the provided email and password.
     * If successful, it also creates a basic user profile in Firestore.
     */
    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(LoginActivity.this, "Registration successful.", Toast.LENGTH_SHORT).show();
                                saveUserIdToSharedPreferences(user.getUid());
                                createFirestoreUserProfile(user.getUid()); // Create initial profile
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Saves the current user's UID to SharedPreferences.
     */
    private void saveUserIdToSharedPreferences(String userId) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CURRENT_USER_ID, userId);
        editor.apply();
        Log.d(TAG, "User ID saved to SharedPreferences: " + userId);
    }

    /**
     * Creates a basic UserProfile document in Firestore for a newly registered user.
     */
    private void createFirestoreUserProfile(String userId) {
        // Check if a profile already exists before creating a new one (important for edge cases)
        db.collection("users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        Log.d(TAG, "Profile for " + userId + " already exists. Skipping creation.");
                        launchMainActivity(); // Profile exists, proceed to main
                    } else {
                        UserProfile newUserProfile = new UserProfile();
                        newUserProfile.setUserId(userId);
                        newUserProfile.setAge(0); // Default age, can be updated later
                        newUserProfile.setAllergies(new ArrayList<>());
                        newUserProfile.setCurrentMedications(new ArrayList<>());
                        newUserProfile.setMedicalConditions(new ArrayList<>());

                        db.collection("users").document(userId)
                                .set(newUserProfile)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "User profile created in Firestore for " + userId);
                                            launchMainActivity();
                                        } else {
                                            Log.e(TAG, "Error creating user profile in Firestore: " + task.getException().getMessage());
                                            Toast.makeText(LoginActivity.this, "Failed to create profile.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                } else {
                    Log.e(TAG, "Error checking user profile existence: " + task.getException().getMessage());
                    Toast.makeText(LoginActivity.this, "Error during profile check.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Launches MainActivity and finishes LoginActivity.
     */
    private void launchMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
        startActivity(intent);
        finish();
    }
}
