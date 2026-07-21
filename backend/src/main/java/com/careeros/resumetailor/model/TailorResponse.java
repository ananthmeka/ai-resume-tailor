package com.careeros.resumetailor.model;

public record TailorResponse(
        String resultId,
        String htmlResume,
        OptimizationResult analysis,
        String originalTextPreview,
        InterviewPrep interviewPrep
) {}
