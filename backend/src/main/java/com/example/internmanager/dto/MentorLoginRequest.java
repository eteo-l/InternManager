package com.example.internmanager.dto;

import jakarta.validation.constraints.NotBlank;

public record MentorLoginRequest(@NotBlank String token) {
}
