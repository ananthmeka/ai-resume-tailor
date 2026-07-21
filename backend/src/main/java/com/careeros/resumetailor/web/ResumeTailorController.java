package com.careeros.resumetailor.web;

import com.careeros.resumetailor.config.LlmLimits;
import com.careeros.resumetailor.model.InterviewPrep;
import com.careeros.resumetailor.model.TailorRequest;
import com.careeros.resumetailor.model.TailorResponse;
import com.careeros.resumetailor.service.PdfExportService;
import com.careeros.resumetailor.service.GeneratedResultStore;
import com.careeros.resumetailor.service.ResumeHtmlRenderer;
import com.careeros.resumetailor.service.ResumeParseService;
import com.careeros.resumetailor.service.ResumeTailorPipeline;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResumeTailorController {

    private final ResumeParseService parseService;
    private final ResumeTailorPipeline pipeline;
    private final ResumeHtmlRenderer htmlRenderer;
    private final PdfExportService pdfExportService;
    private final GeneratedResultStore generatedResultStore;
    private final boolean interviewQuestionsEnabled;
    private final LlmLimits llmLimits;

    public ResumeTailorController(
            ResumeParseService parseService,
            ResumeTailorPipeline pipeline,
            ResumeHtmlRenderer htmlRenderer,
            PdfExportService pdfExportService,
            GeneratedResultStore generatedResultStore,
            LlmLimits llmLimits,
            @Value("${app.features.interview-questions:true}") boolean interviewQuestionsEnabled) {
        this.parseService = parseService;
        this.pipeline = pipeline;
        this.htmlRenderer = htmlRenderer;
        this.pdfExportService = pdfExportService;
        this.generatedResultStore = generatedResultStore;
        this.llmLimits = llmLimits;
        this.interviewQuestionsEnabled = interviewQuestionsEnabled;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "ai-resume-tailor");
    }

    @PostMapping(value = "/tailor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TailorResponse tailor(
            @RequestPart("resume") MultipartFile resume,
            @RequestPart("jobDescription") String jobDescription,
            @RequestPart(value = "resumeLength", required = false) String resumeLength,
            HttpServletRequest httpRequest) throws java.io.IOException {
        TailorRequest req = new TailorRequest(jobDescription, resumeLength);
        String text = parseService.extractText(resume);
        var structured = pipeline.extractStructured(text);
        pauseBetweenLlmCalls();
        var result = pipeline.optimize(structured, req.jobDescription(), req.resumeLengthOrDefault());
        InterviewPrep interviewPrep = interviewQuestionsEnabled
                ? pipeline.generateInterviewQuestions(result.optimizedResume(), req.jobDescription())
                : new InterviewPrep(java.util.List.of());
        String html = htmlRenderer.render(result.optimizedResume());
        String resultId = generatedResultStore.put(authenticatedUser(httpRequest), html);
        String preview = text.length() > 8000 ? text.substring(0, 8000) + "…" : text;
        return new TailorResponse(resultId, html, result, preview, interviewPrep);
    }

    @GetMapping("/results/{resultId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable String resultId,
            HttpServletRequest httpRequest) {
        var html = generatedResultStore.getHtml(resultId, authenticatedUser(httpRequest));
        if (html.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = pdfExportService.htmlToPdf(html.get());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tailored-resume.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String authenticatedUser(HttpServletRequest request) {
        Object user = request.getAttribute("authenticatedUser");
        return user == null ? "local-user" : user.toString();
    }

    private void pauseBetweenLlmCalls() {
        int ms = llmLimits.pauseMsBetweenLlmCalls();
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
