package com.careeros.resumetailor.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TailorRequest(
        @NotBlank String jobDescription,
        @Pattern(regexp = "ONE_PAGE|TWO_PAGES|EXECUTIVE", message = "length must be ONE_PAGE, TWO_PAGES, or EXECUTIVE")
        String resumeLength
) {
    public String resumeLengthOrDefault() {
        return resumeLength == null || resumeLength.isBlank() ? "TWO_PAGES" : resumeLength;
    }
}
