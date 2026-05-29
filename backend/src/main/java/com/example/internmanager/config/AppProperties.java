package com.example.internmanager.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Storage storage = new Storage();
    private final Cors cors = new Cors();
    private final Auth auth = new Auth();

    public Cors getCors() {
        return cors;
    }

    public Auth getAuth() {
        return auth;
    }

    public Storage getStorage() {
        return storage;
    }

    public static class Auth {

        private String mentorTokenSha256;

        public String getMentorTokenSha256() {
            return mentorTokenSha256;
        }

        public void setMentorTokenSha256(String mentorTokenSha256) {
            this.mentorTokenSha256 = mentorTokenSha256;
        }
    }

    public static class Cors {

        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Storage {

        private String dbPath;
        private String legacyCsvPath;

        public String getDbPath() {
            return dbPath;
        }

        public void setDbPath(String dbPath) {
            this.dbPath = dbPath;
        }

        public String getLegacyCsvPath() {
            return legacyCsvPath;
        }

        public void setLegacyCsvPath(String legacyCsvPath) {
            this.legacyCsvPath = legacyCsvPath;
        }
    }
}
