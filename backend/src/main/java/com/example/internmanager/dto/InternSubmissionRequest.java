package com.example.internmanager.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record InternSubmissionRequest(
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请填写正确的实习生手机号") String phone,
    @NotBlank @Pattern(regexp = "^[0-9Xx]{15,18}$", message = "身份证号格式不正确") String idNumber,
    @NotBlank String grade,
    @NotBlank String gender,
    @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请填写正确的紧急联系人手机号") String emergencyPhone,
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
