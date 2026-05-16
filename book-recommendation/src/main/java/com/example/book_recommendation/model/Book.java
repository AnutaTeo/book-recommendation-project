package com.example.book_recommendation.model;

import java.util.ArrayList;
import java.util.List;

public class Book {

    private String id;
    private String title;
    private String author;
    private String readingLevel;
    private List<String> themes = new ArrayList<>();

    public Book() {
    }

    public Book(String id, String title, String author, String readingLevel, List<String> themes) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.readingLevel = readingLevel;
        this.themes = themes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author != null ? author : "Unknown";
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getReadingLevel() {
        return readingLevel != null ? readingLevel : "Not specified";
    }

    public void setReadingLevel(String readingLevel) {
        this.readingLevel = readingLevel;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }
}