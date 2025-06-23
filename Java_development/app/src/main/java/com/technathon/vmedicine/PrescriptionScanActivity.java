package com.technathon.vmedicine;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap; // Not directly used yet, but kept for potential future use
import android.graphics.BitmapFactory; // Not directly used yet, but kept for potential future use
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.content.SharedPreferences; // Added import for SharedPreferences

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrescriptionScanActivity extends AppCompatActivity {

    private static final String TAG = "PrescriptionScan";
    private static final int CAMERA_PERMISSION_CODE = 101; // Not strictly needed with ActivityResultLauncher, but kept

    private static final String PREFS_NAME = "VMedicinePrefs"; // Constant for SharedPreferences file name
    private static final String KEY_CURRENT_USER_ID = "current_user_id"; // Constant for the key

    private String currentUserId; // Declared to store the retrieved user ID

    private ImageView ivPrescriptionImage;
    private Button btnCaptureImage;
    private Button btnProcessImage; // Button to trigger OCR
    private Uri currentPhotoUri; // URI of the captured image file

    // ActivityResultLauncher for camera intent
    private ActivityResultLauncher<Uri> takePictureLauncher;

    // ActivityResultLauncher for permission request
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription_scan);

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
        Log.d(TAG, "Current User ID in PrescriptionScanActivity: " + currentUserId);
        // --- End: Retrieve currentUserId from SharedPreferences ---


        // Initialize UI elements
        ivPrescriptionImage = findViewById(R.id.ivPrescriptionImage);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        btnProcessImage = findViewById(R.id.btnProcessImage);

        // Initialize ActivityResultLauncher for taking pictures
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        // Image successfully captured and saved to currentPhotoUri
                        if (currentPhotoUri != null) {
                            Log.d(TAG, "Image captured URI: " + currentPhotoUri.toString());
                            ivPrescriptionImage.setImageURI(currentPhotoUri);
                            btnProcessImage.setVisibility(View.VISIBLE); // Show process button
                            Toast.makeText(this, "Image captured!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "currentPhotoUri is null after successful capture result!");
                            Toast.makeText(this, "Image URI is missing after capture.", Toast.LENGTH_LONG).show();
                            btnProcessImage.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "Image capture cancelled or failed.");
                        Toast.makeText(this, "Image capture cancelled or failed.", Toast.LENGTH_SHORT).show();
                        currentPhotoUri = null; // Clear URI if capture failed
                        btnProcessImage.setVisibility(View.GONE); // Hide process button
                    }
                }
        );

        // Initialize ActivityResultLauncher for permission request
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, launch camera
                        dispatchTakePictureIntent();
                    } else {
                        Toast.makeText(this, "Camera permission is required to capture images.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Set click listener for Capture Image button
        btnCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermissionAndCapture();
            }
        });

        // Set click listener for Process Image button
        btnProcessImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPhotoUri != null) {
                    // Call the ML Kit processing method
                    processImageWithMLKit(currentPhotoUri);
                } else {
                    Toast.makeText(PrescriptionScanActivity.this, "No image to process.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Checks for CAMERA permission and dispatches the camera intent if granted.
     * Requests permission if not granted.
     */
    private void checkCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, dispatch intent
            dispatchTakePictureIntent();
        } else {
            // Request permission using the ActivityResultLauncher
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Launches the camera application to take a picture.
     * The image will be saved to the URI provided via FileProvider.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(); // Create a file to save the image
                Log.d(TAG, "Created temporary image file: " + photoFile.getAbsolutePath());
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Error creating photo file: " + ex.getMessage(), ex);
                Toast.makeText(this, "Error creating photo file: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                return; // Stop execution if file creation fails
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                // Get a content URI for the file using FileProvider
                currentPhotoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                Log.d(TAG, "FileProvider URI generated: " + currentPhotoUri.toString());
                // Pass the URI to the camera app
                takePictureLauncher.launch(currentPhotoUri);
            } else {
                Log.e(TAG, "photoFile is null, cannot dispatch take picture intent.");
                Toast.makeText(this, "Failed to prepare for image capture.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "No camera app found to handle the request.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No camera app found to handle MediaStore.ACTION_IMAGE_CAPTURE intent.");
        }
    }

    /**
     * Creates a unique temporary image file in the app's external pictures directory.
     * This directory is app-specific and doesn't require WRITE_EXTERNAL_STORAGE permission on modern Android.
     * @return The created File object.
     * @throws IOException if the file cannot be created.
     */
    private File createImageFile() throws IOException {
        // Create an image file name based on timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // Get the app-specific external pictures directory
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir == null) {
            // Fallback to internal app storage if external app-specific storage is not available
            Log.w(TAG, "getExternalFilesDir(Environment.DIRECTORY_PICTURES) returned null. Falling back to internal storage.");
            storageDir = new File(getFilesDir(), "Pictures");
        }

        // Ensure the directory exists. If not, try to create it.
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs(); // mkdirs creates parent directories as needed
            if (!created) {
                Log.e(TAG, "Failed to create directory for images: " + storageDir.getAbsolutePath());
                throw new IOException("Failed to create directory for images: " + storageDir.getAbsolutePath());
            } else {
                Log.d(TAG, "Successfully created storage directory: " + storageDir.getAbsolutePath());
            }
        }

        // Create the temporary image file
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        Log.d(TAG, "Image file created at: " + image.getAbsolutePath());
        return image;
    }

    /**
     * Processes the captured image using ML Kit Text Recognition.
     * @param imageUri The URI of the image to process.
     */
    private void processImageWithMLKit(Uri imageUri) {
        try {
            // Create an InputImage object from the URI
            InputImage image = InputImage.fromFilePath(this, imageUri);
            // Get an instance of TextRecognizer for Latin script (default for English)
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            Toast.makeText(this, "Extracting text from image...", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Starting text recognition process...");

            // Process the image
            recognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text result) {
                            // Text recognition successful
                            String recognizedText = result.getText();
                            if (recognizedText.isEmpty()) {
                                Toast.makeText(PrescriptionScanActivity.this, "No text found in the image.", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "Text recognition completed, but no text was found.");
                            } else {
                                Toast.makeText(PrescriptionScanActivity.this, "Text extracted successfully!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Text recognition successful. Passing to handler.");
                                // Pass the recognized text to a handler method for further processing (e.g., AI parsing)
                                handleRecognizedText(recognizedText);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Text recognition failed
                            Toast.makeText(PrescriptionScanActivity.this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Text recognition failed: ", e);
                        }
                    });

        } catch (IOException e) {
            Toast.makeText(this, "Error preparing image for processing: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "IOException creating InputImage from URI: ", e);
        } catch (Exception e) { // Catch any other unexpected exceptions
            Toast.makeText(this, "An unexpected error occurred during processing: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Unexpected error in processImageWithMLKit: ", e);
        }
    }

    /**
     * Handles the recognized text. Currently logs it and shows a toast.
     * In future steps, this will be where the text is sent to an AI for parsing.
     * @param text The full text recognized by ML Kit.
     */
    private void handleRecognizedText(String text) {
        // Log the full recognized text for debugging purposes
        Log.d(TAG, "Full Recognized Text:\n" + text);

        // Instead of just a toast, now we will pass this text to a new Activity
        Intent intent = new Intent(PrescriptionScanActivity.this, PrescriptionReviewActivity.class);
        // Put the recognized text as an extra in the Intent
        intent.putExtra("scanned_text", text);
        startActivity(intent);

        // Finish this activity so user doesn't come back here with back button immediately
        finish();
    }
}