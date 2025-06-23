package com.technathon.vmedicine;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.technathon.vmedicine.data.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit; // For OkHttpClient timeouts

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatBotActivity extends AppCompatActivity {

    private static final String TAG = "ChatBotActivity";
    // IMPORTANT: Replace "YOUR_ACTUAL_GEMINI_API_KEY_HERE" with your actual Gemini API Key.
    private static final String GEMINI_API_KEY = "AIzaSyBbFGrqfLMQMQTsOu8MOKbN97ybUlGSQgA";

    private EditText etChatMessage;
    private Button btnSendMessage;
    private LinearLayout chatContainer;
    private ScrollView chatScrollView;

    private OkHttpClient okHttpClient;
    private ExecutorService executorService;
    private FirebaseFirestore db;
    private String currentUserId = "testUser123"; // Placeholder User ID

    private List<JSONObject> chatHistory = new ArrayList<>(); // Stores chat history for context
    private UserProfile currentUserProfile; // To store fetched user profile for personalization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        // Initialize UI elements
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        chatContainer = findViewById(R.id.chatContainer);
        chatScrollView = findViewById(R.id.chatScrollView);

        // Initialize OkHttp and ExecutorService
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // Increased timeout for API calls
                .readTimeout(60, TimeUnit.SECONDS)   // Increased timeout
                .build();
        executorService = Executors.newSingleThreadExecutor();
        db = FirebaseFirestore.getInstance();

        // Fetch user profile for personalization
        fetchUserProfile();

        // Set up send button click listener
        btnSendMessage.setOnClickListener(v -> sendMessage());

        // Add a welcome message from the bot
        addBotMessage("Hello! I'm VMedicine AI. How can I help you today regarding your health or medicines? You can ask me about general medicine info, or even questions about your medical profile (allergies, conditions, current medicines).");
    }

    /**
     * Fetches the current user's profile from Firestore to personalize bot responses.
     */
    private void fetchUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                currentUserProfile = document.toObject(UserProfile.class);
                                Log.d(TAG, "User profile fetched for chatbot personalization.");
                            } else {
                                Log.d(TAG, "No user profile found for " + currentUserId + ". Chatbot responses will be general.");
                            }
                        } else {
                            Log.e(TAG, "Error fetching user profile for chatbot: " + task.getException().getMessage());
                            Toast.makeText(ChatBotActivity.this, "Could not load profile for personalization.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Handles sending a message: displays user message, calls API, displays bot response.
     */
    private void sendMessage() {
        String userMessage = etChatMessage.getText().toString().trim();
        if (userMessage.isEmpty()) {
            Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show();
            return;
        }

        addMessage(userMessage, true); // Display user's message
        etChatMessage.setText(""); // Clear input field
        btnSendMessage.setEnabled(false); // Disable send button

        // Construct the full prompt including user profile data
        String fullPrompt = buildPersonalizedPrompt(userMessage);
        callGeminiApi(fullPrompt);
    }

    /**
     * Builds a personalized prompt for the Gemini API based on the user's profile.
     */
    private String buildPersonalizedPrompt(String userMessage) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are VMedicine AI, a helpful assistant specialized in general medicine information. ");
        promptBuilder.append("Provide concise and direct answers. DO NOT give specific medical advice or dosages, always recommend consulting a doctor/pharmacist. ");
        promptBuilder.append("If a query is about personal health or medicine, use the provided user profile context. ");
        promptBuilder.append("If a question is outside medical/health domain, politely decline. ");
        promptBuilder.append("Keep responses brief, usually 1-3 sentences.\n\n");

        if (currentUserProfile != null) {
            promptBuilder.append("User Profile Context:\n");
            promptBuilder.append("- Age: ").append(currentUserProfile.getAge()).append(" years\n");
            if (currentUserProfile.getMedicalConditions() != null && !currentUserProfile.getMedicalConditions().isEmpty()) {
                promptBuilder.append("- Medical Conditions: ").append(String.join(", ", currentUserProfile.getMedicalConditions())).append("\n");
            }
            if (currentUserProfile.getAllergies() != null && !currentUserProfile.getAllergies().isEmpty()) {
                promptBuilder.append("- Allergies: ").append(String.join(", ", currentUserProfile.getAllergies())).append("\n");
            }
            if (currentUserProfile.getCurrentMedications() != null && !currentUserProfile.getCurrentMedications().isEmpty()) {
                promptBuilder.append("- Current Medications: ").append(String.join(", ", currentUserProfile.getCurrentMedications())).append("\n");
            }
            promptBuilder.append("\n"); // Separate context from query
        } else {
            promptBuilder.append("User profile is not available. Provide general medical information.\n\n");
        }

        promptBuilder.append("User Query: ").append(userMessage);
        return promptBuilder.toString();
    }


    /**
     * Adds a message to the chat display.
     * @param message The message text.
     * @param isUser True if it's a user message, false if it's a bot message.
     */
    private void addMessage(String message, boolean isUser) {
        runOnUiThread(() -> {
            TextView messageTextView = new TextView(this);
            messageTextView.setText(message);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int margin = (int) getResources().getDisplayMetrics().density * 8; // 8dp margin

            if (isUser) {
                params.gravity = Gravity.END;
                params.setMargins(margin * 4, margin, margin, margin); // Left margin wider for user
                messageTextView.setBackgroundResource(R.drawable.user_chat_bubble);
                messageTextView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            } else {
                params.gravity = Gravity.START;
                params.setMargins(margin, margin, margin * 4, margin); // Right margin wider for bot
                messageTextView.setBackgroundResource(R.drawable.bot_chat_bubble);
                messageTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            }
            messageTextView.setPadding(margin * 2, margin, margin * 2, margin);
            messageTextView.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.7)); // 70% width
            messageTextView.setLayoutParams(params);
            chatContainer.addView(messageTextView);

            // Scroll to the bottom
            chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    /**
     * Calls the Gemini API with the given prompt.
     * @param prompt The prompt to send to the AI.
     */
    private void callGeminiApi(String prompt) {
        executorService.execute(() -> {
            try {
                // Add user message to chat history for context
                JSONObject userMessagePart = new JSONObject().put("text", prompt);
                JSONObject userContent = new JSONObject().put("role", "user").put("parts", new JSONArray().put(userMessagePart));
                chatHistory.add(userContent); // Add to history

                JSONArray contentsArray = new JSONArray(chatHistory); // Use entire history

                JSONObject payload = new JSONObject();
                payload.put("contents", contentsArray);

                MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

                RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .build();

                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            addBotMessage("Sorry, I couldn't connect. Please check your internet connection.");
                            Log.e(TAG, "Gemini API call failed: " + e.getMessage(), e);
                            btnSendMessage.setEnabled(true);
                        });
                        // Remove last user message from history if API call failed for next retry
                        if (!chatHistory.isEmpty()) chatHistory.remove(chatHistory.size() - 1);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        runOnUiThread(() -> btnSendMessage.setEnabled(true));
                        if (response.isSuccessful()) {
                            try {
                                String responseBody = response.body() != null ? response.body().string() : "";
                                Log.d(TAG, "Gemini API Response: " + responseBody);

                                JSONObject jsonResponse = new JSONObject(responseBody);
                                String generatedText = "I'm sorry, I couldn't generate a response.";

                                if (jsonResponse.has("candidates") && jsonResponse.getJSONArray("candidates").length() > 0) {
                                    JSONObject candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
                                    if (candidate.has("content") && candidate.getJSONObject("content").has("parts") && candidate.getJSONObject("content").getJSONArray("parts").length() > 0) {
                                        generatedText = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                                        // Add bot message to chat history for context
                                        JSONObject botMessagePart = new JSONObject().put("text", generatedText);
                                        JSONObject botContent = new JSONObject().put("role", "model").put("parts", new JSONArray().put(botMessagePart));
                                        chatHistory.add(botContent);
                                    }
                                }
                                final String finalGeneratedText = generatedText;
                                runOnUiThread(() -> addBotMessage(finalGeneratedText));

                            } catch (JSONException e) {
                                runOnUiThread(() -> {
                                    addBotMessage("I encountered an error trying to understand the response.");
                                    Log.e(TAG, "Error parsing Gemini response: " + e.getMessage(), e);
                                });
                                // Remove last user message from history if parsing failed
                                if (!chatHistory.isEmpty()) chatHistory.remove(chatHistory.size() - 1);
                            }
                        } else {
                            runOnUiThread(() -> {
                                try {
                                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                                    addBotMessage("I'm having trouble processing that request. Error: " + response.code());
                                    Log.e(TAG, "Gemini API HTTP Error: " + response.code() + " - " + errorBody);
                                } catch (IOException e) {
                                    addBotMessage("I'm having trouble processing that request. Unknown error.");
                                    Log.e(TAG, "Gemini API HTTP Error (no body): " + response.code());
                                }
                                // Remove last user message from history if API call failed
                                if (!chatHistory.isEmpty()) chatHistory.remove(chatHistory.size() - 1);
                            });
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    addBotMessage("An unexpected error occurred. Please try again.");
                    Log.e(TAG, "Error building/sending Gemini request: " + e.getMessage(), e);
                    btnSendMessage.setEnabled(true);
                });
                // Remove last user message from history if unexpected error before API call
                if (!chatHistory.isEmpty()) chatHistory.remove(chatHistory.size() - 1);
            }
        });
    }

    /**
     * Adds a message from the bot to the chat display.
     * @param message The message text.
     */
    private void addBotMessage(String message) {
        addMessage(message, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // Ensure executor service is shut down
    }
}
