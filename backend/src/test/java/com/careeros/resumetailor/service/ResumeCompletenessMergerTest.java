package com.careeros.resumetailor.service;

import com.careeros.resumetailor.model.StructuredResume;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResumeCompletenessMergerTest {

    @Test
    void restoresMissingExperienceFromBase() {
        StructuredResume base = new StructuredResume(
                new StructuredResume.Contact("A", null, null, null, null, null),
                "Summary",
                List.of(),
                List.of(
                        new StructuredResume.ExperienceEntry("Cisco", "Architect", "BLR", "2018", "Present", List.of("a")),
                        new StructuredResume.ExperienceEntry("OldCo", "Dev", "BLR", "2010", "2018", List.of("b"))),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        StructuredResume sparse = new StructuredResume(
                null,
                "",
                List.of(),
                List.of(new StructuredResume.ExperienceEntry("Cisco", "Architect", "BLR", "2018", "Present", List.of("x"))),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        StructuredResume merged = ResumeCompletenessMerger.merge(base, sparse);
        assertEquals(2, merged.experience().size());
        assertEquals("x", merged.experience().get(0).bullets().get(0));
        assertEquals("OldCo", merged.experience().get(1).company());
        assertEquals("A", merged.contact().fullName());
        assertFalse(merged.professionalSummary().isBlank());
    }
}
