package com.example.internmanager.model;

import java.time.Instant;
import java.time.LocalDate;

public record InternRecord(
    String id,
    String name,
    String grade,
    String gender,
    String school,
    LocalDate startDate,
    LocalDate endDate,
    String department,
    String campus,
    EmploymentStatus employmentStatus,
    String taskTracking,
    String mentor,
    String note,
    FormStatus status,
    ResourceStatus accessStatus,
    ResourceStatus networkStatus,
    Instant updatedAt
) {
}
