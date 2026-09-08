package com.skillsphere.common.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KeywordExtractionService {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "is", "a", "an", "and", "or", "of", "to", "for", "with", "this", "that", "these", "those",
            "in", "on", "at", "by", "from", "up", "about", "into", "over", "after", "your", "my", "his", "her",
            "their", "our", "it", "its", "they", "them", "we", "us", "you", "he", "she", "who", "whom", "which",
            "what", "why", "how", "where", "when", "then", "there", "here", "each", "every", "all", "any",
            "both", "few", "more", "most", "some", "such", "no", "nor", "not", "only", "own", "same", "so",
            "than", "too", "very", "can", "will", "shall", "should", "would", "could", "may", "might", "must",
            "do", "does", "did", "done", "doing", "have", "has", "had", "having", "be", "is", "am", "are", "was",
            "were", "been", "being", "get", "gets", "got", "getting", "go", "goes", "went", "gone", "going",
            "develop", "build", "create", "implement", "write", "code", "program", "application", "project",
            "assignment", "task", "exercise", "question", "answer", "student", "trainer", "course", "batch",
            "class", "object", "method", "function", "variable", "system", "using", "use", "used", "requirements",
            "description", "title", "name", "due", "date", "marks", "maximum", "total", "score", "grade", "feedback"
    ));

    private static final Set<String> TECH_VOCABULARY = new HashSet<>(Arrays.asList(
            "java", "spring", "boot", "rest", "api", "jpa", "hibernate", "mysql", "sql", "database",
            "controller", "service", "repository", "dto", "html", "css", "javascript", "js", "ts",
            "typescript", "react", "angular", "vue", "python", "django", "flask", "aws", "docker",
            "kubernetes", "git", "github", "maven", "gradle", "junit", "mockito", "collections",
            "list", "map", "set", "queue", "hashmap", "arraylist", "hashset", "thread", "concurrency",
            "socket", "network", "oop", "polymorphism", "inheritance", "encapsulation", "abstraction",
            "exception", "json", "xml", "http", "microservices", "security", "oauth", "jwt", "mongodb",
            "postgresql", "oracle", "jdbc", "servlet", "jsp", "thymeleaf", "bootstrap", "jquery",
            "node", "express", "nosql", "redis", "kafka", "rabbitmq", "maven", "jenkins", "cicd",
            "agile", "scrum", "restful"
    ));

    public List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> extracted = new LinkedHashSet<>();
        
        // Let's first search for exact matching tech vocabulary (case-insensitive)
        String normalizedText = text.toLowerCase();
        for (String techWord : TECH_VOCABULARY) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(techWord) + "\\b");
            if (p.matcher(normalizedText).find()) {
                extracted.add(capitalizeWord(techWord));
            }
        }

        // Also extract capitalized words that are not stop words (often frameworks, technologies, class names)
        Pattern capitalizedPattern = Pattern.compile("\\b[A-Z][a-zA-Z0-9_]*\\b");
        Matcher matcher = capitalizedPattern.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            String lower = word.toLowerCase();
            if (word.length() > 2 && !STOP_WORDS.contains(lower) && !TECH_VOCABULARY.contains(lower)) {
                extracted.add(word);
            }
        }

        // Let's also look for common two-word technical phrases (e.g. Spring Boot, REST API)
        List<String> phrases = Arrays.asList("Spring Boot", "REST API", "Web Application", "Web Service", "SQL Database", "Object Oriented");
        for (String phrase : phrases) {
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(phrase) + "\\b");
            if (p.matcher(text).find()) {
                extracted.add(phrase);
            }
        }

        return new ArrayList<>(extracted);
    }

    private String capitalizeWord(String word) {
        if (word == null || word.isEmpty()) return word;
        if ("api".equals(word)) return "API";
        if ("jwt".equals(word)) return "JWT";
        if ("sql".equals(word)) return "SQL";
        if ("nosql".equals(word)) return "NoSQL";
        if ("html".equals(word)) return "HTML";
        if ("css".equals(word)) return "CSS";
        if ("oop".equals(word)) return "OOP";
        if ("xml".equals(word)) return "XML";
        if ("json".equals(word)) return "JSON";
        if ("http".equals(word)) return "HTTP";
        if ("dto".equals(word)) return "DTO";
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
