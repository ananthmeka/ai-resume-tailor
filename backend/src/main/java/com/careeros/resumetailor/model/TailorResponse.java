package com.careeros.resumetailor.model;

public record TailorResponse(
        String htmlResume,
        OptimizationResult analysis,
        String originalTextPreview
) {}
