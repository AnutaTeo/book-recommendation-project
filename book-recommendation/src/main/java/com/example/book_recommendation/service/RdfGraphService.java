package com.example.book_recommendation.service;
import org.apache.jena.rdf.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
public class RdfGraphService {

    public Map<String, List<Map<String, String>>> parseRdfFile(MultipartFile file) {
        List<Map<String, String>> nodes = new ArrayList<>();
        List<Map<String, String>> edges = new ArrayList<>();

        Map<String, Integer> nodeIds = new HashMap<>();
        int[] counter = {1};

        try (InputStream inputStream = file.getInputStream()) {

            Model model = ModelFactory.createDefaultModel();
            model.read(inputStream, "http://example.org/book-recommendation#", "RDF/XML");

            StmtIterator iterator = model.listStatements();

            while (iterator.hasNext()) {
                Statement statement = iterator.nextStatement();

                String subject = simplify(statement.getSubject().toString());
                String predicate = simplify(statement.getPredicate().toString());
                String object = simplify(statement.getObject().toString());

                if (predicate.equals("description")) {
                    continue;
                }

                int subjectId = getNodeId(subject, nodeIds, nodes, counter);
                int objectId = getNodeId(object, nodeIds, nodes, counter);

                Map<String, String> edge = new HashMap<>();
                edge.put("from", String.valueOf(subjectId));
                edge.put("to", String.valueOf(objectId));
                edge.put("label", predicate);

                edges.add(edge);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error reading RDF file: " + e.getMessage());
        }

        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);

        return result;
    }

    private int getNodeId(
            String label,
            Map<String, Integer> nodeIds,
            List<Map<String, String>> nodes,
            int[] counter
    ) {
        if (!nodeIds.containsKey(label)) {
            int id = counter[0]++;
            nodeIds.put(label, id);

            Map<String, String> node = new HashMap<>();
            node.put("id", String.valueOf(id));
            node.put("label", label);

            nodes.add(node);
        }

        return nodeIds.get(label);
    }

    private String simplify(String value) {
        if (value.contains("#")) {
            return value.substring(value.lastIndexOf("#") + 1);
        }

        if (value.contains("/")) {
            return value.substring(value.lastIndexOf("/") + 1);
        }

        return value;
    }
}