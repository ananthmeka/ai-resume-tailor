package com.careeros.resumetailor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewQuestion(
        String category,
        String question,
        String rationale,
        String groundedIn
) {}
