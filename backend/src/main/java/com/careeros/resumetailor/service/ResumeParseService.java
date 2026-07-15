package com.careeros.resumetailor.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeParseService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Resume file exceeds 10MB limit");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".pdf") && !name.endsWith(".docx") && !name.endsWith(".doc") && !name.endsWith(".txt")) {
            throw new IllegalArgumentException("Supported formats: PDF, DOCX, DOC, TXT");
        }
        try (InputStream in = file.getInputStream()) {
            String text = tika.parseToString(in);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Could not extract text from resume. Try a text-based PDF or DOCX.");
            }
            return text.trim();
        } catch (TikaException e) {
            throw new IllegalArgumentException("Failed to parse resume: " + e.getMessage(), e);
        }
    }
}
