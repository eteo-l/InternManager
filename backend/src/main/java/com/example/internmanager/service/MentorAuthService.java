package com.example.internmanager.service;

import com.example.internmanager.exception.ApiException;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MentorAuthService {

    private static final String SESSION_KEY = "mentor_authenticated";
    private static final String MENTOR_TOKEN = "mentor-2026";

    public boolean isAuthenticated(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_KEY));
    }

    public void authenticate(String rawToken, HttpSession session) {
        if (!matchesConfiguredToken(rawToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token 不正确");
        }

        session.setAttribute(SESSION_KEY, Boolean.TRUE);
    }

    public void requireAuthenticated(HttpSession session) {
        if (!isAuthenticated(session)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Mentor 尚未登录");
        }
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    private boolean matchesConfiguredToken(String rawToken) {
        byte[] expected = MENTOR_TOKEN.getBytes(StandardCharsets.UTF_8);
        byte[] actual = normalizeToken(rawToken).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private String normalizeToken(String rawToken) {
        return rawToken == null ? "" : rawToken.trim();
    }
}
