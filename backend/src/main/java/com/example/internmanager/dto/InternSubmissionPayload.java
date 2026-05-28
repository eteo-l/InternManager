package com.example.internmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record InternSubmissionPayload(
    @NotBlank String name,
    @NotBlank String grade,
    @NotBlank String gender,
    @NotBlank String school,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotBlank String department,
    @NotBlank String campus,
    @NotBlank String mentor,
    String note
) {
}
