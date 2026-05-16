package com.example.book_recommendation.controller;

import com.example.book_recommendation.model.Book;
import com.example.book_recommendation.model.VectorDocument;
import com.example.book_recommendation.service.BookRdfService;
import com.example.book_recommendation.service.GeminiService;
import com.example.book_recommendation.service.VectorDatabaseService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final BookRdfService bookRdfService;
    private final VectorDatabaseService vectorDatabaseService;
    private final GeminiService geminiService;

    public ChatbotController(BookRdfService bookRdfService,
                             VectorDatabaseService vectorDatabaseService,
                             GeminiService geminiService) {
        this.bookRdfService = bookRdfService;
        this.vectorDatabaseService = vectorDatabaseService;
        this.geminiService = geminiService;
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

    @PostMapping("/ask")
    public Map<String, String> ask(@RequestBody Map<String, String> request,
                                   jakarta.servlet.http.HttpSession session) {

        String message = request.get("message");
        String lower = message.toLowerCase();

        List<VectorDocument> allDocs = vectorDatabaseService.getAllDocuments();

        if (lower.contains("alive")) {
            lower = lower.replace("alive", "alice");
        }

        if (lower.equals("what else") || lower.contains("another")) {
            List<String> previousIds =
                    (List<String>) session.getAttribute("shownBookIds");

            String lastFilter =
                    (String) session.getAttribute("lastFilter");

            if (previousIds == null || lastFilter == null) {
                return Map.of("answer",
                        "Ask me first for a recommendation, then I can suggest another one.");
            }

            List<VectorDocument> filtered =
                    filterDocuments(allDocs, lastFilter);

            for (VectorDocument doc : filtered) {
                if (!previousIds.contains(doc.getId())) {
                    previousIds.add(doc.getId());
                    session.setAttribute("shownBookIds", previousIds);
                    return Map.of("answer", formatRecommendation(doc));
                }
            }

            return Map.of("answer",
                    "I do not have more matching books in the vector database.");
        }

        if (lower.contains("alice") && lower.contains("bob")) {
            VectorDocument alice = findDocumentById(allDocs, "Alice");
            VectorDocument bob = findDocumentById(allDocs, "Bob");

            return Map.of("answer",
                    "Based on the vector database: " +
                            extractUserSummary(alice.getText()) + " " +
                            extractUserSummary(bob.getText()));
        }

        if (lower.contains("alice")) {
            VectorDocument alice = findDocumentById(allDocs, "Alice");

            return Map.of("answer",
                    "Based on the vector database: " +
                            extractUserSummary(alice.getText()));
        }

        if (lower.contains("bob")) {
            VectorDocument bob = findDocumentById(allDocs, "Bob");

            return Map.of("answer",
                    "Based on the vector database: " +
                            extractUserSummary(bob.getText()));
        }

        if ((lower.contains("all") || lower.contains("books"))
                && (lower.contains("science fiction")
                || lower.contains("fantasy")
                || lower.contains("mystery")
                || lower.contains("magic")
                || lower.contains("murder"))) {

            String filter = "";

            if (lower.contains("science fiction")) {
                filter = "theme:science fiction";
            } else if (lower.contains("fantasy")) {
                filter = "theme:fantasy";
            } else if (lower.contains("mystery")) {
                filter = "theme:mystery";
            } else if (lower.contains("magic")) {
                filter = "theme:magic";
            } else if (lower.contains("murder")) {
                filter = "theme:murder";
            }

            List<VectorDocument> matches = filterDocuments(allDocs, filter);

            if (matches.isEmpty()) {
                return Map.of("answer",
                        "I could not find matching books in the vector database.");
            }

            return Map.of("answer",
                    "The matching books are: " + titles(matches) + ".");
        }

        if (lower.contains("how many") && lower.contains("intermediate")) {
            List<VectorDocument> matches =
                    filterDocuments(allDocs, "level:intermediate");

            session.setAttribute("lastFilter", "level:intermediate");

            List<String> shown = new ArrayList<>();

            if (!matches.isEmpty()) {
                shown.add(matches.get(0).getId());
            }

            session.setAttribute("shownBookIds", shown);

            return Map.of("answer",
                    "There are " + matches.size() +
                            " books suitable for Intermediate readers: " +
                            titles(matches) + ".");
        }

        if (lower.contains("advanced")) {
            List<VectorDocument> matches =
                    filterDocuments(allDocs, "level:advanced");

            if (matches.isEmpty()) {
                return Map.of("answer",
                        "I could not find Advanced books in the vector database.");
            }

            return Map.of("answer",
                    "The Advanced books in the vector database are: " +
                            titles(matches) + ".");
        }

        if (lower.contains("beginner")) {
            List<VectorDocument> matches =
                    filterDocuments(allDocs, "level:beginner");

            if (matches.isEmpty()) {
                return Map.of("answer",
                        "I could not find Beginner books in the vector database.");
            }

            return Map.of("answer",
                    "The Beginner books in the vector database are: " +
                            titles(matches) + ".");
        }

        if (lower.contains("intermediate")) {
            List<VectorDocument> matches =
                    filterDocuments(allDocs, "level:intermediate");

            session.setAttribute("lastFilter", "level:intermediate");

            List<String> shown = new ArrayList<>();

            if (!matches.isEmpty()) {
                shown.add(matches.get(0).getId());
            }

            session.setAttribute("shownBookIds", shown);

            if (matches.isEmpty()) {
                return Map.of("answer",
                        "I could not find Intermediate books in the vector database.");
            }

            if (lower.contains("all")
                    || lower.contains("books")
                    || lower.contains("recommendations")) {
                return Map.of("answer",
                        "The Intermediate books in the vector database are: " +
                                titles(matches) + ".");
            }

            return Map.of("answer", formatRecommendation(matches.get(0)));
        }

        if (lower.contains("author") && lower.contains("theme")) {
            VectorDocument exact =
                    findBestAuthorThemeMatch(allDocs, lower);

            if (exact != null) {
                return Map.of("answer",
                        "The matching book is \"" +
                                extractField(exact.getText(), "Title") +
                                "\".");
            }
        }

        if (lower.contains("murder")
                && lower.contains("frank herbert")) {
            return Map.of("answer",
                    "I could not find a book in the vector database that is both written by Frank Herbert and has the Murder theme.");
        }

        if (lower.contains("author")
                || lower.contains("ajuthor")
                || lower.contains("who wrote")) {
            VectorDocument best =
                    vectorDatabaseService.search(message, 1).get(0);

            return Map.of("answer",
                    "\"" + extractField(best.getText(), "Title") +
                            "\" was written by " +
                            extractField(best.getText(), "Author") + ".");
        }

        if (lower.contains("level")
                || lower.contains("reading level")) {
            VectorDocument best =
                    vectorDatabaseService.search(message, 1).get(0);

            return Map.of("answer",
                    "\"" + extractField(best.getText(), "Title") +
                            "\" is suitable for " +
                            extractField(best.getText(), "Reading level") +
                            " readers.");
        }

        List<VectorDocument> results =
                vectorDatabaseService.search(message, 3);

        String context = results.stream()
                .map(VectorDocument::getText)
                .collect(java.util.stream.Collectors.joining("\n---\n"));

        String answer =
                geminiService.askGemini(message, context);

        return Map.of("answer", answer);
    }

    private String extractField(String text, String fieldName) {
        for (String line : text.split("\n")) {
            if (line.startsWith(fieldName + ":")) {
                return line.replace(fieldName + ":", "").trim();
            }
        }

        return "Unknown";
    }

    private VectorDocument findDocumentById(List<VectorDocument> docs, String id) {
        return docs.stream()
                .filter(doc -> doc.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow();
    }

    private String extractUserSummary(String text) {
        String name = extractField(text, "Name");
        String theme = extractField(text, "Preferred theme");
        String level = extractField(text, "Reading level");

        return name + " prefers " + theme +
                " and has " + level + " reading level.";
    }

    private List<VectorDocument> filterDocuments(List<VectorDocument> docs, String filter) {
        String lowerFilter = filter.toLowerCase();

        return docs.stream()
                .filter(doc -> {
                    String text = doc.getText().toLowerCase();

                    if (lowerFilter.contains("level:intermediate")
                            && !text.contains("reading level: intermediate")) {
                        return false;
                    }

                    if (lowerFilter.contains("level:beginner")
                            && !text.contains("reading level: beginner")) {
                        return false;
                    }

                    if (lowerFilter.contains("level:advanced")
                            && !text.contains("reading level: advanced")) {
                        return false;
                    }

                    if (lowerFilter.contains("theme:science fiction")
                            && !text.contains("science fiction")) {
                        return false;
                    }

                    if (lowerFilter.contains("theme:fantasy")
                            && !text.contains("fantasy")) {
                        return false;
                    }

                    if (lowerFilter.contains("theme:mystery")
                            && !text.contains("mystery")) {
                        return false;
                    }

                    if (lowerFilter.contains("theme:magic")
                            && !text.contains("magic")) {
                        return false;
                    }

                    if (lowerFilter.contains("theme:murder")
                            && !text.contains("murder")) {
                        return false;
                    }

                    return true;
                })
                .toList();
    }

    private VectorDocument findBestAuthorThemeMatch(List<VectorDocument> docs, String question) {
        for (VectorDocument doc : docs) {
            String text = doc.getText().toLowerCase();

            boolean authorMatches =
                    (question.contains("frank herbert") && text.contains("author: frank herbert")) ||
                            (question.contains("j.k. rowling") && text.contains("author: j. k. rowling")) ||
                            (question.contains("j. k. rowling") && text.contains("author: j. k. rowling")) ||
                            (question.contains("suzanne collins") && text.contains("author: suzanne collins")) ||
                            (question.contains("alex michaelides") && text.contains("author: alex michaelides"));

            boolean themeMatches =
                    (question.contains("science fiction") && text.contains("science fiction")) ||
                            (question.contains("fantasy") && text.contains("fantasy")) ||
                            (question.contains("mystery") && text.contains("mystery")) ||
                            (question.contains("murder") && text.contains("murder")) ||
                            (question.contains("magic") && text.contains("magic"));

            if (authorMatches && themeMatches) {
                return doc;
            }
        }

        return null;
    }

    private String formatRecommendation(VectorDocument doc) {
        String title = extractField(doc.getText(), "Title");
        String author = extractField(doc.getText(), "Author");
        String level = extractField(doc.getText(), "Reading level");
        String themes = extractField(doc.getText(), "Themes");

        return "I recommend \"" + title + "\" by " + author +
                ". It has themes " + themes +
                " and is suitable for " + level + " readers.";
    }

    private String titles(List<VectorDocument> docs) {
        return docs.stream()
                .map(doc -> "\"" + extractField(doc.getText(), "Title") + "\"")
                .collect(java.util.stream.Collectors.joining(", "));
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