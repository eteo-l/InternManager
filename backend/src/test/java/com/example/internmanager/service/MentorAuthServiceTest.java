package com.example.internmanager.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.internmanager.config.AppProperties;
import com.example.internmanager.exception.ApiException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class MentorAuthServiceTest {

    @Test
    void authenticateAcceptsSha256OfConfiguredToken() {
        MentorAuthService service = new MentorAuthService(properties());
        MockHttpSession session = new MockHttpSession();

        service.authenticate(buildToken(), session);

        assertTrue(service.isAuthenticated(session));
    }

    @Test
    void authenticateRejectsWrongToken() {
        MentorAuthService service = new MentorAuthService(properties());
        MockHttpSession session = new MockHttpSession();

        assertThrows(ApiException.class, () -> service.authenticate("wrong-token", session));
        assertFalse(service.isAuthenticated(session));
    }

    @Test
    void authenticateAcceptsOnlyFirstSixteenCharacters() {
        MentorAuthService service = new MentorAuthService(propertiesForToken("mentor-2026abcde"));
        MockHttpSession session = new MockHttpSession();

        service.authenticate("mentor-2026abcdeXYZ", session);

        assertTrue(service.isAuthenticated(session));
    }

    @Test
    void authenticateRejectsWhenFirstSixteenCharactersDoNotMatch() {
        MentorAuthService service = new MentorAuthService(propertiesForToken("mentor-2026abcde"));
        MockHttpSession session = new MockHttpSession();

        assertThrows(ApiException.class, () -> service.authenticate("mentor-2026abcdEXYZ", session));
        assertFalse(service.isAuthenticated(session));
    }

    @Test
    void logoutInvalidatesSession() {
        MentorAuthService service = new MentorAuthService(properties());
        MockHttpSession session = new MockHttpSession();

        service.authenticate(buildToken(), session);
        service.logout(session);

        assertThrows(IllegalStateException.class, () -> session.getAttribute("mentor_authenticated"));
    }

    private static AppProperties properties() {
        return propertiesForToken(buildToken());
    }

    private static AppProperties propertiesForToken(String token) {
        AppProperties properties = new AppProperties();
        properties.getAuth().setMentorTokenSha256(sha256(token));
        return properties;
    }

    private static String buildToken() {
        byte[] tokenBytes = new byte[] {109, 101, 110, 116, 111, 114, 45, 50, 48, 50, 54};
        return new String(tokenBytes, StandardCharsets.UTF_8);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
