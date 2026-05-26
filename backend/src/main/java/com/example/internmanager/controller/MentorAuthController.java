package com.example.internmanager.controller;

import com.example.internmanager.dto.MentorLoginRequest;
import com.example.internmanager.dto.MentorSessionResponse;
import com.example.internmanager.dto.MessageResponse;
import com.example.internmanager.service.MentorAuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mentor/auth")
public class MentorAuthController {

    private final MentorAuthService mentorAuthService;

    public MentorAuthController(MentorAuthService mentorAuthService) {
        this.mentorAuthService = mentorAuthService;
    }

    @PostMapping("/login")
    public MentorSessionResponse login(@Valid @RequestBody MentorLoginRequest request, HttpSession session) {
        mentorAuthService.authenticate(request.token(), session);
        return new MentorSessionResponse(true);
    }

    @GetMapping("/session")
    public MentorSessionResponse getSession(HttpSession session) {
        return new MentorSessionResponse(mentorAuthService.isAuthenticated(session));
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpSession session) {
        mentorAuthService.logout(session);
        return new MessageResponse("已退出 Mentor 会话");
    }
}
