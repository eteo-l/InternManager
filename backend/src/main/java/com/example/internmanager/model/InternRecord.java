package com.example.internmanager.model;

import java.time.Instant;
import java.time.LocalDate;

public record InternRecord(
    String id,
    String name,
    String phone,
    String idNumber,
    String grade,
    String gender,
    String emergencyPhone,
    String school,
    LocalDate startDate,
    LocalDate endDate,
    String department,
    String campus,
    String mentor,
    String note,
    FormStatus status,
    ResourceStatus accessStatus,
    ResourceStatus networkStatus,
    Instant updatedAt
) {
}
