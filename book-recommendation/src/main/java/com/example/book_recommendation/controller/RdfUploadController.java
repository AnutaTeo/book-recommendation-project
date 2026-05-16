package com.example.book_recommendation.controller;
import com.example.book_recommendation.service.RdfGraphService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
public class RdfUploadController {

    private final RdfGraphService rdfGraphService;

    public RdfUploadController(RdfGraphService rdfGraphService) {
        this.rdfGraphService = rdfGraphService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload-rdf")
    public String uploadRdfFile(
            @RequestParam("file") MultipartFile file,
            Model model
    ) {
        Map<String, List<Map<String, String>>> graphData =
                rdfGraphService.parseRdfFile(file);

        model.addAttribute("nodes", graphData.get("nodes"));
        model.addAttribute("edges", graphData.get("edges"));

        return "graph";
    }
}