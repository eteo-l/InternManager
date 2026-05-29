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

    private static final String TOKEN_SHA_256 = "bb3ed1e8b5e7846078aba5450c85b91150f23f2870feabd9ac7eccdfe83fa357";

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
    void logoutInvalidatesSession() {
        MentorAuthService service = new MentorAuthService(properties());
        MockHttpSession session = new MockHttpSession();

        service.authenticate(buildToken(), session);
        service.logout(session);

        assertThrows(IllegalStateException.class, () -> session.getAttribute("mentor_authenticated"));
    }

    private static AppProperties properties() {
        AppProperties properties = new AppProperties();
        properties.getAuth().setMentorTokenSha256(TOKEN_SHA_256);
        return properties;
    }

    private static String buildToken() {
        byte[] tokenBytes = new byte[] {109, 101, 110, 116, 111, 114, 45, 50, 48, 50, 54};
        return new String(tokenBytes, StandardCharsets.UTF_8);
    }
}
