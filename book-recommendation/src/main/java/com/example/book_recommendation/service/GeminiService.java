package com.example.book_recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GeminiService {

    private static final String API_KEY = "AIzaSyBD3cNJF8mj2Nk87QxAOJ2UPWcaCS7bR38";

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                    + API_KEY;

    private final OkHttpClient client = new OkHttpClient();

    private final ObjectMapper mapper = new ObjectMapper();

    public String askGemini(String question, String context) {

        try {

            String prompt = """
                    You are a book recommendation chatbot.

                    ONLY use this vector database context:

                    %s

                    User question:
                    %s

                    If the answer is not present in the context,
                    say you cannot find it in the vector database.
                    """.formatted(context, question);

            String json = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": %s
                            }
                          ]
                        }
                      ]
                    }
                    """.formatted(mapper.writeValueAsString(prompt));

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(URL)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                return "Gemini API error.";
            }

            String responseBody = response.body().string();

            JsonNode root = mapper.readTree(responseBody);

            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (IOException e) {
            e.printStackTrace();
            return "Could not contact Gemini.";
        }
    }
}