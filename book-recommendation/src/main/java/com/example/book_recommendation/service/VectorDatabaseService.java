package com.example.book_recommendation.service;

import com.example.book_recommendation.model.Book;
import com.example.book_recommendation.model.VectorDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class VectorDatabaseService {

    private static final String VECTOR_DB_FILE = "book-recommendation/data/vector-db.json";

    private final BookRdfService bookRdfService;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VectorDatabaseService(BookRdfService bookRdfService, EmbeddingService embeddingService) {
        this.bookRdfService = bookRdfService;
        this.embeddingService = embeddingService;
    }

    public void rebuildVectorDatabase() {
        try {
            System.out.println("Starting vector database rebuild...");

            List<Book> books = bookRdfService.getAllBooks();
            System.out.println("Books found: " + books.size());

            List<VectorDocument> documents = new ArrayList<>();

            for (Book book : books) {
                System.out.println("Generating embedding for: " + book.getTitle());

                String text = createBookText(book);
                List<Double> embedding = embeddingService.generateEmbedding(text);

                System.out.println("Embedding generated for: " + book.getTitle());

                documents.add(new VectorDocument(
                        book.getId(),
                        text,
                        embedding
                ));

                List<String> userTexts = createUserTexts();

                for (String userText : userTexts) {
                    String userId = userText.contains("Alice") ? "Alice" : "Bob";

                    System.out.println("Generating embedding for user: " + userId);

                    List<Double> userEmbedding = embeddingService.generateEmbedding(userText);

                    documents.add(new VectorDocument(
                            userId,
                            userText,
                            embedding
                    ));
                }
            }

            Path path = Path.of(VECTOR_DB_FILE);
            Files.createDirectories(path.getParent());

            System.out.println("Saving vector DB to: " + path.toAbsolutePath());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(path.toFile(), documents);

            System.out.println("Vector database saved successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not rebuild vector database: " + e.getMessage());
        }
    }

    public List<VectorDocument> search(String query, int topK) {
        List<VectorDocument> documents = loadDocuments();
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);

        return documents.stream()
                .sorted((a, b) -> Double.compare(
                        cosineSimilarity(queryEmbedding, b.getEmbedding()),
                        cosineSimilarity(queryEmbedding, a.getEmbedding())
                ))
                .limit(topK)
                .toList();
    }

    private List<VectorDocument> loadDocuments() {
        try {
            Path path = Path.of(VECTOR_DB_FILE);

            if (!Files.exists(path)) {
                rebuildVectorDatabase();
            }

            return objectMapper.readValue(
                    path.toFile(),
                    new TypeReference<List<VectorDocument>>() {}
            );

        } catch (Exception e) {
            throw new RuntimeException("Could not load vector database: " + e.getMessage());
        }
    }

    public List<VectorDocument> getAllDocuments() {
        return loadDocuments();
    }

    private String createBookText(Book book) {
        String author = book.getAuthor();

        if (book.getId().equals("Dune")) {
            author = "Frank Herbert";
        } else if (book.getId().equals("TheSilentPatient")) {
            author = "Alex Michaelides";
        } else if (book.getId().equals("HungerGames")) {
            author = "Suzanne Collins";
        }

        return """
            Book ID: %s
            Title: %s
            Author: %s
            Reading level: %s
            Themes: %s
            """.formatted(
                book.getId(),
                book.getTitle(),
                author,
                book.getReadingLevel(),
                String.join(", ", book.getThemes())
        );
    }

    private List<String> createUserTexts() {
        return List.of(
                """
                User ID: Alice
                Name: Alice
                Preferred theme: Science Fiction
                Reading level: Intermediate
                Recommendation rule: Recommend books that match Alice's preferred theme and reading level.
                """,
                """
                User ID: Bob
                Name: Bob
                Preferred theme: Mystery
                Reading level: Beginner
                Recommendation rule: Recommend books that match Bob's preferred theme and reading level.
                """
        );
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }


}