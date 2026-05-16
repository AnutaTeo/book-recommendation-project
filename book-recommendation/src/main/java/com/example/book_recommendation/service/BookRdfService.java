package com.example.book_recommendation.service;
import com.example.book_recommendation.model.Book;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import org.apache.jena.query.*;
import java.util.ArrayList;
import java.util.List;

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


    public List<Book> getAllBooks() {
        Model model = loadModel();

        String queryString = """
            PREFIX ex: <http://example.org/book-recommendation#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

            SELECT ?book ?title ?author ?readingLevel
            WHERE {
                ?book rdf:type ex:Book .
                OPTIONAL { ?book ex:title ?title . }
                OPTIONAL { ?book ex:author ?author . }
                OPTIONAL { ?book ex:suitableForReadingLevel ?readingLevel . }
            }
            """;

        List<Book> books = new ArrayList<>();

        try (QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model)) {
            ResultSet results = queryExecution.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                String bookUri = solution.getResource("book").getURI();
                String id = extractId(bookUri);

                String title = solution.contains("title")
                        ? solution.getLiteral("title").getString()
                        : id;

                String author = solution.contains("author")
                        ? solution.getLiteral("author").getString()
                        : "Unknown";

                String readingLevel = solution.contains("readingLevel")
                        ? solution.getLiteral("readingLevel").getString()
                        : "Not specified";

                Book book = new Book();
                book.setId(id);
                book.setTitle(title);
                book.setAuthor(author);
                book.setReadingLevel(readingLevel);
                book.setThemes(getThemesForBook(model, bookUri));

                books.add(book);
            }
        }

        return books;
    }

    public Book getBookById(String bookId) {
        Model model = loadModel();

        String bookUri = BASE_URI + bookId;

        String queryString = """
            PREFIX ex: <http://example.org/book-recommendation#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

            SELECT ?title ?author ?readingLevel
            WHERE {
                <%s> rdf:type ex:Book .
                OPTIONAL { <%s> ex:title ?title . }
                OPTIONAL { <%s> ex:author ?author . }
                OPTIONAL { <%s> ex:suitableForReadingLevel ?readingLevel . }
            }
            """.formatted(bookUri, bookUri, bookUri, bookUri);

        try (QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model)) {
            ResultSet results = queryExecution.execSelect();

            if (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                Book book = new Book();
                book.setId(bookId);

                book.setTitle(solution.contains("title")
                        ? solution.getLiteral("title").getString()
                        : bookId);

                book.setAuthor(solution.contains("author")
                        ? solution.getLiteral("author").getString()
                        : "Unknown");

                book.setReadingLevel(solution.contains("readingLevel")
                        ? solution.getLiteral("readingLevel").getString()
                        : "Not specified");

                book.setThemes(getThemesForBook(model, bookUri));

                return book;
            }
        }

        return null;
    }

    private List<String> getThemesForBook(Model model, String bookUri) {
        String queryString = """
            PREFIX ex: <http://example.org/book-recommendation#>

            SELECT ?theme
            WHERE {
                <%s> ex:hasTheme ?theme .
            }
            """.formatted(bookUri);

        List<String> themes = new ArrayList<>();

        try (QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model)) {
            ResultSet results = queryExecution.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                themes.add(solution.get("theme").toString());
            }
        }

        return themes;
    }

    private String extractId(String uri) {
        if (uri.contains("#")) {
            return uri.substring(uri.lastIndexOf("#") + 1);
        }

        return uri.substring(uri.lastIndexOf("/") + 1);
    }





}