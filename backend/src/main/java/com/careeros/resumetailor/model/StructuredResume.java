package com.careeros.resumetailor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StructuredResume(
        Contact contact,
        String professionalSummary,
        List<SkillGroup> skillGroups,
        List<ExperienceEntry> experience,
        List<ProjectEntry> projects,
        List<EducationEntry> education,
        List<CertificationEntry> certifications,
        List<String> otherSections
) {
    public StructuredResume {
        if (skillGroups == null) {
            skillGroups = new ArrayList<>();
        }
        if (experience == null) {
            experience = new ArrayList<>();
        }
        if (projects == null) {
            projects = new ArrayList<>();
        }
        if (education == null) {
            education = new ArrayList<>();
        }
        if (certifications == null) {
            certifications = new ArrayList<>();
        }
        if (otherSections == null) {
            otherSections = new ArrayList<>();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
            String fullName,
            String email,
            String phone,
            String location,
            String linkedIn,
            String website
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillGroup(String category, List<String> skills) {
        public SkillGroup {
            if (skills == null) {
                skills = new ArrayList<>();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExperienceEntry(
            String company,
            String title,
            String location,
            String startDate,
            String endDate,
            List<String> bullets
    ) {
        public ExperienceEntry {
            if (bullets == null) {
                bullets = new ArrayList<>();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectEntry(
            String name,
            String timeframe,
            List<String> technologies,
            List<String> bullets
    ) {
        public ProjectEntry {
            if (technologies == null) {
                technologies = new ArrayList<>();
            }
            if (bullets == null) {
                bullets = new ArrayList<>();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EducationEntry(
            String institution,
            String degree,
            String field,
            String graduationDate,
            String details
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CertificationEntry(String name, String issuer, String date) {}
}
