package com.careeros.resumetailor.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PdfExportServiceTest {

    @Test
    void rendersPdfWithoutPdfBoxVersionConflict() {
        String html = """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="UTF-8"/><title>Resume</title></head>
                <body><h1>Test Candidate</h1><p>Cloud and AI engineering</p></body></html>
                """;

        byte[] pdf = new PdfExportService().htmlToPdf(html);

        assertThat(pdf).hasSizeGreaterThan(100);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
