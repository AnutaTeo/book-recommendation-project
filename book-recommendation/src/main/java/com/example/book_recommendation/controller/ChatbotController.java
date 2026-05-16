package com.example.book_recommendation.controller;

import com.example.book_recommendation.model.Book;
import com.example.book_recommendation.model.VectorDocument;
import com.example.book_recommendation.service.BookRdfService;
import com.example.book_recommendation.service.VectorDatabaseService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final BookRdfService bookRdfService;
    private final VectorDatabaseService vectorDatabaseService;

    public ChatbotController(BookRdfService bookRdfService, VectorDatabaseService vectorDatabaseService) {
        this.bookRdfService = bookRdfService;
        this.vectorDatabaseService = vectorDatabaseService;
    }

    @GetMapping("/starters")
    public Map<String, List<String>> getStarters(@RequestParam String pageUrl) {
        List<String> starters = new ArrayList<>();

        if (pageUrl.startsWith("/books/") && !pageUrl.equals("/books")) {
            String bookId = pageUrl.substring(pageUrl.lastIndexOf("/") + 1);
            Book book = bookRdfService.getBookById(bookId);

            if (book != null) {
                starters.add("Who wrote " + book.getTitle() + "?");
                starters.add("What themes does " + book.getTitle() + " have?");
                starters.add("Recommend a book similar to " + book.getTitle() + ".");
            }
        } else if (pageUrl.equals("/books")) {
            starters.add("What is a book that I am most likely to enjoy from this list?");
            starters.add("Which books are suitable for Beginner readers?");
            starters.add("Show me books with the Science Fiction theme.");
        } else {
            starters.add("What books are available?");
            starters.add("Find a book by author and theme.");
            starters.add("Recommend a book for Alice.");
        }

        return Map.of("starters", starters);
    }

    private String extractField(String text, String fieldName) {

        for (String line : text.split("\n")) {

            if (line.startsWith(fieldName + ":")) {
                return line.replace(fieldName + ":", "").trim();
            }
        }

        return "Unknown";
    }

    @PostMapping("/ask")
    public Map<String, String> ask(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String lower = message.toLowerCase();

        List<VectorDocument> results = vectorDatabaseService.search(message, 1);

        if (results.isEmpty()) {
            return Map.of("answer", "I could not find matching information in the vector database.");
        }

        VectorDocument bestMatch = results.get(0);
        String text = bestMatch.getText();

        String title = extractField(text, "Title");
        String author = extractField(text, "Author");
        String level = extractField(text, "Reading level");
        String themes = extractField(text, "Themes");

        if (lower.contains("alice")) {
            return Map.of("answer",
                    "Alice prefers Science Fiction and has an Intermediate reading level. " +
                            "Based on the current book data, a close match is Hunger Games because it has the Science Fiction theme.");
        }

        if (lower.contains("bob")) {
            return Map.of("answer",
                    "Bob prefers Mystery and has a Beginner reading level. " +
                            "The Silent Patient matches Bob's Mystery theme, but its reading level is Intermediate.");
        }

        if (lower.contains("my reading level")) {
            return Map.of("answer",
                    "I need to know which user you are. Alice has Intermediate reading level, and Bob has Beginner reading level.");
        }

        if (lower.contains("theme") || lower.contains("genre")) {
            return Map.of("answer",
                    "\"" + title + "\" has these themes: " + themes + ".");
        }

        if (lower.contains("author") || lower.contains("who wrote")) {
            return Map.of("answer",
                    "\"" + title + "\" was written by " + author + ".");
        }

        if (lower.contains("reading level") || lower.contains("level")) {
            return Map.of("answer",
                    "\"" + title + "\" is suitable for " + level + " readers.");
        }

        if (lower.contains("what book has") || lower.contains("find a book")) {
            return Map.of("answer",
                    "The matching book is \"" + title + "\" by " + author +
                            ". Themes: " + themes + ". Reading level: " + level + ".");
        }

        if (lower.contains("why")) {
            return Map.of("answer",
                    "Because the closest match in the vector database is \"" + title +
                            "\", which has themes " + themes + " and reading level " + level + ".");
        }

        return Map.of("answer",
                "I recommend \"" + title + "\" by " + author +
                        ". It has themes " + themes + " and is suitable for " + level + " readers.");
    }

    @PostMapping("/rebuild-vector-db")
    public Map<String, String> rebuildVectorDb() {
        System.out.println("Rebuild endpoint was called");

        vectorDatabaseService.rebuildVectorDatabase();

        return Map.of(
                "message",
                "Vector database rebuilt successfully from RDF/XML books data."
        );
    }

    @GetMapping("/search")
    public Map<String, Object> searchVectorDb(@RequestParam String query) {
        return Map.of(
                "query", query,
                "results", vectorDatabaseService.search(query, 3)
        );
    }

}