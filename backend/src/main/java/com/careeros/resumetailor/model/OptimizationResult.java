package com.careeros.resumetailor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OptimizationResult(
        StructuredResume optimizedResume,
        String targetRoleSummary,
        List<ResumeChange> changes,
        List<String> missingKeywords,
        List<String> recommendations,
        MatchScores matchScores
) {
    public OptimizationResult {
        if (changes == null) {
            changes = new ArrayList<>();
        }
        if (missingKeywords == null) {
            missingKeywords = new ArrayList<>();
        }
        if (recommendations == null) {
            recommendations = new ArrayList<>();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResumeChange(String area, String change, String reason, String confidence) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchScores(
            Integer overall,
            Integer skills,
            Integer experience,
            Integer leadership,
            Integer domain
    ) {}
}
