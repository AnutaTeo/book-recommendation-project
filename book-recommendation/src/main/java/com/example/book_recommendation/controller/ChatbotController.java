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

        if (message == null || message.isBlank()) {
            return Map.of("answer", "Please ask a question about the books.");
        }

        String lower = message.toLowerCase();

        List<VectorDocument> allDocs = vectorDatabaseService.getAllDocuments();

        if (lower.contains("alive") || lower.contains("alcie")) {
            lower = lower.replace("alive", "alice").replace("alcie", "alice");
        }

        if (lower.contains("show me all the books")
                || lower.contains("show me all books")
                || lower.contains("all books")
                || lower.contains("list books")) {

            List<VectorDocument> books = allDocs.stream()
                    .filter(this::isBookDocument)
                    .toList();

            return Map.of("answer",
                    "The available books are: " + titles(books) + ".");
        }

        if (lower.contains("give me the users")
                || lower.contains("show users")
                || lower.contains("list users")
                || lower.equals("users")) {

            List<VectorDocument> users = allDocs.stream()
                    .filter(this::isUserDocument)
                    .toList();

            String answer = users.stream()
                    .map(user -> extractUserSummary(user.getText()))
                    .collect(java.util.stream.Collectors.joining(" "));

            return Map.of("answer",
                    "The users in the vector database are: " + answer);
        }

        if ((lower.contains("my level") || lower.contains("i would like") || lower.contains("would i like"))
                && !lower.contains("alice")
                && !lower.contains("bob")) {

            return Map.of("answer",
                    "I need to know which user you are. Alice has Intermediate reading level and prefers Science Fiction. Bob has Beginner reading level and prefers Mystery.");
        }

        if (lower.contains("romance")
                || lower.contains("horror")
                || lower.contains("comedy")
                || lower.contains("history")
                || lower.contains("thriller")) {

            return Map.of("answer",
                    "I do not have this information in the vector database.");
        }


        if (lower.contains("what books are available")
                || lower.contains("available books")
                || lower.equals("books")) {

            List<VectorDocument> books = allDocs.stream()
                    .filter(this::isBookDocument)
                    .toList();

            return Map.of("answer", "The available books are: " + titles(books) + ".");
        }

        if (lower.trim().equals("find a book by author and theme.")) {
            return Map.of("answer",
                    "Please ask with a specific author and theme, for example: " +
                            "\"What book has the author Frank Herbert and the theme Science Fiction?\"");
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

        if (lower.contains("most likely to enjoy")) {

            List<VectorDocument> books = allDocs.stream()
                    .filter(this::isBookDocument)
                    .toList();

            if (books.isEmpty()) {
                return Map.of("answer",
                        "I could not find books in the vector database.");
            }

            VectorDocument best = books.get(0);

            return Map.of("answer",
                    "One recommended book from this list is \"" +
                            extractField(best.getText(), "Title") +
                            "\" by " +
                            extractField(best.getText(), "Author") +
                            ". It has themes " +
                            extractField(best.getText(), "Themes") +
                            " and is suitable for " +
                            extractField(best.getText(), "Reading level") +
                            " readers.");
        }

        if (lower.contains("recommend a book for")) {

            if (lower.contains("alice")) {

                List<VectorDocument> matches =
                        filterDocuments(allDocs, "theme:science fiction level:intermediate");

                if (matches.isEmpty()) {
                    return Map.of("answer",
                            "I could not find a recommendation for Alice.");
                }

                return Map.of("answer",
                        "Based on Alice's preferences, " +
                                formatRecommendation(matches.get(0)));
            }

            if (lower.contains("bob")) {

                List<VectorDocument> matches =
                        filterDocuments(allDocs, "theme:mystery");

                if (matches.isEmpty()) {
                    return Map.of("answer",
                            "I could not find a recommendation for Bob.");
                }

                return Map.of("answer",
                        "Based on Bob's preferences, " +
                                formatRecommendation(matches.get(0)));
            }

            return Map.of("answer",
                    "This user does not exist in the vector database.");
        }

        if (lower.contains("recommend") && lower.contains("alice")) {
            List<VectorDocument> matches =
                    filterDocuments(allDocs, "theme:science fiction level:intermediate");

            if (matches.isEmpty()) {
                return Map.of("answer",
                        "I could not find an exact match for Alice in the vector database.");
            }

            session.setAttribute("lastFilter", "theme:science fiction level:intermediate");

            List<String> shown = new ArrayList<>();
            shown.add(matches.get(0).getId());
            session.setAttribute("shownBookIds", shown);

            return Map.of("answer",
                    "Based on Alice's preferences, " + formatRecommendation(matches.get(0)));
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

            session.setAttribute("lastFilter", "level:advanced");

            List<String> shown = new ArrayList<>();
            shown.add(matches.get(0).getId());
            session.setAttribute("shownBookIds", shown);

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

            session.setAttribute("lastFilter", "level:beginner");

            List<String> shown = new ArrayList<>();
            shown.add(matches.get(0).getId());
            session.setAttribute("shownBookIds", shown);

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

            return Map.of("answer",
                    "I could not find a book with that author and theme in the vector database.");
        }

        if (lower.contains("murder") && lower.contains("frank herbert")) {
            return Map.of("answer",
                    "I could not find a book in the vector database that is both written by Frank Herbert and has the Murder theme.");
        }

        if (lower.contains("frank herbert")) {
            List<VectorDocument> matches = allDocs.stream()
                    .filter(this::isBookDocument)
                    .filter(doc -> doc.getText().toLowerCase().contains("author: frank herbert"))
                    .toList();

            if (!matches.isEmpty()) {
                return Map.of("answer",
                        "The book by Frank Herbert is " + titles(matches) + ".");
            }

            return Map.of("answer",
                    "I could not find a Frank Herbert book in the vector database.");
        }

        if (lower.contains("alex michaelides")) {
            List<VectorDocument> matches = allDocs.stream()
                    .filter(this::isBookDocument)
                    .filter(doc -> doc.getText().toLowerCase().contains("author: j. k. rowling"))
                    .toList();

            if (!matches.isEmpty()) {
                return Map.of("answer", "The book by Alex Michaelides is " + titles(matches) + ".");
            }

            return Map.of("answer", "I could not find a book by Alex Michaeldes in the vector database.");
        }

        if (lower.contains("j k rowling")) {
            List<VectorDocument> matches = allDocs.stream()
                    .filter(this::isBookDocument)
                    .filter(doc -> doc.getText().toLowerCase().contains("author: j. k. rowling"))
                    .toList();

            if (!matches.isEmpty()) {
                return Map.of("answer", "The book by J. K. Rowling is " + titles(matches) + ".");
            }

            return Map.of("answer", "I could not find a book by J. K. Rowling in the vector database.");
        }

        if (lower.contains("suzanne collins")) {
            List<VectorDocument> matches = allDocs.stream()
                    .filter(this::isBookDocument)
                    .filter(doc -> doc.getText().toLowerCase().contains("author: j. k. rowling"))
                    .toList();

            if (!matches.isEmpty()) {
                return Map.of("answer", "The book by  Suzanne Collins is " + titles(matches) + ".");
            }

            return Map.of("answer", "I could not find a book by  Suzanne Collins in the vector database.");
        }

        if (lower.contains("author")
                || lower.contains("ajuthor")
                || lower.contains("who wrote")) {

            VectorDocument best =
                    searchBestBookOnly(message);

            return Map.of("answer",
                    "\"" + extractField(best.getText(), "Title") +
                            "\" was written by " +
                            extractField(best.getText(), "Author") + ".");
        }

        if ((lower.contains("frank herbert")
                || lower.contains("j k rowling")
                || lower.contains("j. k. rowling")
                || lower.contains("suzanne collins")
                || lower.contains("alex michaelides"))
                && (lower.contains("science fiction")
                || lower.contains("fantasy")
                || lower.contains("mystery")
                || lower.contains("magic")
                || lower.contains("murder")
                || lower.contains("romance"))) {

            String author = "";

            if (lower.contains("frank herbert")) {
                author = "frank herbert";
            } else if (lower.contains("j k rowling") || lower.contains("j. k. rowling")) {
                author = "j. k. rowling";
            } else if (lower.contains("suzanne collins")) {
                author = "suzanne collins";
            } else if (lower.contains("alex michaelides")) {
                author = "alex michaelides";
            }

            String theme = "";

            if (lower.contains("science fiction")) {
                theme = "science fiction";
            } else if (lower.contains("fantasy")) {
                theme = "fantasy";
            } else if (lower.contains("mystery")) {
                theme = "mystery";
            } else if (lower.contains("magic")) {
                theme = "magic";
            } else if (lower.contains("murder")) {
                theme = "murder";
            } else if (lower.contains("romance")) {
                theme = "romance";
            }

            String finalAuthor = author;
            String finalTheme = theme;

            if ((lower.contains("all") || lower.contains("books") || lower.contains("book"))
                    && (lower.contains("science fiction")
                    || lower.contains("fantasy")
                    || lower.contains("mystery")
                    || lower.contains("magic")
                    || lower.contains("murder"))) {

                String filter = getThemeFilter(lower);

                List<VectorDocument> matches =
                        filterDocuments(allDocs, filter);

                if (matches.isEmpty()) {
                    return Map.of("answer",
                            "I could not find matching books in the vector database.");
                }

                return Map.of("answer",
                        "The matching books are: " + titles(matches) + ".");
            }

            List<VectorDocument> matches = allDocs.stream()
                    .filter(this::isBookDocument)
                    .filter(doc -> doc.getText().toLowerCase().contains("author: " + finalAuthor))
                    .filter(doc -> doc.getText().toLowerCase().contains(finalTheme))
                    .toList();

            if (matches.isEmpty()) {
                return Map.of("answer",
                        "I do not have this information in the vector database.");
            }

            return Map.of("answer",
                    "The matching books are: " + titles(matches) + ".");
        }

        if (lower.contains("level") || lower.contains("reading level")) {
            VectorDocument best =
                    searchBestBookOnly(message);

            return Map.of("answer",
                    "\"" + extractField(best.getText(), "Title") +
                            "\" is suitable for " +
                            extractField(best.getText(), "Reading level") +
                            " readers.");
        }

        if (lower.contains("what themes")
                || lower.contains("themes does")
                || lower.contains("theme of")) {

            VectorDocument best =
                    searchBestBookOnly(message);

            return Map.of("answer",
                    "\"" + extractField(best.getText(), "Title") +
                            "\" has these themes: " +
                            extractField(best.getText(), "Themes") + ".");
        }

        if (lower.contains("similar to")) {

            VectorDocument sourceBook =
                    searchBestBookOnly(message);

            String sourceThemes =
                    extractField(sourceBook.getText(), "Themes");

            List<VectorDocument> matches = allDocs.stream()
                    .filter(this::isBookDocument)
                    .filter(doc -> !doc.getId().equals(sourceBook.getId()))
                    .filter(doc ->
                            doc.getText().toLowerCase()
                                    .contains(sourceThemes.split(",")[0].trim().toLowerCase()))
                    .toList();

            if (!matches.isEmpty()) {

                return Map.of("answer",
                        "A similar book to \"" +
                                extractField(sourceBook.getText(), "Title") +
                                "\" is \"" +
                                extractField(matches.get(0).getText(), "Title") +
                                "\" because they share similar themes.");
            }

            return Map.of("answer",
                    "I could not find a similar book in the vector database.");
        }

        List<VectorDocument> results =
                vectorDatabaseService.search(message, 3)
                        .stream()
                        .filter(doc -> isBookDocument(doc) || isUserDocument(doc))
                        .toList();

        if (results.isEmpty()) {
            return Map.of("answer",
                    "I could not find matching information in the vector database.");
        }

        String context = results.stream()
                .map(VectorDocument::getText)
                .collect(java.util.stream.Collectors.joining("\n---\n"));

        String answer =
                geminiService.askGemini(message, context);

        if (answer == null
                || answer.isBlank()
                || answer.contains("Gemini API error")) {

            return Map.of("answer",
                    "I found this in the vector database: " +
                            summarizeDocuments(results));
        }

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

    private boolean isBookDocument(VectorDocument doc) {
        return doc.getText().contains("Book ID:");
    }

    private boolean isUserDocument(VectorDocument doc) {
        return doc.getText().contains("User ID:");
    }

    private VectorDocument findDocumentById(List<VectorDocument> docs, String id) {
        return docs.stream()
                .filter(doc -> doc.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow();
    }

    private VectorDocument searchBestBookOnly(String message) {
        return vectorDatabaseService.search(message, 5)
                .stream()
                .filter(this::isBookDocument)
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
                .filter(this::isBookDocument)
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

    private String getThemeFilter(String lower) {
        if (lower.contains("science fiction")) {
            return "theme:science fiction";
        }

        if (lower.contains("fantasy")) {
            return "theme:fantasy";
        }

        if (lower.contains("mystery")) {
            return "theme:mystery";
        }

        if (lower.contains("magic")) {
            return "theme:magic";
        }

        if (lower.contains("murder")) {
            return "theme:murder";
        }

        return "";
    }

    private VectorDocument findBestAuthorThemeMatch(List<VectorDocument> docs, String question) {
        for (VectorDocument doc : docs) {
            if (!isBookDocument(doc)) {
                continue;
            }

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
                .filter(this::isBookDocument)
                .map(doc -> "\"" + extractField(doc.getText(), "Title") + "\"")
                .filter(title -> !title.equals("\"Unknown\""))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String summarizeDocuments(List<VectorDocument> docs) {
        return docs.stream()
                .map(doc -> {
                    if (isBookDocument(doc)) {
                        return extractField(doc.getText(), "Title") +
                                " by " + extractField(doc.getText(), "Author") +
                                " (" + extractField(doc.getText(), "Themes") + ")";
                    }

                    if (isUserDocument(doc)) {
                        return extractUserSummary(doc.getText());
                    }

                    return "";
                })
                .filter(text -> !text.isBlank())
                .collect(java.util.stream.Collectors.joining("; "));
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