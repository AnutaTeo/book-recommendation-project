package com.example.book_recommendation.model;

import java.util.List;

public class VectorDocument {

    private String id;
    private String text;
    private List<Double> embedding;

    public VectorDocument() {
    }

    public VectorDocument(String id, String text, List<Double> embedding) {
        this.id = id;
        this.text = text;
        this.embedding = embedding;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
}