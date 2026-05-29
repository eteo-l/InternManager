package com.example.internmanager.service;

import com.example.internmanager.exception.ApiException;
import com.example.internmanager.config.AppProperties;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MentorAuthService {

    private static final String SESSION_KEY = "mentor_authenticated";
    private static final int SHA_256_HEX_LENGTH = 64;

    private final byte[] mentorTokenSha256;

    public MentorAuthService(AppProperties appProperties) {
        this.mentorTokenSha256 = parseConfiguredHash(appProperties.getAuth().getMentorTokenSha256());
    }

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
        byte[] actual = sha256(normalizeToken(rawToken));
        return MessageDigest.isEqual(mentorTokenSha256, actual);
    }

    private String normalizeToken(String rawToken) {
        return rawToken == null ? "" : rawToken.trim();
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private byte[] parseConfiguredHash(String value) {
        String normalized = value == null ? "" : value.trim();

        if (normalized.length() != SHA_256_HEX_LENGTH) {
            throw new IllegalStateException("Mentor token SHA-256 is not configured");
        }

        return HexFormat.of().parseHex(normalized);
    }
}
