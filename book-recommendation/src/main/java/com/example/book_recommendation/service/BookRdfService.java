package com.example.book_recommendation.service;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;

@Service
public class BookRdfService {

    private static final String BASE_URI = "http://example.org/book-recommendation#";
    private static final String RDF_FILE = "book-recommendation/data/books.rdf";

    public void addBook(
            String title,
            String author,
            String theme1,
            String theme2,
            String readingLevel
    ) {
        Model model = loadModel();

        String bookId = createBookId(title);

        Resource book = model.createResource(BASE_URI + bookId);
        Resource bookClass = model.createResource(BASE_URI + "Book");

        Property titleProperty = model.createProperty(BASE_URI, "title");
        Property authorProperty = model.createProperty(BASE_URI, "author");
        Property themeProperty = model.createProperty(BASE_URI, "hasTheme");
        Property readingLevelProperty = model.createProperty(BASE_URI, "suitableForReadingLevel");

        book.addProperty(RDF.type, bookClass);
        book.addProperty(titleProperty, title);
        book.addProperty(authorProperty, author);
        book.addProperty(themeProperty, theme1);

        if (theme2 != null && !theme2.isBlank()) {
            book.addProperty(themeProperty, theme2);
        }

        book.addProperty(readingLevelProperty, readingLevel);

        saveModel(model);
    }

    public void updateBookReadingLevel(String bookId, String newReadingLevel) {
        Model model = loadModel();

        Resource book = model.getResource(BASE_URI + bookId);
        Property readingLevelProperty = model.createProperty(BASE_URI, "suitableForReadingLevel");

        book.removeAll(readingLevelProperty);
        book.addProperty(readingLevelProperty, newReadingLevel);

        saveModel(model);
    }

    private Model loadModel() {
        Model model = ModelFactory.createDefaultModel();

        Path path = Path.of(RDF_FILE);

        if (!path.toFile().exists()) {
            throw new RuntimeException(
                    "RDF file not found at: " + path.toAbsolutePath()
            );
        }

        try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
            model.read(inputStream, BASE_URI, "RDF/XML");
        } catch (Exception e) {
            throw new RuntimeException("Could not read RDF file: " + e.getMessage());
        }

        model.setNsPrefix("ex", BASE_URI);
        return model;
    }

    private void saveModel(Model model) {
        try (FileOutputStream outputStream = new FileOutputStream(Path.of(RDF_FILE).toFile())) {
            model.write(outputStream, "RDF/XML-ABBREV");
        } catch (Exception e) {
            throw new RuntimeException("Could not save RDF file: " + e.getMessage());
        }
    }

    private String createBookId(String title) {
        return title.replaceAll("[^a-zA-Z0-9]", "");
    }
}