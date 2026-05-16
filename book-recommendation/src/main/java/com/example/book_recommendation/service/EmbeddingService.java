package com.example.book_recommendation.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private static final int VECTOR_SIZE = 128;

    public List<Double> generateEmbedding(String text) {
        double[] vector = new double[VECTOR_SIZE];

        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.isBlank()) continue;

            int index = Math.abs(word.hashCode()) % VECTOR_SIZE;
            vector[index] += 1.0;
        }

        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }

        norm = Math.sqrt(norm);

        List<Double> result = new ArrayList<>();

        for (double value : vector) {
            result.add(norm == 0 ? 0.0 : value / norm);
        }

        return result;
    }
}