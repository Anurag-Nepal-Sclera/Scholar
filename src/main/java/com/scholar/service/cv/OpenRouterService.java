package com.scholar.service.cv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with OpenRouter AI API.
 * Handles keyword extraction and email generation.
 */
@Service
@Slf4j
public class OpenRouterService {

    @Value("${scholar.ai.openrouter.api-key}")
    private String apiKey;

    @Value("${scholar.ai.openrouter.model}")
    private String model;

    @Value("${scholar.ai.openrouter.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Extracts a comprehensive list of technical research keywords from CV text using AI.
     */
    public List<String> extractKeywords(String text) {
        log.info("Extracting comprehensive technical keywords using AI for text length: {}", text.length());
        
        String prompt = "You are an expert academic research profiler. Analyze the following CV text and extract a comprehensive list of exactly 200 technical keywords and keyphrases. " +
                "Focus strictly on: \n" +
                "1. Research Areas & Sub-domains (e.g., Computer Vision, Quantum Mechanics)\n" +
                "2. Specific Algorithms & Models (e.g., Transformer, ResNet-50, K-means)\n" +
                "3. Technical Skills & Tools (e.g., PyTorch, LaTeX, CRISPR)\n" +
                "4. Methodologies & Techniques (e.g., Reinforcement Learning, Spectrophotometry)\n" +
                "5. Application Domains (e.g., Healthcare, Autonomous Driving)\n\n" +
                "RULES:\n" +
                "- IGNORE generic words: study, method, results, approach, paper, data, analysis, experience, project, education.\n" +
                "- Ensure phrases are preserved (e.g., 'Natural Language Processing' not just 'Processing').\n" +
                "- Rank them by technical significance.\n" +
                "- Return ONLY a comma-separated list of keywords. No numbering, no preamble, no markdown formatting.\n\n" +
                "CV Text:\n" + text;

        try {
            String response = callAi(prompt);
            if (response == null) return List.of();
            
            // Clean up common AI artifacts (like "Here are the keywords:" or markdown)
            String cleanResponse = response.replaceAll("(?i)here are.*:", "")
                                         .replaceAll("```", "")
                                         .replaceAll("\n", ",");

            String[] parts = cleanResponse.split(",");
            List<String> keywords = new ArrayList<>();
            for (String part : parts) {
                String clean = part.trim().toLowerCase();
                // Basic validation: length and pattern
                if (clean.length() >= 2 && !clean.matches("\\d+") && !isGeneric(clean)) {
                    keywords.add(clean);
                }
            }
            log.info("AI extracted {} technical keywords", keywords.size());
            return keywords;
        } catch (Exception e) {
            log.error("AI technical keyword extraction failed", e);
            return List.of();
        }
    }

    private boolean isGeneric(String word) {
        return List.of("study", "method", "results", "approach", "paper", "data", "analysis", "experience", "project", "education", "work", "using", "used").contains(word);
    }

    /**
     * Generates a personalized research outreach email using AI.
     */
    public String generateOutreachEmail(String studentKeywords, String professorName, String university, String matchedKeywords) {
        log.info("Generating personalized outreach email for professor: {}", professorName);
        
        String prompt = "You are an assistant for a prospective PhD student. " +
                "Generate a professional, concise, and highly personalized outreach email to Prof. " + professorName + " at " + university + ". " +
                "The student's research interests include: " + studentKeywords + ". " +
                "The specific research alignment found with this professor is in: " + matchedKeywords + ". " +
                "Mention the research alignment naturally. Keep the tone academic, respectful, and eager. " +
                "The email should be around 150-200 words. Return only the email body.";

        try {
            return callAi(prompt);
        } catch (Exception e) {
            log.error("AI email generation failed", e);
            return null;
        }
    }

    private String callAi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        OpenRouterRequest request = OpenRouterRequest.builder()
                .model(model)
                .messages(List.of(new Message("user", prompt)))
                .build();

        HttpEntity<OpenRouterRequest> entity = new HttpEntity<>(request, headers);

        try {
            OpenRouterResponse response = restTemplate.postForObject(apiUrl, entity, OpenRouterResponse.class);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            log.error("OpenRouter API call failed", e);
        }
        return null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class OpenRouterRequest {
        private String model;
        private List<Message> messages;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Message {
        private String role;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class OpenRouterResponse {
        private List<Choice> choices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Choice {
        private Message message;
    }
}
