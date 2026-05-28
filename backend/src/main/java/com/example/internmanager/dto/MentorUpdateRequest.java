package com.example.internmanager.dto;

import com.example.internmanager.model.FormStatus;
import com.example.internmanager.model.EmploymentStatus;
import com.example.internmanager.model.ResourceStatus;
import jakarta.validation.constraints.NotNull;

public record MentorUpdateRequest(
    @NotNull FormStatus status,
    @NotNull ResourceStatus accessStatus,
    @NotNull ResourceStatus networkStatus,
    @NotNull EmploymentStatus employmentStatus,
    @NotNull InternSubmissionPayload intern
) {
}
