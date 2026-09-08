package com.skillsphere.common.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeywordEvaluationService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordEvaluationResult {
        private int totalKeywords;
        private int matchedKeywords;
        private int missingKeywords;
        private double percentage;
        private int obtainedMarks;
        private String matchedList;
        private String missingList;
    }

    public KeywordEvaluationResult evaluate(String studentText, String requiredKeywords, int maximumMarks) {
        if (requiredKeywords == null || requiredKeywords.trim().isEmpty() || studentText == null || studentText.trim().isEmpty()) {
            return KeywordEvaluationResult.builder()
                    .totalKeywords(0)
                    .matchedKeywords(0)
                    .missingKeywords(0)
                    .percentage(0.0)
                    .obtainedMarks(0)
                    .matchedList("")
                    .missingList(requiredKeywords != null ? requiredKeywords : "")
                    .build();
        }

        String[] keywords = requiredKeywords.split(",");
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        // Normalizing student text (case insensitive, tolerant of punctuation, whitespace normalized)
        String normalizedDoc = studentText.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ");

        for (String kw : keywords) {
            String trimmedKw = kw.trim();
            if (trimmedKw.isEmpty()) continue;
            
            // Normalize keyword for matching
            String normalizedKw = trimmedKw.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
            if (normalizedKw.isEmpty()) continue;

            if (normalizedDoc.contains(normalizedKw)) {
                matched.add(trimmedKw);
            } else {
                missing.add(trimmedKw);
            }
        }

        int totalKw = matched.size() + missing.size();
        double pct = totalKw > 0 ? ((double) matched.size() / totalKw) * 100.0 : 0.0;
        int obtainedMarks = (int) Math.round((pct / 100.0) * maximumMarks);

        return KeywordEvaluationResult.builder()
                .totalKeywords(totalKw)
                .matchedKeywords(matched.size())
                .missingKeywords(missing.size())
                .percentage(Math.round(pct * 100.0) / 100.0)
                .obtainedMarks(obtainedMarks)
                .matchedList(String.join(", ", matched))
                .missingList(String.join(", ", missing))
                .build();
    }
}
