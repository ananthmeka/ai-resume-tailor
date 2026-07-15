package com.careeros.resumetailor.service;

import com.careeros.resumetailor.model.StructuredResume;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.stream.Collectors;

@Service
public class ResumeHtmlRenderer {

    public String render(StructuredResume resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <title>Resume</title>
                  <style>
                    body { font-family: Georgia, 'Times New Roman', serif; font-size: 11pt; color: #111; max-width: 8.5in; margin: 0 auto; padding: 0.5in; line-height: 1.35; }
                    h1 { font-size: 20pt; margin: 0 0 4px 0; font-family: Arial, Helvetica, sans-serif; }
                    .contact { font-size: 10pt; margin-bottom: 12px; color: #333; }
                    h2 { font-size: 12pt; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid #333; margin: 14px 0 6px 0; font-family: Arial, Helvetica, sans-serif; }
                    .entry-title { font-weight: bold; }
                    .entry-meta { font-style: italic; color: #444; margin-bottom: 4px; }
                    ul { margin: 4px 0 8px 0; padding-left: 18px; }
                    li { margin-bottom: 3px; }
                    .skills-line { margin-bottom: 4px; }
                  </style>
                </head>
                <body>
                """);

        if (resume.contact() != null) {
            StructuredResume.Contact c = resume.contact();
            if (c.fullName() != null && !c.fullName().isBlank()) {
                sb.append("<h1>").append(esc(c.fullName())).append("</h1>");
            }
            sb.append("<div class=\"contact\">");
            sb.append(joinContact(c));
            sb.append("</div>");
        }

        if (resume.professionalSummary() != null && !resume.professionalSummary().isBlank()) {
            sb.append("<h2>Professional Summary</h2>");
            sb.append("<p>").append(esc(resume.professionalSummary())).append("</p>");
        }

        if (!resume.skillGroups().isEmpty()) {
            sb.append("<h2>Skills</h2>");
            for (StructuredResume.SkillGroup g : resume.skillGroups()) {
                if (g.skills() == null || g.skills().isEmpty()) {
                    continue;
                }
                String skills = g.skills().stream().map(ResumeHtmlRenderer::esc).collect(Collectors.joining(", "));
                if (g.category() != null && !g.category().isBlank()) {
                    sb.append("<div class=\"skills-line\"><strong>").append(esc(g.category())).append(":</strong> ")
                            .append(skills).append("</div>");
                } else {
                    sb.append("<div class=\"skills-line\">").append(skills).append("</div>");
                }
            }
        }

        if (!resume.experience().isEmpty()) {
            sb.append("<h2>Experience</h2>");
            for (StructuredResume.ExperienceEntry e : resume.experience()) {
                sb.append("<div class=\"entry-title\">").append(esc(nullToEmpty(e.title())));
                if (e.company() != null && !e.company().isBlank()) {
                    sb.append(" — ").append(esc(e.company()));
                }
                sb.append("</div>");
                sb.append("<div class=\"entry-meta\">");
                sb.append(esc(joinDates(e.startDate(), e.endDate())));
                if (e.location() != null && !e.location().isBlank()) {
                    sb.append(" | ").append(esc(e.location()));
                }
                sb.append("</div>");
                sb.append(bullets(e.bullets()));
            }
        }

        if (!resume.projects().isEmpty()) {
            sb.append("<h2>Projects</h2>");
            for (StructuredResume.ProjectEntry p : resume.projects()) {
                sb.append("<div class=\"entry-title\">").append(esc(nullToEmpty(p.name()))).append("</div>");
                if (p.timeframe() != null && !p.timeframe().isBlank()) {
                    sb.append("<div class=\"entry-meta\">").append(esc(p.timeframe())).append("</div>");
                }
                if (p.technologies() != null && !p.technologies().isEmpty()) {
                    sb.append("<div class=\"entry-meta\">")
                            .append(p.technologies().stream().map(ResumeHtmlRenderer::esc).collect(Collectors.joining(", ")))
                            .append("</div>");
                }
                sb.append(bullets(p.bullets()));
            }
        }

        if (!resume.education().isEmpty()) {
            sb.append("<h2>Education</h2>");
            for (StructuredResume.EducationEntry ed : resume.education()) {
                sb.append("<div class=\"entry-title\">").append(esc(nullToEmpty(ed.degree())));
                if (ed.field() != null && !ed.field().isBlank()) {
                    sb.append(" in ").append(esc(ed.field()));
                }
                sb.append("</div>");
                sb.append("<div class=\"entry-meta\">").append(esc(nullToEmpty(ed.institution())));
                if (ed.graduationDate() != null && !ed.graduationDate().isBlank()) {
                    sb.append(" — ").append(esc(ed.graduationDate()));
                }
                sb.append("</div>");
                if (ed.details() != null && !ed.details().isBlank()) {
                    sb.append("<p>").append(esc(ed.details())).append("</p>");
                }
            }
        }

        if (!resume.certifications().isEmpty()) {
            sb.append("<h2>Certifications</h2><ul>");
            for (StructuredResume.CertificationEntry cert : resume.certifications()) {
                sb.append("<li>").append(esc(nullToEmpty(cert.name())));
                if (cert.issuer() != null && !cert.issuer().isBlank()) {
                    sb.append(" (").append(esc(cert.issuer())).append(")");
                }
                if (cert.date() != null && !cert.date().isBlank()) {
                    sb.append(" — ").append(esc(cert.date()));
                }
                sb.append("</li>");
            }
            sb.append("</ul>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String bullets(java.util.List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder("<ul>");
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                b.append("<li>").append(esc(item)).append("</li>");
            }
        }
        b.append("</ul>");
        return b.toString();
    }

    private static String joinContact(StructuredResume.Contact c) {
        StringBuilder b = new StringBuilder();
        appendPart(b, c.email());
        appendPart(b, c.phone());
        appendPart(b, c.location());
        appendPart(b, c.linkedIn());
        appendPart(b, c.website());
        return b.toString();
    }

    private static void appendPart(StringBuilder b, String part) {
        if (part != null && !part.isBlank()) {
            if (!b.isEmpty()) {
                b.append(" | ");
            }
            b.append(esc(part));
        }
    }

    private static String joinDates(String start, String end) {
        if (start == null && end == null) {
            return "";
        }
        if (start == null) {
            return nullToEmpty(end);
        }
        if (end == null || end.isBlank()) {
            return start + " – Present";
        }
        return start + " – " + end;
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(nullToEmpty(s));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
