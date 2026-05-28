package com.example.internmanager.controller;

import com.example.internmanager.dto.InternSubmissionPayload;
import com.example.internmanager.dto.InternSubmissionRequest;
import com.example.internmanager.model.InternRecord;
import com.example.internmanager.service.InternService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interns")
public class PublicInternController {

    private final InternService internService;

    public PublicInternController(InternService internService) {
        this.internService = internService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InternRecord submit(@Valid @RequestBody InternSubmissionPayload payload) {
        return internService.create(toValidatedRequest(payload));
    }

    private InternSubmissionRequest toValidatedRequest(InternSubmissionPayload payload) {
        InternSubmissionRequest request = new InternSubmissionRequest(
            payload.name(),
            payload.grade(),
            payload.gender(),
            payload.school(),
            payload.startDate(),
            payload.endDate(),
            payload.department(),
            payload.campus(),
            payload.mentor(),
            payload.note()
        );

        internService.validateRequest(request);
        return request;
    }
}
