package com.careeros.resumetailor.service;

/** Keeps Groq/OpenAI requests under free-tier TPM (input + estimated output). */
public final class LlmInputTruncator {

    private LlmInputTruncator() {}

    public static String truncatePlain(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "\n...[truncated for model limits]";
    }

    /** Preserves start (contact/summary) and end (recent roles) for long resumes. */
    public static String truncateResumeText(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        int markerLen = 80;
        int body = max - markerLen;
        int head = (int) (body * 0.72);
        int tail = body - head;
        return s.substring(0, head)
                + "\n\n...[middle omitted — extract ALL employers from text above AND below]...\n\n"
                + s.substring(s.length() - tail);
    }

    public static boolean isTokenLimitError(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null
                    && (msg.contains("rate_limit_exceeded")
                            || msg.contains("Request too large")
                            || msg.contains("tokens per minute"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /** Groq messages include e.g. "try again in 4.545s". */
    public static long retryDelayMillis(String message, long defaultMs) {
        if (message == null) {
            return defaultMs;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("try again in ([0-9.]+)s").matcher(message);
        if (m.find()) {
            double seconds = Double.parseDouble(m.group(1));
            return (long) (seconds * 1000) + 750;
        }
        if (message.contains("tokens per minute")) {
            return Math.max(defaultMs, 15_000);
        }
        return defaultMs;
    }
}
