# Semantic Web Book Recommendation System

## Team Members
- Teodorescu Ana-Maria
- Surugiu Ioana-Monica

## Project Description

The Semantic Web Book Recommendation System is a Spring Boot web application that combines Semantic Web technologies with AI-enhanced Retrieval-Augmented Generation (RAG).

The application manages books represented in RDF/XML format and allows semantic querying, ontology visualization and intelligent chatbot interaction.

Users can:
- Upload and visualize RDF/XML data
- Add and modify books using RDF
- Execute SPARQL queries
- Explore an OWL ontology
- Receive AI-enhanced book recommendations through a chatbot

The chatbot uses a vector database generated from RDF/XML book data together with Gemini LLM integration to generate responses based on retrieved semantic context instead of relying only on model knowledge.

---

## Contributions

| Member | Contribution |
|---|---|
| Teodorescu Ana-Maria | Created and managed RDF/XML book dataset, implemented RDF parsing and graph visualization using Apache Jena, developed book management functionality, created the OWL ontology in Protégé, visualized ontology in GraphDB, implemented SPARQL queries and integrated semantic web technologies into the application. |
| Surugiu Ioana-Monica | Implemented the floating chatbot interface, context-aware conversation starters, vector database generation, embeddings integration, Retrieval-Augmented Generation (RAG) pipeline, Gemini LLM integration, chatbot recommendation logic, user preference support (Alice/Bob), semantic filtering by author/theme/reading level and chatbot UI functionality. |


## Public Repository
https://github.com/AnutaTeo/book-recommendation-project


## Technologies

### Backend
- Java
- Spring Boot
- Apache Jena

### Semantic Web
- RDF/XML
- OWL
- SPARQL
- Protégé
- GraphDB

### Frontend
- HTML
- CSS
- JavaScript
- Thymeleaf

### AI / RAG
- Vector Embeddings
- Vector Database
- Retrieval-Augmented Generation (RAG)
- Google Gemini API
