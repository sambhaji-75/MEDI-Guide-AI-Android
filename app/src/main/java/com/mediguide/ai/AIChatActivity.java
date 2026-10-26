package com.mediguide.ai;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class AIChatActivity extends AppCompatActivity {

    private final List<String> messages = new ArrayList<>();

    private ChatAdapter adapter;
    private EditText edtMessage;
    private Spinner spinnerLanguage;
    private RecyclerView recycler;
    private Button sendButton;
    private GeminiApi api;

    private String language = "English";
    private boolean languageReady = false;

    private final String[] languageValues = {
            "English", "Hindi", "Marathi"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        recycler = findViewById(R.id.recyclerChat);
        edtMessage = findViewById(R.id.edtMessage);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        sendButton = findViewById(R.id.btnSend);

        adapter = new ChatAdapter(messages);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        ArrayAdapter<CharSequence> languageAdapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.health_languages,
                        android.R.layout.simple_spinner_item
                );

        languageAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerLanguage.setAdapter(languageAdapter);

        spinnerLanguage.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (position >= 0 &&
                                position < languageValues.length) {

                            language = languageValues[position];

                            if (languageReady) {
                                saveLanguage(language);
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        setupGeminiApi();
        loadLanguage();

        sendButton.setOnClickListener(v -> {

            String question = edtMessage.getText()
                    .toString()
                    .trim();

            if (TextUtils.isEmpty(question)) {
                edtMessage.setError("Please enter your question");
                return;
            }

            if (!languageReady) {
                addAiMessage(
                        "AI: Please wait while your language preference loads."
                );
                return;
            }

            messages.add("You: " + question);
            adapter.notifyItemInserted(messages.size() - 1);
            recycler.scrollToPosition(messages.size() - 1);

            edtMessage.setText("");
            sendButton.setEnabled(false);

            askGemini(question);
        });
    }

    private void setupGeminiApi() {

        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(GeminiApi.class);
    }

    private void loadLanguage() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            languageReady = true;
            return;
        }

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    String saved = doc.getString("preferredLanguage");

                    if (saved != null) {

                        language = normalizeLanguage(saved);

                        for (int i = 0; i < languageValues.length; i++) {

                            if (languageValues[i].equals(language)) {
                                spinnerLanguage.setSelection(i);
                                break;
                            }
                        }
                    }

                    languageReady = true;
                })
                .addOnFailureListener(e -> {

                    languageReady = true;

                    addAiMessage(
                            "AI: Could not load your saved language. "
                                    + "You can still select a language."
                    );
                });
    }

    private String normalizeLanguage(String value) {

        if (value == null) {
            return "English";
        }

        String v = value.trim().toLowerCase();

        if (v.equals("hindi") || v.equals("हिंदी")) {
            return "Hindi";
        }

        if (v.equals("marathi") || v.equals("मराठी")) {
            return "Marathi";
        }

        return "English";
    }

    private void saveLanguage(String selectedLanguage) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("preferredLanguage", selectedLanguage);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        android.util.Log.e(
                                "GeminiAPI",
                                "Could not save language preference",
                                e
                        )
                );
    }

    private void askGemini(String question) {

        String apiKey = BuildConfig.GEMINI_API_KEY;

        if (apiKey == null || apiKey.trim().isEmpty()) {

            addAiMessage(
                    "AI: Gemini API key is missing. "
                            + "Please configure GEMINI_API_KEY in Gradle."
            );

            sendButton.setEnabled(true);
            return;
        }

        String prompt =
                "You are MediGuide-AI, an educational health assistant. "
                        + "Reply in " + language + " only. "
                        + "Use natural, clear language. "
                        + "Provide educational information about nutrition, hygiene, "
                        + "disease prevention, first aid, medicine safety, pregnancy, "
                        + "mother care, baby care, vaccination, child growth, elderly care, "
                        + "healthy habits and common health conditions. "
                        + "Explain medical terms in simple language. "
                        + "Do not claim to diagnose the user or prescribe personalized treatment. "
                        + "Do not invent medical facts. "
                        + "For emergencies, recommend immediate medical help. "
                        + "For medicine doses, vaccination schedules, pregnancy complications "
                        + "or serious symptoms, recommend a qualified healthcare professional. "
                        + "Answer this question clearly: "
                        + question;

        GeminiRequest request = new GeminiRequest();
        request.contents = new ArrayList<>();

        Content content = new Content();
        content.parts = new ArrayList<>();

        Part part = new Part();
        part.text = prompt;

        content.parts.add(part);
        request.contents.add(content);

        api.generate(
                "gemini-2.5-flash",
                apiKey,
                request
        ).enqueue(new Callback<GeminiResponse>() {

            @Override
            public void onResponse(
                    @NonNull Call<GeminiResponse> call,
                    @NonNull Response<GeminiResponse> response) {

                try {

                    if (response.isSuccessful()
                            && response.body() != null) {

                        String answer = response.body().getText();

                        addAiMessage("AI: " + answer);

                    } else {

                        String details = "";

                        try {
                            if (response.errorBody() != null) {
                                details = response.errorBody().string();
                            }
                        } catch (Exception e) {
                            details = "Could not read error details: "
                                    + e.getMessage();
                        }

                        if (TextUtils.isEmpty(details)) {
                            details = "The server did not provide error details.";
                        }

                        String errorMessage =
                                "Gemini API Error: HTTP "
                                        + response.code()
                                        + " - "
                                        + response.message()
                                        + "\n"
                                        + details;

                        android.util.Log.e(
                                "GeminiAPI",
                                errorMessage
                        );

                        addAiMessage(errorMessage);
                    }

                } catch (Exception e) {

                    android.util.Log.e(
                            "GeminiAPI",
                            "Error processing the response",
                            e
                    );

                    addAiMessage(
                            "AI: Could not process the server response. "
                                    + e.getMessage()
                    );

                } finally {
                    sendButton.setEnabled(true);
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<GeminiResponse> call,
                    @NonNull Throwable t) {

                android.util.Log.e(
                        "GeminiAPI",
                        "Network request failed",
                        t
                );

                addAiMessage(
                        "AI: Network request failed. "
                                + "Check your internet connection. Details: "
                                + t.getMessage()
                );

                sendButton.setEnabled(true);
            }
        });
    }

    private void addAiMessage(String message) {

        messages.add(message);

        int position = messages.size() - 1;

        adapter.notifyItemInserted(position);
        recycler.scrollToPosition(position);
    }

    interface GeminiApi {

        @POST("v1beta/models/{model}:generateContent")
        Call<GeminiResponse> generate(
                @Path("model") String model,
                @Query("key") String key,
                @Body GeminiRequest request
        );
    }

    static class GeminiRequest {
        List<Content> contents;
    }

    static class Content {
        String role = "user";
        List<Part> parts;
    }

    static class Part {
        String text;
    }

    static class GeminiResponse {

        List<Candidate> candidates;

        String getText() {

            if (candidates == null || candidates.isEmpty()) {
                return "No response received from Gemini.";
            }

            Candidate candidate = candidates.get(0);

            if (candidate.content == null
                    || candidate.content.parts == null
                    || candidate.content.parts.isEmpty()
                    || candidate.content.parts.get(0).text == null) {

                return "Gemini returned no text. Check the API response.";
            }

            return candidate.content.parts.get(0).text;
        }
    }

    static class Candidate {
        Content content;
    }

    static class ChatAdapter
            extends RecyclerView.Adapter<ChatAdapter.Holder> {

        private final List<String> data;

        ChatAdapter(List<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat, parent, false);

            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull Holder holder,
                int position) {

            holder.text.setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class Holder extends RecyclerView.ViewHolder {

            TextView text;

            Holder(View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.txtMessage);
            }
        }
    }
}