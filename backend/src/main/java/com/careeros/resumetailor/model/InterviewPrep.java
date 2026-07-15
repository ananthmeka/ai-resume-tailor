package com.careeros.resumetailor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewPrep(List<InterviewQuestion> questions) {
    public InterviewPrep {
        if (questions == null) {
            questions = new ArrayList<>();
        }
    }
}
