package com.careeros.resumetailor.service;

import com.careeros.resumetailor.model.StructuredResume;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fills gaps when the LLM returns a sparse optimizedResume (missing roles, contact, etc.).
 */
public final class ResumeCompletenessMerger {

    private ResumeCompletenessMerger() {}

    public static StructuredResume merge(StructuredResume base, StructuredResume optimized) {
        if (optimized == null) {
            return base;
        }
        StructuredResume.Contact contact = mergeContact(base.contact(), optimized.contact());
        String summary = blank(optimized.professionalSummary())
                ? base.professionalSummary()
                : optimized.professionalSummary();
        List<StructuredResume.SkillGroup> skills = optimized.skillGroups().isEmpty()
                ? base.skillGroups()
                : optimized.skillGroups();
        List<StructuredResume.ExperienceEntry> experience = mergeExperience(
                base.experience(), optimized.experience());
        List<StructuredResume.ProjectEntry> projects = mergeProjects(base.projects(), optimized.projects());
        List<StructuredResume.EducationEntry> education = optimized.education().isEmpty()
                ? base.education()
                : optimized.education();
        List<StructuredResume.CertificationEntry> certifications = optimized.certifications().isEmpty()
                ? base.certifications()
                : optimized.certifications();
        List<String> other = optimized.otherSections().isEmpty()
                ? base.otherSections()
                : optimized.otherSections();
        return new StructuredResume(
                contact, summary, skills, experience, projects, education, certifications, other);
    }

    private static StructuredResume.Contact mergeContact(
            StructuredResume.Contact base, StructuredResume.Contact opt) {
        if (opt == null) {
            return base;
        }
        if (base == null) {
            return opt;
        }
        return new StructuredResume.Contact(
                pick(opt.fullName(), base.fullName()),
                pick(opt.email(), base.email()),
                pick(opt.phone(), base.phone()),
                pick(opt.location(), base.location()),
                pick(opt.linkedIn(), base.linkedIn()),
                pick(opt.website(), base.website()));
    }

    private static List<StructuredResume.ExperienceEntry> mergeExperience(
            List<StructuredResume.ExperienceEntry> base, List<StructuredResume.ExperienceEntry> optimized) {
        Map<String, StructuredResume.ExperienceEntry> optimizedByKey = new LinkedHashMap<>();
        for (StructuredResume.ExperienceEntry e : optimized) {
            optimizedByKey.put(experienceKey(e), e);
        }
        List<StructuredResume.ExperienceEntry> merged = new ArrayList<>();
        for (StructuredResume.ExperienceEntry b : base) {
            String key = experienceKey(b);
            merged.add(optimizedByKey.getOrDefault(key, b));
            optimizedByKey.remove(key);
        }
        merged.addAll(optimizedByKey.values());
        return merged;
    }

    private static List<StructuredResume.ProjectEntry> mergeProjects(
            List<StructuredResume.ProjectEntry> base, List<StructuredResume.ProjectEntry> optimized) {
        Map<String, StructuredResume.ProjectEntry> optimizedByKey = new LinkedHashMap<>();
        for (StructuredResume.ProjectEntry p : optimized) {
            optimizedByKey.put(projectKey(p), p);
        }
        List<StructuredResume.ProjectEntry> merged = new ArrayList<>();
        for (StructuredResume.ProjectEntry b : base) {
            String key = projectKey(b);
            merged.add(optimizedByKey.getOrDefault(key, b));
            optimizedByKey.remove(key);
        }
        merged.addAll(optimizedByKey.values());
        return merged;
    }

    private static String experienceKey(StructuredResume.ExperienceEntry e) {
        return normalize(e.company()) + "|" + normalize(e.title()) + "|" + normalize(e.startDate());
    }

    private static String projectKey(StructuredResume.ProjectEntry p) {
        return normalize(p.name()) + "|" + normalize(p.timeframe());
    }

    private static String normalize(String s) {
        if ( s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String pick(String preferred, String fallback) {
        return blank(preferred) ? fallback : preferred;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
