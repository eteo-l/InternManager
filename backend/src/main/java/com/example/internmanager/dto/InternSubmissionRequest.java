package com.example.internmanager.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record InternSubmissionRequest(
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

    @AssertTrue(message = "实习结束时间不能早于开始时间")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
