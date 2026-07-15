package com.careeros.resumetailor.service;

import com.careeros.resumetailor.model.OptimizationResult;
import com.careeros.resumetailor.model.StructuredResume;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ResumeTailorPipeline {

    private static final String EXTRACT_SYSTEM = """
            You extract resume content into strict JSON. Output ONLY valid JSON matching this schema:
            {
              "contact": { "fullName", "email", "phone", "location", "linkedIn", "website" },
              "professionalSummary": string,
              "skillGroups": [ { "category": string, "skills": [string] } ],
              "experience": [ { "company", "title", "location", "startDate", "endDate", "bullets": [string] } ],
              "projects": [ { "name", "timeframe", "technologies": [string], "bullets": [string] } ],
              "education": [ { "institution", "degree", "field", "graduationDate", "details" } ],
              "certifications": [ { "name", "issuer", "date" } ],
              "otherSections": [string]
            }
            Rules: Copy facts exactly from the source. Do not invent employers, dates, skills, or degrees.
            If a field is missing in the source, use null or empty arrays.
            """;

    private static final String OPTIMIZE_SYSTEM = """
            You optimize a structured resume for a specific job while keeping the base resume as the ONLY source of truth.
            Output ONLY valid JSON:
            {
              "optimizedResume": { same schema as input structured resume },
              "targetRoleSummary": string,
              "changes": [ { "area", "change", "reason", "confidence": "high|medium|low" } ],
              "missingKeywords": [string],
              "recommendations": [string],
              "matchScores": { "overall", "skills", "experience", "leadership", "domain" }
            }
            Mandatory rules:
            1. NEVER add skills, employers, projects, certifications, degrees, or metrics not supported by the base resume.
            2. You MAY reorder sections, rewrite bullets for clarity, summarize older roles, and emphasize JD-aligned achievements.
            3. Quantify achievements ONLY if numbers appear in the base resume or are clearly implied (do not invent percentages).
            4. Maintain a coherent theme aligned with the job (e.g. cloud, leadership) using truthful content only.
            5. ATS-friendly: plain text structure, no tables/icons; use strong action verbs.
            6. Resume length hint: ONE_PAGE = ~1 page equivalent (summarize aggressively), TWO_PAGES = standard, EXECUTIVE = lead with executive summary + selective detail.
            7. missingKeywords: JD terms the candidate lacks (do NOT suggest falsely adding them as experience).
            8. matchScores: integers 0-100, estimates only.
            """;

    private final OpenAiChatService openAi;
    private final ObjectMapper objectMapper;

    public ResumeTailorPipeline(OpenAiChatService openAi, ObjectMapper objectMapper) {
        this.openAi = openAi;
        this.objectMapper = objectMapper;
    }

    public StructuredResume extractStructured(String resumeText) {
        try {
            String json = openAi.chatJson(EXTRACT_SYSTEM, "Resume text:\n\n" + truncate(resumeText, 28000));
            return objectMapper.readValue(json, StructuredResume.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse resume structure: " + e.getMessage(), e);
        }
    }

    public OptimizationResult optimize(StructuredResume base, String jobDescription, String lengthHint) {
        try {
            String baseJson = objectMapper.writeValueAsString(base);
            String user = """
                    Target resume length: %s
                    
                    Job description:
                    %s
                    
                    Base structured resume (source of truth):
                    %s
                    """.formatted(lengthHint, truncate(jobDescription, 12000), truncate(baseJson, 28000));
            String json = openAi.chatJson(OPTIMIZE_SYSTEM, user);
            return objectMapper.readValue(json, OptimizationResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to optimize resume: " + e.getMessage(), e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "\n...[truncated]";
    }
}
