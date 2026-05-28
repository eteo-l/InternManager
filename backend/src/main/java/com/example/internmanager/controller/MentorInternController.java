package com.example.internmanager.controller;

import com.example.internmanager.dto.InternSubmissionPayload;
import com.example.internmanager.dto.InternSubmissionRequest;
import com.example.internmanager.dto.MessageResponse;
import com.example.internmanager.dto.MentorUpdateRequest;
import com.example.internmanager.model.InternRecord;
import com.example.internmanager.service.InternService;
import com.example.internmanager.service.MentorAuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mentor/interns")
public class MentorInternController {

    private final InternService internService;
    private final MentorAuthService mentorAuthService;

    public MentorInternController(InternService internService, MentorAuthService mentorAuthService) {
        this.internService = internService;
        this.mentorAuthService = mentorAuthService;
    }

    @GetMapping
    public List<InternRecord> list(HttpSession session) {
        mentorAuthService.requireAuthenticated(session);
        return internService.list();
    }

    @PutMapping("/{id}")
    public InternRecord update(@PathVariable String id, @Valid @RequestBody MentorUpdateRequest request, HttpSession session) {
        mentorAuthService.requireAuthenticated(session);
        return internService.update(id, validateRequest(request));
    }

    @PostMapping("/{id}/approve")
    public InternRecord approve(@PathVariable String id, HttpSession session) {
        mentorAuthService.requireAuthenticated(session);
        return internService.approve(id);
    }

    @DeleteMapping("/{id}")
    public MessageResponse reject(@PathVariable String id, HttpSession session) {
        mentorAuthService.requireAuthenticated(session);
        internService.reject(id);
        return new MessageResponse("已打回并移除该实习生记录");
    }

    @DeleteMapping
    public MessageResponse clear(HttpSession session) {
        mentorAuthService.requireAuthenticated(session);
        internService.clear();
        return new MessageResponse("已清空所有实习生记录");
    }

    private MentorUpdateRequest validateRequest(MentorUpdateRequest request) {
        InternSubmissionPayload payload = request.intern();
        InternSubmissionRequest validatedIntern = new InternSubmissionRequest(
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

        internService.validateRequest(validatedIntern);

        return new MentorUpdateRequest(
            request.status(),
            request.accessStatus(),
            request.networkStatus(),
            request.employmentStatus(),
            request.taskTracking(),
            new InternSubmissionPayload(
                validatedIntern.name(),
                validatedIntern.grade(),
                validatedIntern.gender(),
                validatedIntern.school(),
                validatedIntern.startDate(),
                validatedIntern.endDate(),
                validatedIntern.department(),
                validatedIntern.campus(),
                validatedIntern.mentor(),
                validatedIntern.note()
            )
        );
    }
}
