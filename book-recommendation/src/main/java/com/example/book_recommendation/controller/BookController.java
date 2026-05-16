package com.example.book_recommendation.controller;

import com.example.book_recommendation.model.Book;
import com.example.book_recommendation.service.BookRdfService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class BookController {

    private final BookRdfService bookRdfService;

    public BookController(BookRdfService bookRdfService) {
        this.bookRdfService = bookRdfService;
    }

    @GetMapping("/books")
    public String listBooks(Model model) {
        List<Book> books = bookRdfService.getAllBooks();
        model.addAttribute("books", books);
        return "books";
    }

    @GetMapping("/books/{bookId}")
    public String bookDetails(@PathVariable String bookId, Model model) {
        Book book = bookRdfService.getBookById(bookId);

        if (book == null) {
            return "redirect:/books";
        }

        model.addAttribute("book", book);
        return "book-details";
    }
}