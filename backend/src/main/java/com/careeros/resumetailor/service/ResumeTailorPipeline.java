package com.careeros.resumetailor.service;

import com.careeros.resumetailor.config.LlmLimits;
import com.careeros.resumetailor.model.InterviewPrep;
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
            Rules:
            - Copy facts exactly from the source. Do not invent employers, dates, skills, or degrees.
            - Include EVERY employment entry and EVERY project from the source (oldest to newest in experience array).
            - If a field is missing in the source, use null or empty arrays.
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
            2. optimizedResume MUST contain the same employers and date ranges as the base resume — same count of experience entries. You MAY rewrite bullets and reorder roles for JD fit; you MUST NOT drop roles.
            3. optimizedResume MUST include contact (from base), professionalSummary (rewritten for JD), skillGroups, projects, education, and certifications from base (rewritten or reordered as needed).
            4. Quantify achievements ONLY if numbers appear in the base resume (do not invent percentages).
            5. ATS-friendly: plain text structure; strong action verbs.
            6. Length hints: ONE_PAGE = shorter bullets/summary but still list every employer with at least one bullet; TWO_PAGES = standard detail; EXECUTIVE = strong executive summary plus selective depth on recent roles.
            7. missingKeywords: JD terms the candidate lacks (do not add as fake experience).
            8. matchScores: integers 0-100.
            9. Every item in "changes" MUST reflect something actually present in optimizedResume.
            """;

    private static final String INTERVIEW_SYSTEM = """
            You generate interview preparation questions for a candidate applying to a specific job.
            Output ONLY valid JSON:
            {
              "questions": [
                {
                  "category": "behavioral|technical|leadership|domain|project",
                  "question": string,
                  "rationale": string,
                  "groundedIn": string
                }
              ]
            }
            Rules:
            - Produce 10 to 14 questions mixing categories.
            - Ground every question in the job description AND specific facts from the candidate resume (employers, projects, technologies, leadership scope).
            - groundedIn cites the resume anchor (e.g. "Cisco — platform migration" or project name).
            - Do NOT assume skills or employers not on the resume.
            - Include 2-4 project-deep-dive questions when projects exist.
            """;

    private final OpenAiChatService openAi;
    private final ObjectMapper objectMapper;
    private final LlmLimits limits;

    public ResumeTailorPipeline(OpenAiChatService openAi, ObjectMapper objectMapper, LlmLimits limits) {
        this.openAi = openAi;
        this.objectMapper = objectMapper;
        this.limits = limits;
    }

    public StructuredResume extractStructured(String resumeText) {
        int[] sizes = {limits.maxResumeChars(), limits.maxResumeCharsFallback()};
        IllegalStateException last = null;
        for (int max : sizes) {
            try {
                String body = LlmInputTruncator.truncateResumeText(resumeText, max);
                int chars = body.length();
                String json = openAi.chatJson(EXTRACT_SYSTEM, "Resume text:\n\n" + body, chars);
                return objectMapper.readValue(json, StructuredResume.class);
            } catch (IllegalStateException e) {
                last = e;
                if (!LlmInputTruncator.isTokenLimitError(e)) {
                    throw wrapExtract(e);
                }
            } catch (Exception e) {
                throw wrapExtract(e);
            }
        }
        throw wrapExtract(last != null ? last : new IllegalStateException("extract failed"));
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
                    """.formatted(
                    lengthHint,
                    LlmInputTruncator.truncatePlain(jobDescription, limits.maxJdChars()),
                    LlmInputTruncator.truncatePlain(baseJson, limits.maxStructuredJsonChars()));
            String json = openAi.chatJson(OPTIMIZE_SYSTEM, user, user.length());
            OptimizationResult raw = objectMapper.readValue(json, OptimizationResult.class);
            StructuredResume merged = ResumeCompletenessMerger.merge(base, raw.optimizedResume());
            return new OptimizationResult(
                    merged,
                    raw.targetRoleSummary(),
                    raw.changes(),
                    raw.missingKeywords(),
                    raw.recommendations(),
                    raw.matchScores());
        } catch (Exception e) {
            if (LlmInputTruncator.isTokenLimitError(e)) {
                throw tokenLimitUserError(e);
            }
            throw new IllegalStateException("Failed to optimize resume: " + e.getMessage(), e);
        }
    }

    public InterviewPrep generateInterviewQuestions(StructuredResume tailored, String jobDescription) {
        try {
            String resumeJson = objectMapper.writeValueAsString(tailored);
            String user = """
                    Job description:
                    %s
                    
                    Candidate tailored resume (source of truth for their background):
                    %s
                    """.formatted(
                    LlmInputTruncator.truncatePlain(jobDescription, limits.maxJdChars()),
                    LlmInputTruncator.truncatePlain(resumeJson, limits.maxStructuredJsonChars()));
            String json = openAi.chatJson(INTERVIEW_SYSTEM, user);
            return objectMapper.readValue(json, InterviewPrep.class);
        } catch (Exception e) {
            if (LlmInputTruncator.isTokenLimitError(e)) {
                return new InterviewPrep(java.util.List.of());
            }
            throw new IllegalStateException("Failed to generate interview questions: " + e.getMessage(), e);
        }
    }

    private static IllegalStateException wrapExtract(Exception e) {
        if (LlmInputTruncator.isTokenLimitError(e)) {
            return tokenLimitUserError(e);
        }
        return new IllegalStateException("Failed to parse resume structure: " + e.getMessage(), e);
    }

    private static IllegalStateException tokenLimitUserError(Throwable e) {
        return new IllegalStateException(
                "Groq/OpenAI token limit exceeded (free tier ~12k tokens/min). "
                        + "Use a shorter resume/JD, wait 60 seconds and retry, upload DOCX/TXT, "
                        + "or set GENERATE_INTERVIEW_QUESTIONS=false and redeploy. "
                        + "Details: "
                        + e.getMessage(),
                e);
    }
}
