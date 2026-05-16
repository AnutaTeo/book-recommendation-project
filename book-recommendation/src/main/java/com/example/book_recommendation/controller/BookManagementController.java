package com.example.book_recommendation.controller;

import com.example.book_recommendation.service.BookRdfService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class BookManagementController {

    private final BookRdfService bookRdfService;

    public BookManagementController(BookRdfService bookRdfService) {
        this.bookRdfService = bookRdfService;
    }

    @GetMapping("/books/add")
    public String addBookPage() {
        return "add-book";
    }

    @PostMapping("/books/add")
    public String addBook(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String theme1,
            @RequestParam(required = false) String theme2,
            @RequestParam String readingLevel
    ) {
        bookRdfService.addBook(title, author, theme1, theme2, readingLevel);
        return "redirect:/books/add?success";
    }

    @GetMapping("/books/edit-hunger-games")
    public String editHungerGamesPage() {
        return "edit-hunger-games";
    }

    @PostMapping("/books/edit-hunger-games")
    public String editHungerGames(
            @RequestParam String readingLevel
    ) {
        bookRdfService.updateBookReadingLevel("HungerGames", readingLevel);
        return "redirect:/books/edit-hunger-games?success";
    }
}